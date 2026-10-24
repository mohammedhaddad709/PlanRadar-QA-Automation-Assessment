package com.planradar.qa.core.utils.actions;

import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.core.utils.RunMode;
import com.planradar.qa.core.utils.WaitManager;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.function.Function;

/**
 * Synchronised, retrying element interactions shared by all page objects. Every action waits (explicit, polling)
 * until the element is ready and retries on the transient Selenium exceptions a React UI produces while re-rendering
 * ({@link WaitManager}). No fixed sleeps for synchronisation: the only pause is the presentation pacing of
 * {@code run.mode=demo} ({@link #pace()}), applied after a user-visible action has already completed.
 */
public final class ElementActions {

    private final WebDriver driver;
    private final Duration timeout;
    private final RunMode mode;
    private WebElement highlighted;

    public ElementActions(WebDriver driver) {
        this.driver = driver;
        this.timeout = WaitManager.explicitTimeout();
        this.mode = RunMode.current();
    }

    // ---------- waiting ----------

    /** Explicit wait with the default timeout; {@code null}/{@code false} results keep polling. */
    public <T> T until(Function<WebDriver, T> condition) {
        return wait(timeout).until(condition);
    }

    public ElementActions waitForVisibility(By locator) {
        until(d -> d.findElement(locator).isDisplayed() ? Boolean.TRUE : null);
        return this;
    }

    /** Waits until no element matching the locator is displayed (removed or hidden). */
    public ElementActions waitForInvisibility(By locator) {
        until(d -> d.findElements(locator).stream().noneMatch(ElementActions::displayed));
        return this;
    }

    /** Non-failing visibility check with its own timeout, for optional UI (toasts, confirmations, messages). */
    public boolean isDisplayed(By locator, Duration within) {
        return awaitQuietly(within, d -> d.findElements(locator).stream().anyMatch(ElementActions::displayed));
    }

    /** Polls the condition for at most {@code within}; {@code false} on timeout instead of an exception. */
    public boolean awaitQuietly(Duration within, Function<WebDriver, Boolean> condition) {
        try {
            wait(within).until(condition);
            return true;
        } catch (TimeoutException notMet) {
            return false;
        }
    }

    // ---------- interacting ----------

    public ElementActions click(By locator) {
        until(d -> {
            WebElement element = ready(locator);
            highlight(element);
            element.click();
            LogsManager.info("Clicked " + locator);
            return true;
        });
        return pace();
    }

    /** JavaScript click for controls that are visually hidden by design (hover-only buttons, sr-only inputs). */
    public ElementActions clickJS(By locator) {
        until(d -> {
            WebElement element = scrollTo(d.findElement(locator));
            highlight(element);
            js().executeScript("arguments[0].click();", element);
            LogsManager.info("Clicked (JS) " + locator);
            return true;
        });
        return pace();
    }

    public ElementActions hover(By locator) {
        until(d -> {
            WebElement element = scrollTo(d.findElement(locator));
            highlight(element);
            new Actions(d).moveToElement(element).perform();
            return true;
        });
        return pace();
    }

    /**
     * Replaces the content of a React-controlled input/textarea. {@code WebElement.clear()} does not fire React's
     * onChange and the old value can come back, so the field is emptied through the keyboard like a user would.
     */
    public ElementActions type(By locator, String text) {
        until(d -> {
            WebElement element = ready(locator);
            highlight(element);
            element.click();
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            if (text != null && !text.isEmpty()) element.sendKeys(text);
            LogsManager.info("Typed into " + locator);
            return true;
        });
        return pace();
    }

    /** Empties a field through the keyboard and leaves it (Tab) so the application's validation runs. */
    public ElementActions clearWithKeyboard(By locator) {
        type(locator, "");
        return pressKeys(locator, Keys.TAB);
    }

    /** Key or key combination sent to an element, e.g. {@code pressKeys(input, Keys.CONTROL, Keys.ENTER)}. */
    public ElementActions pressKeys(By locator, CharSequence... keys) {
        until(d -> {
            d.findElement(locator).sendKeys(Keys.chord(keys));
            return true;
        });
        return this;
    }

