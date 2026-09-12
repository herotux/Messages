# Messages — Project Roadmap

> Living project tracker. **GitHub Issues are now the source of truth for active work.** This file mirrors the phase structure and key decisions so the repository remains self-documenting.

## Current state — 2026-09-12

- [x] `main` stabilization baseline exists
- [x] One-shot theme cleanup workflow removed
- [x] M3 theme migration committed on `main`
- [x] Branch inventory completed
- [x] Feature divergence audit completed
- [x] GitHub Issues enabled and phase trackers created
- [x] Room / Entity / DAO / migration batch completed
- [x] Gradle / dependency / toolchain audit completed
- [x] CI workflow hardening batch completed
- [ ] Regression audit: Theme / Room / DAO / bank SMS / crash-prone paths
- [ ] Validate final CI run on `main`
- [ ] Reconcile long-lived feature branches before merging

### GitHub issue trackers

| Phase / workstream | Issue | Status |
|---|---:|---|
| Phase 0 — Core stabilization & CI | #25 | Open — current priority |
| Phase 1 — Material 3 Theme System | #26 | Open |
| Phase 2 — SMS Automation Pro | #27 | Open |
| Phase 3 — SMS Templates Pro | #28 | Open |
| Phase 4 — SMS Backup Pro | #29 | Open |
| Phase 5 — Scheduled SMS Pro | #30 | Open |
| Phase 6 — Encrypted SMS | #31 | Open — protocol review before production crypto |
| Phase 7 — Secure Sessions / Ratchet | #32 | Open — blocked by Phase 6 design |
| Phase 8 — Testing / hardening / release | #33 | Open |
| Branch reconciliation | #34 | Open |

## Branch audit

| Branch | Relation to `main` | Decision |
|---|---|---|
| `feat/plugin-sms-automation` | 95 ahead / 173 behind; diverged | Do not merge blindly. Rebase/reconstruct from current `main` and isolate plugin work. |
| `feat/message-annotations` | 37 ahead / 161 behind; diverged | Keep draft; rebase/reconstruct against current `main` before validation. |
| `chore/theme-library-ui-patch` | 2 ahead / 121 behind; diverged | Re-evaluate whether the tiny Settings change is still needed on current `main`; cherry-pick only if required. |
| `chore/minimal-commons` | 0 ahead / 260 behind | Historical branch; no current delta against `main`. |
| `tmp-unused` | temporary | Confirm it is unused, then delete. |

### Important findings

- `main` has a centralized `ThemeManager` with built-in/user/imported/community themes, favorites, search/sort/recent support, and theme selection. `ThemeScheduleManager` handles scheduled day/night theme switching. These exist, but integration coverage across all screens still needs verification.
- The canonical Android CI workflow runs Core/FOSS/GPlay unit tests and lint. The signed FOSS release build is gated on those jobs for `main`.
- Duplicate PR validation workflows were removed. Historical self-modifying `perf-optimize-v4` and `extract-minimal-commons` workflows were removed from `main`.
- CI validation now uses read-only repository permissions and no longer triggers the canonical Android CI on historical `chore/minimal-commons` or `test/foss-release-signed` branches.
- The current automation branch contains a minimal `SmsAutomationPlugin` with only `MARK_READ` and `DELETE`, SharedPreferences JSON persistence, simple sender/body matching, and no Forward/Auto Reply/priority/AND-OR-NOT/rate-limit/loop-protection/rule tester. It is an early implementation, not the finished Automation Pro feature. The branch also contains unrelated Plugin Store/license work, so Phase 2 requires reconstruction/isolation.
- PR #24 (`feat/message-annotations`) is still a draft and currently reports `mergeable=false`; it targets an older `main` SHA, so it requires reconciliation before it can be considered merge-ready.

## Phase 0 — Stabilization

Tracked in **GitHub Issue #25**.

- [x] Git/branch audit
- [ ] Build matrix validation (Core/FOSS/GPlay) — final post-hardening CI pending
- [ ] Lint validation — final post-hardening CI pending
- [x] Room / Entity / DAO / migration audit
- [x] Dependency and Gradle audit
- [x] Workflow/CI audit
- [ ] Regression audit
- [x] CI hardening

**Gate:** `main` builds, tests and lints successfully with no known blocking regression and unnecessary automation is removed/disabled.

## Phase 1 — Theme System

Tracked in **GitHub Issue #26**.

