# What this product asks of ankka

*As of 2026-10-20.* Collected here and copied to `ankka-brain/roadmap.md` when raised. Checked
against `ankka/specs/` first: several are specified and waiting on a release.

1. **Throughput and latency of one entity on the hot path.** A book is one event sourced entity
   per outcome; the product needs a stated bound on commands per second and p99 latency per
   entity, and a way to measure it (the exchange's performance suite will).
2. **Awaiting a workflow from an endpoint** (spec 048, merged in 0.12.0): used instead of polling
   for a deposit's outcome.
3. **Streaming view queries** (spec 047): the book's depth and the price stream to the interface.
4. **Cross-project access** (spec 040, in 0.12.0): the grants between `predictions` and
   `payments`.
5. **Personal data erasure** (spec 042, draft): forecasts, orders and positions as personal fields.
   Until it ships the product follows voice's pattern: personal fields sealed under a key per
   player, erased by destroying the key.
6. **A rate limit at the gateway**: an order endpoint on the internet needs one; today a service
   limits itself.
