package com.planradar.qa.systems.planradar.mandatory;

import com.planradar.qa.core.utils.report.Covers;
import com.planradar.qa.systems.planradar.config.PlanRadarConfig;
import com.planradar.qa.systems.planradar.flows.FormSetupFlow;
import com.planradar.qa.systems.planradar.flows.TicketFlow;
import com.planradar.qa.systems.planradar.pages.ProjectPage;
import com.planradar.qa.systems.planradar.pages.TicketDialog;
import com.planradar.qa.systems.planradar.PlanRadarBaseTest;
import com.planradar.qa.systems.planradar.testdata.FormDefinition;
import com.planradar.qa.systems.planradar.testdata.TestRun;
import com.planradar.qa.systems.planradar.testdata.TicketData;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static com.planradar.qa.systems.planradar.testdata.AssessmentData.INSPECTION_NOTES;
import static com.planradar.qa.systems.planradar.testdata.AssessmentData.INSPECTION_STATUS;
import static com.planradar.qa.systems.planradar.testdata.AssessmentData.INSPECTOR_NAME;

/**
 * Scenario 4 – mandatory fields block saving a ticket until they have a value.
 * Checked in the real ticket workflow of the project the form is assigned to; every ticket title is unique per run.
 */
@Epic("PlanRadar Form Builder")
@Feature("Scenario 4 - Mandatory fields")
@Owner("Mohamed El-Hadad")
public class MandatoryFieldsTest extends PlanRadarBaseTest {

    private static final String TICKET_PRECONDITIONS = "**Preconditions:** the form under test is assigned to project "
            + "*QA Assessment - Form Builder* (DEP-006), selectable in *Create Ticket* (DEP-007), and the Location "
            + "*QA Testing Location* is selectable (DEP-008). *Inspector Name* (Short Text) and *Inspection Status* "
            + "(List, no default) are mandatory.";

    @Test(description = "TC-005 — Empty mandatory text field blocks saving", priority = 5)
    @Story("Create a ticket")
    @Severity(SeverityLevel.BLOCKER)
    @Covers({"TC-MAN-02", "TC-MAN-08"})
    @Description("**Objective:** a new ticket cannot be saved while a mandatory text field is empty.\n\n"
            + TICKET_PRECONDITIONS + "\n\n"
            + "**Expected result:** *Inspector Name* is marked `*`; after *Save* a validation message is shown next to "
            + "it, the dialog stays open, and no ticket is created.")
    public void emptyMandatoryTextBlocksSave() {
        TicketFlow tickets = fixture().requireTicketReady().tickets();
        String title = TestRun.ticketTitle("TC-005");

        TicketDialog ticket = tickets.fill(tickets.startNewTicket(), TicketData.builder()
                .title(title)
                .value(INSPECTION_STATUS, "Pass")          // every other mandatory field is filled
                .build());
        verify().assertTrue(ticket.isMarkedRequired(INSPECTOR_NAME), "Inspector Name is marked as required (*)");
        ticket.clickSave();

        assertSaveBlocked(ticket, INSPECTOR_NAME);
        ticket.discard();
        verify().assertFalse(tickets.isListed(title), "The invalid ticket was not created");
    }

    @Test(description = "TC-006 — Empty mandatory List field blocks saving", priority = 6)
    @Story("Create a ticket")
    @Severity(SeverityLevel.BLOCKER)
    @Covers({"TC-MAN-07", "TC-MAN-08"})
    @Description("**Objective:** a new ticket cannot be saved while a mandatory List field has no selection.\n\n"
            + TICKET_PRECONDITIONS + "\n\n"
            + "**Expected result:** *Inspection Status* starts empty and is marked `*`; after *Save* a validation "
            + "message is shown next to it, the dialog stays open, and no ticket is created.")
    public void emptyMandatoryListBlocksSave() {
        TicketFlow tickets = fixture().requireTicketReady().tickets();
        String title = TestRun.ticketTitle("TC-006");

        TicketDialog ticket = tickets.fill(tickets.startNewTicket(), TicketData.builder()
                .title(title)
                .value(INSPECTOR_NAME, "List Validation Inspector")
                .build());
        verify().assertEquals(ticket.selectedOption(INSPECTION_STATUS), "", "Inspection Status has no default and starts empty");
        verify().assertTrue(ticket.isMarkedRequired(INSPECTION_STATUS), "Inspection Status is marked as required (*)");
        ticket.clickSave();

        assertSaveBlocked(ticket, INSPECTION_STATUS);
        ticket.discard();
        verify().assertFalse(tickets.isListed(title), "The invalid ticket was not created");
    }

