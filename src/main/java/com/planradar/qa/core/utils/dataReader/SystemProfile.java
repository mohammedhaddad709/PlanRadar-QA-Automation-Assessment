package com.planradar.qa.core.utils.dataReader;

import java.util.Properties;

/**
 * Namespaced configuration of one system, read directly from {@code systems/<system>.properties} so it works
 * regardless of {@code active.system}. Precedence: JVM {@code -D} property, then file default.
 */
public final class SystemProfile {

    private final String system;
    private final Properties defaults = new Properties();

    public SystemProfile(String system) {
        this.system = system;
        PropertyReader.load(resource(), defaults);
        if (defaults.isEmpty()) throw new IllegalStateException(resource() + " not found on the classpath");
    }

    public String system() {
        return system;
    }

    /** Value or empty string when neither a JVM property nor a file default exists. */
    public String get(String key) {
        String fromJvm = System.getProperty(key);
        return (fromJvm != null ? fromJvm : defaults.getProperty(key, "")).trim();
    }

    public String require(String key) {
        String value = get(key);
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing setting '" + key + "' for system '" + system
                    + "': add it to " + resource() + " or pass -D" + key);
        }
        return value;
    }

    /** Absolute URL with exactly one trailing slash, so callers can append relative paths. */
    public String requireBaseUrl(String key) {
        return require(key).replaceAll("/+$", "") + "/";
    }

    /** Joins a base URL and a path without doubling or dropping slashes. */
    public static String join(String baseUrl, String path) {
        return baseUrl.replaceAll("/+$", "") + "/" + (path == null ? "" : path.replaceAll("^/+", ""));
    }

    private String resource() {
        return "systems/" + system + ".properties";
    }
}
