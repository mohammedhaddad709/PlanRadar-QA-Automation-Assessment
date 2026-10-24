package com.planradar.qa.systems.planradar.pages;

import com.planradar.qa.core.utils.actions.ElementActions;
import com.planradar.qa.systems.planradar.config.PlanRadarConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base of every PlanRadar page object. All interactions go through {@link ElementActions}; locators prefer
 * PlanRadar's own {@code data-testid}s, taken from the live DOM.
 */
public abstract class PlanRadarPage {

    private static final Pattern ACCOUNT_ID = Pattern.compile("/dr/(\\d+)");

    protected final WebDriver driver;
    protected final ElementActions actions;

    protected PlanRadarPage(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
    }

    protected static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    /** XPath string literal that also works when the text contains a quote. */
    protected static String literal(String text) {
        if (!text.contains("'")) return "'" + text + "'";
        return "concat('" + text.replace("'", "', \"'\", '") + "')";
    }

    /** PlanRadar builds test ids from labels with spaces replaced by underscores. */
    protected static String idPart(String label) {
        return label.replace(' ', '_');
    }

    /** Account id from the app URL after login, e.g. {@code https://www.planradar.com/dr/1618231/...}. */
    protected String accountId() {
        Matcher matcher = ACCOUNT_ID.matcher(driver.getCurrentUrl());
        if (!matcher.find()) {
            throw new IllegalStateException("Not inside the PlanRadar app (log in first): " + driver.getCurrentUrl());
        }
        return matcher.group(1);
    }

    /** Absolute app URL for this account, e.g. {@code appUrl("/ticket-types")}. */
    protected String appUrl(String path) {
        return PlanRadarConfig.baseUrl() + "dr/" + accountId() + path;
    }
}
