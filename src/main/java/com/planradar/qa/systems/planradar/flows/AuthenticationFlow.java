package com.planradar.qa.systems.planradar.flows;

import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.systems.planradar.config.PlanRadarCredentials;
import com.planradar.qa.systems.planradar.pages.LoginPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** Business-level authentication: one verified login before every test, session cleanup after it. */
public final class AuthenticationFlow {

    private final WebDriver driver;

    public AuthenticationFlow(WebDriver driver) {
        this.driver = driver;
    }

    /** Opens the login page, enters the configured credentials and verifies the authenticated app shell. */
    @Step("Log in to PlanRadar as the administrator")
    public void login() {
        PlanRadarCredentials credentials = PlanRadarCredentials.require();
        LoginPage login = new LoginPage(driver).open()
                .enterEmail(credentials.email())
                .enterPassword(credentials.password());
        login.submit();
        if (!login.isAuthenticated()) {
            throw new IllegalStateException("Login reached " + driver.getCurrentUrl()
                    + " but the authenticated app shell is not shown");
        }
    }

    /** Why the login page did not let us in (security challenge or visible error), for failure reports. */
    public String failureReason() {
        return new LoginPage(driver).problem().orElse("no error shown on the page");
    }

    /** Cleanup after a test: end the session (cookies + storage). Never fails the test. */
    @Step("End the PlanRadar session")
    public void endSession() {
        try {
            new LoginPage(driver).clearSession();
        } catch (RuntimeException cleanupProblem) {
            LogsManager.warn("Session cleanup skipped: " + cleanupProblem.getClass().getSimpleName());
        }
    }
}
