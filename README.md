# Lesson 3.8: independent API test repair

| Material | Purpose |
|---|---|
| [test-cases.xlsx](test-cases.xlsx) | API-2008: case summary, preconditions, endpoints, numbered actions and expected results |
| [RideConflictTest.kt](api-tests/RideConflictTest.kt) | Prepared failing test: a conflicting order must preserve the active ride |

## Install

1. Use the existing [AI-for-Kotlin-practice](https://github.com/nebius-academy-templates/AI-for-Kotlin-practice) project with the Week 3 `test-repair` skill and hooks already installed.
2. Finish and close the repair from lesson 3.6.
3. Copy `api-tests/RideConflictTest.kt` from this package to `api-tests/src/test/kotlin/tests/RideConflictTest.kt` in the practice project.
4. Copy `test-cases.xlsx` to `test-cases/API-2008.xlsx` in the practice project. Create the `test-cases/` directory if needed.
5. Start `fake-api` using the project's existing instructions. Work from the practice project root.

## Repair

The supplied test compiles and intentionally fails. Read API-2008 on the workbook's `Case Summary` and `Steps` sheets and the matching operations in `fake-api/openapi.yaml`, then ask:

```text
Use the test-repair skill to fix tests.RideConflictTest.testConflictingOrderPreservesActiveRide.
Use API-2008 in test-cases/API-2008.xlsx and the API contract as the expected behavior.
```

During repair, change only `api-tests/src/test/kotlin/tests/RideConflictTest.kt`. Preserve the case, method name, Allure ID and all required checks. Keep the same token and sandbox session throughout the scenario; do not reset between steps. Do not change the backend, clients or shared setup, delete or disable the test, or hide failures with retries.

Follow lesson 3.8 for verification and submission. The final test must execute fresh and pass with matching JUnit and HTTP evidence. Keep generated evidence outside Git.
