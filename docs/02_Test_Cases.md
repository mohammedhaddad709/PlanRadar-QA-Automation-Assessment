# Test Cases – PlanRadar Form Builder

**Common precondition (applies to all cases):** U1 (in-house user with *Forms and Lists* permission) is logged in to the web app. Path to the builder: **Forms and Lists › Forms**.

Priority: **H** = High, **M** = Medium, **L** = Low. The **Auto** column marks cases whose behaviour is also covered by the automated suite (scenarios 1 and 4, TC-001…TC-009); the mapping is in [Automated_Test_Cases.md](Automated_Test_Cases.md).

---

## Scenario 1 – Create a form with different field types and configurations

| ID | Title | Steps | Expected result | Pri | Auto |
|---|---|---|---|---|---|
| TC-FB-01 | Create a form with a name only | 1. Click **+ Create a new form**. 2. Click **Enter form name**, type `QA Form 01`, click ✔. 3. Click **Save**. | Form is saved and shown in the forms list. The **Title** field is added automatically and cannot be removed. | H | ✅ |
| TC-FB-02 | Cannot save a form without a name | 1. Click **+ Create a new form**. 2. Leave the name empty. 3. Click **Save**. | Save is blocked and a validation message asks for a name. No form is created. | H | |
| TC-FB-03 | Name with spaces only | Enter `"   "` as the name, then save. | Treated as empty and blocked, same as TC-FB-02. | M | |
| TC-FB-04 | Long name / special characters | Enter a 255-character name, then `QA <b>Form</b> & "Test" – عربي`, and save each one. | Either saved correctly (no HTML rendered, Unicode correct) or a clear max-length message. No crash and no truncation without warning. | M | |
| TC-FB-05 | Duplicate form name | Create a second form with the same name as an existing one. | Behaviour is consistent: either blocked with a message or allowed. Record the result. | L | |
| TC-FB-06 | Add a form description | Click **Enter form description**, type text, click ✔, then **Save**. Re-open the form. | Description is saved and shown. | L | ✅ |
| TC-FB-07 | Add a Short text field | Under **Create a field**, click **Short text**. Rename it to `Inspector Name`. Save. | Field appears at the bottom of the form with the new name, and it is still there after saving and re-opening. | H | ✅ |
| TC-FB-08 | Add a Long text field | Same as TC-FB-07 with **Long text**, named `Observations`. | Field saved. In a ticket it accepts multi-line text. | H | |
| TC-FB-09 | Add a Number field + boundaries | Add a **Number** field named `Floor Number`. In a ticket, enter `0`, `-1`, `999999999`, `1.5` and `abc`. | Integers are accepted. A decimal value and letters are rejected or not typeable. Very large values are either accepted or rejected with a clear message. | H | |
| TC-FB-10 | Add a Decimal field + locale | Add a **Decimal** field. In a ticket, enter `12.5`, then switch the user language to German and enter `12,5`. | The separator follows the locale. The value is stored and shown correctly in both languages. | M | |
| TC-FB-11 | Add a Date field | Add a **Date** field named `Inspection Date`. | In a ticket a date picker opens. The selected date is saved and shown in the user's date format. | H | |
| TC-FB-12 | Add a Time field | Add a **Time** field. | A time picker in hours:minutes is shown and the value is saved. | M | |
| TC-FB-13 | Add a Checkbox field | Add a **Checkbox** field named `Safety Confirmed`. | It can be checked and unchecked in a ticket, and the state is saved. | H | |
| TC-FB-14 | Add a List field (single selection) | Add a **List** field and select a **Default list** (global list). Save. | Only values of the chosen list are offered in the ticket, and only one can be selected. | H | ✅ |
| TC-FB-15 | List field – multi-selection | Open the List field **Settings**, enable **Allow multi-selection**, then **Save**. | Several values can be selected in the ticket. | H | |
| TC-FB-16 | List field – disable values | In the list **Settings**, clear 2 values and save. | The disabled values are not offered in tickets. | M | |
| TC-FB-17 | Multi-level list | Assign a list with sub-items (up to 10 levels). | The hierarchy is shown and only valid paths can be selected. | M | |
| TC-FB-18 | Add a User field | Add a **User** field and enable **Multi-selection**. | Only project users are offered. Several users can be selected. User groups are not offered. | M | |
| TC-FB-19 | Add an Attachment field + limit | Add an **Attachment** field and set **Maximum number of attachments** = 3. | A 4th file is blocked with a message. Supported types (pdf, jpg, png, docx, zip…) are accepted. An unsupported type (e.g. `.exe`) is rejected. | H | |
| TC-FB-20 | Add a Checklist field | Add a **Checklist** field. Select **Pass, Fail, N/A**, enable notes, and set **Mandatory for** = Fail. | The ticket shows Pass/Fail/N/A with the chosen colours. Choosing *Fail* without a note blocks saving. | M | |
| TC-FB-21 | Predefined field only once | Add **Status**, then try to add **Status** again. | The second add is not possible (option disabled or hidden). | H | |
| TC-FB-22 | Title field cannot be removed | Hover over **Title**. | No remove icon / removal not possible. | H | ✅ |
| TC-FB-23 | Reorder fields (drag & drop) | Drag `Observations` above `Inspector Name` and save. | The new order is saved and used in the ticket. | M | |
| TC-FB-24 | Add a field by dragging to a position | Drag **Short text** between two existing fields. | The field is inserted at the drop position, not at the bottom. | M | |
| TC-FB-25 | Duplicate a custom field | Hover over a custom field and click **duplicate**. | A copy with the same type and properties is created. Predefined fields have no duplicate icon. | M | |
| TC-FB-26 | Remove a field with no data | Remove a custom field that is not used by any ticket, then save. | The field is removed. | M | |
| TC-FB-27 | Remove a custom field that has ticket data | Create a ticket with a value in `Inspector Name`, then try to remove the field. | Removal is blocked with an explanation. Ticket data is kept. | H | |
| TC-FB-28 | Group & separator | Add a **Group** with 2 fields and a **Separator** headline. | In the ticket the group can be collapsed/expanded and the separator headline is shown. Removing the group keeps its fields. | L | |
| TC-FB-29 | Rename predefined field | Click **Status** and try to change its name. | The name is read-only. | M | |
| TC-FB-30 | Preview form | Click **Preview form**. | The preview matches the configured fields, order and required markers (*). | M | |
| TC-FB-31 | Unsaved changes warning | Add a field, then leave the page without saving. | A "leave without saving?" warning appears, or the change is clearly lost. Record the result. | M | |
| TC-FB-32 | Form settings | Under **Form Settings**, turn off *Comments* and *Plan position*, then save. | Tickets of this form show no comments section and no plan-position option. | M | |
| TC-FB-33 | Add form to existing project | Go to the project, open **Forms**, add the new form, and save. | The form can be selected when creating a ticket in this project. A new form is **not** assigned to any project automatically. | H | ✅ |
| TC-FB-34 | Edit a form already used in a project | Add a new field to a form that already has tickets. | Existing tickets show the new field (empty). Old data is not changed. | H | |
| TC-FB-35 | Conditional visibility | Show `Observations` only when `Safety Confirmed` is checked. | In the ticket the field is hidden until the checkbox is checked. | M | |

