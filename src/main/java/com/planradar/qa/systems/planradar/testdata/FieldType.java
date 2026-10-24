package com.planradar.qa.systems.planradar.testdata;

/** Custom field types of the PlanRadar form builder ("Create a field" palette). */
public enum FieldType {
    SHORT_TEXT("Short Text", "shorttext", true),
    LONG_TEXT("Long Text", "longtext", true),
    CHECKBOX("Checkbox", "checkbox", true),
    TIME("Time", "time", true),
    DATE("Date", "date", true),
    LIST("List", "lists", true),
    NUMBER("Number", "number", true),
    DECIMAL("Decimal", "decimal", true),
    USER("User", "user", false),
    ATTACHMENT("Attachment", "attachment", false),
    CHECKLIST("Checklist", "checklist", false);

    private final String label;
    private final String builderKey;
    private final boolean supportsDefaultValue;

    FieldType(String label, String builderKey, boolean supportsDefaultValue) {
        this.label = label;
        this.builderKey = builderKey;
        this.supportsDefaultValue = supportsDefaultValue;
    }

    /** Label shown in the palette, e.g. "Short Text". */
    public String label() {
        return label;
    }

    /** Per "Edit Field Properties": User, Attachment and Checklist fields have no default value. */
    public boolean supportsDefaultValue() {
        return supportsDefaultValue;
    }

    /** Type key in the properties panel's test ids, e.g. {@code Setup_Forms_shorttext_Name}. */
    public String builderKey() {
        return builderKey;
    }

    /** data-testid of the palette tile, e.g. {@code ticket_type_form_field_Short_Text_add}. */
    public String paletteTestId() {
        return "ticket_type_form_field_" + label.replace(' ', '_') + "_add";
    }
}
