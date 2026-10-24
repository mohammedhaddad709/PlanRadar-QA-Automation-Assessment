package com.planradar.qa.systems.planradar;

import com.planradar.qa.core.drivers.DriverFactory;
import com.planradar.qa.core.drivers.WebDriverProvider;
import com.planradar.qa.core.media.ScreenshotsManager;
import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.core.validations.Checkpoints;
import com.planradar.qa.core.validations.SoftAssertions;
import com.planradar.qa.systems.planradar.config.PlanRadarCredentials;
import com.planradar.qa.systems.planradar.fixtures.AssessmentFixture.Status;
import com.planradar.qa.systems.planradar.fixtures.AssessmentFixture;
import com.planradar.qa.systems.planradar.fixtures.Dependency;
import com.planradar.qa.systems.planradar.flows.AuthenticationFlow;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.asserts.Assertion;
import org.testng.asserts.SoftAssert;

/**
 * Every test method runs in its own fresh browser and starts authenticated (DEP-001). Missing credentials or a
 * failed login skip the test as BLOCKED with a screenshot and a reason – never with the password.
 */
public abstract class PlanRadarBaseTest implements WebDriverProvider {

    private WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public final void openBrowserAndLogin() {
        if (!PlanRadarCredentials.isConfigured()) {
            AssessmentFixture.record(Dependency.DEP_001, Status.FAILED, PlanRadarCredentials.EMAIL + " / " + PlanRadarCredentials.PASSWORD + " not set");
            throw new SkipException("BLOCKED by DEP-001: credentials are not configured. Copy "
                    + "config/credentials.example.properties to " + PlanRadarCredentials.DEFAULT_FILE + " and fill it in, "
                    + "or set the same names as environment variables / -D properties.");
        }
        driver = DriverFactory.create();
        AuthenticationFlow authentication = new AuthenticationFlow(driver);
        try {
            authentication.login();
            AssessmentFixture.record(Dependency.DEP_001, Status.PASSED, "Logged in: " + driver.getCurrentUrl());
        } catch (RuntimeException loginFailure) {
            String reason = loginFailure.getMessage() + " | page: " + authentication.failureReason()
                    + " | url: " + driver.getCurrentUrl();
            ScreenshotsManager.capture(driver, "DEP-001_login_failure");
            Allure.addAttachment("Login failure", "text/plain", reason, ".txt");
            LogsManager.error("Login failed: " + reason);
            AssessmentFixture.record(Dependency.DEP_001, Status.FAILED, reason);
            throw new SkipException("BLOCKED by DEP-001 (not authenticated): " + reason, loginFailure);
        }
    }

    @AfterMethod(alwaysRun = true)
    public final void closeBrowser() {
        if (driver == null) return;
        try {
            new AuthenticationFlow(driver).endSession();
            driver.quit();
        } finally {
            driver = null;
        }
    }

    @Override
    public final WebDriver getWebDriver() {
        return driver;
    }

    protected final WebDriver browser() {
        if (driver == null) throw new IllegalStateException("Browser not started");
        return driver;
    }

    /** Setup dependencies, resolved on demand in this test's browser. */
    protected final AssessmentFixture fixture() {
        return new AssessmentFixture(browser());
    }

    /** Hard assertions; each one is shown in the report with its expected and actual value. */
    protected final Assertion verify() {
        return Checkpoints.hard();
    }

    /** Soft assertions of this test; evaluated by the reporting listener right after the test method. */
    protected final SoftAssert softly() {
        return SoftAssertions.current();
    }
}
