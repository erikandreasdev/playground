---
trigger: always_on
---

# Spring Boot 3.x & Architecture Standards

## 1. Project Foundation
* **Version:** strictly usage of Spring Boot 3.x (and consequently Java 21+).
* **Build Tool:** Maven (`pom.xml`) is mandatory. Gradle is forbidden.
* **Upgrades:** When modifying legacy code, automatically suggest upgrading deprecated javax.* imports to jakarta.*.

## 2. Dependency Selection Protocol
* **"Spring First" Policy:** Before adding an external library, verify if a Spring Boot Starter exists.
    * *Example:* Use `spring-boot-starter-validation` instead of raw Hibernate Validator.
    * *Example:* Use `RestTemplate` or `WebClient` instead of `OkHttp` or `Retrofit` unless strictly necessary.
    * *Example:* Use Spring's `StringUtils` instead of `Apache Commons Lang`.

## 3. Dynamic Architecture Strategy
* **Context:** Assess the complexity of the specific package/module you are working on.
* **Scenario A: Simple Module (< 10 files)**
    * **Pattern:** Layered MVC.
    * **Structure:** `Web (Controller) -> Service -> Data (Repository)`.
    * **Goal:** Speed and simplicity.
* **Scenario B: Complex Module (>= 10 files)**
    * **Pattern:** Hexagonal Architecture (Ports & Adapters) + DDD.
    * **Structure:**
        * **Domain:** Entities, Value Objects, Domain Events (No Spring dependencies).
        * **Application:** Input/Output Ports (Interfaces), Use Cases.
        * **Infrastructure:** Adapters (Rest Controllers, JPA Repositories) implementing the Ports.
    * **Trigger:** If you add the 11th file to an MVC package, stop and propose a refactor to Hexagonal.

## 4. Decoupling & Cohesion
* **Interfaces First:** Services must define interfaces. Controllers inject the interface, not the concrete class.
* **Event-Driven:** If Component A needs to trigger Component B (and they are in different domains), use `ApplicationEventPublisher`. Do not inject Service B into Service A.
* **Lombok:** Use `@RequiredArgsConstructor` for constructor injection to keep code clean.

## 5. Testing Strategy
* **Slices over Context:** Avoid `@SpringBootTest` for every test. It is slow.
    * Use `@WebMvcTest` for Controllers.
    * Use `@DataJpaTest` for Repositories.
    * Use generic JUnit 5 + Mockito for Services (fastest).
* **Integration Tests:** Use `@SpringBootTest` only when testing full flows or configuration wiring.
* **Testcontainers:** Mandatory for testing real persistence or external infrastructure (Redis, Kafka).

## 6. Observability
* **Actuator:** Ensure `spring-boot-starter-actuator` is present in production-grade services.
* **Health Checks:** Implement custom `HealthIndicator` for critical dependencies (e.g. database connectivity, external APIs).