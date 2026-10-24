"""System catalog shared by suite generation, test documentation and the test dashboard.

Each entry names the Java packages (relative to BASE) whose test classes belong to that system.
One system is registered: PlanRadar. A second application is added as another entry with its own
packages under com.planradar.qa.systems.<key>, a systems/<key>.properties profile, test data and
a runner/environments.json entry.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = "com.planradar.qa.systems."
TEST_SOURCE_ROOT = ROOT / "src" / "test" / "java"
LEGACY_SUITE_ROOT = ROOT / "src" / "test" / "resources" / "suites"
GENERATED_SUITE_ROOT = ROOT / "suites"
CATALOG_DOC_ROOT = ROOT / "docs" / "test-catalog"

SYSTEMS = {
    "planradar": {
        "title": "PlanRadar Form Builder",
        "packages": ["planradar"],
        # No legacy ordering suite: modules run alphabetically, and every test resolves the setup it
        # needs through AssessmentFixture, so any class or method can also run alone.
        "order_from": [],
        # Block names of the generated suite (they become the Allure "suite" of each test).
        "modules": {
            "formbuilder": "Scenario 1 - Form creation and configuration",
            "mandatory": "Scenario 4 - Mandatory fields",
        },
        "e2e": [],
        "exclude": [],
    },
}


def fqcn(relative_class):
    """'planradar.formbuilder.FormBuilderTest' -> 'com.planradar.qa.systems.planradar.formbuilder.FormBuilderTest'."""
    return BASE + relative_class


def system_for_class(class_name):
    """Return the system key owning a fully qualified class name, or None."""
    for key, system in SYSTEMS.items():
        for package in system["packages"]:
            if class_name.startswith(BASE + package + "."):
                return key
    return None
