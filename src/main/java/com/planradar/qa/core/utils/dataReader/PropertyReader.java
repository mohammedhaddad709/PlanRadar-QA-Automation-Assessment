package com.planradar.qa.core.utils.dataReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * Shared framework configuration: {@code environment.properties}, {@code webapp.properties}, {@code waits.properties}
 * plus the selected system profile ({@code systems/<active.system>.properties}). JVM {@code -D} properties always
 * override file defaults. System-specific code reads its own profile through {@link SystemProfile} instead, so it
 * does not depend on which system is active.
 */
public final class PropertyReader {

    private static final List<String> BASE_RESOURCES = List.of("environment.properties", "webapp.properties", "waits.properties");
    private static final Properties DEFAULTS = loadDefaults();

    private PropertyReader() { }

    /** Value or empty string when neither a JVM property nor a file default exists. */
    public static String get(String key) {
        String fromJvm = System.getProperty(key);
        return (fromJvm != null ? fromJvm : DEFAULTS.getProperty(key, "")).trim();
    }

    public static String get(String key, String fallback) {
        String value = get(key);
        return value.isEmpty() ? fallback : value;
    }

    public static String require(String key) {
        String value = get(key);
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing setting '" + key + "': add it to " + BASE_RESOURCES + " or pass -D" + key);
        }
        return value;
    }

    public static int requireInt(String key) {
        String value = require(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Setting '" + key + "' must be an integer but was '" + value + "'", ex);
        }
    }

    /** Selected system for this JVM: {@code -Dactive.system}, else {@code environment.properties}. */
    public static String activeSystem() {
        String fromJvm = System.getProperty("active.system");
        if (fromJvm != null && !fromJvm.isBlank()) return fromJvm.trim();
        Properties environment = new Properties();
        load("environment.properties", environment);
        return environment.getProperty("active.system", "").trim();
    }

    private static Properties loadDefaults() {
        Properties properties = new Properties();
        BASE_RESOURCES.forEach(resource -> load(resource, properties));
        String system = activeSystem();
        if (!system.isEmpty()) load("systems/" + system + ".properties", properties);
        return properties;
    }

    static void load(String resource, Properties target) {
        try (InputStream stream = PropertyReader.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream != null) target.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + resource, ex);
        }
    }
}
