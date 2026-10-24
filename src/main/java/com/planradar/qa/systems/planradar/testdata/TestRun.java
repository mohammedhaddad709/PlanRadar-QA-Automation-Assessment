package com.planradar.qa.systems.planradar.testdata;

import com.planradar.qa.systems.planradar.config.PlanRadarConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/** One identifier per JVM run; every artifact the suite creates carries it, so runs never collide. */
public final class TestRun {

    private static final String ID = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private TestRun() { }

    public static String id() {
        return ID;
    }

    /** e.g. {@code QA_Assessment_Form_20261024_040501}. */
    public static String name(String kind) {
        return PlanRadarConfig.artifactPrefix() + "_" + kind + "_" + ID;
    }

    /** Unique ticket title per test case, e.g. {@code QA_Assessment TC-007 20261024_040501-3}. */
    public static String ticketTitle(String testCaseId) {
        return PlanRadarConfig.artifactPrefix() + " " + testCaseId + " " + ID + "-" + SEQUENCE.incrementAndGet();
    }
}
