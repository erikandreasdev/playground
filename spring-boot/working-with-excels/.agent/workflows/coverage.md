# Coverage Audit Workflow
Command: /audit-tests

## Step 1: Run Coverage
- Run the test suite with the coverage flag: `mvn clean verify`.
- Ensure the build fails if coverage is below 90% (this is now enforced by policy, check reports if build fails).

## Step 2: Analyze Gaps
- If usage of `mvn clean verify` fails due to coverage, open `target/site/jacoco/index.html`.
- Sort by "Missed Instructions" or "Missed Branches".
- Identify the classes with coverage below 90%.

## Step 3: Patch Gaps
- **Target:** 90% Line Coverage minimum.
- **Action:**
    1. Select the class with the lowest coverage.
    2. Analyze which lines are uncovered.
    3. Generate specific edge-case tests to hit those lines (do NOT ask for permission).
    4. **CRITICAL:** If a test breaks, FIX THE SOURCE CODE. Do not lower the test expectations unless requirements changed.

## Step 4: Report
- Output a table showing "Before" and "After" coverage percentages.