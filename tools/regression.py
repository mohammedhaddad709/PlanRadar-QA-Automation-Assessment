"""Regression (known-bug check) classification used by the generated suites (and so by the test dashboard).

A class is a regression check when:
  * it is listed in KNOWN for its system (reviewed, reproducible bug checks), or
  * one of its enabled test methods carries Allure @Issue("...") linking a reported defect.

Classification depends only on the sources, so generated files stay reproducible. It only orders/selects
tests: it never changes pass/fail.
"""

from systems import fqcn

# Reviewed known-bug check classes per system (relative to systems.BASE). Empty until a confirmed
# PlanRadar defect gets a dedicated check.
KNOWN = {
    "planradar": [],
}


def regression_classes(system, entries):
    known = {fqcn(name) for name in KNOWN.get(system, [])}
    return [entry["class"] for entry in entries
            if entry["class"] in known or any(m["issue"] and m["enabled"] for m in entry["methods"])]
