---
description: Instructions to achieve high quality code standards with 80% coverage
---

1 Test Structure

Follow Arrange-Act-Assert (AAA) pattern
One assertion per test method (prefer single concept per test)
Use descriptive test method names that explain the scenario
Test method names should follow: methodName_scenario_expectedBehavior

2 Test Coverage

Aim for minimum 80% code coverage
100% coverage for domain logic
Test all public methods
Test edge cases and boundary conditions
Test error scenarios and exception handling

3 Test Types

Write unit tests for domain logic
Write integration tests for adapters
Use Testcontainers for database integration tests
Use @WebMvcTest for controller testing
Use @DataJpaTest for repository testing

4 Test Isolation

Tests must be independent and isolated
Do not share state between tests
Use @BeforeEach for test setup
Clean up resources in @AfterEach
Avoid test interdependencies

5 Mocking

Mock external dependencies
Do not mock domain objects
Use Mockito for mocking
Verify interactions on mocks only when behavior matters
Prefer real objects over mocks for value objects