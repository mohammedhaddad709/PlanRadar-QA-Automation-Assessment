package com.planradar.qa.systems.planradar.testdata;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The form under test. The predefined Title field is added by PlanRadar itself and is not listed here. */
public final class FormDefinition {

    private final String name;
    private final String description;
    private final List<FieldDefinition> fields;

    private FormDefinition(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "form name");
        this.description = builder.description;
        this.fields = List.copyOf(builder.fields);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<FieldDefinition> fields() {
        return fields;
    }

    @Override
    public String toString() {
        return name + " (" + fields.size() + " custom fields)";
    }

    /** Global lists the List fields depend on. */
    public Set<ListDefinition> lists() {
        Set<ListDefinition> lists = new LinkedHashSet<>();
        fields.stream().filter(f -> f.list() != null).forEach(f -> lists.add(f.list()));
        return lists;
    }

    public static final class Builder {
        private String name;
        private String description;
        private final List<FieldDefinition> fields = new ArrayList<>();

        private Builder() { }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder field(FieldDefinition field) {
            if (fields.stream().anyMatch(f -> f.name().equals(field.name()))) {
                throw new IllegalStateException("Duplicate field name: " + field.name());
            }
            fields.add(field);
            return this;
        }

        public FormDefinition build() {
            if (fields.isEmpty()) throw new IllegalStateException("Form '" + name + "' has no custom fields");
            return new FormDefinition(this);
        }
    }
}
