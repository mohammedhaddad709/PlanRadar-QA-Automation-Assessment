package com.planradar.qa.systems.planradar.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One project: its Forms tab ("Manage Forms of a Project") with the per-project field configuration. */
public final class ProjectPage extends PlanRadarPage {

    private static final Pattern PROJECT_ID = Pattern.compile("/projects/(\\d+)/");

    private static final By PROJECT_SEARCH = testId("search_projects");
    private static final By ASSIGN_FORM = testId("ticket-type-assign-form-button");
    private static final By ASSIGN_OVERLAY = testId("assign-form-overlay");
    private static final By ASSIGN_SEARCH = testId("Forms_search");
    private static final By APPLY = testId("ticket-type-configure-form-apply-button");
    private static final By CANCEL = testId("ticket-type-configure-form-cancel-button");

    private String projectId;

    public ProjectPage(WebDriver driver) {
        super(driver);
    }

    /** Opens the project by name from the projects list and remembers its id. */
    public ProjectPage open(String projectName) {
        driver.get(appUrl("/projects_list/"));
        actions.type(PROJECT_SEARCH, projectName);
        actions.click(By.xpath("//*[contains(@class,'ReactVirtualized__Table__row')]//*[normalize-space(text())="
                + literal(projectName) + "]"));
        actions.until(d -> PROJECT_ID.matcher(d.getCurrentUrl()).find());
        Matcher matcher = PROJECT_ID.matcher(driver.getCurrentUrl());
        matcher.find();
        projectId = matcher.group(1);
        return this;
    }

    public String projectId() {
        return projectId;
    }

    public ProjectPage openFormsTab() {
        driver.get(appUrl("/projects/" + projectId + "/ticket_layouts"));
        actions.waitForVisibility(ASSIGN_FORM);
        return this;
    }

    public boolean isFormAssigned(String formName) {
        return actions.isDisplayed(assignedRow(formName), Duration.ofSeconds(8));
    }

    /**
     * Assign form › "+" next to the form. PlanRadar then opens the per-project configuration; the assignment is
     * stored only after {@link #applyConfiguration()}.
     */
    public ProjectPage assignForm(String formName) {
        actions.click(ASSIGN_FORM);
        actions.waitForVisibility(ASSIGN_OVERLAY);
        actions.type(ASSIGN_SEARCH, formName);
        actions.click(By.xpath("//*[@data-testid='assign-form-overlay']//*[span[normalize-space(.)=" + literal(formName)
                + "]]//*[@data-testid='project-assign-form-plus-button']"));
        actions.waitForVisibility(APPLY);
        return this;
    }

    /** Opens the per-project configuration of an assigned form. */
    public ProjectPage openFormConfiguration(String formName) {
        actions.click(assignedRow(formName));
        actions.waitForVisibility(APPLY);
        return this;
    }

    // ---------- per-project field configuration ----------

    public boolean isMandatoryInProject(String fieldName) {
        return actions.isChecked(mandatoryInput(fieldName));
    }

    @Step("Set \"{fieldName}\" mandatory in this project: {mandatory}")
    public ProjectPage setMandatoryInProject(String fieldName, boolean mandatory) {
        actions.setChecked(By.xpath(rowXpath(fieldName) + "//label[.//input[@type='checkbox']]"),
                mandatoryInput(fieldName), mandatory);
        return this;
    }

    /** List assigned to a List field in this project (global lists are shown read-only). */
    public String projectListOf(String fieldName) {
        return actions.getText(By.xpath(rowXpath(fieldName) + "//*[starts-with(@data-testid,'Select_Option_')]"));
    }

    @Step("Apply the project's form configuration")
    public ProjectPage applyConfiguration() {
        actions.click(APPLY);
        actions.waitForInvisibility(APPLY);
        return this;
    }

    @Step("Cancel the project's form configuration")
    public ProjectPage cancelConfiguration() {
        actions.click(CANCEL);
        actions.waitForInvisibility(CANCEL);
        return this;
    }

    private static By mandatoryInput(String fieldName) {
        return By.xpath(rowXpath(fieldName) + "//input[@type='checkbox']");
    }

    private static String rowXpath(String fieldName) {
        return "//*[@data-testid=" + literal("Field_Row_" + fieldName) + "]";
    }

    private static By assignedRow(String formName) {
        return By.xpath("//*[starts-with(@data-testid,'applied-type-row')][normalize-space(.)=" + literal(formName) + "]");
    }
}
