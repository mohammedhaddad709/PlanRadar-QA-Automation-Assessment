package com.planradar.qa.systems.planradar.config;

import com.planradar.qa.core.utils.dataReader.SystemProfile;

/**
 * PlanRadar settings, read from its own profile {@code systems/planradar.properties} (independent of
 * {@code active.system}); every key overridable with {@code -D<key>=value}. Credentials are deliberately not here,
 * see {@link PlanRadarCredentials}. Browser and wait settings are shared, see {@code webapp.properties} and
 * {@code waits.properties}.
 */
public final class PlanRadarConfig {

    public static final String SYSTEM = "planradar";
    private static final SystemProfile PROFILE = new SystemProfile(SYSTEM);

    private PlanRadarConfig() { }

    /** Base URL with exactly one trailing slash, e.g. {@code https://www.planradar.com/}. */
    public static String baseUrl() {
        return PROFILE.requireBaseUrl("planradar.baseUrl");
    }

    public static String loginUrl() {
        return SystemProfile.join(baseUrl(), PROFILE.require("planradar.login.path"));
    }

    /** Existing project the form under test is assigned to. */
    public static String projectName() {
        return PROFILE.require("planradar.project.name");
    }

    /** Folder/plan of the project used as ticket Location; "" when the project does not need one. */
    public static String ticketLocation() {
        return PROFILE.get("planradar.ticket.location");
    }

    /** Prefix of every artifact the suite creates (forms, tickets). */
    public static String artifactPrefix() {
        return PROFILE.require("planradar.artifact.prefix");
    }
}
