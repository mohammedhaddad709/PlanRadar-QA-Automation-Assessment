package com.planradar.qa.systems.planradar.pages;

import com.planradar.qa.systems.planradar.pages.components.DatePicker;
import com.planradar.qa.systems.planradar.pages.components.ReactSelect;
import com.planradar.qa.systems.planradar.testdata.FieldType;
import com.planradar.qa.systems.planradar.testdata.TicketData;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

/**
 * The ticket dialog ({@code ticket-form-modal}) for a new ticket and for an existing one. Custom fields are located
 * by PlanRadar's test id {@code ticket_form_field_<Field_Name>}; Title is {@code ticket-form-field-subject}.
 */
public final class TicketDialog extends PlanRadarPage {

    private static final String MODAL = "//*[@data-testid='ticket-form-modal']";
    private static final By MODAL_BY = By.xpath(MODAL);
    private static final String FORM_SELECTOR = MODAL + "//*[@data-testid='ticket-form-selector']";
    private static final By TITLE = By.xpath(MODAL + "//*[@data-testid='ticket-form-field-subject']");

    private static final By LOCATION = By.xpath(MODAL + "//*[@data-testid='component-selector-input-group']");
    private static final By PLAN_TREE = testId("plan-tree-selector-list");

    /** New ticket: "Save" keeps the dialog open; the second button reads "Close" when empty, "Save and close" otherwise. */
    private static final By NEW_SAVE = testId("tickets_details_savenewticket");
    private static final By NEW_SAVE_AND_CLOSE = testId("tickets_details_closenewticket");
    /** Existing ticket. */
    private static final By EDIT_SAVE = testId("tickets_details_save");
    /** The header ✕ (an existing ticket's header also has report, settings and previous/next buttons). */
    private static final By HEADER_CLOSE = By.xpath(MODAL + "//*[@data-testid='ui-modal-v2-header-test-id']"
            + "//button[.//i[contains(@class,'fa-times')]]");
    private static final By CONFIRM_LEAVE =
            By.xpath("//*[@data-testid='ui-modal-v2-test-id']//button[normalize-space(.)='Confirm']");

    /** List fields load from the server after the dialog opens; a skeleton placeholder is shown meanwhile. */
    private static final By SKELETON = By.xpath(MODAL + "//*[contains(@class,'animated-skeleton')]");

    /** Inline "You have not filled in all the required fields." / "cannot be empty" and the red error toast. */
    private static final By ANY_VALIDATION_ERROR = By.xpath(MODAL + "//*[contains(@class,'text-fg-error')]"
            + " | " + MODAL + "//*[contains(text(),'not filled in all the required fields')]"
            + " | //*[contains(@class,'Toastify__toast--error')]");

    public TicketDialog(WebDriver driver) {
        super(driver);
    }

    TicketDialog awaitOpen() {
        actions.waitForVisibility(MODAL_BY);
        return awaitFieldsLoaded();
    }

    /** Values must not be read before the skeleton placeholders are gone. */
    public TicketDialog awaitFieldsLoaded() {
        actions.waitForVisibility(TITLE);
        actions.waitForInvisibility(SKELETON);
        return this;
    }

    public TicketDialog selectForm(String formName) {
        ReactSelect selector = new ReactSelect(driver, FORM_SELECTOR);
        if (!selector.selectedText().equals(formName)) selector.choose(formName);
        return awaitFieldsLoaded();
    }

    /** Folder/plan of the project used as Location; skipped when none is configured. */
    public TicketDialog selectLocation(String planOrFolder) {
        if (planOrFolder == null || planOrFolder.isBlank()) return this;
        if (actions.getTextOrEmpty(LOCATION).contains(planOrFolder)) return this;
        actions.click(LOCATION);
        actions.waitForVisibility(PLAN_TREE);
        // no tree search: searching filters to plans and hides folders; nodes are a flat list, pick by label
        actions.click(By.xpath("//*[@data-testid='plan-tree-selector-list']//*[starts-with(@data-testid,'plan-tree-selector-node-')"
                + " and not(contains(@data-testid,'file-name'))][.//span[normalize-space(.)=" + literal(planOrFolder) + "]]"));
        actions.until(d -> d.findElement(LOCATION).getText().contains(planOrFolder));
        return this;
    }

    // ---------- entering data ----------

    public TicketDialog title(String title) {
        if (title != null) actions.type(TITLE, title);
        return this;
    }

    public TicketDialog text(String fieldName, String value) {
        actions.type(field(fieldName), value);
        return this;
    }

    @Step("Clear field \"{fieldName}\"")
    public TicketDialog clear(String fieldName) {
        actions.clearWithKeyboard(field(fieldName));
        return this;
    }

