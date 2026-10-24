# Test Execution Report & Defect Log

Results of the automated run of the two automated scenarios (see [Automated_Test_Cases.md](Automated_Test_Cases.md)) and the status of the manual cases in [02_Test_Cases.md](02_Test_Cases.md). Status values: **Pass**, **Fail**, **Blocked**, **Not Run**. Evidence of the reported run is stored under `docs/evidence/`.

## 1. Run information

| Item | Value |
|---|---|
| Tester | Mohamed El-Hadad |
| Date | 24 Oct 2026 |
| Account / project | PlanRadar account `1618231` (admin) – project **QA Assessment - Form Builder** (id `1618243`) |
| Browser / OS | Chrome 155 (headless, 1920×1080) / Windows 11 |
| Stack | Java 21 · Selenium 4.49.0 · TestNG 7.12.0 · Allure 2.29.1 |
| Command | `mvn -B test -Dheadless=true` (suite `suites/planradar.xml`) |
| Run id | `20261024_052112` (form `QA_Assessment_Form_20261024_052112`), 248 s |
| Evidence | [run-20261024_052112_execution-summary.md](evidence/run-20261024_052112_execution-summary.md), screenshots in [evidence/screenshots/](evidence/screenshots/) |

The same suite was also executed before the move to the shared-core / system-adapter layout, headless (run id
`20261024_043442`) and headed (run id `20261024_041619`), with the same result: 9 / 9 passed.

## 2. Summary

| Scenario | Automated | Pass | Fail | Blocked | Manual cases (02_Test_Cases.md) |
|---|---|---|---|---|---|
| 1 – Form builder configuration | 4 (TC-001 … TC-004) | 4 | 0 | 0 | 35 – designed, 6 covered by automation |
| 2 – Field permissions | 0 | – | – | – | 14 – designed, Not Run (needs three role accounts) |
| 3 – Default values | 0 | – | – | – | 19 – designed, Not Run |
| 4 – Mandatory fields | 5 (TC-005 … TC-009) | 5 | 0 | 0 | 16 – designed, 6 covered by automation |
| **Total** | **9** | **9** | **0** | **0** | **84 designed** |

All eight environment dependencies (DEP-001 … DEP-008) were resolved by the run itself (login, Forms & Lists permission, global lists, form creation, project lookup, assignment, form and Location selectable in the new-ticket dialog).

## 3. Execution log (automated run `20261024_052112`)

| ID | Status | Actual result / notes | Evidence |
|---|---|---|---|
| TC-001 | Pass | Form created with Short Text, Long Text, Date, Decimal, two List and Checkbox fields; listed on the Forms page; reopened form shows name, description, predefined *Title* (not deletable) and all 7 custom fields. | `TC-001_PASSED_*.png` |
| TC-002 | Pass | Name, type, *Mandatory* checkbox, `*` canvas marker and default value of every field match the test data (Decimal normalised `125.50`). | `TC-002_PASSED_*.png` |
| TC-003 | Pass | Both global lists have 3 items; List fields reference the right list, offer the values in order; Inspection Status has no default, Inspection Category defaults to *Electrical*. | `TC-003_PASSED_*.png` |
| TC-004 | Pass | Form assigned to the project; per-project *Mandatory* flags and lists equal the form configuration; form appears in the project's forms and in the new-ticket form picker. | `TC-004_PASSED_*.png` |
| TC-005 | Pass | Empty mandatory *Inspector Name*: field marked `*`, validation message shown next to it, dialog stays open, ticket not created (search shows "0 ticket"). | `TC-005_PASSED_*.png` |
| TC-006 | Pass | Empty mandatory *Inspection Status* (no default): starts empty, marked `*`, validation message, dialog stays open, ticket not created. | `TC-006_PASSED_*.png` |
| TC-007 | Pass | All mandatory fields filled: dialog closes, ticket listed, Title / Inspector Name / Status persisted after reopening. | `TC-007_PASSED_*.png` |
| TC-008 | Pass | Clearing *Inspector Name* on a saved ticket: save blocked with a message next to the field; after *Discard changes* the original value is still there. | `TC-008_PASSED_*.png` |
| TC-009 | Pass | Project-level override (*Inspector Name* not mandatory): no `*`, ticket saved without the value; flag restored and verified afterwards. | `TC-009_PASSED_*.png` |

Manual cases of 02_Test_Cases.md not covered by the automation are **Not Run** in this report.

## 4. Observations (from the automation session)

- The tickets search is token based and matches several columns: searching for the full title `QA_Assessment TC-008 20261024_041619-4` returned **all** tickets of the project because "Assessment" also matches the project name. With the 20-row page size this makes a saved ticket hard to find by title. Logged as F9 in [03_Recommendations.md](03_Recommendations.md); the automation searches by the unique run suffix instead.
- `data-testid="tickets-list-container"` sits on a zero-height floating action bar rather than the table (F10); the "N tickets" counter has no test id. The automation waits for the counter by CSS class.
- The default value of a List field is loaded asynchronously in the properties panel; reading it immediately after opening the panel returns an empty value for a few hundred milliseconds.
- In the *Create new ticket* dialog the footer button `tickets_details_closenewticket` changes from **Close** to **Save and close** once a value is entered (F5).
- Assigning a form to a project is only persisted after **Apply** in the per-project mandatory table (F6).

## 5. Defect log

No functional defect was found in the automated scope: all behaviours under test (field configuration persistence, project assignment, mandatory-field enforcement on create/edit, project-level override) worked as expected. Usability and testability findings are tracked as F1 … F10 in [03_Recommendations.md](03_Recommendations.md).

| Bug ID | Title | Severity | Priority | Related TC | Status |
|---|---|---|---|---|---|
| – | No functional defect in the automated scope | – | – | – | – |

### Defect template
```
Bug ID:        BUG-00X
Title:         <short, specific: what is wrong, where>
Environment:   Chrome 1xx / Windows 11 / planradar.com account
Preconditions: <form, field config, user role>
Steps:
  1.
  2.
  3.
Expected:      <from the test case>
Actual:        <what happened>
Severity:      Critical | High | Medium | Low
Priority:      P1 | P2 | P3
Attachments:   screenshot / video / console log
Related TC:    TC-XXX
```

## 6. Issues encountered during testing (process)

| Issue | Impact | Workaround |
|---|---|---|
| Only one (admin) account available | Scenario 2 (field permissions per role) cannot be automated | Kept as manual cases; automation targets scenarios 1 and 4 which need only the admin |
| Tickets search returns the whole project for the full title; list paginated at 20 rows | Positive "ticket is listed" checks failed once the project exceeded 20 tickets | Search by the unique run suffix and wait until the result counter equals the rendered rows |
| Test data accumulates in the shared account (one form per run, 3 tickets per run) | Clutter in Forms and Tickets lists | Every artefact is prefixed `QA_Assessment` and carries the run id; can be bulk-deleted by prefix |
| Selenium 4.49 ships CDP 153, Chrome is 155 | Warning only (no CDP features used) | None needed |
