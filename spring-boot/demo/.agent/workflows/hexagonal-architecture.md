---
description: Analyze project structure and apply hexagonal architecture
---

Steps:
- Analyze project
- Apply hexagonal + DDD strategy
- Test everything is working 

Hexagonal architecture example:
- adapters
-- inbound (group by purpose inside)
-- outbound (group by purpose inside)
- config (group by purpose inside)
- core
-- exceptions (group by purpose inside)
-- internal
--- domain (group by purpose inside)
--- services (group by purpose inside)
--- usecases (group by purpose inside)
--- value objects (group by purpose inside)
-- ports
--- inbound (group by purpose inside)
--- outbound (group by purpose inside)
