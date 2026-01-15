---
description: 
---

# Architecture Health Check
Command: /check-arch

## Step 1: Scan Complexity
- Iterate through all top-level packages in `src/main/java`.
- Count the number of Java files in each package (recursively).

## Step 2: Evaluate & Report
- **Pass:** If package < 10 files AND uses MVC.
- **Pass:** If package >= 10 files AND uses Hexagonal (detect existence of `domain/` or `ports/` sub-packages).
- **FAIL:** If package >= 10 files BUT still uses flat MVC.

## Step 3: Refactoring Proposal
- For any "FAIL" package, generate a plan to migrate it to Hexagonal:
    1. Isolate the Domain entities.
    2. Extract Service logic into Use Case implementations.
    3. Create Ports (Interfaces) for the Repository layer.
    4. Move JPA Repositories to `infrastructure/adapters/output`.