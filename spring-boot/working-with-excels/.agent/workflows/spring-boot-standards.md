---
description: 
---

# Spring Boot 3 Standards Workflow
Command: /audit-spring

## Step 1: Dependency Check
- Scan `pom.xml` for non-Spring libraries where a Starter exists.
    - `hibernate-validator` -> **Action:** Use `spring-boot-starter-validation`.
    - `retrofit` / `okhttp` -> **Action:** Use `WebClient` or `RestTemplate`.
    - `commons-lang` -> **Action:** Use `org.springframework.util.StringUtils`.

## Step 2: Import Check
- Scan for `javax.*` imports -> **Action:** Replace with `jakarta.*` (for Spring Boot 3+).

## Step 3: Injection & Architecture
- Scan for `@Autowired` on fields -> **Action:** Replace with Constructor Injection + `@RequiredArgsConstructor` (Lombok).
- Scan for `public class Controller` injecting `public class Service` (concrete) -> **Action:** Extract Interface for Service and inject that.

## Step 4: Testing Strategy Check
- Scan for `@SpringBootTest` in simple Unit Tests -> **Action:** Replace with `@WebMvcTest` (Controllers) or `@DataJpaTest` (Repos) or plain Mockito (Services).
