# Iranian bank registry

The registry is intentionally local/offline. SMS content and sender information are not uploaded anywhere, and bank logos are bundled in the APK.

## Data sources

1. `masihgh/iranian-bank-list` — primary card-prefix dataset (`banks.json`).
2. `IR-Banks/ir-banks-info` — cross-check for card prefixes and IBAN bank codes (`src/data/banks.ts`, `src/data/sheba.ts`).
3. `amastaneh/IranianBankLogos` — 64px logo sprite and published sprite coordinates, used as a reference/source for logo coverage.
4. `zegond/logos-per-banks` — CC0 SVG source used as the current fallback/master source for logos that are not present in the first sprite.

## Reconciliation rules

- Card lookup uses **longest-prefix matching**, not a fixed six-digit lookup. This is required for Blu (`62198618` / `62198619`) versus Saman (`621986`).
- Historical/merged institutions are represented as aliases or legacy IBAN codes where the current bank is known.
- Bank identification from an SMS sender is separate from card/IBAN identification. A sender is not considered a bank merely because a card BIN or IBAN code matches a bank.
- Known non-bank senders such as `V.REFAH` and `V.MASKAN` are explicitly excluded.
- Logos are referenced by Android drawable name and are bundled in the APK. No CDN or runtime network request is part of this design.

## Logo pipeline

The canonical Android asset pipeline is:

```text
SVG source
  -> tools/import_iranian_bank_vectors.py
  -> Android VectorDrawable XML
  -> app/src/main/res/drawable/bank_*.xml
  -> APK
```

The GitHub Actions workflow `.github/workflows/import-bank-vectors.yml` runs the importer and validates the generated VectorDrawable XML. Generated XML assets are committed so installed APKs remain fully offline.

`tools/import_iranian_bank_logos.py` is the older 64px PNG-sprite importer and is retained only as a legacy/development reference; it is not the runtime asset pipeline.

## Resource mapping

`IranianBankRegistry.BankInfo.logoResourceName` stores the Android drawable name without the `R.drawable.` prefix. UI code should resolve this name locally and must gracefully handle `null` for banks whose logo source is still missing.
