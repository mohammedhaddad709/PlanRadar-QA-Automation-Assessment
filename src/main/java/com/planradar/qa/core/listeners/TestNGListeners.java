package com.planradar.qa.core.listeners;

import com.planradar.qa.core.drivers.DriverFactory;
import com.planradar.qa.core.drivers.WebDriverProvider;
import com.planradar.qa.core.media.ScreenshotsManager;
import com.planradar.qa.core.utils.dataReader.PropertyReader;
import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.core.utils.OutputPaths;
import com.planradar.qa.core.utils.RunMode;
import com.planradar.qa.core.utils.report.AllureEnvironmentManager;
import com.planradar.qa.core.utils.report.Covers;
import com.planradar.qa.core.utils.report.FailureClassification;
import com.planradar.qa.core.validations.SoftAssertions;
import io.qameta.allure.Allure;
import org.openqa.selenium.BuildInfo;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.testng.IConfigurationListener;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared reporting lifecycle, registered through {@code META-INF/services/org.testng.ITestNGListener}; system-specific
 * content comes from the {@link SystemReportingHooks} of the system under test:
 * <ul>
 *   <li>before the run: fresh screenshots and Allure results directories ({@link OutputPaths}); Allure categories
 *       (triage classification);</li>
 *   <li>after every test method: soft assertions are evaluated, manual test cases covered are tagged, a final
 *       screenshot named by test-case id and status plus a run-context attachment are added; failures also get the
 *       page HTML and the browser console;</li>
 *   <li>after the run: Allure environment (incl. browser version) and the system's execution summary.</li>
 * </ul>
 * Progress lines {@code [PROGRESS] START|PASS|FAIL|SKIP class#method} let the embedded runner follow a run.
 */
public final class TestNGListeners implements IExecutionListener, IInvokedMethodListener, ITestListener, IConfigurationListener {

    private static final Set<ITestResult> REPORTED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final List<SystemReportingHooks> HOOKS = ServiceLoader.load(SystemReportingHooks.class).stream()
            .map(ServiceLoader.Provider::get).toList();

    @Override
    public void onExecutionStart() {
        recreate(OutputPaths.screenshots());
        recreate(OutputPaths.allureResults());
        AllureEnvironmentManager.writeCategories();
        Optional<SystemReportingHooks> active = activeHooks();
        LogsManager.info("Run started: system=" + PropertyReader.activeSystem() + ", run mode=" + RunMode.current().describe()
                + ", browser=" + DriverFactory.browser()
                + ", execution=" + DriverFactory.executionType() + active.map(h -> ", url=" + h.target()).orElse(""));
        active.ifPresent(SystemReportingHooks::onExecutionStart);
    }

    @Override
    public void onExecutionFinish() {
        Optional<SystemReportingHooks> active = activeHooks();
        Map<String, String> environment = new LinkedHashMap<>(active.map(SystemReportingHooks::environment).orElse(Map.of()));
        environment.put("System", PropertyReader.activeSystem());
        environment.put("Run.mode", RunMode.current().describe());
        environment.put("Browser", DriverFactory.describe());
        environment.put("Window", DriverFactory.window());
        environment.put("Selenium", new BuildInfo().getReleaseLabel());
        environment.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        environment.put("Java", System.getProperty("java.version"));
        AllureEnvironmentManager.writeEnvironment(environment);
        active.ifPresent(SystemReportingHooks::onExecutionFinish);
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod()) LogsManager.info("[PROGRESS] START " + qualifiedName(result));
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) return;
        SoftAssertions.assertAll(result);   // must run before the status is read and the screenshot is named
        report(result);
    }

    /** A test skipped by its setup (e.g. login BLOCKED) is never invoked, so it is reported from here. */
    @Override
    public void onTestSkipped(ITestResult result) {
        report(result);
    }

    private void report(ITestResult result) {
        synchronized (REPORTED) {
            if (!REPORTED.add(result)) return;
        }
        String id = testCaseId(result);
        String status = statusOf(result);
        Throwable error = result.getThrowable();
        String classification = result.getStatus() == ITestResult.SUCCESS ? "-" : FailureClassification.of(error).name();
        WebDriver driver = driverOf(result);
        Optional<SystemReportingHooks> hooks = hooksOf(result);
        boolean inAllureTest = Allure.getLifecycle().getCurrentTestCase().isPresent();

        if (inAllureTest) {
            Covers covers = result.getMethod().getConstructorOrMethod().getMethod().getAnnotation(Covers.class);
            if (covers != null) for (String manualCase : covers.value()) Allure.label("tag", manualCase);
            if (!"-".equals(classification)) Allure.label("tag", classification);
        }
        if (driver != null) ScreenshotsManager.capture(driver, id + "_" + status);
        if (inAllureTest) {
            Allure.addAttachment("Run context", "text/plain", context(result, id, status, classification, driver), ".txt");
            if (result.getStatus() != ITestResult.SUCCESS && driver != null) attachDiagnostics(driver);
        }
        if (result.getStatus() != ITestResult.SUCCESS) LogsManager.error(context(result, id, status, classification, driver));

        TestOutcome outcome = new TestOutcome(id, title(result), status, classification, error == null ? "" : error.getMessage());
        hooks.ifPresent(h -> h.onTestReported(outcome));
        LogsManager.info(id + " " + status + (error == null ? "" : " – " + error.getMessage()));
        LogsManager.info("[PROGRESS] " + progressStatus(result) + " " + qualifiedName(result));
    }

    @Override
    public void onConfigurationFailure(ITestResult result) {
        WebDriver driver = driverOf(result);
        if (driver != null) ScreenshotsManager.capture(driver, testCaseId(result) + "_SETUP_FAILURE");
        String classification = FailureClassification.of(result.getThrowable()).name();
        Allure.addAttachment("Setup failure context", "text/plain",
                context(result, testCaseId(result), "SETUP FAILURE", classification, driver), ".txt");
        if (driver != null) attachDiagnostics(driver);
    }

    // ---------- helpers ----------

    private static Optional<SystemReportingHooks> activeHooks() {
        String system = PropertyReader.activeSystem();
        return HOOKS.stream().filter(h -> h.system().equals(system)).findFirst();
    }

    private static Optional<SystemReportingHooks> hooksOf(ITestResult result) {
        return HOOKS.stream().filter(h -> h.ownsTest(result)).findFirst();
    }

    /** Descriptions start with the id: "TC-001 — Create a new form…". */
    static String testCaseId(ITestResult result) {
        String description = result.getMethod().getDescription();
        return description != null && description.matches("^[A-Z][A-Z0-9]*-\\d+ .*")
                ? description.split(" ")[0] : result.getMethod().getMethodName();
    }

    private static String title(ITestResult result) {
        String description = result.getMethod().getDescription();
        if (description == null) return result.getMethod().getMethodName();
        int dash = description.indexOf('—');
        return dash >= 0 ? description.substring(dash + 1).trim() : description;
    }

    private static String qualifiedName(ITestResult result) {
        return result.getTestClass().getName() + "#" + result.getMethod().getMethodName();
    }

    private static String statusOf(ITestResult result) {
        return switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE -> "FAILED";
            case ITestResult.SKIP -> isBlocked(result) ? "BLOCKED" : "SKIPPED";
            default -> "UNKNOWN";
        };
    }

    private static String progressStatus(ITestResult result) {
        return switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASS";
            case ITestResult.FAILURE -> "FAIL";
            default -> "SKIP";
        };
    }

    private static boolean isBlocked(ITestResult result) {
        return result.getThrowable() != null && String.valueOf(result.getThrowable().getMessage()).startsWith("BLOCKED");
    }

    private static WebDriver driverOf(ITestResult result) {
        return result.getInstance() instanceof WebDriverProvider provider ? provider.getWebDriver() : null;
    }

    private static String context(ITestResult result, String id, String status, String classification, WebDriver driver) {
        Throwable error = result.getThrowable();
        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("Test case", id);
        lines.put("Status", status);
        lines.put("Classification", classification);
        lines.put("Finished at", String.valueOf(LocalDateTime.now().withNano(0)));
        hooksOf(result).ifPresent(h -> lines.putAll(h.runContext()));
        lines.put("Run mode", RunMode.current().describe());
        lines.put("Browser", DriverFactory.describe());
        lines.put("Final URL", currentUrl(driver));
        lines.put("Error", error == null ? "none" : error.getClass().getName() + ": " + error.getMessage());
        return lines.entrySet().stream()
                .map(line -> "%-17s%s".formatted(line.getKey() + ":", line.getValue()))
                .collect(Collectors.joining("\n")) + "\n";
    }

    /** Page HTML and browser console at the moment of failure – the two things needed to triage a UI failure. */
    private static void attachDiagnostics(WebDriver driver) {
        try {
            Allure.addAttachment("Page HTML at failure", "text/html", driver.getPageSource(), ".html");
        } catch (RuntimeException browserGone) {
            LogsManager.warn("Page HTML not captured: " + browserGone.getMessage());
        }
        try {
            List<LogEntry> entries = driver.manage().logs().get(LogType.BROWSER).getAll();
            String console = entries.isEmpty() ? "(no warnings or errors)" : entries.stream()
                    .map(e -> e.getLevel() + "  " + e.getMessage()).collect(Collectors.joining("\n"));
            Allure.addAttachment("Browser console (warnings and errors)", "text/plain", console, ".txt");
        } catch (RuntimeException unsupported) {
            // Firefox has no console log API in WebDriver; the screenshot and HTML are still attached
        }
    }

    private static String currentUrl(WebDriver driver) {
        if (driver == null) return "n/a";
        try {
            return driver.getCurrentUrl();
        } catch (RuntimeException browserGone) {
            return "n/a (browser closed)";
        }
    }

    private static void recreate(Path directory) {
        try {
            if (Files.exists(directory)) {
                try (Stream<Path> paths = Files.walk(directory)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("Cannot delete " + path, ex);
                        }
                    });
                }
            }
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot prepare " + directory, ex);
        }
    }
}
