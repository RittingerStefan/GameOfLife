Implement a plausible concurrency issue and its correction in this Java cell
simulation's reproduction system. Read assignment.txt, run.txt, and src/*.java
first. The assignment requires two issue categories, one in eating and one in
reproduction:

1. Data race / data consistency: already implemented in eating.
2. Deadlock, livelock, or performance: your task, in reproduction.

Both faulty and corrected versions must be easy to run and verify. The mistake
must be something a developer could reasonably make while implementing actual
reproduction behavior. Do not add an otherwise useless lock solely to cause a
problem. In particular, a lock wrapped around Exchanger.exchange() without a
credible need for that lock was rejected as too contrived. Do not count the
previous mating tick bug as a performance issue; it was a timing correctness bug.

First inspect the current implementation and propose a concrete plausible
mistake, its real design motivation, the failure mechanism, and its correction.
Explain why it meets category 2, distinguishing a true deadlock or livelock from
timeouts or ordinary contention. Wait for agreement on that proposal before
implementing it.

After agreement, implement independent --reproduction=safe|<fault> switches and a
bounded --demo=reproduction scenario. Safe must remain the default. Use real Cell
threads and measurable evidence of failure and recovery. Scheduling hooks may
select a problematic interleaving, but must not fabricate the reported failure.
Ensure a faulty demo can be stopped or isolated without hanging the user's JVM.
Avoid fragile timing assertions or relying on random runs. Add focused checks and
document exact commands and expected output in run.txt.

Preserve the existing --eating=safe|race and --demo=eating implementations, their
tests, and the normal simulation. Eating uses an intentionally non-atomic
check-then-decrement versus a corrected atomic getAndUpdate. Its demo coordinates
two hungry Cell threads competing for one food unit. Preserve the successful
mating tick remainder wait and stop/join-before-summary behavior. Run the existing
eating checks as well as your new checks. Do not delegate to other agents unless
the user separately authorizes it.

