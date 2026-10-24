package com.planradar.qa.systems.planradar.pages;

import com.planradar.qa.systems.planradar.testdata.ListDefinition;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.List;

/** Forms and Lists › Lists – global lists of the account. */
public final class ListsPage extends PlanRadarPage {

    private static final By SEARCH = testId("Search_List");
    private static final By CREATE_LIST = testId("lists-create-button");
    private static final By DETAILS_MODAL = testId("ticket-type-list-details-modal");
    private static final By LIST_NAME_INPUT = testId("Setup_Lists_Details_List_Name");
    private static final By LIST_NAME_APPLY = testId("setup_list_rename_save_button");
    private static final By CREATE_ITEM = testId("ticket-type-list-details-create-item-button");
    /** The newest item input; Enter adds another item, Ctrl+Enter finishes. */
    private static final By NEWEST_ITEM_INPUT =
            By.xpath("(//input[starts-with(@data-testid,'setup_list_create_list_name_input-')])[last()]");
    private static final By SAVE = testId("setup_list_save_button");
    private static final By CLOSE = testId("setup_list_close_button");
    private static final By CONFIRM_CLOSE = testId("Setup_Lists_Details_CloseWithUnsaved_Confirm");

    public ListsPage(WebDriver driver) {
        super(driver);
    }

    public ListsPage open() {
        driver.get(appUrl("/ticket-types/lists"));
        actions.waitForVisibility(SEARCH);
        return this;
    }

    public boolean exists(String listName) {
        actions.type(SEARCH, listName);
        return actions.isDisplayed(testId("list-name-" + idPart(listName)), Duration.ofSeconds(8));
    }

    /** Number of items shown for the list in the table. */
    public int itemCount(String listName) {
        return Integer.parseInt(actions.getText(testId("list-items-count-" + idPart(listName))));
    }

    public ListsPage create(ListDefinition list) {
        actions.click(CREATE_LIST);
        actions.waitForVisibility(LIST_NAME_INPUT);
        actions.type(LIST_NAME_INPUT, list.name());
        actions.click(LIST_NAME_APPLY);
        actions.click(CREATE_ITEM);
        List<String> values = list.values();
        for (int i = 0; i < values.size(); i++) {
            actions.type(NEWEST_ITEM_INPUT, values.get(i));
            boolean last = i == values.size() - 1;
            if (last) actions.pressKeys(NEWEST_ITEM_INPUT, Keys.CONTROL, Keys.ENTER);
            else actions.pressKeys(NEWEST_ITEM_INPUT, Keys.ENTER);
        }
        actions.click(SAVE);
        // after saving the dialog stays open in view mode (Save disabled) – then close it
        actions.until(d -> !d.findElement(SAVE).isEnabled());
        actions.click(CLOSE);
        if (actions.isDisplayed(CONFIRM_CLOSE, Duration.ofSeconds(2))) actions.click(CONFIRM_CLOSE);
        actions.waitForInvisibility(DETAILS_MODAL);
        return this;
    }
}
