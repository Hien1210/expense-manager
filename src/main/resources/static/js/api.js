/**
 * Shared helper for calling the Expense Manager REST API from static pages.
 *
 * Session: handled entirely by the browser via the HttpOnly JSESSIONID cookie — this file
 * never reads or stores it.
 *
 * CSRF: Spring Security only resolves (and therefore only writes the XSRF-TOKEN cookie for)
 * state-changing requests; a plain page load never sets it. Call API.primeCsrf() once before
 * the first form submit on any page that needs it.
 */
const API = (() => {
    const LOGIN_PAGE = '/login.html';
    const DASHBOARD_PAGE = '/dashboard.html';

    function getCookie(name) {
        const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
        return match ? decodeURIComponent(match[1]) : null;
    }

    function isMutatingMethod(method) {
        return ['POST', 'PUT', 'PATCH', 'DELETE'].indexOf(method) !== -1;
    }

    function primeCsrf() {
        return fetch('/api/v1/auth/csrf', { method: 'GET', credentials: 'same-origin' });
    }

    /**
     * Wraps fetch() with same-origin credentials and the X-XSRF-TOKEN header on mutating
     * requests. By default a 401 response redirects to the login page — pass
     * { redirectOn401: false } for calls where a 401 is an expected outcome to handle inline
     * (e.g. the login form itself, or an auth check on a public page).
     */
    async function apiFetch(url, options) {
        options = options || {};
        const method = (options.method || 'GET').toUpperCase();
        const headers = new Headers(options.headers || {});

        if (isMutatingMethod(method)) {
            const token = getCookie('XSRF-TOKEN');
            if (token) {
                headers.set('X-XSRF-TOKEN', token);
            }
        }
        if (options.body && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }

        const response = await fetch(url, {
            method: method,
            headers: headers,
            body: options.body,
            credentials: 'same-origin'
        });

        const redirectOn401 = options.redirectOn401 !== false;
        if (response.status === 401 && redirectOn401) {
            window.location.href = LOGIN_PAGE;
            return new Promise(() => {
            });
        }

        return response;
    }

    /**
     * For pages that require a logged-in user. Verifies the session server-side via
     * GET /api/v1/auth/me — loading the HTML itself proves nothing, since static pages are
     * publicly accessible. Redirects to the login page on failure.
     */
    async function requireAuth() {
        const response = await apiFetch('/api/v1/auth/me', { redirectOn401: true });
        if (!response.ok) {
            window.location.href = LOGIN_PAGE;
            return null;
        }
        return response.json();
    }

    /**
     * For the login/register pages: if the user already has a valid session, skip straight to
     * the dashboard instead of showing the form again.
     */
    async function redirectIfAuthenticated() {
        const response = await apiFetch('/api/v1/auth/me', { redirectOn401: false });
        if (response.ok) {
            window.location.href = DASHBOARD_PAGE;
        }
    }

    return {
        getCookie: getCookie,
        primeCsrf: primeCsrf,
        apiFetch: apiFetch,
        requireAuth: requireAuth,
        redirectIfAuthenticated: redirectIfAuthenticated,
        LOGIN_PAGE: LOGIN_PAGE,
        DASHBOARD_PAGE: DASHBOARD_PAGE
    };
})();
