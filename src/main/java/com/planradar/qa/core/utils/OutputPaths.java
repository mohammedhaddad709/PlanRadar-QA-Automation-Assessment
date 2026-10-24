package com.planradar.qa.core.utils;

import java.nio.file.Path;

/**
 * Single source of truth for every artifact location written during a run.
 *
 * <p>{@code -Doutput.dir} sets the run root (default {@code target}); the embedded runner passes
 * {@code runner/runs/<id>/output} so concurrent runs never share screenshots, logs or Allure results.
 * {@code -Dallure.results.directory} can be set independently; when absent it derives from the run root.
 * The log file location is resolved by {@code log4j2.properties} from the same {@code output.dir} property.
 */
public final class OutputPaths {

    private OutputPaths() { }

    public static Path root() {
        return Path.of(System.getProperty("output.dir", "target")).toAbsolutePath().normalize();
    }

    public static Path allureResults() {
        String explicit = System.getProperty("allure.results.directory");
        return explicit == null || explicit.isBlank()
                ? root().resolve("allure-results")
                : Path.of(explicit).toAbsolutePath().normalize();
    }

    public static Path screenshots() {
        return root().resolve("screenshots");
    }

    public static Path executionSummary() {
        return root().resolve("execution-summary.md");
    }
}
