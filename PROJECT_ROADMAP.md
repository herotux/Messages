# Messages — Project Roadmap

> Living project tracker. GitHub Issues are the source of truth for actionable work; this file records the phase order and acceptance gates.

## Current state — 2026-09-12

- [x] `main` stabilization baseline exists
- [x] One-shot theme cleanup workflow removed
- [x] M3 theme migration committed on `main`
- [ ] Full `main` audit: Theme / Room / DAO / CI / regressions
- [ ] Validate latest CI run on `main`
- [ ] Reconcile long-lived feature branches before merging

## Phase 0 — Stabilization

- [ ] Git/branch audit
- [ ] Build matrix validation (Core/FOSS/GPlay)
- [ ] Lint validation
- [ ] Unit/integration test audit
- [ ] Dependency and Gradle audit
- [ ] Regression audit
- [ ] CI hardening

**Gate:** `main` builds, tests and lints successfully with no known blocking regression.

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

1. Work starts from an Issue.
2. Each feature gets its own branch.
3. `main` is stabilization-first; unfinished features do not merge.
4. Every completed Issue records validation evidence and the resulting commit/PR.
5. After each meaningful change, update this roadmap and the related Issue.
6. A phase is complete only after its acceptance gate passes.
7. Encrypted SMS must not be improvised; protocol design precedes code.
