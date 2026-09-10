# App Icon Notes

Source artwork, sizing rules, and adaptive-icon wiring for `Quote Garden`.

## Source

`res/drawable-nodpi/ic_launcher_foreground.png` — 1254x1254 RGBA PNG. Full-bleed artwork (squircle background, open book, tulips and leaves, gold curly quote mark, wordmark "The Quote Garden").

The image carries its own background; the adaptive-icon `<background>` color is for the safe-area fill behind any future swap to a transparent foreground.

## Sizes

| Bucket | Size | File |
|---|---|---|
| mdpi | 48x48 | `mipmap-mdpi/ic_launcher.png` |
| hdpi | 72x72 | `mipmap-hdpi/ic_launcher.png` |
| xhdpi | 96x96 | `mipmap-xhdpi/ic_launcher.png` |
| xxhdpi | 144x144 | `mipmap-xxhdpi/ic_launcher.png` |
| xxxhdpi | 192x192 | `mipmap-xxxhdpi/ic_launcher.png` |
| anydpi-v26 | adaptive | `mipmap-anydpi-v26/ic_launcher.xml` |

## Adaptive Icon (Android 8.0+, API 26+)

`res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<adaptive-icon>
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- `<background>` = `@color/ic_launcher_background` = `#F4EFE3` (cream from the source artwork)
- `<foreground>` = full source PNG; Android masks it into the launcher shape (squircle / circle / teardrop depending on launcher)
- `<monochrome>` = same source; on Android 13+ themed icons it gets desaturated to a single-color glyph

## Regenerating the Density Buckets

If the source PNG changes:

```python
from PIL import Image
src = "res/drawable-nodpi/ic_launcher_foreground.png"
im = Image.open(src).convert("RGBA")
for d, s in {"mdpi":48,"hdpi":72,"xhdpi":96,"xxhdpi":144,"xxxhdpi":192}.items():
    im.resize((s, s), Image.LANCZOS).save(f"res/mipmap-{d}/ic_launcher.png", "PNG")
```

For best quality, export the source at exactly 432x432 instead of 1254x1254 (Android adaptive icons cap at 432dp, downscaling from 1254 wastes bytes).

## Manifest

`AndroidManifest.xml` references `@mipmap/ic_launcher`. The anydpi-v26 entry wins on API 26+; older devices fall back to the bitmap density buckets.

## Replacing the Icon

1. Drop the new PNG into `res/drawable-nodpi/ic_launcher_foreground.png` (432x432 recommended, transparent or with background).
2. Run the regeneration snippet above.
3. If the cream tone changes, update `ic_launcher_background` in `res/values/colors.xml`.
