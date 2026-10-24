# Test Plan – PlanRadar Form Builder

| Item | Value |
|---|---|
| Product | PlanRadar Web App – Forms & Lists › Forms (Form Builder) |
| Document version | 1.0 |
| Prepared by | QA Candidate |
| Related docs | [02_Test_Cases.md](02_Test_Cases.md), [03_Recommendations.md](03_Recommendations.md), [04_Test_Execution_Report.md](04_Test_Execution_Report.md) |

---

## 1. Objective
Verify that the PlanRadar form builder lets an in-house user with the **Forms and Lists** permission:

1. Create a form with different field types and configure it correctly.
2. Restrict field **view / edit permissions** per project role, and have tickets enforce them.
3. Set **default values** that are pre-filled when a new ticket is created from the form.
4. Mark fields as **mandatory** and block saving a ticket until they have a value.

## 2. Scope

### In scope
| Area | Details |
|---|---|
| Form CRUD | Create, rename, add description, save, preview, edit an existing form |
| Field types | Short text, Long text, Number, Decimal, Date, Time, Checkbox, List (single/multi), User, Attachment, Checklist, and predefined fields (Title, Status, Priority, Progress, Due date, Assignee) |
| Field management | Add (click & drag), reorder, duplicate, remove, groups, separators |
| Field properties | Mandatory, Default value, Default list, Multi-selection, Max number of attachments, Checklist options |
| Field permissions | "All roles can view and edit" toggle, *Visible To*, *Editable By*, subcontractor rules |
| Project assignment | Add form to an existing project; per-project override of mandatory / default list |
| Ticket behaviour | Default values pre-filled, mandatory validation, permission enforcement on ticket create/edit |

### Out of scope
- Mobile apps (iOS/Android) – forms can only be *edited* in the web app; ticket behaviour on mobile is listed as a recommended follow-up.
- Reports/exports, notifications, approval workflows, numbering sequences.
- Performance, load and security penetration testing.

## 3. Test approach
| Type | How |
|---|---|
| Functional (manual) | Test cases in [02_Test_Cases.md](02_Test_Cases.md), written with equivalence partitioning & boundary values for numeric/text fields |
| Negative | Empty mandatory fields, invalid inputs, forbidden actions for restricted roles |
| Role-based | The same form opened by users with different roles (see §5) |
| Regression (automated) | Scenarios 1 and 4: Java 21, Selenium 4, TestNG, Maven, Allure – Page Object + Flow + Builder; see `src/` and [Automated_Test_Cases.md](Automated_Test_Cases.md) |
| Exploratory | Time-boxed sessions (30 min) on drag & drop, conditional visibility and per-project overrides |

## 4. Test environment
| Item | Value |
|---|---|
| URL | https://www.planradar.com (demo / trial account) |
| Browsers | Chrome (latest) – primary; Firefox & Edge (latest) – smoke |
| OS | Windows 11 |
| Screen sizes | 1920×1080, 1366×768 |
| Automation stack | Java 21, Maven, Selenium WebDriver 4.49, TestNG 7.12, Allure 2.29, Chrome (Selenium Manager) |

## 5. Test users & roles
Field permissions are role-based, so at least three users are needed in the test project.

| User | Type | Project role | Purpose |
|---|---|---|---|
| U1 – Admin | In-house, *Forms and Lists* permission | Project Admin | Builds forms, full access |
| U2 – Editor | In-house | `QA Editor` (custom role) | Should edit only allowed fields |
| U3 – Viewer | In-house | `QA Viewer` (custom role) | Should only view allowed fields |
| U4 – Subcontractor | Subcontractor (free) | – (no role) | Subcontractor-specific restrictions |

> In a demo account, extra users can be invited with e-mail aliases (e.g. `name+editor@gmail.com`).

## 6. Test data
Form name: `QA Form Builder – <date>`

| # | Field | Type | Config under test |
|---|---|---|---|
| 1 | Title | Predefined | Always mandatory, default value "Inspection" |
| 2 | Status | Predefined | Default "Open" |
| 3 | Priority | Predefined | Default "High" |
| 4 | Inspector Name | Short text | Mandatory, default "John Doe" |
| 5 | Observations | Long text | Optional, default multi-line text |
| 6 | Floor Number | Number | Default `3`; boundaries: `-1`, `0`, very large |
| 7 | Area (m²) | Decimal | Default `12.5`; locale decimal separator |
| 8 | Inspection Date | Date | Mandatory, default date |
| 9 | Start Time | Time | Default `08:30` |
| 10 | Safety Confirmed | Checkbox | Default checked |
| 11 | Defect Category | List (single) | Default list + default value |
| 12 | Trades | List (multi) | Multi-selection + several default values |
| 13 | Responsible Person | User | Multi-selection on (no default allowed) |
| 14 | Site Photos | Attachment | Max attachments = 3 |
| 15 | Safety Checklist | Checklist | Pass/Fail/N/A, note mandatory for "Fail" |
| 16 | Internal Cost | Number | Permissions: visible & editable for Admin only |

## 7. Entry criteria
- Demo account created and reachable; at least one project exists.
- Test users U1–U4 invited and active; roles created.
- Test cases reviewed.

## 8. Exit criteria
- 100 % of High-priority test cases executed; ≥ 95 % of all test cases executed.
- No open Critical/High defects in the 4 scenarios in scope.
- Automated TestNG suite passes on Chrome.

## 9. Defect management
Each defect is logged with: ID, title, environment, steps, expected vs. actual, severity (Critical/High/Medium/Low), priority, screenshot/video. Template is in [04_Test_Execution_Report.md](04_Test_Execution_Report.md).

| Severity | Meaning |
|---|---|
| Critical | Data loss, permission bypass, cannot save forms/tickets |
| High | Config not applied (mandatory/default/permission ignored) |
| Medium | Wrong behaviour with a workaround |
| Low | UI/text/cosmetic |

## 10. Risks & mitigations
| Risk | Mitigation |
|---|---|
| Demo account limits (users, projects, trial expiry) | Create the test data early; re-use one project |
| Only one real e-mail → hard to test several roles | Use Gmail `+alias` addresses |
| UI changes break automation selectors | Page Object Model, role/text-based locators, one place to update |
| Global vs. per-project properties confuse expectations | Test cases state explicitly which level is under test |

## 11. Deliverables
1. This test plan.
2. Test cases with expected results – [02_Test_Cases.md](02_Test_Cases.md).
3. Execution report & defect log – [04_Test_Execution_Report.md](04_Test_Execution_Report.md).
4. Automated tests – `src/` + `suites/planradar.xml` (scenarios 1 and 4), described in [Automated_Test_Cases.md](Automated_Test_Cases.md).
5. Recommendations – [03_Recommendations.md](03_Recommendations.md).
