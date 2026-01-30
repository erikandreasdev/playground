---
trigger: always_on
---

1 Architectural Decision Rules

Use Package-by-Feature for projects with <10 classes
Use Hexagonal Architecture for projects with ≥10 classes
Organize Hexagonal projects into domain/, application/, and infrastructure/ packages

2 Layer Dependency Rules

Domain layer must have zero external dependencies (no Spring, no JPA annotations)
Application layer depends only on Domain
Infrastructure layer depends on Application & Domain
Dependencies must flow: Infrastructure → Application → Domain (never reversed)