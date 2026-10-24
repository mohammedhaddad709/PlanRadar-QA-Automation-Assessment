package com.planradar.qa.core.utils;

import com.planradar.qa.core.utils.dataReader.PropertyReader;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;

/**
 * Timeouts from {@code waits.properties} and the polling wait used by every element action: 100 ms polling that
 * retries on the transient exceptions a React UI produces while re-rendering. No fixed sleeps anywhere.
 */
public final class WaitManager {

    private static final Duration POLL = Duration.ofMillis(100);
    private static final List<Class<? extends Throwable>> TRANSIENT = List.of(
            NoSuchElementException.class, StaleElementReferenceException.class,
            ElementNotInteractableException.class, ElementClickInterceptedException.class);

    private WaitManager() { }

    public static Duration explicitTimeout() {
        return Duration.ofSeconds(PropertyReader.requireInt("wait.explicit.seconds"));
    }

    public static Duration pageLoadTimeout() {
        return Duration.ofSeconds(PropertyReader.requireInt("wait.pageLoad.seconds"));
    }

    public static FluentWait<WebDriver> fluentWait(WebDriver driver, Duration within) {
        return new FluentWait<>(driver).withTimeout(within).pollingEvery(POLL).ignoreAll(TRANSIENT);
    }
}
