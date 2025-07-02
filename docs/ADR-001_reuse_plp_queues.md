# ADR: Reuse Existing SQS Queues for Communication with Curious

**Status:** Accepted  
**Date:** 2025-07-02

---

## Context

The product **Curious** requires integration with existing systems via asynchronous messaging. There are already **AWS SQS queues** established for a previous integration with the **Learning and Work Progress** client. These queues are functioning, monitored, and integrated into our infrastructure.

Creating new queues for Curious would increase operational overhead, add configuration complexity particularly with regard to certificate management, and potentially duplicate monitoring .

---

## Decision

We decided to **reuse the existing SQS queues** that were initially created for Learning and Work Progress to also support communication with Curious by Support for additional needs.

This includes:

- Publishing messages from Curious to the same queue endpoints
- Using the existing IAM roles, DLQs (if applicable), and monitoring infrastructure
- Making Curious owners Meganexus aware of this decision so that they are aware of different message types being added to the queue.

---

## Consequences

### Positive:
- Reduces setup time and avoids duplicating infrastructure
- Leverages existing monitoring, alerting, and security policies
- Simplifies maintenance and resource management

### Negative:
- Introduces coupling between multiple products (Learning and Work Progress,  Support for additional needs AND Curious)
- Requires careful message types to avoid conflicts
- Any future changes to the queues will affect both client products

We acknowledge the tighter coupling, but for now, it is an acceptable trade-off for faster delivery and reduced complexity.

---

## Alternatives Considered

- **Create new SQS queues for Curious**  
  This would isolate the systems but:
  - increase setup
  - monitoring overhead
  - configuration time
  - make certificate management more complex