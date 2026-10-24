package com.planradar.qa.core.drivers;

import org.openqa.selenium.WebDriver;

/** Implemented by test classes so the reporting listener can take screenshots of the browser under test. */
public interface WebDriverProvider {

    /** @return the browser of the running test, or {@code null} when none is open */
    WebDriver getWebDriver();
}
