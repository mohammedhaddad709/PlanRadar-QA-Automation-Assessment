package com.planradar.qa.systems.planradar.testdata;

import java.util.Objects;

/**
 * One custom field of the form under test. The builder rejects configurations PlanRadar does not support (a default
 * on a User field, a List field without a list, a list default that is not one of the list's values), so wrong test
 * data fails before a browser is started.
 */
public final class FieldDefinition {

    private final String name;
    private final FieldType type;
    private final boolean mandatory;
    private final String defaultValue;
    private final ListDefinition list;

    private FieldDefinition(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "field name");
        this.type = Objects.requireNonNull(builder.type, "field type");
        this.mandatory = builder.mandatory;
        this.defaultValue = builder.defaultValue;
        this.list = builder.list;
        if (defaultValue != null && !type.supportsDefaultValue()) {
            throw new IllegalStateException(type.label() + " fields do not support a default value: " + name);
        }
        if (type == FieldType.LIST && list == null) {
            throw new IllegalStateException("List field '" + name + "' needs a list");
        }
        if (list != null && defaultValue != null && !list.values().contains(defaultValue)) {
            throw new IllegalStateException("Default '" + defaultValue + "' is not a value of list " + list.name());
        }
    }

    public static Builder builder(String name, FieldType type) {
        return new Builder(name, type);
    }

    public String name() {
        return name;
    }

    public FieldType type() {
        return type;
    }

    public boolean mandatory() {
        return mandatory;
    }

    /** Text default, the option label for List fields, "true" for a checked Checkbox, {@code null} for none. */
    public String defaultValue() {
        return defaultValue;
    }

    public boolean hasDefault() {
        return defaultValue != null;
    }

    public ListDefinition list() {
        return list;
    }

    public static final class Builder {
        private final String name;
        private final FieldType type;
        private boolean mandatory;
        private String defaultValue;
        private ListDefinition list;

        private Builder(String name, FieldType type) {
            this.name = name;
            this.type = type;
        }

        public Builder mandatory() {
            this.mandatory = true;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder checkedByDefault() {
            this.defaultValue = "true";
            return this;
        }

        public Builder list(ListDefinition list) {
            this.list = list;
            return this;
        }

        public FieldDefinition build() {
            return new FieldDefinition(this);
        }
    }
}
