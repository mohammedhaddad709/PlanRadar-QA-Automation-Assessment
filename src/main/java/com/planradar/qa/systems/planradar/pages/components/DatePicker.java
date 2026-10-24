package com.planradar.qa.systems.planradar.pages.components;

import com.planradar.qa.core.utils.actions.ElementActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * PlanRadar's react-datepicker (form-builder default value, ticket date fields). Month and year are native
 * {@code <select>}s, then the day is clicked. The chosen value is read from {@code data-datevalue} (ISO), never from
 * the locale-formatted label.
 */
public final class DatePicker {

    private static final By POPUP = By.cssSelector(".react-datepicker");
    private static final By MONTH = By.cssSelector(".react-datepicker select.react-datepicker__month-select");
    private static final By YEAR = By.cssSelector(".react-datepicker select.react-datepicker__year-select");

    private final ElementActions actions;
    private final By trigger;
    private final By valueHolder;

    /**
     * @param trigger     element that opens the calendar
     * @param valueHolder the {@code [data-testid='date-input']} element carrying {@code data-datevalue}
     */
    public DatePicker(WebDriver driver, By trigger, By valueHolder) {
        this.actions = new ElementActions(driver);
        this.trigger = trigger;
        this.valueHolder = valueHolder;
    }

    public DatePicker pick(LocalDate date) {
        actions.click(trigger);
        actions.waitForVisibility(POPUP);
        actions.selectFromDropdown(YEAR, String.valueOf(date.getYear()));
        actions.selectFromDropdown(MONTH, date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        actions.click(By.cssSelector(".react-datepicker .react-datepicker__day--"
                + String.format("%03d", date.getDayOfMonth()) + ":not(.react-datepicker__day--outside-month)"));
        actions.waitForInvisibility(POPUP);
        return this;
    }

    /** ISO date (yyyy-MM-dd) of the stored value, or "" when no date is set. */
    public String isoValue() {
        if (actions.count(valueHolder) == 0) return "";   // an empty date shows "Choose Date" instead
        String raw = actions.getAttribute(valueHolder, "data-datevalue");
        return raw.length() >= 10 ? raw.substring(0, 10) : "";
    }
}
