package com.example.expense_manager.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for a bug only reproducible on a real embedded server, not MockMvc:
 * when a controller throws ResponseStatusException(409), Spring Boot internally forwards the
 * request to /error to render it. That forwarded request goes back through the Spring Security
 * filter chain as a separate request; if /error itself is not permitAll, the forward gets
 * blocked and the original 409 is silently overwritten with 401. MockMvc does not reproduce
 * this container-level forward, so this had to be caught with a real HTTP client against a real
 * running server (confirmed manually with Spring Security debug logging before this test was
 * written). Uses the JDK's built-in HttpClient instead of TestRestTemplate to avoid adding a new
 * dependency (spring-boot-restclient, needed by TestRestTemplate, is not otherwise on the
 * classpath).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealServerErrorHandlingTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(new java.net.CookieManager())
            .build();

    @Test
    void duplicateEmailOnRegisterReturnsConflictNotUnauthorized() throws Exception {
        String csrfToken = primeCsrfToken();
        String email = "real-server-dup-" + System.nanoTime() + "@example.com";

        HttpResponse<String> first = register(email, csrfToken);
        assertThat(first.statusCode()).isEqualTo(201);

        HttpResponse<String> second = register(email, csrfToken);
        assertThat(second.statusCode())
                .as("a duplicate-email error must reach the client as 409, not be swallowed by a blocked /error forward")
                .isEqualTo(409);
    }

    private String primeCsrfToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/v1/auth/csrf"))
                .GET()
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        Optional<String> setCookie = response.headers().allValues("Set-Cookie").stream()
                .filter(v -> v.startsWith("XSRF-TOKEN="))
                .findFirst();
        assertThat(setCookie).as("XSRF-TOKEN cookie must be set by /api/v1/auth/csrf").isPresent();

        return setCookie.get().substring("XSRF-TOKEN=".length()).split(";", 2)[0];
    }

    private HttpResponse<String> register(String email, String csrfToken) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"password123\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/v1/auth/register"))
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", csrfToken)
                .header("Cookie", "XSRF-TOKEN=" + csrfToken)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
