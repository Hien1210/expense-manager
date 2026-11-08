(function () {
    let pendingVerificationEmail = '';
    let resendCountdownTimer = null;

    function showAlert(el, message, type) {
        el.textContent = message;
        el.className = 'alert show ' + (type === 'success' ? 'alert-success' : 'alert-error');
    }

    function hideAlert(el) {
        el.className = 'alert';
        el.textContent = '';
    }

    function setFieldError(fieldEl, message) {
        const errorEl = fieldEl.querySelector('.field-error');
        if (message) {
            fieldEl.classList.add('has-error');
            if (errorEl) {
                errorEl.textContent = message;
            }
        } else {
            fieldEl.classList.remove('has-error');
            if (errorEl) {
                errorEl.textContent = '';
            }
        }
    }

    async function readErrorMessage(response, fallback) {
        try {
            const body = await response.json();
            if (body && typeof body.message === 'string' && body.message) {
                return body.message;
            }
        } catch (e) {
            // Response had no JSON body — fall back to the generic message below.
        }
        return fallback;
    }

    function initLoginForm() {
        const form = document.getElementById('login-form');
        if (!form) {
            return;
        }

        const alertEl = document.getElementById('login-alert');
        const submitBtn = form.querySelector('button[type="submit"]');

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            hideAlert(alertEl);
            submitBtn.disabled = true;

            const email = form.email.value.trim();
            const password = form.password.value;

            try {
                const response = await API.apiFetch('/api/v1/auth/login', {
                    method: 'POST',
                    body: JSON.stringify({ email: email, password: password }),
                    redirectOn401: false
                });

                if (response.ok) {
                    // Refresh the CSRF cookie right after establishing the session, so the
                    // dashboard the user lands on already has a token ready for its own
                    // mutating requests (e.g. logout) without relying on the pre-login one.
                    await API.primeCsrf();
                    window.location.href = API.DASHBOARD_PAGE;
                    return;
                }

                if (response.status === 401) {
                    showAlert(alertEl, 'Email hoặc mật khẩu không đúng, hoặc tài khoản chưa kích hoạt OTP.', 'error');
                } else if (response.status === 403) {
                    showAlert(alertEl, 'Phiên làm việc đã hết hạn. Vui lòng tải lại trang và thử lại.', 'error');
                } else if (response.status === 400) {
                    showAlert(alertEl, await readErrorMessage(response, 'Dữ liệu đăng nhập không hợp lệ.'), 'error');
                } else {
                    showAlert(alertEl, 'Đã có lỗi xảy ra, vui lòng thử lại sau.', 'error');
                }
            } catch (e) {
                showAlert(alertEl, 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng.', 'error');
            } finally {
                submitBtn.disabled = false;
            }
        });
    }

    function initRegisterForm() {
        const form = document.getElementById('register-form');
        if (!form) {
            return;
        }

        const alertEl = document.getElementById('register-alert');
        const emailField = document.getElementById('email-field');
        const passwordField = document.getElementById('password-field');
        const submitBtn = form.querySelector('button[type="submit"]');

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            hideAlert(alertEl);
            setFieldError(emailField, null);
            setFieldError(passwordField, null);
            submitBtn.disabled = true;

            const email = form.email.value.trim();
            const password = form.password.value;

            try {
                const response = await API.apiFetch('/api/v1/auth/register', {
                    method: 'POST',
                    body: JSON.stringify({ email: email, password: password }),
                    redirectOn401: false
                });

                if (response.status === 201) {
                    pendingVerificationEmail = email;
                    form.style.display = 'none';

                    const otpForm = document.getElementById('otp-form');
                    if (otpForm) {
                        otpForm.style.display = 'block';
                        const titleEl = document.getElementById('auth-title');
                        const subtitleEl = document.getElementById('auth-subtitle');
                        const targetEmailEl = document.getElementById('otp-email-target');
                        if (titleEl) titleEl.textContent = 'Xác thực tài khoản';
                        if (subtitleEl) subtitleEl.textContent = 'Nhập mã 6 chữ số đã gửi tới Gmail';
                        if (targetEmailEl) targetEmailEl.textContent = email;

                        const otpInput = document.getElementById('otp');
                        if (otpInput) otpInput.focus();

                        startResendCountdown();
                    }
                    return;
                }

                if (response.status === 409) {
                    setFieldError(emailField, 'Email này đã được sử dụng.');
                } else if (response.status === 400) {
                    showAlert(alertEl, await readErrorMessage(response, 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.'), 'error');
                } else if (response.status === 403) {
                    showAlert(alertEl, 'Phiên làm việc đã hết hạn. Vui lòng tải lại trang và thử lại.', 'error');
                } else {
                    showAlert(alertEl, 'Đã có lỗi xảy ra, vui lòng thử lại sau.', 'error');
                }
            } catch (e) {
                showAlert(alertEl, 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng.', 'error');
            } finally {
                submitBtn.disabled = false;
            }
        });
    }

    function initOtpForm() {
        const form = document.getElementById('otp-form');
        if (!form) return;

        const alertEl = document.getElementById('register-alert');
        const otpField = document.getElementById('otp-field');
        const submitBtn = form.querySelector('button[type="submit"]');
        const resendBtn = document.getElementById('resend-otp-btn');

        form.addEventListener('submit', async function (event) {
            event.preventDefault();
            hideAlert(alertEl);
            setFieldError(otpField, null);
            submitBtn.disabled = true;

            const otp = form.otp.value.trim();
            if (!/^\d{6}$/.test(otp)) {
                setFieldError(otpField, 'Mã OTP phải gồm đúng 6 chữ số.');
                submitBtn.disabled = false;
                return;
            }

            try {
                const response = await API.apiFetch('/api/v1/auth/verify-otp', {
                    method: 'POST',
                    body: JSON.stringify({ email: pendingVerificationEmail, otp: otp }),
                    redirectOn401: false
                });

                if (response.ok) {
                    window.location.href = '/login.html?registered=1';
                    return;
                }

                if (response.status === 400) {
                    setFieldError(otpField, await readErrorMessage(response, 'Mã OTP không chính xác hoặc đã hết hạn.'));
                } else {
                    showAlert(alertEl, 'Đã có lỗi xảy ra khi xác thực OTP.', 'error');
                }
            } catch (e) {
                showAlert(alertEl, 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng.', 'error');
            } finally {
                submitBtn.disabled = false;
            }
        });

        if (resendBtn) {
            resendBtn.addEventListener('click', async function () {
                hideAlert(alertEl);
                setFieldError(otpField, null);
                resendBtn.disabled = true;

                try {
                    const response = await API.apiFetch('/api/v1/auth/resend-otp', {
                        method: 'POST',
                        body: JSON.stringify({ email: pendingVerificationEmail }),
                        redirectOn401: false
                    });

                    if (response.ok) {
                        showAlert(alertEl, 'Đã gửi lại mã OTP mới đến hộp thư của bạn.', 'success');
                        startResendCountdown();
                    } else {
                        showAlert(alertEl, await readErrorMessage(response, 'Không thể gửi lại mã OTP lúc này.'), 'error');
                        resendBtn.disabled = false;
                    }
                } catch (e) {
                    showAlert(alertEl, 'Không thể kết nối tới máy chủ. Vui lòng thử lại sau.', 'error');
                    resendBtn.disabled = false;
                }
            });
        }
    }

    function startResendCountdown() {
        const resendBtn = document.getElementById('resend-otp-btn');
        const timerEl = document.getElementById('countdown-timer');
        if (!resendBtn || !timerEl) return;

        if (resendCountdownTimer) {
            clearInterval(resendCountdownTimer);
        }

        let secondsLeft = 60;
        resendBtn.disabled = true;
        resendBtn.style.opacity = '0.5';
        resendBtn.style.pointerEvents = 'none';
        timerEl.textContent = `(gửi lại sau ${secondsLeft}s)`;

        resendCountdownTimer = setInterval(function () {
            secondsLeft--;
            if (secondsLeft <= 0) {
                clearInterval(resendCountdownTimer);
                timerEl.textContent = '';
                resendBtn.disabled = false;
                resendBtn.style.opacity = '1';
                resendBtn.style.pointerEvents = 'auto';
            } else {
                timerEl.textContent = `(gửi lại sau ${secondsLeft}s)`;
            }
        }, 1000);
    }

    function showRegisteredBanner() {
        const params = new URLSearchParams(window.location.search);
        if (params.get('registered') === '1') {
            const alertEl = document.getElementById('login-alert');
            if (alertEl) {
                showAlert(alertEl, 'Đăng ký và xác thực tài khoản thành công! Vui lòng đăng nhập.', 'success');
            }
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        // Loading the HTML proves nothing about auth state — check the real session,
        // and separately make sure a fresh CSRF cookie exists before any submit.
        API.redirectIfAuthenticated();
        API.primeCsrf();

        showRegisteredBanner();
        initLoginForm();
        initRegisterForm();
        initOtpForm();
    });
})();
