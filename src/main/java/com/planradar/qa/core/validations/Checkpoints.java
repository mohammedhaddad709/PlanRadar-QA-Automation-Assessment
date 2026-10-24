package com.planradar.qa.core.validations;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.testng.asserts.Assertion;
import org.testng.asserts.IAssert;

import java.util.List;
import java.util.UUID;

/**
 * Every assertion – hard or soft – becomes an Allure step named after its message, with the expected and the actual
 * value as parameters, so the report shows each verification of a test and not only the first one that failed.
 */
public final class Checkpoints {

    private static final Assertion HARD = new Assertion() {
        @Override
        public void onAssertSuccess(IAssert<?> check) {
            record(check, null);
        }

        @Override
        public void onAssertFailure(IAssert<?> check, AssertionError failure) {
            record(check, failure);
        }
    };

    private Checkpoints() { }

    /** Hard assertions: stop the test on the first failure. */
    public static Assertion hard() {
        return HARD;
    }

    static void record(IAssert<?> check, AssertionError failure) {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        if (lifecycle.getCurrentTestCase().isEmpty()) return;
        StepResult step = new StepResult()
                .setName(check.getMessage() == null ? "Verification" : check.getMessage())
                .setStatus(failure == null ? Status.PASSED : Status.FAILED)
                .setParameters(List.of(parameter("Expected", check.getExpected()), parameter("Actual", check.getActual())));
        if (failure != null) step.setStatusDetails(new StatusDetails().setMessage(failure.getMessage()));
        String uuid = UUID.randomUUID().toString();
        lifecycle.startStep(uuid, step);
        lifecycle.stopStep(uuid);
    }

    private static Parameter parameter(String name, Object value) {
        String text = value == null ? "null" : String.valueOf(value);
        return new Parameter().setName(name).setValue(text.isEmpty() ? "(empty)" : text);
    }
}
