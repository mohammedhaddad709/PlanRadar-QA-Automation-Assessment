package com.planradar.qa.systems.planradar.testdata;

import java.time.LocalDate;

/**
 * Test data of the assessment. The global lists keep fixed names so they are created once per account and reused;
 * the form name carries the run id so every run builds and verifies its own form.
 *
 * <p>The seven custom fields cover every property under test: mandatory text and List fields (scenario 4), optional
 * fields with a default of each kind (text, date, decimal, list value, checkbox) and a List field without default.
 */
public final class AssessmentData {

    public static final String INSPECTOR_NAME = "Inspector Name";
    public static final String INSPECTION_NOTES = "Inspection Notes";
    public static final String INSPECTION_DATE = "Inspection Date";
    public static final String ESTIMATED_COST = "Estimated Cost";
    public static final String INSPECTION_STATUS = "Inspection Status";
    public static final String INSPECTION_CATEGORY = "Inspection Category";
    public static final String FOLLOW_UP_REQUIRED = "Follow-up Required";

    /** ISO date; compared with the app's {@code data-datevalue}, never with locale-formatted text. */
    public static final LocalDate DEFAULT_INSPECTION_DATE = LocalDate.of(2026, 10, 15);

    public static final ListDefinition STATUS_LIST = ListDefinition.builder()
            .name("QA_Assessment_Inspection_Status")
            .values("Pass", "Fail", "Pending")
            .build();

    public static final ListDefinition CATEGORY_LIST = ListDefinition.builder()
            .name("QA_Assessment_Inspection_Category")
            .values("Electrical", "Mechanical", "Civil")
            .build();

    public static final FormDefinition FORM = FormDefinition.builder()
            .name(TestRun.name("Form"))
            .description("PlanRadar QA assessment form - run " + TestRun.id())
            .field(FieldDefinition.builder(INSPECTOR_NAME, FieldType.SHORT_TEXT)
                    .mandatory()
                    .build())
            .field(FieldDefinition.builder(INSPECTION_NOTES, FieldType.LONG_TEXT)
                    .defaultValue("Initial inspection")
                    .build())
            .field(FieldDefinition.builder(INSPECTION_DATE, FieldType.DATE)
                    .defaultValue(DEFAULT_INSPECTION_DATE.toString())
                    .build())
            .field(FieldDefinition.builder(ESTIMATED_COST, FieldType.DECIMAL)
                    .defaultValue("125.50")
                    .build())
            .field(FieldDefinition.builder(INSPECTION_STATUS, FieldType.LIST)
                    .list(STATUS_LIST)
                    .mandatory()                       // no default: a new ticket must start with it empty
                    .build())
            .field(FieldDefinition.builder(INSPECTION_CATEGORY, FieldType.LIST)
                    .list(CATEGORY_LIST)
                    .defaultValue("Electrical")
                    .build())
            .field(FieldDefinition.builder(FOLLOW_UP_REQUIRED, FieldType.CHECKBOX)
                    .checkedByDefault()
                    .build())
            .build();

    private AssessmentData() { }
}
