package com.planradar.qa.systems.planradar.testdata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** What a test enters in the "Create new ticket" dialog. */
public final class TicketData {

    private final String title;
    private final Map<String, String> values;

    private TicketData(Builder builder) {
        this.title = builder.title;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(builder.values));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** {@code null} leaves Title untouched. */
    public String title() {
        return title;
    }

    /** Field name → value. List fields take the option label, Checkbox "true"/"false", Date ISO; "" clears a field. */
    public Map<String, String> values() {
        return values;
    }

    @Override
    public String toString() {
        Map<String, String> shown = new LinkedHashMap<>();
        if (title != null) shown.put("Title", title);
        shown.putAll(values);
        return shown.toString();
    }

    public static final class Builder {
        private String title;
        private final Map<String, String> values = new LinkedHashMap<>();

        private Builder() { }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder value(String fieldName, String value) {
            values.put(Objects.requireNonNull(fieldName), Objects.requireNonNull(value));
            return this;
        }

        public TicketData build() {
            return new TicketData(this);
        }
    }
}