- [x] Central `ThemeManager`
- [x] Built-in/custom/imported/community theme sources
- [x] Favorites/search/sort/recent theme support
- [x] Theme scheduling
- [ ] Verify every major screen uses the central theme authority
- [ ] Verify light/dark/dynamic behavior where applicable
- [ ] Remove remaining legacy theme references
- [ ] Add focused theme regression tests

**Gate:** Theme behavior is consistent across supported screens and CI is green.

## Phase 2 — SMS Automation Pro

Tracked in **GitHub Issue #27**.

- [ ] Rebase/reconstruct automation branch from current `main`
- [ ] Rule data model / persistence
- [ ] Rule Engine
- [ ] Conditions: sender/contact/message
- [ ] Operators: contains/equals/starts/ends/regex
- [ ] Time/day conditions
- [ ] AND/OR/NOT
- [ ] Priority
- [ ] Actions: Forward / Auto Reply / Delete / Read state / Star / Archive / Notification / Webhook / Save / Copy / Delay
- [ ] Rate limit / cooldown
- [ ] Loop protection
- [ ] Logs
- [ ] Rule Tester
- [ ] Import/Export
- [ ] Rule Templates
- [ ] UI and Compose integration

**Gate:** Feature is independently testable and safe against loops/rate abuse before merge.

## Phase 3 — SMS Templates Pro

Tracked in **GitHub Issue #28**.

- [ ] Template CRUD
- [ ] Categories
- [ ] Favorites
- [ ] Search
- [ ] Variables
- [ ] Preview
- [ ] Compose integration
- [ ] Import/Export

## Phase 4 — SMS Backup Pro

Tracked in **GitHub Issue #29**.

- [ ] Full/selective backup
- [ ] Date/contact filtering
- [ ] JSON/CSV/TXT
- [ ] Compression
- [ ] Encryption
- [ ] Backup history
- [ ] Automatic backup conditions
- [ ] Selective restore
- [ ] Preview / duplicate detection / verification

## Phase 5 — Scheduled SMS Pro

Tracked in **GitHub Issue #30**.

- [ ] One-time scheduling
- [ ] Daily/weekly/monthly/custom recurrence
- [ ] End date/count
- [ ] Multiple recipients
- [ ] Templates/variables
- [ ] Retry/delivery status/history
- [ ] Pause/resume
- [ ] Calendar/conflict detection
- [ ] Quiet hours

## Phase 6 — Encrypted SMS

Tracked in **GitHub Issue #31**.

- [ ] Threat model and trust model
- [ ] Protocol specification before implementation
- [ ] Device identity keys
- [ ] X25519 key agreement
- [ ] HKDF-SHA-256
- [ ] ChaCha20-Poly1305
- [ ] Versioned wire format (`HMSG1`, future versions)
- [ ] Multipart SMS fragmentation/reassembly
- [ ] Contact public-key management
- [ ] QR exchange
- [ ] Key verification/security code
- [ ] Replay protection
- [ ] Tamper detection
- [ ] Encrypted private-key backup (optional)
- [ ] Compose integration
- [ ] Encrypted/standard conversation indicator
- [ ] Key rotation

**Gate:** No production implementation until the protocol and threat model are reviewed and testable.

## Phase 7 — Secure Sessions / Advanced Crypto

Tracked in **GitHub Issue #32**.

- [ ] Session architecture
- [ ] Per-message key evolution
- [ ] Forward secrecy design
- [ ] Recovery / rotation design
- [ ] Ratchet design and security review

## Phase 8 — Release Hardening

Tracked in **GitHub Issue #33**.

- [ ] Unit tests
- [ ] Integration tests
- [ ] Migration tests
- [ ] UI/regression tests
- [ ] Security tests
- [ ] Release build validation
- [ ] APK artifact verification
- [ ] Release checklist/tag

## Workflow rules

1. Work starts from a tracked GitHub Issue.
2. Each feature gets its own branch.
3. `main` is stabilization-first; unfinished features do not merge.
4. Never merge a heavily diverged branch blindly; compare/rebase/reconstruct first.
5. Every completed task records validation evidence and the resulting commit/PR in its issue.
6. After each meaningful change, update the relevant issue; update this roadmap when phase structure/status changes.
7. A phase is complete only after its acceptance gate passes.
8. Encrypted SMS must not be improvised; protocol design precedes code.
