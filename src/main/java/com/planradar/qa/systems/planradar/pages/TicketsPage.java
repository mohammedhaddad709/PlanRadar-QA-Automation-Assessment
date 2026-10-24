package com.planradar.qa.systems.planradar.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tickets list of one project. */
public final class TicketsPage extends PlanRadarPage {

    private static final By CREATE_TICKET = testId("create-action-btn");
    private static final By SEARCH = testId("tickets_toolbar_searchtickets");
    /** "N tickets" label next to the search box; rendered once the list has been fetched. */
    private static final By RESULT_COUNT = By.cssSelector(".sub-nav-search-result");
    private static final By ANY_TITLE = By.cssSelector("[data-testid^='Tickets_Table_Title_']");

    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\s*(\\d+)");
    private static final Duration LIST_SETTLE = Duration.ofSeconds(10);

    public TicketsPage(WebDriver driver) {
        super(driver);
    }

    public TicketsPage open(String projectId) {
        driver.get(appUrl("/" + projectId + "/defects"));
        actions.waitForVisibility(CREATE_TICKET);
        actions.getText(RESULT_COUNT);
        return this;
    }

    /** "Create Ticket" → the new-ticket dialog with the given form selected. */
    public TicketDialog createTicket(String formName) {
        actions.click(CREATE_TICKET);
        return new TicketDialog(driver).awaitOpen().selectForm(formName);
    }

    public boolean isListed(String title) {
        search(title);
        return actions.isDisplayed(titleCell(title), Duration.ofSeconds(10));
    }

    /** Opens an existing ticket from the list (same dialog, in edit mode). */
    public TicketDialog openTicket(String title) {
        search(title);
        actions.click(titleCell(title));
        return new TicketDialog(driver).awaitOpen();
    }

    /**
     * Filters the list so that the wanted ticket is on the (single) visible page, then waits for the
     * result set to settle. The list is paginated at 20 rows and the search is token based and also
     * matches the project name, so searching for the full title would return every ticket in the
     * project; the unique run suffix ({@code <runId>-<n>}) is used instead. The caller still matches
     * the exact title in the filtered rows.
     */
    private void search(String title) {
        String term = searchTermFor(title);
        actions.type(SEARCH, term);
        actions.pressKeys(SEARCH, Keys.ENTER);

        String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8);
        actions.awaitQuietly(LIST_SETTLE, d -> d.getCurrentUrl().contains("search=" + encoded));
        actions.awaitQuietly(LIST_SETTLE, d -> d.findElements(RESULT_COUNT).stream().findFirst()
                .map(label -> LEADING_NUMBER.matcher(label.getText()))
                .filter(Matcher::find)
                .map(count -> Integer.parseInt(count.group(1)) == d.findElements(ANY_TITLE).size())
                .orElse(false));
    }

    private static String searchTermFor(String title) {
        int lastSpace = title.trim().lastIndexOf(' ');
        return lastSpace < 0 ? title : title.substring(lastSpace + 1);
    }

    private static By titleCell(String title) {
        return By.xpath("//span[starts-with(@data-testid,'Tickets_Table_Title_')][normalize-space(.)=" + literal(title) + "]");
    }
}