    @Test(description = "TC-007 — Ticket with all mandatory fields filled is saved", priority = 7)
    @Story("Create a ticket")
    @Severity(SeverityLevel.BLOCKER)
    @Covers({"TC-MAN-03"})
    @Description("**Objective:** once every mandatory field has a value the ticket is saved with those values.\n\n"
            + TICKET_PRECONDITIONS + "\n\n"
            + "**Expected result:** *Save and close* closes the dialog; the ticket is listed; reopened, it shows the "
            + "entered Title, Inspector Name and Inspection Status and no validation error.")
    public void validTicketIsSaved() {
        TicketFlow tickets = fixture().requireTicketReady().tickets();
        String title = TestRun.ticketTitle("TC-007");

        TicketDialog ticket = tickets.fill(tickets.startNewTicket(), TicketData.builder()
                .title(title)
                .value(INSPECTOR_NAME, "Valid Inspector")
                .value(INSPECTION_STATUS, "Fail")
                .build());
        verify().assertTrue(ticket.saveAndClose(), "The valid ticket is saved and the dialog closes");
        verify().assertTrue(tickets.isListed(title), "The saved ticket is listed");

        TicketDialog reopened = tickets.openTicket(title);
        verify().assertEquals(reopened.titleValue(), title, "Persisted Title");
        verify().assertEquals(reopened.textValue(INSPECTOR_NAME), "Valid Inspector", "Persisted Inspector Name");
        verify().assertEquals(reopened.selectedOption(INSPECTION_STATUS), "Fail", "Persisted Inspection Status");
        verify().assertFalse(reopened.showsValidationError(), "No mandatory validation error remains");
        reopened.discard();
    }

    @Test(description = "TC-008 — Clearing a mandatory field on an existing ticket is blocked", priority = 8)
    @Story("Edit a ticket")
    @Severity(SeverityLevel.CRITICAL)
    @Covers({"TC-MAN-10"})
    @Description("**Objective:** the mandatory rule also applies when an existing ticket is edited.\n\n"
            + TICKET_PRECONDITIONS + " The test first creates a valid ticket.\n\n"
            + "**Expected result:** clearing *Inspector Name* and saving is blocked with a message next to the field; "
            + "after discarding, the stored ticket still has its original value.")
    public void clearingMandatoryFieldOnExistingTicketIsBlocked() {
        TicketFlow tickets = fixture().requireTicketReady().tickets();
        String title = Allure.step("Setup: create a valid ticket", () -> tickets.createTicket(TicketData.builder()
                .title(TestRun.ticketTitle("TC-008"))
                .value(INSPECTOR_NAME, "Original Inspector")
                .value(INSPECTION_STATUS, "Pending")
                .build()));

        TicketDialog ticket = tickets.openTicket(title);
        ticket.clear(INSPECTOR_NAME).clickSaveChanges();
        assertSaveBlocked(ticket, INSPECTOR_NAME);
        ticket.discard();                                 // confirms "You have unsaved changes"

        TicketDialog reopened = tickets.openTicket(title);
        verify().assertEquals(reopened.textValue(INSPECTOR_NAME), "Original Inspector", "Stored value is unchanged");
        reopened.discard();
    }

    @Test(description = "TC-009 — A project-level Mandatory override is respected", priority = 9)
    @Story("Project-level configuration")
    @Severity(SeverityLevel.NORMAL)
    @Covers({"TC-MAN-12"})
    @Description("**Objective:** a project can make a mandatory field optional without changing the form.\n\n"
            + TICKET_PRECONDITIONS + "\n\n"
            + "**Expected result:** with *Inspector Name* set to not mandatory in the project's form configuration, "
            + "the field has no `*` and a ticket without it is saved and listed. The project setting is restored "
            + "afterwards and the restore is verified.")
    public void projectMandatoryOverrideIsRespected() {
        FormDefinition form = fixture().requireTicketReady().form();
        TicketFlow tickets = fixture().tickets();
        FormSetupFlow setup = new FormSetupFlow(browser());

        Allure.step("Project › Forms: make Inspector Name optional for this project only", () ->
                setup.openProjectForms(PlanRadarConfig.projectName()).openFormConfiguration(form.name())
                        .setMandatoryInProject(INSPECTOR_NAME, false).applyConfiguration());
        try {
            String title = TestRun.ticketTitle("TC-009");
            TicketDialog ticket = tickets.fill(tickets.startNewTicket(), TicketData.builder()
                    .title(title)
                    .value(INSPECTION_STATUS, "Pass")
                    .value(INSPECTION_NOTES, "Saved without Inspector Name (project override)")
                    .build());
            verify().assertFalse(ticket.isMarkedRequired(INSPECTOR_NAME), "Inspector Name is no longer marked (*)");
            verify().assertTrue(ticket.saveAndClose(), "The ticket saves without Inspector Name in this project");
            verify().assertTrue(tickets.isListed(title), "The saved ticket is listed");
        } finally {
            Allure.step("Restore: Inspector Name mandatory again in the project", () -> {
                ProjectPage project = setup.openProjectForms(PlanRadarConfig.projectName()).openFormConfiguration(form.name());
                project.setMandatoryInProject(INSPECTOR_NAME, true).applyConfiguration();
                verify().assertTrue(project.openFormConfiguration(form.name()).isMandatoryInProject(INSPECTOR_NAME),
                        "Project configuration is restored");
                project.cancelConfiguration();
            });
        }
    }

    private void assertSaveBlocked(TicketDialog ticket, String fieldName) {
        Allure.step("Saving is blocked because '" + fieldName + "' is empty", () -> {
            verify().assertTrue(ticket.showsValidationError(), "A validation message is shown");
            verify().assertTrue(ticket.isOpen(), "The dialog stays open (save blocked)");
            verify().assertTrue(ticket.showsValidationErrorFor(fieldName), "The message is shown next to " + fieldName);
        });
    }
}
