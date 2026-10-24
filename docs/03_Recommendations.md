# Recommendations – Form Builder Improvements

These come from reading the documented behaviour and from designing the test cases. Each item says the problem, why it matters, and a proposed change. Add items from your own testing session at the end.

## A. Usability

| # | Problem | Why it matters | Recommendation |
|---|---|---|---|
| A1 | A new form is **not added to any project** automatically. Users must go to a different page (project › Forms) to use it. | Users build a form, then cannot find it when creating a ticket. It looks like a bug to them. | After the first **Save**, show a dialog: *"Add this form to projects?"* with a multi-select of projects. |
| A2 | Some properties are **global** (default value) and some are **per project** (mandatory, default list). The difference is hard to see. | Admins expect to change a default value per project and cannot. | Show a small badge next to each property: *Global* / *Can be changed per project*, with a tooltip. |
| A3 | Default value cannot be set for List until a **Default list** is chosen. | The user sees no option and does not know why. | Show the Default value control disabled with the hint *"Select a default list first"*. |
| A4 | Predefined fields can be added only once, and some fields have no default value option. | Missing options with no explanation look like bugs. | Show disabled items with a tooltip explaining why (e.g. *"Already on this form"*, *"This field type has no default value"*). |
| A5 | Form preview shows one user's view. | Admins cannot check how a restricted role will see the form. | Add **Preview as role…** to the preview, using the field permissions. |
| A6 | Leaving the builder with unsaved changes. | Work can be lost. | Auto-save drafts, or always show a *"You have unsaved changes"* dialog. |
| A7 | No undo in the builder. | Removing a field or group by mistake is expensive. | Add Undo/Redo (Ctrl+Z) for builder actions, at least within the session. |

## B. Validation & data quality

| # | Problem | Recommendation |
|---|---|---|
| B1 | Text and number fields have no configurable rules beyond mandatory. | Add optional **min/max length**, **min/max value**, and **regex/pattern** (e.g. e-mail, phone) per field. |
| B2 | A mandatory field can be hidden from a role, which can block that role from saving a ticket (see TC-PERM-11). | Warn in the builder when a field is *Mandatory* but not *Editable By* every role that can create tickets, or skip the check for those roles. |
| B3 | Making a field mandatory after tickets exist leaves old tickets "invalid". | Show how many existing tickets have the field empty and let the admin choose: *require on next edit* / *only for new tickets*. |
| B4 | Checklist "Mandatory for" linked tickets is silently skipped when the user has no permitted form (documented). | Show the user a warning instead of skipping silently. Show the admin a warning in the builder. |
| B5 | Default values are not checked against field rules (e.g. date in the past for "Inspection date"). | Allow dynamic defaults: **Today**, **Today + N days**, **Current user**. These are very common in inspections. |

## C. Permissions

| # | Problem | Recommendation |
|---|---|---|
| C1 | Permissions are set field by field. | Keep the consolidated view and add **role-based templates** ("Read-only role", "Field worker") that can be applied to many fields at once. |
| C2 | It is not clear in the ticket why a field is read-only. | Show a lock icon with *"You have view-only access to this field"*. |
| C3 | Permission changes are not visible in history. | Add an **audit log** for form changes (who changed which field/permission, when). Important for construction compliance. |

## D. Form management

| # | Recommendation |
|---|---|
| D1 | **Form versioning**: show which version of the form a ticket was created with, and compare versions. |
| D2 | **Import/Export** forms as JSON/Excel, to copy forms between accounts (demo → production). |
| D3 | **Field library search**: with many shared fields, add search/filter in the *Add a field* panel. |
| D4 | **Usage info**: on each form show "Used in N projects / N tickets" before delete or edit. |

## E. Testability (for QA/automation)

| # | Recommendation |
|---|---|
| E1 | Most builder and ticket controls already have `data-testid` attributes. Some important ones do not: the **Create a new form** button, rows in the forms and projects tables, and the items in the **Assign form** list. Add test ids there so automation does not depend on English text. Also fix the broken ids found while automating: the *Create Ticket* label renders `data-testid="[object Object]"`, and several ids contain spaces (`Setup_Forms_Short Text_Mandatory`). |
| E2 | Provide a public/test **API for forms** (create form, add field, assign to project) so test data can be set up fast and UI tests can focus on behaviour. |
| E3 | Provide a **sandbox reset** for demo accounts so tests always start from a known state. |

## F. Findings from my test session
| # | Observation | Severity | Suggested fix |
|---|---|---|---|
| F1 | The *Create Ticket* button's label has `data-testid="[object Object]"`. An object is converted to a string instead of the intended id. | Low | Pass the id string to the component. |
| F2 | When a ticket is saved with an empty mandatory field, the error toast only says "Error – cannot be empty" and does not name the field. On a long form the user must search for the red message. | Medium | Name the field(s) in the toast and scroll to or focus the first invalid field. |
| F3 | Leaving the builder with unsaved changes shows an "Unsaved changes" dialog (Close form / Cancel). This works as expected (TC-FB-31). | – | – |
| F4 | The form name input has `maxlength="150"`. Extra characters are cut silently, with no message (TC-FB-04). | Low | Show a character counter, as the field name already does (e.g. 10/500). |
| F5 | In the *Create new ticket* dialog the footer button `tickets_details_closenewticket` is labelled **Close** on an empty dialog but changes to **Save and close** as soon as anything is entered. A user who wants to leave clicks it and triggers a save attempt (and validation errors) instead. | Medium | Keep a separate *Close/Cancel* button next to *Save and close*; give each button its own test id. |
| F6 | Assigning a form to a project opens a per-project *Mandatory field* table with *Apply / Cancel*. After "+" the form already appears in the *Applied forms* list, but it is only saved after **Apply** ("Data successfully created"). The extra step is easy to miss. | Low | Make it clear the assignment is pending (e.g. a "Not applied yet" badge) until Apply is clicked. |
| F7 | The same button swap happens on an **existing** ticket: after any edit, `tickets_details_close` reads **Save and close**. When the change is invalid (e.g. a mandatory field cleared), clicking it only shows "You have not filled in all the required fields." and the user cannot leave through the footer – only the header ✕ offers "Discard changes". | Medium | Keep a footer *Cancel/Discard* button next to *Save and close*. |
| F8 | In the form picker of *Create new ticket*, each option shows the form name **plus its description**; with long descriptions the list gets hard to scan and the name is not searchable separately. | Low | Show the description as secondary text (tooltip/second line in a lighter style) and match the search on the name. |
| F9 | The tickets **search** matches every word of the query against several columns (title, project, location…). Searching for the exact ticket title `QA_Assessment TC-008 …` returned **all 23 tickets** of the project, because "Assessment" also matches the project name "QA Assessment - Form Builder"; only an unusual single token (e.g. the run suffix `20261024_041619-4`) narrows the list. Combined with the 20-row page size, a user cannot reliably find a ticket by pasting its title. | Medium | Rank or require matches on the **Title** first (phrase match), or offer a *Search in: Title / All columns* switch. |
| F10 | On the tickets list `data-testid="tickets-list-container"` is attached to a zero-height floating-action-bar `div`, not to the table. The visible table and the "N tickets" counter have no test ids. | Low | Move/add test ids on the table, its rows and the result counter so automation does not depend on CSS class names. |
