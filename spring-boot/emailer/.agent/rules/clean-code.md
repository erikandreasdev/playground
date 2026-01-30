---
trigger: always_on
---

1 The Rule of Three

Extract duplicated logic after the 3rd repetition
Refactor methods with more than 3 parameters to use a Parameter Object (Record)

2 Naming Conventions

Do not use terms: Data, Info, Manager, Helper, Util
Use nouns for classes
Use verbs for methods
Use intention-revealing names

3 Boolean Parameters

Never use boolean flags in method signatures
Create two distinct, well-named methods instead of boolean parameters

4 Error Handling

Never return null
Never return magic numbers (-1, 0) to indicate errors
Use Optional<T> for single optional values
Return empty collections instead of null for lists
Throw exceptions or use Result<T> types for error conditions

5 Comments vs. Naming

Code should be self-documenting through names
If a method needs a comment to explain what it does, rename it
Use intention-revealing names over comments

6 Anemic Domain Models

Logic must reside in Domain Entities
Do not create Services that only call getters and setters
Move business logic into the Entity itself

7 Side-Effect Free Domain Logic

Domain logic should be side-effect free
Domain methods should be pure functions (no I/O, no external calls)