## Scenario 2 – Field permissions per user role

**Preconditions:** the test project has roles `QA Editor` (U2) and `QA Viewer` (U3), plus a subcontractor U4. The form from Scenario 1 is assigned to the project.

| ID | Title | Steps | Expected result | Pri | Auto |
|---|---|---|---|---|---|
| TC-PERM-01 | Default permissions | Add a new field and open its **Field Permissions**. | **All roles can view and edit** is ON by default. | H | |
| TC-PERM-02 | New role inherits default access | With the toggle ON, create a new project role, give it to a user, and open a ticket as that user. | The new role can view and edit the field. | M | |
| TC-PERM-03 | Restrict view for a role | Turn the toggle OFF on `Internal Cost`. Remove `QA Viewer` from **Visible To**. Save. Open a ticket as U3. | U3 does not see `Internal Cost` in the ticket (web), in the ticket list columns, or in filters. | H | |
| TC-PERM-04 | View-only for a role | `QA Viewer` is in **Visible To** but not in **Editable By**. Open a ticket as U3. | The field is shown read-only. U3 cannot change it. | H | |
| TC-PERM-05 | Edit permission for a role | `QA Editor` is in both lists. Open a ticket as U2 and change the value. | The value can be changed and is saved. | H | |
| TC-PERM-06 | Edit implies view (auto-sync) | Add `QA Viewer` to **Editable By** while it is not in **Visible To**. | `QA Viewer` is added to **Visible To** automatically. | H | |
| TC-PERM-07 | Remove view removes edit | Remove `QA Editor` from **Visible To**. | `QA Editor` is removed from **Editable By** automatically. | H | |
| TC-PERM-08 | Subcontractor – hide field | Remove *Subcontractor* from **Visible To** for `Inspector Name`. Open a ticket as U4. | The field is hidden for U4. | H | |
| TC-PERM-09 | Subcontractor – edit not allowed on custom text | Try to add *Subcontractor* to **Editable By** for a Short text field. | Not possible. Subcontractors can only get edit rights on Status, Progress and Attachment fields. | M | |
| TC-PERM-10 | Subcontractor – attachment edit | Give *Subcontractor* edit rights on `Site Photos`. As U4, upload and delete a file. | Upload and delete work (documented behaviour). | M | |
| TC-PERM-11 | Hidden + mandatory conflict | `Inspector Name` is mandatory but hidden from `QA Editor`. U2 creates a ticket. | U2 is not blocked by a field they cannot see/edit, or gets a clear message. A deadlock (cannot save, cannot fill) is a defect. | H | |
| TC-PERM-12 | Permission change takes effect | While U3 has a ticket open, U1 removes U3's view permission. U3 reloads. | After reload the field is hidden. | M | |
| TC-PERM-13 | API / export does not leak hidden fields | As U3, export a ticket report (PDF/Excel). | Hidden fields are not in the export. | H | |
| TC-PERM-14 | Bulk permission view | Open the consolidated permissions view for the form and change several fields at once. | All changes are saved and match the per-field settings. | L | |

