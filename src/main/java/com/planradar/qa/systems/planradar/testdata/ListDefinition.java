package com.planradar.qa.systems.planradar.testdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A global list (Forms and Lists › Lists) a List field is backed by. */
public final class ListDefinition {

    private final String name;
    private final List<String> values;

    private ListDefinition(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "list name");
        if (builder.values.isEmpty()) throw new IllegalStateException("List '" + name + "' needs at least one value");
        this.values = List.copyOf(builder.values);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return name;
    }

    public List<String> values() {
        return values;
    }

    @Override
    public String toString() {
        return name + " " + values;
    }

    public static final class Builder {
        private String name;
        private final List<String> values = new ArrayList<>();

        private Builder() { }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder values(String... values) {
            this.values.addAll(List.of(values));
            return this;
        }

        public ListDefinition build() {
            return new ListDefinition(this);
        }
    }
}
