# Messages — Project Roadmap

> Living project tracker. GitHub Issues would normally be the source of truth, but Issues are currently disabled for this repository. Until they are enabled, this file is the canonical tracker and every meaningful change must update it.

## Current state — 2026-09-12

- [x] `main` stabilization baseline exists
- [x] One-shot theme cleanup workflow removed
- [x] M3 theme migration committed on `main`
- [x] Branch inventory completed
- [x] Feature divergence audit completed
- [ ] Full `main` audit: Theme / Room / DAO / CI / regressions
- [ ] Validate latest CI run on `main`
- [ ] Reconcile long-lived feature branches before merging

### Branch audit

| Branch | Relation to `main` | Decision |
|---|---|---|
| `feat/plugin-sms-automation` | 95 ahead / 173 behind; diverged | Do not merge blindly. Rebase/reconstruct from current `main` and isolate plugin work. |
| `feat/message-annotations` | 37 ahead / 161 behind; diverged | Keep draft; rebase/reconstruct against current `main` before validation. |
| `chore/theme-library-ui-patch` | 2 ahead / 121 behind; diverged | Re-evaluate whether the tiny Settings change is still needed on current `main`; cherry-pick only if required. |
| `chore/minimal-commons` | 0 ahead / 260 behind | Historical branch; no current delta against `main`. Keep only if needed for history. |
| `tmp-unused` | temporary | Confirm it is unused, then delete. |

### Important findings

- `main` has a centralized `ThemeManager` with built-in/user/imported/community themes, favorites, search/sort/recent support, and theme selection. `ThemeScheduleManager` handles scheduled day/night theme switching. These exist, but integration coverage across all screens still needs verification.
- The active Android CI workflow runs Core/FOSS/GPlay unit tests and lint; the FOSS release APK build is gated on those jobs for `main`. The workflow also contains several branch-specific triggers that should be reviewed during CI hardening.
- The repository currently has a relatively large collection of workflow files. They need classification into required, historical/maintenance, and removable automation before we call Phase 0 complete.
- The current automation branch contains a minimal `SmsAutomationPlugin` with only `MARK_READ` and `DELETE`, SharedPreferences JSON persistence, simple sender/body matching, and no Forward/Auto Reply/priority/AND-OR-NOT/rate-limit/loop-protection/rule tester. It is therefore an early implementation, not the finished Automation Pro feature.
- PR #24 (`feat/message-annotations`) is still a draft and currently reports `mergeable=false`; it targets an older `main` SHA, so it requires reconciliation before it can be considered merge-ready.

## Phase 0 — Stabilization

- [x] Git/branch audit
- [ ] Build matrix validation (Core/FOSS/GPlay)
- [ ] Lint validation
- [ ] Unit/integration test audit
- [ ] Dependency and Gradle audit
- [ ] Workflow/CI audit
- [ ] Regression audit
- [ ] CI hardening

**Gate:** `main` builds, tests and lints successfully with no known blocking regression and unnecessary automation is removed/disabled.

## Phase 1 — Theme System

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

- [ ] Template CRUD
- [ ] Categories
- [ ] Favorites
- [ ] Search
- [ ] Variables
- [ ] Preview
- [ ] Compose integration
- [ ] Import/Export

## Phase 4 — SMS Backup Pro

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

- [ ] Session architecture
- [ ] Per-message key evolution
- [ ] Forward secrecy design
- [ ] Recovery / rotation design
- [ ] Ratchet design and security review

## Phase 8 — Release Hardening

- [ ] Unit tests
- [ ] Integration tests
- [ ] Migration tests
- [ ] UI/regression tests
- [ ] Security tests
- [ ] Release build validation
- [ ] APK artifact verification
- [ ] Release checklist/tag

## Workflow rules

1. Work starts from a tracked task. GitHub Issues are preferred, but are disabled in this repository at the moment; use this roadmap until enabled.
2. Each feature gets its own branch.
3. `main` is stabilization-first; unfinished features do not merge.
4. Never merge a heavily diverged branch blindly; compare/rebase/reconstruct first.
5. Every completed task records validation evidence and the resulting commit/PR.
6. After each meaningful change, update this roadmap.
7. A phase is complete only after its acceptance gate passes.
8. Encrypted SMS must not be improvised; protocol design precedes code.
