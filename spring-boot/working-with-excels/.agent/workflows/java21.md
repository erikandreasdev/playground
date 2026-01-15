---
description: 
---

# Java 21 Code Polish Workflow
Command: /polish

## Step 1: Forbidden Patterns Check
- Scan for `Optional.get()` -> **Action:** Replace with `.orElseThrow()` or `.ifPresent()`.
- Scan for `System.out.println` or `e.printStackTrace()` -> **Action:** Replace with SLF4J logging.
- Scan for String concatenation in logs `log.info("id: " + id)` -> **Action:** Replace with `log.info("id: {}", id)`.

## Step 2: Modern Java 21 Syntax
- Scan for `get(0)` / `get(size()-1)` -> **Action:** Replace with `getFirst()` / `getLast()`.
- Scan for `instanceof` with casts -> **Action:** Refactor to Pattern Matching for `instanceof` or `switch`.
- Scan for blocking I/O (e.g. `ExecutorService`) -> **Action:** Suggest `Executors.newVirtualThreadPerTaskExecutor()` if high concurrency.

## Step 3: Records & Text Blocks
- Convert simple data classes (DTOs) to `record`.
- Convert concatenated strings (SQL, JSON, HTML) to Text Blocks `"""`.

## Step 4: Cleanup
- Remove unused imports.
- Reformat code.