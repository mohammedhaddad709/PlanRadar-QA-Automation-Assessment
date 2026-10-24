package com.planradar.qa.systems.planradar.pages;

import com.planradar.qa.systems.planradar.testdata.FieldDefinition;
import com.planradar.qa.systems.planradar.testdata.FieldType;
import com.planradar.qa.systems.planradar.testdata.FormDefinition;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.time.LocalDate;

/** Form builder: field palette (left), form canvas (middle), properties panel (right). */
public final class FormBuilderPage extends PlanRadarPage {

    private static final By FORM_NAME = testId("Setup_Forms_FormName");
    private static final By FORM_NAME_CONFIRM = testId("Setup_Forms_FormName_Save");
    private static final By FORM_DESCRIPTION = testId("Setup_Forms_FormDescription");
    private static final By FORM_DESCRIPTION_CONFIRM = testId("Setup_Forms_FormDescription_Save");
    private static final By DESCRIPTION_PLACEHOLDER =
            By.xpath("//p[contains(@class,'form-text-placeholder')][normalize-space(.)='Enter form description']");
    private static final By SAVE = testId("form-details-save-button");
    private static final By SUCCESS_TOAST = By.cssSelector(".Toastify__toast--success");

    public FormBuilderPage(WebDriver driver) {
        super(driver);
    }

    FormBuilderPage awaitLoaded() {
        actions.waitForVisibility(SAVE);
        return this;
    }

    // ---------- form header ----------

    public FormBuilderPage setName(String name) {
        actions.type(FORM_NAME, name);
        actions.click(FORM_NAME_CONFIRM);
        return this;
    }

    public FormBuilderPage setDescription(String description) {
        actions.click(DESCRIPTION_PLACEHOLDER);
        actions.type(FORM_DESCRIPTION, description);
        actions.click(FORM_DESCRIPTION_CONFIRM);
        return this;
    }

    /** Name / description as shown in the header of a saved form. */
    public boolean headerShows(String text) {
        return actions.isDisplayed(By.xpath("//*[normalize-space(text())=" + literal(text) + "]"), Duration.ofSeconds(10));
    }

    /** Saves and waits until PlanRadar stored the form (a new form leaves {@code /create}). */
    public FormBuilderPage save() {
        boolean isNew = driver.getCurrentUrl().endsWith("/create");
        actions.click(SAVE);
        if (isNew) {
            actions.until(d -> !d.getCurrentUrl().endsWith("/create"));
        } else {
            actions.isDisplayed(SUCCESS_TOAST, Duration.ofSeconds(10));
        }
        return this;
    }

    // ---------- fields ----------

    /** Adds a custom field from the palette; PlanRadar appends it and opens its properties panel. */
    public FieldPropertiesPanel addField(FieldType type) {
        actions.click(testId(type.paletteTestId()));
        actions.waitForVisibility(FieldPropertiesPanel.FIELD_NAME);
        return new FieldPropertiesPanel(driver);
    }

    /** Adds a field and applies its whole configuration from the test data. */
    public FormBuilderPage addField(FieldDefinition field) {
        FieldPropertiesPanel panel = addField(field.type()).rename(field.name());
        actions.waitForVisibility(canvasName(field.name()));
        if (field.list() != null) panel.setDefaultList(field.list().name());
        if (field.mandatory()) panel.setMandatory(true);
        if (field.hasDefault()) {
            switch (field.type()) {
                case CHECKBOX -> panel.setCheckedByDefault(Boolean.parseBoolean(field.defaultValue()));
                case DATE -> panel.setDefaultDate(LocalDate.parse(field.defaultValue()));
                case LIST -> panel.setDefaultListValue(field.defaultValue());
                default -> panel.setDefaultText(field.defaultValue());
            }
        }
        return this;
    }

    /** Builds the whole form from the test data and saves it. */
    public FormBuilderPage build(FormDefinition form) {
        setName(form.name());
        if (form.description() != null) setDescription(form.description());
        form.fields().forEach(this::addField);
        return save();
    }

    /**
     * Opens a field's properties with its pencil button (shown on hover). The button toggles the panel, so a field
     * whose panel is already open is left as it is.
     */
    public FieldPropertiesPanel openField(String fieldName) {
        if (actions.isDisplayed(FieldPropertiesPanel.FIELD_NAME, Duration.ofMillis(500))
                && fieldName.equals(actions.getValue(FieldPropertiesPanel.FIELD_NAME))) {
            return new FieldPropertiesPanel(driver);
        }
        actions.hover(By.xpath(rowXpath(fieldName)));
        actions.clickJS(By.xpath(rowXpath(fieldName) + "//*[@data-testid='field-edit-button']"));
        actions.until(d -> fieldName.equals(d.findElement(FieldPropertiesPanel.FIELD_NAME).getAttribute("value")));
        return new FieldPropertiesPanel(driver);
    }

    public boolean hasField(String fieldName) {
        return actions.isDisplayed(canvasName(fieldName), Duration.ofSeconds(10));
    }

    /** Mandatory fields are shown as "* Name" on the canvas. */
    public boolean isMarkedRequired(String fieldName) {
        return actions.getText(canvasName(fieldName)).startsWith("*");
    }

    /** Title is added by PlanRadar and its delete button is disabled. */
    public boolean canDelete(String fieldName) {
        By delete = By.xpath(rowXpath(fieldName) + "//*[@data-testid='field-delete-button']");
        return actions.count(delete) > 0 && actions.isEnabled(delete);
    }

    private static By canvasName(String fieldName) {
        String name = literal(fieldName);
        return By.xpath("//span[contains(@class,'field-name')][normalize-space(.)=" + name
                + " or normalize-space(.)=concat('* '," + name + ")]");
    }

    private static String rowXpath(String fieldName) {
        String name = literal(fieldName);
        return "//*[starts-with(@data-testid,'form-field-') and not(starts-with(@data-testid,'form-field-default'))]"
                + "[.//span[contains(@class,'field-name')][normalize-space(.)=" + name
                + " or normalize-space(.)=concat('* '," + name + ")]]";
    }
}
