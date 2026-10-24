package com.planradar.qa.systems.planradar.fixtures;

import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.systems.planradar.config.PlanRadarConfig;
import com.planradar.qa.systems.planradar.flows.FormSetupFlow;
import com.planradar.qa.systems.planradar.flows.TicketFlow;
import com.planradar.qa.systems.planradar.pages.FormsListPage;
import com.planradar.qa.systems.planradar.pages.ProjectPage;
import com.planradar.qa.systems.planradar.pages.TicketDialog;
import com.planradar.qa.systems.planradar.pages.TicketsPage;
import com.planradar.qa.systems.planradar.testdata.AssessmentData;
import com.planradar.qa.systems.planradar.testdata.FormDefinition;
import com.planradar.qa.systems.planradar.testdata.ListDefinition;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves the setup dependencies once per run and shares the result between tests.
 *
 * <p>Tests stay independent: each one asks for what it needs in its own fresh, logged-in browser, and the first
 * test that needs a dependency resolves it. A dependency that failed is not retried; every test that needs it is
 * skipped as <b>BLOCKED</b> (a setup problem, never reported as a product defect).
 */
public final class AssessmentFixture {

    public enum Status { NOT_RUN, PASSED, FAILED }

    public record State(Status status, String detail) { }

    private static final Map<Dependency, State> STATES = Collections.synchronizedMap(new EnumMap<>(Dependency.class));
    private static volatile String projectId;

    static {
        for (Dependency dependency : Dependency.values()) STATES.put(dependency, new State(Status.NOT_RUN, ""));
    }

    private final WebDriver driver;
    private final FormDefinition form = AssessmentData.FORM;

    public AssessmentFixture(WebDriver driver) {
        this.driver = driver;
    }

    /** Snapshot in dependency order (DEP-001 first). */
    public static Map<Dependency, State> states() {
        synchronized (STATES) {
            return Collections.unmodifiableMap(new EnumMap<>(STATES));
        }
    }

    public static void record(Dependency dependency, Status status, String detail) {
        STATES.put(dependency, new State(status, detail));
        LogsManager.info(dependency.id() + " " + status + " - " + detail);
    }

    /** For tests that perform a setup step as one of their own steps (TC-001 builds the form, TC-004 assigns it). */
    public static void markResolved(Dependency dependency, String detail) {
        record(dependency, Status.PASSED, detail);
    }

    public static void rememberProjectId(String id) {
        projectId = id;
    }

    public FormDefinition form() {
        return form;
    }

    public TicketFlow tickets() {
        return new TicketFlow(driver, projectId, form);
    }

    // ---------- what a test can require ----------

    /** Permission to use Forms and Lists + the global lists the form needs. */
    public synchronized AssessmentFixture requireListsReady() {
        resolve(Dependency.DEP_002, this::checkPermission);
        resolve(Dependency.DEP_003, this::ensureLists);
        return this;
    }

    /** ...plus the form under test. */
    public synchronized AssessmentFixture requireFormReady() {
        requireListsReady();
        resolve(Dependency.DEP_004, this::ensureForm);
        return this;
    }

    /** ...plus the project and the assignment. */
    public synchronized AssessmentFixture requireFormAssigned() {
        requireFormReady();
        resolve(Dependency.DEP_005, this::openProject);
        resolve(Dependency.DEP_006, this::ensureAssigned);
        return this;
    }

    /** Everything a ticket test needs: the form can be selected and the Location can be set. */
    public synchronized AssessmentFixture requireTicketReady() {
        requireFormAssigned();
        if (projectId == null) openProject();   // the dependency passed earlier but this JVM state lost the id
        resolve(Dependency.DEP_007, this::checkFormSelectable);
        resolve(Dependency.DEP_008, this::checkLocationSelectable);
        return this;
    }

    // ---------- resolution ----------

    private void resolve(Dependency dependency, Step step) {
        State state = STATES.get(dependency);
        if (state.status() == Status.PASSED) return;
        if (state.status() == Status.FAILED) throw blocked(dependency, state.detail());
        try {
            String detail = Allure.step(dependency.id() + " - " + dependency.requirement(), step::run);
            record(dependency, Status.PASSED, detail);
        } catch (RuntimeException | AssertionError failure) {
            String detail = failure.getClass().getSimpleName() + ": " + failure.getMessage();
            record(dependency, Status.FAILED, detail);
            throw blocked(dependency, detail);
        }
    }

    private static SkipException blocked(Dependency dependency, String detail) {
        return new SkipException("BLOCKED by " + dependency.id() + " (" + dependency.requirement() + "): " + detail);
    }

    @FunctionalInterface
    private interface Step {
        String run();
    }

    // ---------- steps ----------

    private String checkPermission() {
        new FormsListPage(driver).open();   // fails when the user has no Forms and Lists permission
        return "Forms and Lists page opened";
    }

    private String ensureLists() {
        FormSetupFlow setup = new FormSetupFlow(driver);
        StringBuilder detail = new StringBuilder();
        for (ListDefinition list : form.lists()) {
            detail.append(list.name()).append(setup.ensureList(list) ? " created; " : " already existed; ");
        }
        return detail.toString().trim();
    }

    private String ensureForm() {
        FormSetupFlow setup = new FormSetupFlow(driver);
        if (setup.formExists(form.name())) return form.name() + " already exists";
        setup.createForm(form);
        if (!setup.formExists(form.name())) throw new IllegalStateException("Form saved but not listed: " + form.name());
        return form.name() + " created";
    }

    private String openProject() {
        ProjectPage project = new FormSetupFlow(driver).openProjectForms(PlanRadarConfig.projectName());
        rememberProjectId(project.projectId());
        return PlanRadarConfig.projectName() + " (id " + projectId + ")";
    }

    private String ensureAssigned() {
        ProjectPage project = new FormSetupFlow(driver).assignToProject(PlanRadarConfig.projectName(), form.name());
        if (!project.openFormsTab().isFormAssigned(form.name())) {
            throw new IllegalStateException("Form not shown under the project's applied forms after Apply");
        }
        return form.name() + " assigned to " + PlanRadarConfig.projectName();
    }

    private String checkFormSelectable() {
        new TicketsPage(driver).open(projectId).createTicket(form.name()).discard();
        return form.name() + " selectable in the new-ticket dialog";
    }

    private String checkLocationSelectable() {
        String location = PlanRadarConfig.ticketLocation();
        if (location.isBlank()) return "No Location configured (project does not require one)";
        TicketDialog dialog = new TicketsPage(driver).open(projectId).createTicket(form.name()).selectLocation(location);
        dialog.discard();
        return "Location '" + location + "' selectable";
    }
}
