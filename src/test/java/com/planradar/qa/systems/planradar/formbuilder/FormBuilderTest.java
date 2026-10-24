package com.planradar.qa.systems.planradar.formbuilder;

import com.planradar.qa.core.utils.report.Covers;
import com.planradar.qa.systems.planradar.config.PlanRadarConfig;
import com.planradar.qa.systems.planradar.fixtures.AssessmentFixture;
import com.planradar.qa.systems.planradar.fixtures.Dependency;
import com.planradar.qa.systems.planradar.flows.FormSetupFlow;
import com.planradar.qa.systems.planradar.pages.FieldPropertiesPanel;
import com.planradar.qa.systems.planradar.pages.FormBuilderPage;
import com.planradar.qa.systems.planradar.pages.FormsListPage;
import com.planradar.qa.systems.planradar.pages.ListsPage;
import com.planradar.qa.systems.planradar.pages.ProjectPage;
import com.planradar.qa.systems.planradar.pages.TicketDialog;
import com.planradar.qa.systems.planradar.pages.TicketsPage;
import com.planradar.qa.systems.planradar.PlanRadarBaseTest;
import com.planradar.qa.systems.planradar.reporting.Attachments;
import com.planradar.qa.systems.planradar.testdata.FieldDefinition;
import com.planradar.qa.systems.planradar.testdata.FieldType;
import com.planradar.qa.systems.planradar.testdata.FormDefinition;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import java.math.BigDecimal;

/**
 * Scenario 1 – create a form with different field types and configure it correctly.
 * The form is built once per run (TC-001, or the fixture when TC-001 did not run) and then verified from every
 * angle the product exposes: the builder, the global lists, and the project it is assigned to.
 */
@Epic("PlanRadar Form Builder")
@Feature("Scenario 1 - Form creation and configuration")
@Owner("Mohamed El-Hadad")
public class FormBuilderTest extends PlanRadarBaseTest {

    @Test(description = "TC-001 — Create a new form with several field types", priority = 1)
    @Story("Create a form")
    @Severity(SeverityLevel.BLOCKER)
    @Covers({"TC-FB-01", "TC-FB-06", "TC-FB-07", "TC-FB-22"})
    @Description("""
            **Objective:** an administrator can build a new form with several field types and PlanRadar stores it.

            **Preconditions:** admin logged in; global lists `QA_Assessment_Inspection_Status` and
            `QA_Assessment_Inspection_Category` exist (created by the run when missing).

            **Expected result:** the form is listed in *Forms and Lists › Forms*; reopened, it shows its name and
            description, the predefined *Title* field (which cannot be deleted) and all 7 custom fields.""")
    public void createNewForm() {
        FormDefinition form = fixture().requireListsReady().form();
        Attachments.formDefinition(form);
        FormSetupFlow setup = new FormSetupFlow(browser());

        Allure.step("Create the form in the builder and save", () -> {
            if (setup.formExists(form.name())) {
                Allure.step("Form was already created earlier in this run by the fixture – verifying it");
            } else {
                setup.createForm(form);
            }
        });
        AssessmentFixture.markResolved(Dependency.DEP_004, form.name() + " created by TC-001");

        verify().assertTrue(new FormsListPage(browser()).open().isListed(form.name()),
                "The new form appears in Forms and Lists › Forms");

        FormBuilderPage builder = setup.openForm(form.name());
        verify().assertTrue(builder.headerShows(form.name()), "Reopened form shows its name");
        verify().assertTrue(builder.headerShows(form.description()), "Reopened form shows its description");
        verify().assertTrue(builder.hasField("Title"), "Predefined Title field is added automatically");
        verify().assertFalse(builder.canDelete("Title"), "Title field cannot be deleted");
        for (FieldDefinition field : form.fields()) {
            softly().assertTrue(builder.hasField(field.name()), "Field '" + field.name() + "' exists on the saved form");
        }
    }

    @Test(description = "TC-002 — Verify every field configuration is saved", priority = 2)
    @Story("Configure fields")
    @Severity(SeverityLevel.BLOCKER)
    @Covers({"TC-FB-07"})
    @Description("""
            **Objective:** the configuration of every field survives saving and reopening the form.

            **Preconditions:** the form under test exists (DEP-004).

            **Expected result:** for each field the properties panel shows the configured name, field type and
            *Mandatory* checkbox, mandatory fields carry the `*` marker on the canvas, and the default value equals
            the test data (dates compared as ISO, decimals as numbers).""")
    public void verifyFieldConfigurations() {
        FormDefinition form = fixture().requireFormReady().form();
        Attachments.formDefinition(form);
        FormBuilderPage builder = new FormSetupFlow(browser()).openForm(form.name());

        for (FieldDefinition field : form.fields()) {
            Allure.step("Inspect field '" + field.name() + "' (" + field.type().label() + ")", () -> {
                FieldPropertiesPanel panel = builder.openField(field.name());
                softly().assertEquals(panel.fieldName(), field.name(), field.name() + ": name");
                softly().assertEquals(panel.typeKey(), field.type().builderKey(), field.name() + ": field type");
                softly().assertEquals(panel.isMandatory(), field.mandatory(), field.name() + ": Mandatory checkbox");
                softly().assertEquals(builder.isMarkedRequired(field.name()), field.mandatory(),
                        field.name() + ": '*' marker on the canvas");
                if (field.hasDefault()) {
                    softly().assertEquals(defaultShownIn(panel, field), expectedDefault(field), field.name() + ": default value");
                }
            });
        }
    }

