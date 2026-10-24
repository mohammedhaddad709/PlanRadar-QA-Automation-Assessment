package com.planradar.qa.systems.planradar.pages;

import com.planradar.qa.systems.planradar.config.PlanRadarConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Optional;

/** PlanRadar two-step login: e-mail → Continue, then password → Login. */
public final class LoginPage extends PlanRadarPage {

    private static final By EMAIL = testId("Login_Input_Email");
    private static final By CONTINUE = testId("Login_Button_Continue");
    private static final By PASSWORD = By.cssSelector("input[type='password']");
    private static final By SUBMIT = By.cssSelector("button[type='submit']");
    private static final By APP_SHELL = testId("account-selector");

    /** Messages PlanRadar shows for wrong credentials (field errors, alerts, toasts). */
    private static final By LOGIN_ERROR = By.cssSelector(
            "[role='alert'], .Toastify__toast--error, [class*='text-fg-error'], [class*='error-message']");
    /** Security challenges the suite must never bypass – reported as authentication blockers. */
    private static final By CAPTCHA = By.cssSelector(
            "iframe[src*='recaptcha'], iframe[src*='hcaptcha'], iframe[title*='challenge' i], [class*='captcha' i]");
    private static final By ONE_TIME_CODE = By.cssSelector(
            "input[autocomplete='one-time-code'], input[name*='otp' i], input[name*='code' i][inputmode='numeric']");

    private static final String APP_URL = ".*/dr/\\d+.*";

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(PlanRadarConfig.loginUrl());
        actions.waitForVisibility(EMAIL);
        return this;
    }

    public LoginPage enterEmail(String email) {
        actions.type(EMAIL, email);
        actions.click(CONTINUE);
        return this;
    }

    public LoginPage enterPassword(String password) {
        actions.waitForVisibility(PASSWORD);
        actions.type(PASSWORD, password);
        return this;
    }

    /** Submits and waits until the app ({@code /dr/<account id>}) is reached; otherwise fails with the visible reason. */
    public void submit() {
        actions.click(SUBMIT);
        try {
            actions.until(d -> d.getCurrentUrl().matches(APP_URL));
        } catch (TimeoutException notInApp) {
            throw new IllegalStateException("Login did not reach the app: " + problem().orElse("no error shown"), notInApp);
        }
    }

    /** Authentication succeeded: app URL and the app shell (account selector in the side navigation) are shown. */
    public boolean isAuthenticated() {
        return driver.getCurrentUrl().matches(APP_URL) && actions.isDisplayed(APP_SHELL, Duration.ofSeconds(15));
    }

    /** Why the login page did not let us in: a security challenge or the visible error text. */
    public Optional<String> problem() {
        if (visible(CAPTCHA)) return Optional.of("AUTHENTICATION BLOCKER: CAPTCHA challenge shown (not bypassed)");
        if (visible(ONE_TIME_CODE)) return Optional.of("AUTHENTICATION BLOCKER: verification code (MFA) requested");
        for (WebElement error : driver.findElements(LOGIN_ERROR)) {
            try {
                if (error.isDisplayed() && !error.getText().isBlank()) {
                    return Optional.of("Login rejected: " + error.getText().trim());
                }
            } catch (RuntimeException reRendered) {
                // element replaced between lookup and read
            }
        }
        return Optional.empty();
    }

    /** Removes cookies and web storage so nothing leaks into the next test's browser. */
    public void clearSession() {
        driver.manage().deleteAllCookies();
        actions.js().executeScript("try { localStorage.clear(); sessionStorage.clear(); } catch (e) {}");
    }

    private boolean visible(By locator) {
        return driver.findElements(locator).stream().anyMatch(e -> {
            try {
                return e.isDisplayed();
            } catch (RuntimeException stale) {
                return false;
            }
        });
    }
}
