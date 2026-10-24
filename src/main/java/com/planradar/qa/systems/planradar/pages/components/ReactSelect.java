package com.planradar.qa.systems.planradar.pages.components;

import com.planradar.qa.core.utils.actions.ElementActions;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * A react-select drop-down (Default List / Default value in the form builder, List fields in tickets, the ticket's
 * form selector). Located by its container; the menu renders its options with {@code role="option"}.
 */
public final class ReactSelect {

    private static final By OPTION = By.xpath("//*[@role='option']");
    private static final Duration SETTLE = Duration.ofSeconds(3);

    private final WebDriver driver;
    private final ElementActions actions;
    private final String containerXpath;

    /** @param containerXpath XPath of the react-select container (the element holding the control). */
    public ReactSelect(WebDriver driver, String containerXpath) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
        this.containerXpath = containerXpath;
    }

    /** Opens the menu, filters by the option text and picks the exact match. */
    public ReactSelect choose(String option) {
        open();
        // the menu is open and its search input focused; type without clicking it (the placeholder overlays it)
        actions.pressKeys(input(), option);
        // options carry their value in @name (their text may add a description, e.g. a form's description);
        // builder drop-downs without @name are matched by text
        actions.click(By.xpath("//*[@role='option'][@name=" + literal(option) + " or (not(@name) and normalize-space(.)="
                + literal(option) + ")]"));
        actions.until(d -> selectedText().equals(option));
        return this;
    }

    /** Text of the selected value right now, "" when nothing is selected. */
    public String selectedText() {
        List<WebElement> selected = driver.findElements(By.xpath(containerXpath
                + "//*[contains(@class,'singleValue') or contains(@class,'multiValue')]"));
        return selected.isEmpty() ? "" : selected.get(0).getText().trim();
    }

    /**
     * Selected value once the control has settled. Values backed by a server-loaded list (a default list value,
     * a ticket's List field) render a moment after the control itself, so "" is only reported after a short grace
     * period – an empty selection still answers quickly.
     */
    public String settledSelectedText() {
        actions.awaitQuietly(SETTLE, d -> !selectedText().isEmpty());
        return selectedText();
    }

    /** All option labels offered in the menu (opens and closes it). */
    public List<String> options() {
        open();
        actions.until(d -> !d.findElements(OPTION).isEmpty());
        List<String> labels = driver.findElements(OPTION).stream().map(e -> e.getText().trim()).toList();
        actions.pressKeys(input(), Keys.ESCAPE);
        return labels;
    }

    private void open() {
        actions.click(By.xpath(containerXpath + "//*[contains(@class,'control')]"));
    }

    private By input() {
        return By.xpath(containerXpath + "//input[@role='combobox' or contains(@id,'react-select')]");
    }

    private static String literal(String text) {
        if (!text.contains("'")) return "'" + text + "'";
        return "concat('" + text.replace("'", "', \"'\", '") + "')";
    }
}
