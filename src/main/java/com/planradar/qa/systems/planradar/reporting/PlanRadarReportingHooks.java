package com.planradar.qa.systems.planradar.reporting;

import com.planradar.qa.core.listeners.SystemReportingHooks;
import com.planradar.qa.core.listeners.TestOutcome;
import com.planradar.qa.systems.planradar.config.PlanRadarConfig;
import com.planradar.qa.systems.planradar.testdata.AssessmentData;
import com.planradar.qa.systems.planradar.testdata.TestRun;

import java.util.LinkedHashMap;
import java.util.Map;

/** PlanRadar content of the shared report: run id, form and project in every report, plus the execution summary. */
public final class PlanRadarReportingHooks implements SystemReportingHooks {

    @Override
    public String system() {
        return PlanRadarConfig.SYSTEM;
    }

    @Override
    public String target() {
        return PlanRadarConfig.baseUrl();
    }

    @Override
    public Map<String, String> runContext() {
        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("Run id", TestRun.id());
        lines.put("Form under test", AssessmentData.FORM.name());
        lines.put("Project", PlanRadarConfig.projectName());
        return lines;
    }

    @Override
    public Map<String, String> environment() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("Application", PlanRadarConfig.baseUrl());
        entries.put("Project", PlanRadarConfig.projectName());
        entries.put("Form.under.test", AssessmentData.FORM.name());
        entries.put("Run.id", TestRun.id());
        entries.put("Scenarios", "1 Form creation and configuration, 4 Mandatory fields");
        return entries;
    }

    @Override
    public void onTestReported(TestOutcome outcome) {
        ExecutionSummary.record(outcome);
    }

    @Override
    public void onExecutionFinish() {
        ExecutionSummary.write();
    }
}
