# What this product asks of ankka-reasoning

*As of 2026-10-20.* Pinned at 0.2.0, to be cut from `main` before building. Each request is a
feature the product needs and does not build itself, because the graph is ankka-reasoning's.

1. **Release 0.2.0 from `main`**: ankka 0.12.0, the process layer, ankka-contrib's sink; and the
   image pullable by this product's installation.
2. **Scoring and calibration** (named there as the next feature): Brier score and calibration per
   holder from `beliefs-at-resolution`, so that agent forecasters and, later, players are scored
   in one place. Until then `forecasts` scores players here.
3. **Assumptions as a layer**: an assumption a belief rests on, and what resolving it would do to
   the belief, so that "what would change your mind" is answered from records.
4. **List and search routes**: markets by status and category, questions by subject, evidence by
   source and date. Today records are read by id and lists need Cypher on Neo4j; the product's
   interface needs lists without a graph query.
5. **Price observations at volume**: one revision per observation is right; the product throttles
   to one per outcome per minute. A batch route for observations would let it write every trade.
6. **Erasure**: when ankka 042 ships, personal holders. Until then the product writes no person to
   the graph.
