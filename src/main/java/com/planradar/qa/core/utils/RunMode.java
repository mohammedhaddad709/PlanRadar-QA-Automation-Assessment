package com.planradar.qa.core.utils;

import com.planradar.qa.core.utils.dataReader.PropertyReader;

import java.util.Locale;

/**
 * Run profile selected with {@code -Drun.mode=demo|normal|fast} (default {@code normal}, file default in
 * {@code environment.properties}). A mode only supplies defaults: an explicit {@code -Dheadless} / {@code -DexecutionType}
 * still wins, and explicit waits are identical in every mode.
 * <ul>
 *   <li>{@code normal} – visible browser, no artificial delay (the reference configuration);</li>
 *   <li>{@code fast} – headless, no artificial delay;</li>
 *   <li>{@code demo} – visible, maximized browser; every user-visible action is highlighted and followed by a pause
 *       ({@code demo.step.delay.ms}, {@code demo.highlight}) so an audience can follow. The pause is presentation
 *       only and never replaces synchronisation.</li>
 * </ul>
 */
public enum RunMode {
    DEMO, NORMAL, FAST;

    public static RunMode current() {
        String value = PropertyReader.get("run.mode", "normal").toUpperCase(Locale.ROOT);
        try {
            return valueOf(value);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalStateException("Setting 'run.mode' must be demo, normal or fast but was '" + value.toLowerCase(Locale.ROOT) + "'");
        }
    }

    /** Default for the browser when neither {@code -Dheadless} nor {@code -DexecutionType} is given. */
    public boolean headlessByDefault() {
        return this == FAST;
    }

    public boolean maximizeWindow() {
        return this == DEMO;
    }

    /** Pause after each user-visible action; 0 outside demo mode. */
    public long stepDelayMillis() {
        return this == DEMO ? Math.max(0, PropertyReader.requireInt("demo.step.delay.ms")) : 0;
    }

    public boolean highlight() {
        return this == DEMO && Boolean.parseBoolean(PropertyReader.get("demo.highlight", "true"));
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** e.g. {@code demo (step delay 800 ms, highlight on)}. */
    public String describe() {
        return this == DEMO ? key() + " (step delay " + stepDelayMillis() + " ms, highlight " + (highlight() ? "on" : "off") + ")" : key();
    }
}
