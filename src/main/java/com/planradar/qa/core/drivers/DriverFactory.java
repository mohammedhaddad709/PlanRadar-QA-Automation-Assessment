package com.planradar.qa.core.drivers;

import com.planradar.qa.core.utils.dataReader.PropertyReader;
import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.core.utils.RunMode;
import com.planradar.qa.core.utils.WaitManager;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;

/**
 * One fresh browser per test ({@code -Dbrowser=chrome|edge|firefox}; {@code -DexecutionType=Local|LocalHeadless|Remote},
 * {@code -Dheadless=true} is a shortcut for LocalHeadless). Local drivers are resolved by Selenium Manager, so no
 * driver binaries are kept in the repository; Remote runs on the Selenium Grid at {@code remoteHost:remotePort}.
 *
 * <p>PlanRadar needs a desktop-size viewport: below ~1000 px the form builder switches to a narrow layout that hides
 * the canvas and its Save button. Headless browsers open small by default, so every window is set to 1920×1080.
 */
public final class DriverFactory {

    private static final Dimension DESKTOP = new Dimension(1920, 1080);
    private static final List<String> CHROMIUM_ARGS = List.of(
            "--disable-notifications", "--disable-popup-blocking", "--disable-infobars", "--disable-search-engine-choice-screen");

    private static final int MIN_DESKTOP_WIDTH = 1280;

    private static volatile String browserVersion = "unknown";
    private static volatile String window = DESKTOP.width + "x" + DESKTOP.height;

    private DriverFactory() { }

    public static WebDriver create() {
        String browser = browser();
        boolean headless = headless();
        Capabilities options = switch (browser) {
            case "chrome" -> chromeOptions(headless);
            case "edge" -> edgeOptions(headless);
            case "firefox" -> firefoxOptions(headless);
            default -> throw new IllegalStateException("Unsupported browser '" + browser + "' (chrome, edge, firefox)");
        };
        WebDriver driver = remote() ? new RemoteWebDriver(gridUrl(), options) : switch (browser) {
            case "edge" -> new EdgeDriver((EdgeOptions) options);
            case "firefox" -> new FirefoxDriver((FirefoxOptions) options);
            default -> new ChromeDriver((ChromeOptions) options);
        };
        driver.manage().timeouts().pageLoadTimeout(WaitManager.pageLoadTimeout());
        sizeWindow(driver, headless);
        if (driver instanceof HasCapabilities capable) browserVersion = capable.getCapabilities().getBrowserVersion();
        LogsManager.info("Started " + describe() + " " + window + ", run mode " + RunMode.current().describe());
        return driver;
    }

    /** Demo mode maximizes a visible window for the audience, unless the screen is narrower than the desktop layout. */
    private static void sizeWindow(WebDriver driver, boolean headless) {
        if (RunMode.current().maximizeWindow() && !headless) {
            driver.manage().window().maximize();
            Dimension size = driver.manage().window().getSize();
            if (size.width < MIN_DESKTOP_WIDTH) driver.manage().window().setSize(DESKTOP);
        } else {
            driver.manage().window().setSize(DESKTOP);
        }
        Dimension size = driver.manage().window().getSize();
        window = size.width + "x" + size.height;
    }

    public static String browser() {
        return PropertyReader.get("browser", "chrome").toLowerCase();
    }

    /**
     * Local, LocalHeadless or Remote. Precedence: {@code -Dheadless} → {@code -DexecutionType} → {@code headless} file
     * default → the run mode's default ({@code fast} = headless) → {@code executionType} file default.
     */
    public static String executionType() {
        String configured = PropertyReader.get("executionType", "Local");
        String headless = System.getProperty("headless");
        if (headless != null && !headless.isBlank()) {
            if (Boolean.parseBoolean(headless.trim())) return "LocalHeadless";
            return "LocalHeadless".equalsIgnoreCase(configured) ? "Local" : configured;
        }
        String explicitType = System.getProperty("executionType");
        if (explicitType != null && !explicitType.isBlank()) return explicitType.trim();
        if (Boolean.parseBoolean(PropertyReader.get("headless", "false"))) return "LocalHeadless";
        if (RunMode.current().headlessByDefault() && "Local".equalsIgnoreCase(configured)) return "LocalHeadless";
        return configured;
    }

    public static boolean headless() {
        return "LocalHeadless".equalsIgnoreCase(executionType());
    }

    /** e.g. {@code chrome 141.0 (headless)}; the version is the one reported by the last browser started. */
    public static String describe() {
        return browser() + " " + browserVersion + (headless() ? " (headless)" : remote() ? " (grid)" : "");
    }

    public static String browserVersion() {
        return browserVersion;
    }

    /** Window size of the last browser started (1920×1080 unless demo mode maximized it). */
    public static String window() {
        return window;
    }

    private static boolean remote() {
        return "Remote".equalsIgnoreCase(executionType());
    }

    private static URL gridUrl() {
        String url = "http://" + PropertyReader.require("remoteHost") + ":" + PropertyReader.require("remotePort");
        try {
            return new URI(url).toURL();
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new IllegalStateException("Selenium Grid address is invalid: " + url, ex);
        }
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(CHROMIUM_ARGS);
        if (headless) options.addArguments("--headless=new");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setCapability(ChromeOptions.LOGGING_PREFS, browserConsole());
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments(CHROMIUM_ARGS);
        if (headless) options.addArguments("--headless=new");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setCapability(EdgeOptions.LOGGING_PREFS, browserConsole());
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) options.addArguments("-headless");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        return options;
    }

    /** Console warnings and errors, attached to the report when a test fails. */
    private static LoggingPreferences browserConsole() {
        LoggingPreferences preferences = new LoggingPreferences();
        preferences.enable(LogType.BROWSER, Level.WARNING);
        return preferences;
    }
}
