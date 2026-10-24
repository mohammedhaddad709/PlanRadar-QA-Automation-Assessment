package com.planradar.qa.core.listeners;

import org.testng.ITestResult;

import java.util.Map;

/**
 * System-specific reporting plugged into the shared {@link TestNGListeners} lifecycle. Implementations are discovered
 * with {@link java.util.ServiceLoader} ({@code META-INF/services/com.planradar.qa.core.listeners.SystemReportingHooks}).
 *
 * <p>Execution-level callbacks fire only for the hook of the run's {@code active.system}; test-level callbacks fire
 * only for tests the hook {@linkplain #ownsTest owns} (by default: test classes under
 * {@code com.planradar.qa.systems.<system>}).
 */
public interface SystemReportingHooks {

    String SYSTEMS_PACKAGE = "com.planradar.qa.systems.";

    /** System key, as used by {@code active.system} and {@code systems/<system>.properties}. */
    String system();

    default boolean ownsTest(ITestResult result) {
        return result.getTestClass().getName().startsWith(SYSTEMS_PACKAGE + system() + ".");
    }

    /** Application address, logged when the run starts. */
    default String target() {
        return "";
    }

    default void onExecutionStart() { }

    default void onExecutionFinish() { }

    /** Ordered label → value lines added to the run-context attachment of every test of this system. */
    default Map<String, String> runContext() {
        return Map.of();
    }

    /** Ordered entries written to Allure {@code environment.properties} before the shared browser/platform ones. */
    default Map<String, String> environment() {
        return Map.of();
    }

    /** Called once per reported test of this system (passed, failed, skipped or blocked). */
    default void onTestReported(TestOutcome outcome) { }
}
