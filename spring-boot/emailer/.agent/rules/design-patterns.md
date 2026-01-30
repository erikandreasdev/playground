---
trigger: always_on
---

1 Composition & Inheritance

Prefer composition over inheritance
Do not use abstract classes for business logic
Use interfaces with default methods when appropriate
Use sealed interfaces for closed type hierarchies

2 Strategy Pattern

Use sealed interfaces for strategy implementations
Use switch expressions with pattern matching for strategy selection

3 State Pattern

Represent states as sealed types
State transitions must return new immutable instances
State transitions must be handled by Domain Services

4 Command Pattern

Use Records to represent Commands
Use ApplicationEventPublisher for decoupling where appropriate