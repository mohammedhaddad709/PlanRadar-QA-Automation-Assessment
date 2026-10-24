package com.planradar.qa.core.utils.report;

import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.core.utils.OutputPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Files Allure reads from the results directory besides the test results: {@code categories.json} (triage
 * classification, see {@link FailureClassification}) and {@code environment.properties}.
 */
public final class AllureEnvironmentManager {

    /** First matching category wins; "failed" = assertion error, "broken" = any other exception (Allure semantics). */
    private static final String CATEGORIES = """
            [
              {"name": "Blocked - a setup dependency failed (DEPENDENCY_FAILURE)",
               "matchedStatuses": ["skipped"], "messageRegex": "(?s)BLOCKED.*"},
              {"name": "Suspected product defect - verification failed (SUSPECTED_DEFECT)",
               "matchedStatuses": ["failed"]},
              {"name": "Configuration error - missing setting or credentials (CONFIGURATION_ERROR)",
               "matchedStatuses": ["broken"], "messageRegex": "(?s)Missing.*"},
              {"name": "Automation defect - locator or timing (AUTOMATION_DEFECT)",
               "matchedStatuses": ["broken"], "traceRegex": "(?s).*(TimeoutException|NoSuchElementException|StaleElementReferenceException).*"},
              {"name": "Environment error - browser, network or application (ENVIRONMENT_ERROR)",
               "matchedStatuses": ["broken"]}
            ]
            """;

    private AllureEnvironmentManager() { }

    public static void writeCategories() {
        write("categories.json", CATEGORIES);
    }

    /** @param entries ordered key → value pairs; keys must not contain spaces or '=' */
    public static void writeEnvironment(Map<String, String> entries) {
        write("environment.properties", entries.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n")) + "\n");
    }

    private static void write(String fileName, String content) {
        try {
            Files.writeString(Files.createDirectories(OutputPaths.allureResults()).resolve(fileName), content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LogsManager.warn("Allure " + fileName + " not written: " + ex.getMessage());
        }
    }
}