    /** Sets a checkbox to the wanted state by clicking its visible target only when the state differs. */
    public ElementActions setChecked(By clickTarget, By checkboxInput, boolean checked) {
        if (isChecked(checkboxInput) != checked) {
            click(clickTarget);
            until(d -> isChecked(checkboxInput) == checked);
            LogsManager.info("Checkbox " + checkboxInput + " set to " + checked);
        }
        return this;
    }

    public ElementActions selectFromDropdown(By locator, String visibleText) {
        until(d -> {
            WebElement element = scrollTo(d.findElement(locator));
            highlight(element);
            new Select(element).selectByVisibleText(visibleText);
            LogsManager.info("Selected '" + visibleText + "' in " + locator);
            return true;
        });
        return pace();
    }

    // ---------- reading ----------

    /** Checked state of a native checkbox (even when sr-only) or of an ARIA checkbox/switch. */
    public boolean isChecked(By locator) {
        // boxed in an array because FluentWait treats a bare Boolean.FALSE as "not ready yet"
        return until(d -> {
            WebElement element = d.findElement(locator);
            String aria = element.getAttribute("aria-checked");
            return new boolean[]{aria != null ? Boolean.parseBoolean(aria) : element.isSelected()};
        })[0];
    }

    public boolean isEnabled(By locator) {
        return until(d -> new boolean[]{d.findElement(locator).isEnabled()})[0];
    }

    public int count(By locator) {
        return driver.findElements(locator).size();
    }

    /** Value attribute of an input/textarea; may legitimately be "". */
    public String getValue(By locator) {
        return getAttribute(locator, "value");
    }

    public String getAttribute(By locator, String attribute) {
        return until(d -> {
            String value = d.findElement(locator).getAttribute(attribute);
            return value == null ? "" : value;
        });
    }

    /** Visible text, waiting until it is non-empty (a label or counter that has not rendered yet is not "empty"). */
    public String getText(By locator) {
        return until(d -> {
            String text = scrollTo(d.findElement(locator)).getText().trim();
            return text.isEmpty() ? null : text;
        });
    }

    /** Visible text that may legitimately be empty. */
    public String getTextOrEmpty(By locator) {
        return until(d -> d.findElement(locator).getText().trim());
    }

    public WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    public JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // ---------- internals ----------

    /** Demo mode: outline the element about to be used so the audience sees where the action happens. */
    private void highlight(WebElement element) {
        if (!mode.highlight()) return;
        js().executeScript("arguments[0].style.outline='3px solid #ff3d00';arguments[0].style.outlineOffset='2px';", element);
        highlighted = element;
    }

    /**
     * The single pacing point of the framework: after a completed user-visible action, demo mode pauses for
     * {@code demo.step.delay.ms} and then removes the highlight. A no-op in normal and fast mode. Never used to wait
     * for the application – every action above is already synchronised by explicit waits.
     */
    private ElementActions pace() {
        long delay = mode.stepDelayMillis();
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        if (highlighted != null) {
            try {
                js().executeScript("arguments[0].style.outline='';arguments[0].style.outlineOffset='';", highlighted);
            } catch (RuntimeException goneAfterAction) {
                // the action re-rendered or removed the element; nothing left to un-highlight
            }
            highlighted = null;
        }
        return this;
    }

    private FluentWait<WebDriver> wait(Duration within) {
        return WaitManager.fluentWait(driver, within);
    }

    /** Element found, scrolled into the viewport, displayed and enabled; otherwise a transient exception (retry). */
    private WebElement ready(By locator) {
        WebElement element = scrollTo(driver.findElement(locator));
        if (!element.isDisplayed() || !element.isEnabled()) {
            throw new ElementNotInteractableException("Not ready: " + locator);
        }
        return element;
    }

    private WebElement scrollTo(WebElement element) {
        js().executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", element);
        return element;
    }

    private static boolean displayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (StaleElementReferenceException gone) {
            return false;
        }
    }
}
