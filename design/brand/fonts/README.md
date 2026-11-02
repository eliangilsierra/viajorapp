# Bundled fonts

The app bundles two open-licensed families (SIL Open Font License 1.1), as Compose resources in
`ui/src/commonMain/composeResources/font`:

| Family | Files | Upstream | Licence text |
| --- | --- | --- | --- |
| Manrope | `manrope_semibold.ttf`, `manrope_extrabold.ttf` | https://github.com/googlefonts/manrope | `OFL-Manrope.txt` |
| Inter | `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf` | https://github.com/rsms/inter | `OFL-Inter.txt` |

The files are static instances cut from the variable fonts in https://github.com/google/fonts (so weights work
on Android 24 and 25, which ignore variable axes), subsetted to Latin (Basic Latin, Latin-1, Latin Extended-A and
common punctuation) with all layout features kept, including tabular figures (`tnum`). Inter uses optical size 14.
Add the Unicode ranges of a new language before shipping it. Neither licence declares a Reserved Font Name.
