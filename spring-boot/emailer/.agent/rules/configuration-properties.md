---
trigger: always_on
---

1 Application Properties

Use application.yml over application.properties
Use profiles for environment-specific configuration
Externalize all configuration values
Never hardcode configuration in source code

2 Configuration Classes

Use @ConfigurationProperties for type-safe configuration
Validate configuration properties
Group related properties in configuration classes
Use records for immutable configuration

3 Secrets Management

Never commit secrets to version control
Use environment variables for sensitive data
Use Spring Cloud Config or external vaults for production
Document required configuration in README