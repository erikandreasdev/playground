---
trigger: always_on
---

1 Logging Strategy

Use SLF4J for logging abstraction
Use appropriate log levels (ERROR, WARN, INFO, DEBUG, TRACE)
Log business events at INFO level
Log technical details at DEBUG level
Log errors with stack traces at ERROR level

2 Logging Content

Include correlation IDs in logs
Log method entry/exit only at DEBUG level
Do not log sensitive information (passwords, tokens, PII)
Use structured logging where possible
Include relevant context in log messages

3 Performance

Use parameterized logging (avoid string concatenation)
Guard expensive debug statements with isDebugEnabled()
Do not log in tight loops
Avoid excessive logging