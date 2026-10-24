package com.planradar.qa.systems.planradar.config;

import com.planradar.qa.core.utils.dataReader.JsonReader;
import com.planradar.qa.core.utils.dataReader.LocalCredentials;

/**
 * The administrator account the suite logs in with ({@code test-data/planradar/login-data.json}, role {@code admin}).
 * Resolution order for e-mail and password:
 * <ol>
 *   <li>per-run override {@code -Ddata.planradar.login-data.admin.email} / {@code .password};</li>
 *   <li>{@code PLANRADAR_EMAIL} / {@code PLANRADAR_PASSWORD} as JVM {@code -D} property, environment variable or
 *       entry of the git-ignored {@code config/credentials.local.properties} ({@link LocalCredentials}).</li>
 * </ol>
 * The checked-in login-data file holds no secrets. Values are never logged and never appear in reports.
 */
public final class PlanRadarCredentials {

    public static final String EMAIL = "PLANRADAR_EMAIL";
    public static final String PASSWORD = "PLANRADAR_PASSWORD";
    public static final String DEFAULT_FILE = LocalCredentials.DEFAULT_FILE;

    private static final String ROLE = "admin";

    private final String email;
    private final String password;

    private PlanRadarCredentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static boolean isConfigured() {
        JsonReader loginData = loginData();
        return LocalCredentials.usable(resolve(loginData, "email", EMAIL))
                && LocalCredentials.usable(resolve(loginData, "password", PASSWORD));
    }

    public static PlanRadarCredentials require() {
        JsonReader loginData = loginData();
        String email = resolve(loginData, "email", EMAIL);
        String password = resolve(loginData, "password", PASSWORD);
        if (!LocalCredentials.usable(email) || !LocalCredentials.usable(password)) {
            throw new IllegalStateException("Missing credentials " + EMAIL + " / " + PASSWORD + ": pass them as -D, "
                    + "as environment variables, or copy config/credentials.example.properties to " + DEFAULT_FILE);
        }
        return new PlanRadarCredentials(email.trim(), password.trim());
    }

    public String email() {
        return email;
    }

    public String password() {
        return password;
    }

    @Override
    public String toString() {
        return "Credentials[REDACTED]";
    }

    private static JsonReader loginData() {
        return new JsonReader(PlanRadarConfig.SYSTEM, "login-data");
    }

    private static String resolve(JsonReader loginData, String field, String name) {
        String override = loginData.override(ROLE + "." + field);
        if (LocalCredentials.usable(override)) return override;
        String value = LocalCredentials.resolve(name);
        return value != null ? value : loginData.get(ROLE + "." + field);
    }
}
