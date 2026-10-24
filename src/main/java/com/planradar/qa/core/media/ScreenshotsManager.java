package com.planradar.qa.core.media;

import com.planradar.qa.core.utils.logs.LogsManager;
import com.planradar.qa.core.utils.OutputPaths;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Viewport screenshots saved under {@code <output.dir>/screenshots} and attached to the Allure report. */
public final class ScreenshotsManager {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("HHmmss");

    private ScreenshotsManager() { }

    /** @return the saved file, or {@code null} when the browser is already gone (never fails the test). */
    public static Path capture(WebDriver driver, String name) {
        if (!(driver instanceof TakesScreenshot camera)) return null;
        try {
            byte[] png = camera.getScreenshotAs(OutputType.BYTES);
            Path directory = Files.createDirectories(OutputPaths.screenshots());
            String safe = name.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path file = directory.resolve(safe + "_" + LocalDateTime.now().format(STAMP) + ".png");
            Files.write(file, png);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png");
            return file;
        } catch (Exception unavailable) {
            LogsManager.warn("Screenshot '" + name + "' unavailable: " + unavailable.getClass().getSimpleName());
            return null;
        }
    }
}
