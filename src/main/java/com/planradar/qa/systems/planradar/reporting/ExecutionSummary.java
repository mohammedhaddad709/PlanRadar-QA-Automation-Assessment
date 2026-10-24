package com.planradar.qa.systems.planradar.reporting;

import com.planradar.qa.core.drivers.DriverFactory;
import com.planradar.qa.core.listeners.TestOutcome;
import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.core.utils.OutputPaths;
import com.planradar.qa.core.utils.RunMode;
import com.planradar.qa.systems.planradar.config.PlanRadarConfig;
import com.planradar.qa.systems.planradar.fixtures.AssessmentFixture;
import com.planradar.qa.systems.planradar.fixtures.Dependency;
import com.planradar.qa.systems.planradar.testdata.AssessmentData;
import com.planradar.qa.systems.planradar.testdata.TestRun;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Human-readable result of one run: dependencies, every test case with status and triage classification. */
final class ExecutionSummary {

    private static final List<String> ROWS = Collections.synchronizedList(new ArrayList<>());

    private ExecutionSummary() { }

    static void record(TestOutcome outcome) {
        ROWS.add("| " + outcome.testCaseId() + " | " + outcome.title() + " | " + outcome.status() + " | "
                + outcome.classification() + " | " + oneLine(outcome.message()) + " |");
    }

    static void write() {
        Path file = OutputPaths.executionSummary();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, render(), StandardCharsets.UTF_8);
            LogsManager.info("Execution summary written to " + file);
        } catch (IOException ex) {
            LogsManager.warn("Execution summary not written: " + ex.getMessage());
        }
    }

    private static String render() {
        StringBuilder md = new StringBuilder("# PlanRadar Form Builder – execution summary\n\n");
        md.append("| Item | Value |\n|---|---|\n")
                .append("| Run id | ").append(TestRun.id()).append(" |\n")
                .append("| Finished | ").append(LocalDateTime.now().withNano(0)).append(" |\n")
                .append("| Base URL | ").append(PlanRadarConfig.baseUrl()).append(" |\n")
                .append("| Project | ").append(PlanRadarConfig.projectName()).append(" |\n")
                .append("| Form created by this run | ").append(AssessmentData.FORM.name()).append(" |\n")
                .append("| Run mode | ").append(RunMode.current().describe()).append(" |\n")
                .append("| Browser | ").append(DriverFactory.describe()).append(" |\n\n");

        md.append("## Dependencies\n\n| ID | Requirement | Status | Detail |\n|---|---|---|---|\n");
        for (Map.Entry<Dependency, AssessmentFixture.State> entry : AssessmentFixture.states().entrySet()) {
            md.append("| ").append(entry.getKey().id()).append(" | ").append(entry.getKey().requirement())
                    .append(" | ").append(entry.getValue().status()).append(" | ")
                    .append(oneLine(entry.getValue().detail())).append(" |\n");
        }

        md.append("\n## Test cases\n\n| ID | Title | Status | Classification | Message |\n|---|---|---|---|---|\n");
        synchronized (ROWS) {
            ROWS.stream().sorted().forEach(row -> md.append(row).append('\n'));
        }
        return md.toString();
    }

    private static String oneLine(String text) {
        if (text == null) return "";
        String line = text.replace('\n', ' ').replace('|', '/').trim();
        return line.length() > 220 ? line.substring(0, 220) + "…" : line;
    }
}
