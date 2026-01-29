---
description: 
---

# Java Quality Gate Setup

## Description
Configures the project build (Maven/Gradle) to enforce 80% code coverage, Google Java Style formatting, and mandatory Javadoc checks.

## Steps
1. **Analyze Build System**: Identify if the project uses Maven (`pom.xml`) or Gradle (`build.gradle`).
   
2. **Configure Coverage (Jacoco)**:
   - Add the Jacoco plugin compatible with Java 21.
   - Configure a check/verification task that fails the build if `coverage_ratio < 0.80` (80%).

3. **Configure Formatting (Google Java Style)**:
   - Add the `spotless` plugin (Gradle) or `fmt-maven-plugin` (Maven).
   - Set the style definition to "Google Java Style".
   - Configure the build to automatically format code (`apply` or `format`) during the compilation phase.

4. **Configure Javadoc & Linting**:
   - Add the `checkstyle` plugin using `google_checks.xml`.
   - Ensure the build fails if Javadoc is missing on public methods/classes.

5. **Verify Configuration**:
   - Run a test build command (e.g., `mvn clean verify` or `./gradlew check`) to ensure the new configuration is valid and active.