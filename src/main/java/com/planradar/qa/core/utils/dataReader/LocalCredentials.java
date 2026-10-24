package com.planradar.qa.core.utils.dataReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Secret lookup shared by every system: JVM {@code -D<NAME>} → environment variable {@code NAME} → the git-ignored
 * file {@code config/credentials.local.properties} ({@code -Dcredentials.file} overrides the path).
 * Values are never logged.
 */
public final class LocalCredentials {

    public static final String DEFAULT_FILE = "config/credentials.local.properties";

    private LocalCredentials() { }

    /** @return the first non-blank value, or {@code null} */
    public static String resolve(String name) {
        String value = System.getProperty(name);
        if (usable(value)) return value;
        value = System.getenv(name);
        if (usable(value)) return value;
        value = localFile().getProperty(name);
        return usable(value) ? value : null;
    }

    public static boolean usable(String value) {
        return value != null && !value.isBlank();
    }

    private static Properties localFile() {
        Properties properties = new Properties();
        Path file = Path.of(System.getProperty("credentials.file", DEFAULT_FILE)).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) return properties;
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + file, ex);
        }
        return properties;
    }
}
