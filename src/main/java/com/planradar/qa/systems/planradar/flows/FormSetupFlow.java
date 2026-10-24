package com.planradar.qa.systems.planradar.flows;

import com.planradar.qa.systems.planradar.pages.FormBuilderPage;
import com.planradar.qa.systems.planradar.pages.FormsListPage;
import com.planradar.qa.systems.planradar.pages.ListsPage;
import com.planradar.qa.systems.planradar.pages.ProjectPage;
import com.planradar.qa.systems.planradar.testdata.FormDefinition;
import com.planradar.qa.systems.planradar.testdata.ListDefinition;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** Creates and assigns the form under test and the global lists it needs. Every step is idempotent. */
public final class FormSetupFlow {

    private final WebDriver driver;

    public FormSetupFlow(WebDriver driver) {
        this.driver = driver;
    }

    /** @return true when the list had to be created, false when it already existed */
    @Step("Ensure global list \"{list.name}\" exists")
    public boolean ensureList(ListDefinition list) {
        ListsPage lists = new ListsPage(driver).open();
        if (lists.exists(list.name())) return false;
        lists.create(list);
        if (!new ListsPage(driver).open().exists(list.name())) {
            throw new IllegalStateException("List '" + list.name() + "' was saved but is not listed");
        }
        return true;
    }

    @Step("Create form \"{form.name}\" in the form builder")
    public FormBuilderPage createForm(FormDefinition form) {
        return new FormsListPage(driver).open().createNewForm().build(form);
    }

    public boolean formExists(String formName) {
        return new FormsListPage(driver).open().isListed(formName);
    }

    @Step("Open form \"{formName}\"")
    public FormBuilderPage openForm(String formName) {
        return new FormsListPage(driver).open().openForm(formName);
    }

    /** @return the project page on its Forms tab (the project id is known from then on) */
    @Step("Open project \"{projectName}\" › Forms")
    public ProjectPage openProjectForms(String projectName) {
        return new ProjectPage(driver).open(projectName).openFormsTab();
    }

    /** Assigns the form, leaving the per-project configuration as the form defines it. */
    @Step("Assign form \"{formName}\" to project \"{projectName}\"")
    public ProjectPage assignToProject(String projectName, String formName) {
        ProjectPage project = openProjectForms(projectName);
        if (!project.isFormAssigned(formName)) {
            project.assignForm(formName).applyConfiguration();
        }
        return project;
    }
}
