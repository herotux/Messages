# Iranian bank registry

The registry is intentionally local/offline. SMS content and sender information are not uploaded anywhere.

## Data sources

1. `masihgh/iranian-bank-list` — primary card-prefix dataset (`banks.json`).
2. `IR-Banks/ir-banks-info` — cross-check for card prefixes and IBAN bank codes (`src/data/banks.ts`, `src/data/sheba.ts`).
3. `amastaneh/IranianBankLogos` — 64px logo sprite and published sprite coordinates. Its README states commercial and non-commercial use is free.
4. `zegond/logos-per-banks` — CC0 SVG source used as a fallback for logos that are not present in the first sprite.

## Reconciliation rules

- Card lookup uses **longest-prefix matching**, not a fixed six-digit lookup. This is required for Blu (`62198618` / `62198619`) versus Saman (`621986`).
- Historical/merged institutions are represented as aliases or legacy IBAN codes where the current bank is known.
- Bank identification from an SMS sender is separate from card/IBAN identification. A sender is not considered a bank merely because a card BIN or IBAN code matches a bank.
- Known non-bank senders such as `V.REFAH` and `V.MASKAN` are explicitly excluded.
- Logos are referenced by Android drawable name and are intended to be bundled in the APK. No CDN or runtime network request is part of this design.

## Logo import

`tools/import_iranian_bank_logos.py` imports the published 64px sprite and crops its individual logos into `app/src/main/res/drawable`.

The importer is a build/development utility only; generated PNGs should be committed to the repository so installed APKs remain fully offline.
