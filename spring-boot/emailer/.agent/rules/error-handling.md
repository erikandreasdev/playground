---
trigger: always_on
---

1 Exception Strategy

Use unchecked exceptions for programming errors
Use checked exceptions for recoverable business errors (sparingly)
Create custom domain exceptions
Do not catch generic Exception
Always log exceptions with context

2 Exception Hierarchy

Create a base domain exception class
Extend base exception for specific domain errors
Include meaningful error messages
Include relevant context in exceptions

3 Exception Handling in Controllers

Use @ExceptionHandler for controller-level handling
Use @ControllerAdvice for global exception handling
Return appropriate HTTP status codes
Return structured error responses

4 Validation Exceptions

Throw validation exceptions early (fail fast)
Use meaningful exception messages
Include field names and invalid values in messages
Group related validations when appropriate