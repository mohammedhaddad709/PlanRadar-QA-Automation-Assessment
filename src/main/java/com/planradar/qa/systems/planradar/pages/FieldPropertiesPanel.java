package com.planradar.qa.systems.planradar.pages;

import com.planradar.qa.systems.planradar.pages.components.DatePicker;
import com.planradar.qa.systems.planradar.pages.components.ReactSelect;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.LocalDate;
import java.util.List;

/** Right-hand "field properties" panel of the form builder for the selected field. */
public final class FieldPropertiesPanel extends PlanRadarPage {

    static final String PANEL = "//aside[@data-testid='ticket-type-form-properties-panel']";

    static final By FIELD_NAME = By.xpath(PANEL + "//textarea[@name='name']");
    private static final By MANDATORY_LABEL = By.xpath(PANEL + "//label[substring(@data-testid,"
            + "string-length(@data-testid) - string-length('_Mandatory') + 1)='_Mandatory']");
    private static final By MANDATORY_INPUT = By.xpath(PANEL + "//input[@name='required']");
    private static final By DEFAULT_TEXT = By.xpath(PANEL + "//*[@placeholder='Set a default value']");
    private static final By DEFAULT_CHECKBOX_LABEL = By.xpath(PANEL + "//label[@data-testid='form-field-default-checkbox']");
    private static final By DEFAULT_CHECKBOX_INPUT = By.xpath(PANEL + "//label[@data-testid='form-field-default-checkbox']"
            + "//input[@type='checkbox']");
    private static final By DEFAULT_DATE_TRIGGER = By.xpath(PANEL + "//*[@data-id='Setup_Forms_Date_DefaultValue']");
    private static final By DEFAULT_DATE_VALUE = By.xpath(PANEL + "//*[@data-testid='date-input']");
    private static final String DEFAULT_LIST = PANEL + "//*[@id='Setup_Forms_List_DefaultValue']";
    private static final String DEFAULT_LIST_VALUE = PANEL + "//*[@data-testid='form-list-default-value']";

    FieldPropertiesPanel(WebDriver driver) {
        super(driver);
    }

    public String fieldName() {
        return actions.getValue(FIELD_NAME);
    }

    /** Field type read from the panel's test ids, e.g. {@code Setup_Forms_decimal_Name} → {@code decimal}. */
    public String typeKey() {
        return actions.getAttribute(FIELD_NAME, "data-testid").replace("Setup_Forms_", "").replace("_Name", "");
    }

    public FieldPropertiesPanel rename(String name) {
        actions.type(FIELD_NAME, name);
        return this;
    }

    public FieldPropertiesPanel setMandatory(boolean mandatory) {
        actions.setChecked(MANDATORY_LABEL, MANDATORY_INPUT, mandatory);
        return this;
    }

    public boolean isMandatory() {
        return actions.isChecked(MANDATORY_INPUT);
    }

    // ---------- default values ----------

    public FieldPropertiesPanel setDefaultText(String value) {
        actions.type(DEFAULT_TEXT, value);
        return this;
    }

    public String defaultText() {
        return actions.getValue(DEFAULT_TEXT);
    }

    public FieldPropertiesPanel setCheckedByDefault(boolean checked) {
        actions.setChecked(DEFAULT_CHECKBOX_LABEL, DEFAULT_CHECKBOX_INPUT, checked);
        return this;
    }

    public boolean isCheckedByDefault() {
        return actions.isChecked(DEFAULT_CHECKBOX_INPUT);
    }

    public FieldPropertiesPanel setDefaultDate(LocalDate date) {
        datePicker().pick(date);
        return this;
    }

    /** ISO yyyy-MM-dd or "". */
    public String defaultDate() {
        return datePicker().isoValue();
    }

    // ---------- List fields ----------

    public FieldPropertiesPanel setDefaultList(String listName) {
        new ReactSelect(driver, DEFAULT_LIST).choose(listName);
        return this;
    }

    public String defaultList() {
        return new ReactSelect(driver, DEFAULT_LIST).settledSelectedText();
    }

    /** Only available once a default list is chosen; offers that list's values. */
    public FieldPropertiesPanel setDefaultListValue(String value) {
        new ReactSelect(driver, DEFAULT_LIST_VALUE).choose(value);
        return this;
    }

    /** Default value of the List field, "" when none (the list's values load from the server first). */
    public String defaultListValue() {
        return new ReactSelect(driver, DEFAULT_LIST_VALUE).settledSelectedText();
    }

    /** Values the List field offers (the "Default value" drop-down lists the assigned list's items). */
    public List<String> listValues() {
        return new ReactSelect(driver, DEFAULT_LIST_VALUE).options();
    }

    private DatePicker datePicker() {
        return new DatePicker(driver, DEFAULT_DATE_TRIGGER, DEFAULT_DATE_VALUE);
    }
}