    @Test(description = "TC-003 — Verify List field configuration (list, values, default)", priority = 3)
    @Story("Configure fields")
    @Severity(SeverityLevel.CRITICAL)
    @Covers({"TC-FB-14"})
    @Description("""
            **Objective:** List fields are bound to the right global list and offer exactly its values.

            **Preconditions:** the form under test exists (DEP-004).

            **Expected result:** both global lists hold 3 values; each List field references its list, offers the
            values in the defined order, and has the configured default (none for *Inspection Status*,
            *Electrical* for *Inspection Category*).""")
    public void verifyListFieldConfiguration() {
        FormDefinition form = fixture().requireFormReady().form();

        Allure.step("Global lists hold the expected number of values", () -> {
            ListsPage lists = new ListsPage(browser()).open();
            form.lists().forEach(list -> {
                verify().assertTrue(lists.exists(list.name()), "Global list exists: " + list.name());
                verify().assertEquals(lists.itemCount(list.name()), list.values().size(), "Item count of " + list.name());
            });
        });

        FormBuilderPage builder = new FormSetupFlow(browser()).openForm(form.name());
        form.fields().stream().filter(f -> f.type() == FieldType.LIST).forEach(field ->
                Allure.step("List field '" + field.name() + "'", () -> {
                    FieldPropertiesPanel panel = builder.openField(field.name());
                    verify().assertEquals(panel.defaultList(), field.list().name(), field.name() + ": assigned list");
                    verify().assertEquals(panel.listValues(), field.list().values(), field.name() + ": available values");
                    verify().assertEquals(panel.defaultListValue(), field.hasDefault() ? field.defaultValue() : "",
                            field.name() + ": default value (single selection)");
                }));
    }

    @Test(description = "TC-004 — Assign the form to the project and verify it is available for tickets", priority = 4)
    @Story("Use the form in a project")
    @Severity(SeverityLevel.BLOCKER)
    @Covers({"TC-FB-33"})
    @Description("""
            **Objective:** the new form can be used for tickets once it is assigned to a project.

            **Preconditions:** the form under test exists (DEP-004); project *QA Assessment - Form Builder* exists.

            **Expected result:** after *Assign form › +* the per-project configuration shows the same *Mandatory*
            flags and lists as the form; after *Apply* the form is listed under the project's forms and can be
            selected in *Create Ticket*.""")
    public void assignFormToProject() {
        FormDefinition form = fixture().requireFormReady().form();
        ProjectPage project = new FormSetupFlow(browser()).openProjectForms(PlanRadarConfig.projectName());
        AssessmentFixture.rememberProjectId(project.projectId());
        AssessmentFixture.markResolved(Dependency.DEP_005, PlanRadarConfig.projectName() + " (id " + project.projectId() + ")");

        boolean newAssignment = !project.isFormAssigned(form.name());
        Allure.step(newAssignment ? "Assign the form (+)" : "Form already assigned by the fixture – open its configuration", () -> {
            if (newAssignment) project.assignForm(form.name());
            else project.openFormConfiguration(form.name());
        });

        Allure.step("Per-project configuration matches the form", () -> {
            for (FieldDefinition field : form.fields()) {
                softly().assertEquals(project.isMandatoryInProject(field.name()), field.mandatory(),
                        field.name() + ": mandatory in project");
                if (field.list() != null) {
                    softly().assertEquals(project.projectListOf(field.name()), field.list().name(),
                            field.name() + ": list used in project");
                }
            }
        });
        if (newAssignment) project.applyConfiguration();
        else project.cancelConfiguration();

        verify().assertTrue(project.openFormsTab().isFormAssigned(form.name()), "Form is listed under the project's forms");
        AssessmentFixture.markResolved(Dependency.DEP_006, form.name() + " assigned (TC-004)");

        TicketDialog dialog = new TicketsPage(browser()).open(project.projectId()).createTicket(form.name());
        verify().assertTrue(dialog.isOpen(), "The form is selectable when creating a ticket");
        AssessmentFixture.markResolved(Dependency.DEP_007, form.name() + " selectable (TC-004)");
        dialog.discard();
    }

    /** What the panel shows for the field's default, normalised like {@link #expectedDefault}. */
    private static String defaultShownIn(FieldPropertiesPanel panel, FieldDefinition field) {
        return switch (field.type()) {
            case CHECKBOX -> String.valueOf(panel.isCheckedByDefault());
            case DATE -> panel.defaultDate();
            case LIST -> panel.defaultListValue();
            case DECIMAL, NUMBER -> decimal(panel.defaultText());
            default -> panel.defaultText();
        };
    }

    private static String expectedDefault(FieldDefinition field) {
        return switch (field.type()) {
            case DECIMAL, NUMBER -> decimal(field.defaultValue());
            default -> field.defaultValue();
        };
    }

    /** 125.50 and 125.5 are the same number; compare numbers, not strings. */
    private static String decimal(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return new BigDecimal(raw.replace(',', '.').trim()).stripTrailingZeros().toPlainString();
    }
}
