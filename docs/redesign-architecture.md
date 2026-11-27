# SproutBank UX Redesign — Architecture & Design

Scope: parent-facing ergonomics + kid-facing intuition, plus the 10 reported defects.
All financial invariants from the existing ledger design (append-only, idempotent,
atomic, reconciliation) are preserved. Nothing in this redesign weakens them.

---

## Issue → Root cause → Fix map

| # | Report | Root cause in code | Fix (file) |
|---|--------|--------------------|------------|
| 1 | Kids not configurable; "Mia" hardcoded | `Db.seed()` inserts a fixed child; no add/edit/delete UI; `ChildEntity` has no fields for editing beyond avatar | `ChildEntity` + `Db` (no seed-on-first-run when onboarding exists, idempotent seed for upgrades), `ChildDao` (archive instead of delete — ledger is append-only), Family tab CRUD + `KidEditorDialog` (`ui/Parent.kt`) |
| 2 | Chores screen shows other dates' chores for selected date | `ChoresScreen` lists `state.chores` (all, timeless); `choreState()` ignores dates entirely; completions have no date concept | Add `dateMs` (local-day) to `ChoreCompletionEntity`; `choreState(chore, date)` and `choreStateOfDate(date)` become date-aware; empty-day UI states (`No chores on this day` / `Nothing done yet`) (`ui/Chores.kt`, `vm`) |
| 3 | No kid-friendly alert when request > balance | `RequestSpendDialog` submits blindly | Pre-send validation in dialog: friendly "You have $X — you're $Y short" with OK/Cancel; parent still sees request if child confirms anyway (`ui/RewardBank.kt`) |
| 4 | Parent switch buried in Profile tab; chore emoji is a text field; chores not multi-kid; edits not atomic | `onOpenParent` only in `ProfileScreen`; `AddChoreDialog` has `OutlinedTextField` for emoji and single `childId` | Top-right parent button on every kid screen via shared `KidTopBar` (`MainActivity`); PIN gate incl. setup on first entry (`ui/Parent.kt`); `CHORE_ICON_LIBRARY` (48 curated icons in categories) replacing the emoji text field; `ChoreEntity.assigneeIds` (JSON) + atomic replace via `withTransaction` (`data/*`) |
| 5 | Stale credit toast re-appears on every parent↔kid switch | `CelebrationBanner` shows `latestCelebration` — a DB query, **not** an event; re-subscribing on re-entry replays the latest row | Track `lastShownCelebrationId` in the VM; expose a **one-shot** `celebrationEvents: Channel<CelebrationEntity>`; banner consumes and dismisses; no re-subscription replay (`vm`, `MainActivity`) |
| 6 | Battery drain; waiting state spins forever | `Sprout` mascot runs an `infiniteRepeatable` even when SLEEPING/idle; `ChoreCard` PENDING shows an indeterminate `CircularProgressIndicator` (runs continuously) | Pause animation when not visible + default `animate=false` for decorative uses (`Sprout.kt`); static "Sent — waiting for parent ✓" chip instead of a spinner; only animate on active transitions (`ChoreCard.kt`) |
| 7 | Same generic reject presets for chore + spend | `DeclineReasonDialog` has one preset list; used for both | Two distinct preset sets: chores (effort/behavior) vs money (timing/budget); plus quick amounts for spend; separate titles and icons (`ui/Parent.kt`) |
| 8 | Reversal doesn't re-open the chore for the kid | `reverseTransaction` only appends the ledger row; completion stays APPROVED | If the reversed row was sourced by a completion, atomically reset that completion to PENDING→available (delete the decision, set status REOPEN with decidedAt null); `choreState` treats REOPEN as AVAILABLE for that date (`data/Repository.kt`) |
| 9 | No back-dated credit for forgotten chores | Only `recordCredit` at "now" | Parent calendar: past-30-day picker; `recordBackdatedCredit` posts CASH/POINTS with `timestampMs` = selected day noon, `kind="CREDIT"`, note "Backdated credit (chore title)"; optional link to a chore completion marked APPROVED same-day — all in one `withTransaction` so date and amounts agree (`data/Repository.kt`, `vm`, Parent UI) |
| 10 | Hardcoded chores on first run; no first-launch guidance | `Db.seed()` unconditional on empty DB | `settings.onboarded` flag; first launch routes to `OnboardingScreen` wizard: PIN → kids (name/avatar) → chores (icon library, values, **negative chores** = debits) → goal; only then ProfileSelect. Negative chore: `rewardCents` may be negative, charged as a DEBIT with its own kind (`PENALTY`) so statements read cleanly (`ui/Onboarding.kt`, `data/*`) |

---

## Data model changes (Room v5 → v6)

```
children:      + isArchived INTEGER NOT NULL DEFAULT 0     -- "delete" for UI, ledger keeps FK id
chores:        + assigneeIds TEXT NOT NULL DEFAULT '[]'    -- JSON [childId]; [] = everyone
               + cadence TEXT NOT NULL DEFAULT 'DAILY'      -- DAILY | WEEKLY (per-day reuse, issue 2/8)
chore_completions: + dateMs INTEGER NOT NULL DEFAULT 0     -- local-day epoch (per-date state)
                    + UNIQUE(choreId, childId, dateMs)       -- one submission per chore/kid/day
ledger:        (unchanged) — backdated rows just carry an older timestampMs
settings:      + onboarded INTEGER NOT NULL DEFAULT 0
```

