package com.planradar.qa.systems.planradar.flows;

import com.planradar.qa.systems.planradar.config.PlanRadarConfig;
import com.planradar.qa.systems.planradar.pages.TicketDialog;
import com.planradar.qa.systems.planradar.pages.TicketsPage;
import com.planradar.qa.systems.planradar.reporting.Attachments;
import com.planradar.qa.systems.planradar.testdata.FieldDefinition;
import com.planradar.qa.systems.planradar.testdata.FieldType;
import com.planradar.qa.systems.planradar.testdata.FormDefinition;
import com.planradar.qa.systems.planradar.testdata.TicketData;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import org.openqa.selenium.WebDriver;

import java.util.LinkedHashMap;
import java.util.Map;

/** Ticket actions on the assessment project with the form under test. */
public final class TicketFlow {

    private final WebDriver driver;
    private final String projectId;
    private final FormDefinition form;

    public TicketFlow(WebDriver driver, String projectId, FormDefinition form) {
        this.driver = driver;
        this.projectId = projectId;
        this.form = form;
    }

    /** New-ticket dialog with the form and the configured Location already selected. */
    @Step("Start a new ticket with the form under test")
    public TicketDialog startNewTicket() {
        return new TicketsPage(driver).open(projectId)
                .createTicket(form.name())
                .selectLocation(PlanRadarConfig.ticketLocation());
    }

    @Step("Fill the ticket")
    public TicketDialog fill(@Param(mode = Parameter.Mode.HIDDEN) TicketDialog dialog, @Param("Ticket data") TicketData data) {
        Attachments.ticketData(data);
        return dialog.fill(data, fieldTypes());
    }

    /** Creates a valid ticket and returns its title; fails when PlanRadar does not store it. */
    @Step("Create a valid ticket \"{data.title}\"")
    public String createTicket(TicketData data) {
        TicketDialog dialog = fill(startNewTicket(), data);
        if (!dialog.saveAndClose()) {
            throw new IllegalStateException("Ticket '" + data.title() + "' was not saved (dialog stayed open)");
        }
        return data.title();
    }

    @Step("Open ticket \"{title}\"")
    public TicketDialog openTicket(String title) {
        return new TicketsPage(driver).open(projectId).openTicket(title);
    }

    @Step("Search the project's tickets for \"{title}\"")
    public boolean isListed(String title) {
        return new TicketsPage(driver).open(projectId).isListed(title);
    }

    private Map<String, FieldType> fieldTypes() {
        Map<String, FieldType> types = new LinkedHashMap<>();
        for (FieldDefinition field : form.fields()) types.put(field.name(), field.type());
        return types;
    }
}
