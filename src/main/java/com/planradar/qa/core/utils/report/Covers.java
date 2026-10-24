package com.planradar.qa.core.utils.report;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Manual test cases of {@code docs/02_Test_Cases.md} an automated test covers; shown as Allure tags. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Covers {
    String[] value();
}
