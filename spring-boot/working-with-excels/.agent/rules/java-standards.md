---
trigger: always_on
---

# Modern Java Standards (Java 21 Edition)

## 1. Java 21 Syntax & Features
* **Sequenced Collections:**
    * **Rule:** Use `getFirst()`, `getLast()`, `removeFirst()`, `removeLast()` instead of accessing index `0` or `size()-1`.
    * *Why:* It is cleaner and safer for all ordered collections (List, Deque, SortedSet).
* **Pattern Matching & Switch:**
    * **Rule:** Use Pattern Matching for `switch` to handle type casting automatically.
    * *Example:* `case User u -> u.getName()` instead of checking `instanceof` and casting.
    * **Rule:** Use **Record Patterns** to deconstruct data directly in the case label.
* **Virtual Threads (Project Loom):**
    * **Context:** High-concurrency I/O tasks (e.g., sending emails, parallel API calls).
    * **Rule:** Use `Executors.newVirtualThreadPerTaskExecutor()` instead of standard thread pools for blocking I/O operations.

## 2. Records & Data Structures
* **Records:** Mandatory for all DTOs, Event objects, and Configuration holders.
* **Validation:** Put validation logic inside the **Compact Constructor** of the record.
    * *Example:* `public record User(String email) { public User { if (email == null) throw ... } }`
* **Immutability:** Default to immutable collections (`List.of()`, `Map.of()`) unless mutation is strictly required.

## 3. Readability & Flow
* **Unnamed Variables (Draft):** If a variable in a lambda or catch block is unused, use `_` (underscore) to signify intent (if preview features are enabled).
* **Guard Clauses:** Avoid deep nesting. Return early.
* **Text Blocks:** Use `"""` for JSON, SQL, or HTML strings to avoid messy escaping.

## 4. Error Handling & Logging
* **Exceptions:** Throw specific, unchecked exceptions.
* **Logging:**
    * **Rule:** Use SLF4J with parameterized logging.
    * *Ban:* String concatenation in log statements (e.g., `log.info("User " + id)`).

## 5. Modern Optional Usage
* **Rule:** Avoid `Optional.get()` entirely. It defeats the purpose.
* **Preferred:**
    * `Optional.map()`, `Optional.filter()`, `Optional.orElseThrow()`.
    * Use `ifPresentOrElse(val -> ..., () -> ...)` for side effects.
* **Context:** Use Optional only for *return types*. Do not use Optional for method parameters or class fields (creates unnecessary wrappers).

## 6. Stream API Best Practices
* **Readability:** Break Stream chains onto new lines for each intermediate operation (`.filter()`, `.map()`).
* **Debugging:** Prefer `toList()` (Java 16+) over `collect(Collectors.toList())`.
* **Complexity:** If a Stream chain is longer than 5 operations, extract logic into a private helper method or loop to preserve readability.