---
trigger: always_on
---

# Java Coding Standards (Java 21)

## Context
You are working on a strict Java 21 project. All code you generate must pass specific quality gates.

## Requirements

1. **Mandatory Testing**: 
   - NEVER generate a new Java class without a corresponding Test class (JUnit 5).
   - Tests must cover at least 80% of the logic branches.
   - If modifying existing code, update the tests first.

2. **Google Java Style**:
   - Use 2-space indentation.
   - Follow standard naming conventions (camelCase for variables, PascalCase for classes).
   - Always run the formatter before presenting code to the user.

3. **Javadoc**:
   - Every public class and public method MUST have a Javadoc block.
   - Use standard Google Style Javadoc (params, returns, throws).
   - Do not use lazy comments like "This is a method." Explain *why* it exists.
4. **Architecture**:
   - Use hexagonal + DDD strategy

## Behavior
If a build fails due to coverage or style, analyze the report, fix the specific violation, and retry automatically.