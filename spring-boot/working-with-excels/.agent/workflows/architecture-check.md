---
description: 
---

# Spring Boot 3.x + Java 21 Hexagonal Architecture + DDD Rules

## Package Structure
```
com.company.project.{bounded-context}
├── domain
│   ├── model (entities, value objects, aggregates)
│   ├── repository (ports - interfaces only)
│   └── service (domain services, use cases)
├── application
│   ├── port.in (input ports - use case interfaces)
│   ├── port.out (output ports - repository interfaces)
│   └── service (application services implementing use cases)
├── adapter
│   ├── in.web (controllers, REST adapters)
│   ├── in.event (message listeners)
│   ├── out.persistence (JPA implementations)
│   └── out.external (third-party integrations)
└── config (Spring configuration)
```

## Domain Layer (Core - No Dependencies)
- Use aggregates as consistency boundaries
- Entities must have identity, value objects are immutable
- Use Java 21 records for value objects
- Domain events for aggregate communication
- No Spring annotations in domain
- Business logic stays in domain entities/services

## Application Layer (Use Cases)
- One use case = one input port interface
- Commands and queries separated (CQRS-lite)
- Use case implementations orchestrate domain logic
- Return domain objects or specific DTOs
- Handle transactions at this layer (`@Transactional`)

## Adapters Layer
- **Input adapters**: Convert external requests to use case calls
- **Output adapters**: Implement repository ports with JPA/external APIs
- Use mappers (MapStruct preferred) between layers
- Controllers call use cases via input ports only
- Never expose domain entities directly via REST

## Port Definitions
- Input ports: Define what application does (interfaces in `application.port.in`)
- Output ports: Define what application needs (interfaces in `application.port.out`)
- Ports use domain objects, not infrastructure types

## Dependency Rules (Strict)
- Domain → No dependencies (pure Java)
- Application → Domain only
- Adapters → Application + Domain + Infrastructure
- **Direction**: Adapters → Application → Domain (never reverse)

## Java 21 Standards
- Records for value objects, DTOs, commands, queries
- Sealed interfaces for port hierarchies
- Pattern matching in adapter mapping logic
- Virtual threads for async use cases

## Naming Conventions
- Use cases: `{Verb}{Noun}UseCase` (e.g., `CreateUserUseCase`)
- Commands: `{Verb}{Noun}Command` (e.g., `CreateUserCommand`)
- Queries: `Get{Noun}Query` or `Find{Noun}Query`
- Domain events: `{Noun}{PastTense}Event` (e.g., `UserCreatedEvent`)
- Aggregates: Clear business names (e.g., `Order`, `Customer`)

## DDD Tactical Patterns
- Aggregate roots are transaction boundaries
- Repositories only for aggregate roots
- Use factory methods in aggregates for creation
- Value objects enforce invariants in constructors
- Domain services for cross-aggregate operations

## Testing Strategy
- Domain: Pure unit tests (no Spring)
- Application: Test use cases with mocked ports
- Adapters: Integration tests per adapter type
- Use ArchUnit to enforce architecture rules

## Anti-Patterns to Avoid
- No anemic domain models (rich behavior in entities)
- No infrastructure leakage into domain
- No direct entity-to-DTO mapping in domain
- No `@Entity` annotations in domain layer (use separate persistence models)

## Configuration
- Each bounded context can be independently deployable
- Shared kernel in separate package if needed
- Use Spring profiles for adapter implementations