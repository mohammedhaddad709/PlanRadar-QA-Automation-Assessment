package com.planradar.qa.core.listeners;

/** Final result of one test as reported by {@link TestNGListeners}; {@code classification} is "-" for a pass. */
public record TestOutcome(String testCaseId, String title, String status, String classification, String message) {
}
