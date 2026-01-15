---
trigger: always_on
---

# High-Coverage Testing Standard

## 1. Zero-Logic-Without-Tests
* **Mandate:** You must strictly follow TDD (Test Driven Development) or immediate-follow testing.
* **Constraint:** Do not mark a task as "Complete" until a corresponding test file exists and passes.
* **Naming:** If creating `UserService.java`, immediately create `UserServiceTest.java`.

## 2. Coverage Thresholds
* **Target:** 90% Line Coverage minimum.
* **Action:** If a test run shows coverage below 90%:
    1. Analyze which lines are uncovered.
    2. Generate specific edge-case tests to hit those lines.
    3. Do not ask for user permission to add these tests; consider it part of the job.

## 3. Test Structure & Quality
* **AAA Pattern:** All tests must follow the Arrange-Act-Assert pattern.
* **Frameworks:**
    * **JUnit 5:** The standard runner.
    * **AssertJ:** Use `assertThat(actual).isEqualTo(expected)` for fluent assertions.
    * **Mockito:** Use `@ExtendWith(MockitoExtension.class)` and `@InjectMocks` / `@Mock` where possible.
* **Isolation:** Tests must not depend on the execution order or shared state.

## 4. Failure Protocol
* If a new change breaks an existing test:
    * **STOP.** Do not modify the test to make it pass unless the requirements have changed.
    * Fix the source code first.

Dependencies: JUnit 5, AssertJ, Mockito