    public TicketDialog choose(String listFieldName, String option) {
        new ReactSelect(driver, fieldXpath(listFieldName)).choose(option);
        return this;
    }

    public TicketDialog date(String dateFieldName, LocalDate date) {
        new DatePicker(driver, field(dateFieldName),
                By.xpath(fieldXpath(dateFieldName) + "/following-sibling::*[@data-testid='date-input']")).pick(date);
        return this;
    }

    public TicketDialog checked(String checkboxFieldName, boolean checked) {
        actions.setChecked(field(checkboxFieldName), By.xpath(fieldXpath(checkboxFieldName) + "//input[@type='checkbox']"), checked);
        return this;
    }

    /** Enters a value according to the field type (List → option, Date → ISO date, Checkbox → true/false). */
    public TicketDialog enter(String fieldName, FieldType type, String value) {
        return switch (type) {
            case LIST -> choose(fieldName, value);
            case DATE -> date(fieldName, LocalDate.parse(value));
            case CHECKBOX -> checked(fieldName, Boolean.parseBoolean(value));
            default -> value.isEmpty() ? clear(fieldName) : text(fieldName, value);
        };
    }

    public TicketDialog fill(TicketData data, Map<String, FieldType> types) {
        title(data.title());
        data.values().forEach((name, value) -> enter(name, types.get(name), value));
        return this;
    }

    // ---------- reading ----------

    public String titleValue() {
        return actions.getValue(TITLE);
    }

    public String textValue(String fieldName) {
        return actions.getValue(field(fieldName));
    }

    /** Selected option of a List field once it has settled, "" when none. */
    public String selectedOption(String listFieldName) {
        awaitFieldsLoaded();
        return new ReactSelect(driver, fieldXpath(listFieldName)).settledSelectedText();
    }

    /** The field label is shown as "* Name" when PlanRadar treats the field as mandatory. */
    public boolean isMarkedRequired(String fieldName) {
        // deepest element whose whole text is "* Name" (works whether or not the asterisk is its own span)
        String label = literal("* " + fieldName);
        return actions.isDisplayed(By.xpath(MODAL + "//*[normalize-space(.)=" + label
                + " and not(.//*[normalize-space(.)=" + label + "])]"), Duration.ofSeconds(3));
    }

    // ---------- saving / closing ----------

    /** New ticket: "Save" (dialog stays open). */
    @Step("Click \"Save\"")
    public TicketDialog clickSave() {
        actions.click(NEW_SAVE);
        return this;
    }

    /** New ticket: "Save and close"; true when the ticket was stored and the dialog closed. */
    @Step("Click \"Save and close\"")
    public boolean saveAndClose() {
        actions.click(NEW_SAVE_AND_CLOSE);
        return closes();
    }

    /** Existing ticket: "Save" (dialog stays open). */
    @Step("Click \"Save\" on the existing ticket")
    public TicketDialog clickSaveChanges() {
        actions.click(EDIT_SAVE);
        return this;
    }

    /**
     * Leaves without saving through the header ✕, confirming "You have unsaved changes" when asked. The footer button
     * is not used: it turns into "Save and close" as soon as anything changed and would try to save.
     */
    @Step("Close the ticket dialog without saving")
    public void discard() {
        if (!isOpen()) return;
        actions.click(HEADER_CLOSE);
        if (actions.isDisplayed(CONFIRM_LEAVE, Duration.ofSeconds(3))) actions.click(CONFIRM_LEAVE);
        actions.waitForInvisibility(MODAL_BY);
    }

    public boolean isOpen() {
        return actions.isDisplayed(MODAL_BY, Duration.ofSeconds(2));
    }

    /** A validation message (inline or toast) is shown. */
    public boolean showsValidationError() {
        return actions.isDisplayed(ANY_VALIDATION_ERROR, Duration.ofSeconds(6));
    }

    /** A validation message is shown next to this field (within its nearest containers). */
    public boolean showsValidationErrorFor(String fieldName) {
        By near = By.xpath("(" + fieldXpath(fieldName) + "/ancestor::*[position() <= 3]"
                + "//*[contains(@class,'text-fg-error') or contains(text(),'not filled in all the required fields')"
                + " or contains(text(),'cannot be empty')])[1]");
        return actions.isDisplayed(near, Duration.ofSeconds(6));
    }

    private boolean closes() {
        try {
            actions.waitForInvisibility(MODAL_BY);
            return true;
        } catch (TimeoutException stillOpen) {
            return false;
        }
    }

    private static String fieldXpath(String fieldName) {
        return MODAL + "//*[@data-testid=" + literal("ticket_form_field_" + idPart(fieldName)) + "]";
    }

    private static By field(String fieldName) {
        return By.xpath(fieldXpath(fieldName));
    }
}
