package com.planradar.qa.core.utils.report;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.testng.SkipException;

/**
 * First-pass triage of a non-passing result, kept separate from the TestNG status. A failed assertion is a
 * <em>suspected</em> defect until a person confirms it; a missing element is most likely the automation's problem.
 */
public enum FailureClassification {
    /** A setup dependency (DEP-xxx) failed; the test was skipped as BLOCKED. */
    DEPENDENCY_FAILURE,
    /** An assertion on application behaviour failed. */
    SUSPECTED_DEFECT,
    /** Locator / synchronisation problem – check the screenshot first. */
    AUTOMATION_DEFECT,
    /** Browser, driver or network problem. */
    ENVIRONMENT_ERROR,
    /** Missing or invalid configuration (settings, credentials). */
    CONFIGURATION_ERROR;

    public static FailureClassification of(Throwable failure) {
        if (failure == null) return AUTOMATION_DEFECT;
        String message = String.valueOf(failure.getMessage());
        if (failure instanceof SkipException && message.startsWith("BLOCKED")) return DEPENDENCY_FAILURE;
        if (failure instanceof IllegalStateException && message.startsWith("Missing")) return CONFIGURATION_ERROR;
        if (failure instanceof AssertionError) return SUSPECTED_DEFECT;
        if (failure instanceof TimeoutException || failure instanceof NoSuchElementException
                || failure instanceof StaleElementReferenceException) {
            return AUTOMATION_DEFECT;
        }
        if (failure instanceof WebDriverException) return ENVIRONMENT_ERROR;
        return AUTOMATION_DEFECT;
    }
}
