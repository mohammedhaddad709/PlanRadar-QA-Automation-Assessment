package com.planradar.qa.systems.planradar.reporting;

import com.planradar.qa.systems.planradar.testdata.FieldDefinition;
import com.planradar.qa.systems.planradar.testdata.FormDefinition;
import com.planradar.qa.systems.planradar.testdata.TicketData;
import io.qameta.allure.Allure;

import java.util.Map;

/** Test data used by a test, attached as readable HTML tables. */
public final class Attachments {

    private static final String STYLE = "<style>body{font-family:sans-serif;font-size:13px}"
            + "table{border-collapse:collapse}th,td{border:1px solid #ccc;padding:4px 8px;text-align:left}"
            + "th{background:#f2f2f2}</style>";

    private Attachments() { }

    public static void formDefinition(FormDefinition form) {
        StringBuilder html = new StringBuilder(STYLE)
                .append("<p><b>Form:</b> ").append(escape(form.name()))
                .append("<br><b>Description:</b> ").append(escape(form.description())).append("</p>")
                .append("<table><tr><th>Field</th><th>Type</th><th>Mandatory</th><th>Default value</th><th>List (values)</th></tr>");
        for (FieldDefinition field : form.fields()) {
            html.append("<tr><td>").append(escape(field.name()))
                    .append("</td><td>").append(escape(field.type().label()))
                    .append("</td><td>").append(field.mandatory() ? "Yes" : "No")
                    .append("</td><td>").append(field.hasDefault() ? escape(field.defaultValue()) : "–")
                    .append("</td><td>").append(field.list() == null ? "–"
                            : escape(field.list().name() + " (" + String.join(", ", field.list().values()) + ")"))
                    .append("</td></tr>");
        }
        Allure.addAttachment("Test data – form under test", "text/html", html.append("</table>").toString(), ".html");
    }

    public static void ticketData(TicketData data) {
        StringBuilder html = new StringBuilder(STYLE).append("<table><tr><th>Field</th><th>Value entered</th></tr>");
        if (data.title() != null) row(html, "Title", data.title());
        for (Map.Entry<String, String> value : data.values().entrySet()) row(html, value.getKey(), value.getValue());
        Allure.addAttachment("Test data – ticket", "text/html", html.append("</table>").toString(), ".html");
    }

    private static void row(StringBuilder html, String field, String value) {
        html.append("<tr><td>").append(escape(field)).append("</td><td>")
                .append(value.isEmpty() ? "<i>(left empty)</i>" : escape(value)).append("</td></tr>");
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
