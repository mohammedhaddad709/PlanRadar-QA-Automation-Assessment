package com.planradar.qa.core.utils.dataReader;

import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Reads {@code test-data/<system>/<name>.json} from the test classpath; values are addressed with dotted paths
 * ({@code admin.email}).
 *
 * <p>Per-run overrides: {@code -Ddata.<system>.<name>.<path>} (e.g. {@code -Ddata.planradar.login-data.admin.email})
 * wins over the file value. Never put passwords in these files; credentials come from each system's
 * credential provider.
 */
public final class JsonReader {

    private static final String TEST_DATA_ROOT = "test-data/";

    private final String overridePrefix;
    private final Map<String, Object> data;

    public JsonReader(String system, String name) {
        this.overridePrefix = "data." + system + "." + name + ".";
        this.data = read(TEST_DATA_ROOT + system + "/" + name + ".json");
    }

    /** JVM property {@code data.<system>.<name>.<path>} for this path, or {@code null} when not set. */
    public String override(String path) {
        return System.getProperty(overridePrefix + path);
    }

    /** Override when set, otherwise the file value; "" when the path does not exist. */
    public String get(String path) {
        String override = override(path);
        return override != null ? override : fileValue(path);
    }

    private String fileValue(String path) {
        Object node = data;
        for (String segment : path.split("\\.")) {
            if (!(node instanceof Map<?, ?> map)) return "";
            node = map.get(segment);
        }
        return node == null || node instanceof Map<?, ?> ? "" : String.valueOf(node);
    }

    private static Map<String, Object> read(String resource) {
        try (InputStream stream = JsonReader.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Test data " + resource + " not found on the classpath");
            return new Json().toType(new String(stream.readAllBytes(), StandardCharsets.UTF_8), Json.MAP_TYPE);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + resource, ex);
        }
    }
}
