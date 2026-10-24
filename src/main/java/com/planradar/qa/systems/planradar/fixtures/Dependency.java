package com.planradar.qa.systems.planradar.fixtures;

/** Setup dependencies of the suite, in the order they are resolved. */
public enum Dependency {
    DEP_001("Authentication with the configured credentials"),
    DEP_002("Permission to open Forms and Lists"),
    DEP_003("Global lists for the List fields exist"),
    DEP_004("Form under test exists"),
    DEP_005("Assessment project exists"),
    DEP_006("Form under test is assigned to the project"),
    DEP_007("Form under test is selectable when creating a ticket"),
    DEP_008("Configured ticket Location is selectable");

    private final String requirement;

    Dependency(String requirement) {
        this.requirement = requirement;
    }

    /** e.g. {@code DEP-001} */
    public String id() {
        return name().replace('_', '-');
    }

    public String requirement() {
        return requirement;
    }
}