Migration 5→6 recreates `chore_completions` (UNIQUE constraint can't be added by ALTER);
rows are copied, `dateMs` backfilled from `submittedAtMs`. Existing unique
pendingFor() guard is superseded by the table constraint.

**Per-day semantics (issues 2, 8):** a chore is "available on date D" iff
no completion exists for (chore, child, D) in {PENDING, APPROVED}. REOPEN makes
it available again for that same D. Weekly chores are available any day (single
submission per week is enforced by the same row-unique constraint on dateMs =
the submitted day; simplification kept intentionally).

**Ledger rule for REOPEN (issue 8):** reversal already appends a negative row
mirroring the credit. The completion is reset in the same transaction. Because
the ledger row is untouched (append-only), the audit trail remains: credit →
reversal → new credit when re-approved. Kid sees the chore again on D.

**Backdated credit (issue 9):** parent picks day D ∈ [today−30, today]; posts
CASH/POINTS rows with `timestampMs = D 12:00 local`; note prefixed
"Backdated:"; if tied to a chore, a completion row for (chore, child, D) with
status APPROVED and `submittedAtMs = D 12:00` is inserted atomically. Ledger
ordering by timestamp keeps the day's statement correct.

---

## UX redesign (iOS-quality polish, still kid-warm)

**Navigation.** Kid screens get a shared `KidTopBar`: greeting + avatar on the
left, **Parent Hub button pinned top-right** (shield icon, always visible on
all five tabs — issue 4). Tapping prompts PIN (or setup-if-none). Parent Hub
becomes full-screen with its own back. Bottom bar unchanged.

**Visual language.** Larger 28-32dp corner radii, softer elevation via the
existing ThemedCard, SF-style large titles, section headers with
`labelLarge` + tertiary color, chips for statuses, haptics via `tactileClickable`
everywhere. Status colors: success/pending/error are already theme-mapped.

**Chores (kid).** Horizontal day strip (7-day page, snap) + per-date sections:
`All done` / `Nothing planned` / chore list with per-date state. Waiting =
static badge "Sent ✓ Waiting for grown-up" (no spinner — issue 6).

**Bank (kid).** Hero card + big numbers; spend request dialog validates
against balance and shows a friendly alert with the shortfall before sending
(issue 3): "You have $4.20. This needs $6.00 — you're $1.80 short. Send anyway
and parent will see it?" with "Fix amount" and "Send anyway" actions.

**Family (parent).** Kid cards: avatar, name (editable dialog), balances,
archive. "Add Kid" flow with avatar library. Chore editor: icon library
(categories: Home, Self-care, Learning, Pets, Outdoor, Special), multi-kid
selection chips (default: everyone), money+points values, and a "Deduction"
toggle for negative chores (issue 4/10) with red styling and clear wording.

**Onboarding (issue 10).** First launch → 4 steps: welcome/PIN → kids →
chores (suggested library with icons and values, tap-to-include, custom add,
deductions) → done. Sets `onboarded=1`; DB seed of the demo child only runs
when upgrading a pre-onboarding install (version guard), never on fresh runs.

**Battery (issue 6).** All decorative infinite animations: mascot bounce runs
only when the composable is visible and reduced-animations is off; sleeping
mascot = static; waiting states = static chips; celebration = one-shot with
finite timeline; background Canvas is already static (single draw).

---

## Files changed (full sources delivered)

| File | Change |
|------|--------|
| `data/Entities.kt` | v6 schema: archived flag, assigneeIds, cadence, dateMs, unique index, onboarded |
| `data/Daos.kt` | kid CRUD+archive, chores per-assignee queries, per-date completion queries, settings |
| `data/Db.kt` | MIGRATION_5_6 (table recreate + backfill), seed only for legacy upgrades |
| `data/Repository.kt` | addKid/editKid/archiveKid; addChore/updateChore atomic; per-date completion submit; REOPEN on reversal; backdated credit; negative chore approval posts DEBIT |
| `vm/SproutViewModel.kt` | one-shot celebration events (fix #5), date-aware choreState APIs, kid CRUD passthrough, backdated ops, onboarding state |
| `MainActivity.kt` | KidTopBar with parent button top-right; onboarding routing; celebration as events |
| `ui/Onboarding.kt` (new) | 4-step wizard |
| `ui/Chores.kt` | date-aware strip + per-date lists + empty states |
| `ui/Home.kt` | uses KidTopBar, date-aware today list, static sleeping sprout |
| `ui/RewardBank.kt` | balance check + friendly alert in request dialog |
| `ui/Parent.kt` | family CRUD, chore icon library editor, distinct reject presets, backdated credit calendar, PIN-on-entry w/ setup |
| `ui/components/ChoreCard.kt` | static waiting badge; deduction styling |
| `ui/components/Sprout.kt` | visibility-gated animation |
| `ui/components/Celebration.kt` | one-shot event consumption (no replay) |
| `ui/components/Dialogs.kt` | kid editor dialog, icon library picker |

Financial invariants (idempotency keys, unique indices, transactions,
reconciler) are unchanged in spirit; new mutations run in `withTransaction`.