## Scenario 3 – Default values

**Preconditions:** the form from Scenario 1 is assigned to the project.

| ID | Title | Steps | Expected result | Pri | Auto |
|---|---|---|---|---|---|
| TC-DEF-01 | Short text default | Set default `John Doe` on `Inspector Name`. Save. Create a new ticket with the form. | The field is pre-filled with `John Doe`. | H | |
| TC-DEF-02 | Long text default (multi-line) | Set a 3-line default on `Observations`. | The ticket shows the same 3 lines, with line breaks kept. | M | |
| TC-DEF-03 | Number default | Default `3` on `Floor Number`. | Ticket shows `3`. | H | |
| TC-DEF-04 | Decimal default | Default `12.5`. | Ticket shows `12.5` (or `12,5` in a comma locale). | M | |
| TC-DEF-05 | Date default | Set a default date. | Ticket shows that date in the user's format. | H | |
| TC-DEF-06 | Time default | Default `08:30`. | Ticket shows `08:30`. | M | |
| TC-DEF-07 | Checkbox default checked | Default = checked on `Safety Confirmed`. | Checkbox is checked in a new ticket. | H | |
| TC-DEF-08 | Predefined defaults | Status = *Open*, Priority = *High*, Progress = 20 %, Title = `Inspection`. | New ticket is pre-filled with these values. | H | |
| TC-DEF-09 | List default (single) | Assign a default list, then choose one default value. | The value is pre-selected. Only values from the assigned list can be chosen as default. | H | |
| TC-DEF-10 | List default (multi) | Enable multi-selection and choose 2 default values. | Both are pre-selected. | M | |
| TC-DEF-11 | Multi-level list default | Choose an end value of a 3-level list. | Only end values can be chosen. The ticket shows the full path. | M | |
| TC-DEF-12 | Fields without default | Open properties of Due date, Assignee, User, Attachment, Checklist. | No **Default value** option is offered. | M | |
| TC-DEF-13 | Default can be changed by user | In a new ticket, change the default value of `Inspector Name` and save. | The new value is saved. The default does not override it. | H | |
| TC-DEF-14 | Default not applied to existing tickets | Create a ticket, then change the default value in the form. Open the old ticket. | The old ticket keeps its value. Only new tickets get the new default. | H | |
| TC-DEF-15 | Default is global (not per project) | Assign the form to 2 projects. Try to change the default value in one project's form settings. | Not possible per project (global property). Both projects use the same default. | M | |
| TC-DEF-16 | Default + mandatory | `Inspector Name` is mandatory with a default value. Create a ticket without touching it. | The ticket saves, because the default fills the mandatory field. | H | |
| TC-DEF-17 | Default respects field validation | Try to set default `abc` on a Number field, or a 300-character default on Short text. | Invalid defaults are rejected in the builder. | M | |
| TC-DEF-18 | Default for a hidden field | Default set on a field hidden for `QA Viewer`. U3 creates a ticket, then U1 opens it. | The default value is still stored on the ticket (or the documented behaviour). Record the result. | M | |
| TC-DEF-19 | Default on mobile app | Create a ticket from the form in the iOS/Android app. | Defaults are pre-filled the same as on web. | M | |

