package com.planradar.qa.systems.planradar.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/** Forms and Lists › Forms – all forms of the account. */
public final class FormsListPage extends PlanRadarPage {

    private static final By SEARCH = By.id("search-types");
    private static final By CREATE_FORM = testId("Setup_Forms_CreateNewForm");

    public FormsListPage(WebDriver driver) {
        super(driver);
    }

    public FormsListPage open() {
        driver.get(appUrl("/ticket-types"));
        actions.waitForVisibility(SEARCH);
        return this;
    }

    public FormBuilderPage createNewForm() {
        actions.click(CREATE_FORM);
        return new FormBuilderPage(driver).awaitLoaded();
    }

    public boolean isListed(String formName) {
        search(formName);
        return actions.isDisplayed(row(formName), Duration.ofSeconds(10));
    }

    public FormBuilderPage openForm(String formName) {
        search(formName);
        actions.click(row(formName));
        return new FormBuilderPage(driver).awaitLoaded();
    }

    private void search(String formName) {
        actions.type(SEARCH, formName);
    }

    /** Clickable name of a form in the list: {@code [data-testid='ticket-type-name-<Name_with_underscores>']}. */
    private static By row(String formName) {
        return By.xpath("//*[@data-testid=" + literal("ticket-type-name-" + idPart(formName)) + "]//*[@role='button']");
    }
}
