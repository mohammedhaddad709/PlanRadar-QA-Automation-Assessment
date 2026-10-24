# PlanRadar Form Builder – execution summary

| Item | Value |
|---|---|
| Run id | 20261024_052112 |
| Finished | 2026-10-24T05:25:12 |
| Base URL | https://www.planradar.com/ |
| Project | QA Assessment - Form Builder |
| Form created by this run | QA_Assessment_Form_20261024_052112 |
| Browser | chrome 155.0.8059.80 (headless) |

## Dependencies

| ID | Requirement | Status | Detail |
|---|---|---|---|
| DEP-001 | Authentication with the configured credentials | PASSED | Logged in: https://www.planradar.com/dr/1618231 |
| DEP-002 | Permission to open Forms and Lists | PASSED | Forms and Lists page opened |
| DEP-003 | Global lists for the List fields exist | PASSED | QA_Assessment_Inspection_Status already existed; QA_Assessment_Inspection_Category already existed; |
| DEP-004 | Form under test exists | PASSED | QA_Assessment_Form_20261024_052112 created by TC-001 |
| DEP-005 | Assessment project exists | PASSED | QA Assessment - Form Builder (id 1618243) |
| DEP-006 | Form under test is assigned to the project | PASSED | QA_Assessment_Form_20261024_052112 assigned (TC-004) |
| DEP-007 | Form under test is selectable when creating a ticket | PASSED | QA_Assessment_Form_20261024_052112 selectable (TC-004) |
| DEP-008 | Configured ticket Location is selectable | PASSED | Location 'QA Testing Location' selectable |

## Test cases

| ID | Title | Status | Classification | Message |
|---|---|---|---|---|
| TC-001 | Create a new form with several field types | PASSED | - |  |
| TC-002 | Verify every field configuration is saved | PASSED | - |  |
| TC-003 | Verify List field configuration (list, values, default) | PASSED | - |  |
| TC-004 | Assign the form to the project and verify it is available for tickets | PASSED | - |  |
| TC-005 | Empty mandatory text field blocks saving | PASSED | - |  |
| TC-006 | Empty mandatory List field blocks saving | PASSED | - |  |
| TC-007 | Ticket with all mandatory fields filled is saved | PASSED | - |  |
| TC-008 | Clearing a mandatory field on an existing ticket is blocked | PASSED | - |  |
| TC-009 | A project-level Mandatory override is respected | PASSED | - |  |
