# Typography direction: Waypoint

All families are open-licensed and bundleable in a Compose Multiplatform app
(`composeResources/font`). Licence statements below are from upstream (Google Fonts
listings) and must be re-checked against the `OFL.txt` shipped with each font file
before bundling. No font file is included in this repository at the concept stage.

| Role | Family | Weights | Licence |
| --- | --- | --- | --- |
| Display / headings | Manrope | ExtraBold / SemiBold | SIL OFL 1.1 |
| Body / UI | Inter | Regular / Medium / SemiBold | SIL OFL 1.1 |
| Figures / data | Inter | tabular-nums feature for dates, prices, distances | SIL OFL 1.1 |

## Scale (sp, mobile)

| Token | Size / line height | Family role | Use |
| --- | --- | --- | --- |
| display | 32 / 40 | display | trip title on the overview |
| headline | 24 / 32 | display | screen titles |
| title | 18 / 26 | display | card titles, day headings |
| body | 16 / 24 | body | default reading text (minimum for long text) |
| label | 14 / 20 | body | buttons, chips, navigation labels |
| caption | 12 / 16 | body | metadata, never the only carrier of meaning |

## Rules

- At most two families on screen. Use tabular figures wherever dates, prices, distances or times are aligned.
- Support Dynamic Type / font scale up to 200 percent; layouts reflow, nothing is clipped.
- Localised text (English and Spanish at 1.0, per PRD I18N-01) must be checked for the extended Latin glyph set before a family is accepted.
- The logotype is NOT set in these fonts: it is hand-drawn geometry converted to outlines, so the logo never depends on a font install or licence.