## Scenario 4 – Mandatory fields

| ID | Title | Steps | Expected result | Pri | Auto |
|---|---|---|---|---|---|
| TC-MAN-01 | Title is always mandatory | Create a ticket and leave **Title** empty. Click **Save**. | Saving is blocked and Title is highlighted. | H | |
| TC-MAN-02 | Mandatory Short text | Make `Inspector Name` mandatory (no default). Create a ticket, leave it empty, click Save. | Saving is blocked. The field is marked (* / red) with a clear message. The ticket is not created. | H | ✅ |
| TC-MAN-03 | Mandatory filled → saves | Same as TC-MAN-02, fill the field, click Save. | The ticket is saved. | H | ✅ |
| TC-MAN-04 | Whitespace-only value | Enter `"   "` in a mandatory text field. | Treated as empty and saving is blocked. | M | |
| TC-MAN-05 | Mandatory Date | Make `Inspection Date` mandatory and leave it empty. | Saving is blocked. | H | |
| TC-MAN-06 | Mandatory Checkbox | Make `Safety Confirmed` mandatory, leave it unchecked. | Record the documented behaviour (does unchecked count as a value?). It must be clear to the user. | M | |
| TC-MAN-07 | Mandatory List / User / Attachment | Make each mandatory and leave them empty. | Saving is blocked for each one. | H | ✅ |
| TC-MAN-08 | Required marker shown | Open a new ticket. | Every mandatory field shows a required marker (*). | M | ✅ |
| TC-MAN-09 | Several mandatory fields empty | Leave 3 mandatory fields empty and save. | All 3 are highlighted together, not one at a time. | M | |
| TC-MAN-10 | Edit existing ticket – clear a mandatory field | Open a saved ticket, clear a mandatory field, and save. | Saving is blocked. | H | ✅ |
| TC-MAN-11 | Mandatory added to a form with old tickets | Make a field mandatory after tickets exist. Edit an old ticket where it is empty. | The user must fill it before saving the edit (or documented behaviour). | M | |
| TC-MAN-12 | Per-project override | The form is in projects A and B. In project B's form settings, turn **Mandatory** off for `Inspector Name`. | Required in A, optional in B. | H | ✅ |
| TC-MAN-13 | Mandatory field hidden by condition | Mandatory field is hidden by conditional visibility. | A hidden mandatory field does not block saving. | H | |
| TC-MAN-14 | Checklist conditional mandatory | Checklist response *Fail* with a mandatory note. Select Fail and add no note. | Saving is blocked. Selecting *Pass* without a note saves. | M | |
| TC-MAN-15 | Mandatory on mobile / offline | Create a ticket offline in the mobile app with an empty mandatory field. | Blocked on the device, the same as on web. | M | |
| TC-MAN-16 | Shared field mandatory per form | A shared field is mandatory on the Shared fields page. Turn it off on one form. | Optional on that form only. | M | |

---

### Traceability
| Requirement (from task) | Test cases |
|---|---|
| Variety of field types & configurations accurately applied | TC-FB-01 … TC-FB-35 |
| Permissions per user role enforced | TC-PERM-01 … TC-PERM-14 |
| Default values populated on load | TC-DEF-01 … TC-DEF-19 |
| Mandatory fields enforced | TC-MAN-01 … TC-MAN-16 |
| Form added to existing project | TC-FB-33 |
