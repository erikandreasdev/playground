---
trigger: always_on
---

1 Constructor Injection

Always use constructor injection
Never use field injection (@Autowired on fields)
Avoid setter injection unless optional dependency
Use @RequiredArgsConstructor from Lombok

2 Dependency Declaration

Declare dependencies as final fields
Use interfaces for dependencies, not implementations
Inject ports (interfaces), not adapters (implementations)

3 Circular Dependencies

Avoid circular dependencies
Refactor design if circular dependencies occur
Use events or mediator pattern to break cycles