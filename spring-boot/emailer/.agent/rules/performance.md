---
trigger: always_on
---

1 General Performance Rules

Optimize after measuring (avoid premature optimization)
Profile before optimizing
Focus on algorithmic efficiency first
Document performance-critical sections

2 Database Performance

Use appropriate indexes
Avoid N+1 query problems
Use fetch joins for eager loading when necessary
Paginate large result sets
Use database-specific optimizations sparingly

3 Caching

Cache expensive computations
Cache at appropriate layers
Set appropriate cache expiration policies
Invalidate cache when data changes
Document caching strategy