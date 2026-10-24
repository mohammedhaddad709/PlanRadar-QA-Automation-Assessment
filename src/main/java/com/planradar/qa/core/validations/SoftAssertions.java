package com.planradar.qa.core.validations;

import org.testng.ITestResult;
import org.testng.asserts.IAssert;
import org.testng.asserts.SoftAssert;

/**
 * Soft assertions scoped to the running test (one {@link SoftAssert} per thread). The reporting listener calls
 * {@link #assertAll(ITestResult)} right after the test method, so collected failures fail the test itself – not a
 * TestNG configuration method – and the final screenshot still shows the state the assertions were made on.
 * Each check is also recorded as an Allure step ({@link Checkpoints}).
 */
public final class SoftAssertions {

    private static final ThreadLocal<SoftAssert> CURRENT = ThreadLocal.withInitial(ReportedSoftAssert::new);

    private SoftAssertions() { }

    public static SoftAssert current() {
        return CURRENT.get();
    }

    public static void assertAll(ITestResult result) {
        try {
            CURRENT.get().assertAll();
        } catch (AssertionError collected) {
            if (result.getThrowable() != null) collected.addSuppressed(result.getThrowable());
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(collected);
        } finally {
            CURRENT.remove();
        }
    }

    private static final class ReportedSoftAssert extends SoftAssert {
        @Override
        public void onAssertSuccess(IAssert<?> check) {
            Checkpoints.record(check, null);
        }

        @Override
        public void onAssertFailure(IAssert<?> check, AssertionError failure) {
            Checkpoints.record(check, failure);
        }
    }
}
