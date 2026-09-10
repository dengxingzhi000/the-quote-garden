# Per-Image Generation Prompts

Each prompt below is independent. Send one prompt at a time to the image generation AI. Each prompt is written for **Android** specifically (not iOS, not Web).

**Important conventions used in every prompt:**

- **Pixel dimensions (W x H) are exact source size** - export at this size, not larger, not smaller
- **Background:** "transparent" means alpha 0 PNG. If a color backdrop is desired it is specified per prompt
- **Color:** never specify hex codes - the designer chooses within the editorial theme. Only describe mood (warm, soft, muted, etc.)
- **Density math:** Android's baseline is mdpi (1x). Source pixel size = dp display size x device density. For drawable-nodpi we export at 3x by convention (xxxhdpi). Vector XML is density-independent
- **Format:** PNG for raster illustrations, JPG only for photographic content and QR codes, Vector Drawable XML for monochrome icons

**Target platform: Android** (Compose, Material 3, offline-first daily-quote app, editorial / hand-illustrated aesthetic).

---

## 1. App Icon Foreground

```
Target platform: Android (adaptive launcher icon foreground)

Generate a single PNG image at exactly 432 x 432 pixels.
Background: fully transparent (alpha 0).
Format: PNG with alpha channel.

Subject: a stylized open book with leaves and a flower growing from its pages, with a gold curly opening quote mark floating above. Calm, editorial, hand-illustrated feel, not photorealistic. Soft cream highlights allowed inside the artwork itself.

Design intent: Android will mask this into a squircle / circle / teardrop depending on the launcher. Keep the main subject within the central 66 percent safe zone. Do not place important details in the outer 17 percent margin (those will be cropped on some launchers).

Display context: shown at 108 x 108 dp on a launcher's home screen.
Output filename: ic_launcher_foreground.png
Drop into: res/drawable-nodpi/ic_launcher_foreground.png
```

---

## 2. Brand Mark

```
Target platform: Android (small in-screen brand glyph)

Generate a single PNG image at exactly 512 x 512 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a small, refined brand glyph suitable for showing next to a heading on the home and browse screens. Single-color or two-color max. Motif suggestions: a serif letter Q with a small leaf, or a minimal open-book shape. Should read clearly even at 40 dp.

Display context: shown at 40 x 40 dp in the top bar of Home and Browse.
Output filename: ic_brand_mark.png
Drop into: res/drawable-nodpi/ic_brand_mark.png
```

---

## 3. Loading Ring (Vector)

```
Target platform: Android (Vector Drawable XML)

Generate a single Android Vector Drawable XML file.
Format: res/drawable/*.xml with <vector> root element.
Viewport: 24 x 24 (Android default).
Path commands: standard SVG path syntax supported by Android.
Do NOT use SVG-only features (filters, masks, foreignObject).
Do NOT use fill gradients (use solid color or none).
Background: none (transparent viewport).

Subject: a thin rotating ring, partial arc only (not a full circle), no fill, just a stroke. Calm, not flashy. Two concentric rings acceptable.

Display context: shown at 48 x 48 dp on Home screen during loading.
Output filename: ic_quiet_loading.xml
Drop into: res/drawable/ic_quiet_loading.xml
```

---

## 4. Empty State - Nothing Here Yet

```
Target platform: Android (empty-state illustration)

Generate a single PNG image at exactly 512 x 512 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a large stylized opening quote mark, single or double. Soft, hand-drawn feel. Should feel quiet, not alarming.

Display context: shown at 120 x 120 dp above the title on the "Nothing here yet" empty state on Home.
Output filename: empty_quote.png
Drop into: res/drawable-nodpi/empty_quote.png
```

---

## 5. Empty State - No Saved Quotes

```
Target platform: Android (empty-state illustration)

Generate a single PNG image at exactly 512 x 512 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: an open book viewed from above, with one small bookmark or a single leaf resting on the page. Editorial, calm, lots of negative space.

Display context: shown at 160 x 160 dp above the title on the "No saved quotes yet" empty state on Me.
Output filename: empty_favorites.png
Drop into: res/drawable-nodpi/empty_favorites.png
```

---

## 6. Empty State - No Category Lines

```
Target platform: Android (empty-state illustration)

Generate a single PNG image at exactly 512 x 512 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a single page with a single leaf, or a single closed book. Minimalist. Should suggest "empty but waiting".

Display context: shown at 120 x 120 dp on the empty CategoryDetail screen and the "No lines in this category yet" state on Home.
Output filename: empty_category.png
Drop into: res/drawable-nodpi/empty_category.png
```

---

## 7. Empty State - Nothing to Browse

```
Target platform: Android (empty-state illustration)

Generate a single PNG image at exactly 512 x 512 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a compass rose, or an empty shelf / grid, or a map outline. Suggests "no direction yet" without being negative.

Display context: shown at 160 x 160 dp above the title on the "Nothing to browse yet" empty state on Browse.
Output filename: empty_browse.png
Drop into: res/drawable-nodpi/empty_browse.png
```

---

## 8. Offline / Error State

```
Target platform: Android (error-state illustration)

Generate a single PNG image at exactly 512 x 512 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a plug with a disconnected cable, or a broken WiFi signal, or a torn page. Should clearly signal "no connection" without being aggressive.

Display context: shown at 120 x 120 dp above the error message on Home when sync fails.
Output filename: img_offline.png
Drop into: res/drawable-nodpi/img_offline.png
```

---

## 9. Theme Icon - Light (Sun)

```
Target platform: Android (Vector Drawable XML)

Generate a single Android Vector Drawable XML file.
Viewport: 24 x 24.
Use single color stroke. The app will tint this with MaterialTheme.colorScheme.onBackground.
Background: none.

Subject: a simple sun glyph - a circle with short rays around it. Outline only, not filled. Stroke width around 1.5 to 2 dp.

Display context: shown at 24 x 24 dp next to the "Light" option in the theme dialog.
Output filename: ic_theme_light.xml
Drop into: res/drawable/ic_theme_light.xml
```

---

## 10. Theme Icon - Dark (Moon)

```
Target platform: Android (Vector Drawable XML)

Generate a single Android Vector Drawable XML file.
Viewport: 24 x 24.
Use single color fill or stroke. The app will tint this.
Background: none.

Subject: a crescent moon. Outline or solid fill - either is fine. Stroke width around 1.5 to 2 dp if outline.

Display context: shown at 24 x 24 dp next to the "Dark" option in the theme dialog.
Output filename: ic_theme_dark.xml
Drop into: res/drawable/ic_theme_dark.xml
```

---

## 11. Theme Icon - System (Auto)

```
Target platform: Android (Vector Drawable XML)

Generate a single Android Vector Drawable XML file.
Viewport: 24 x 24.
Use single color stroke. The app will tint this.
Background: none.

Subject: a capital letter A inside a circular arrow, or just a stylized A with a small auto-refresh symbol. Outline only. Stroke width around 1.5 to 2 dp.

Display context: shown at 24 x 24 dp next to the "System" option in the theme dialog.
Output filename: ic_theme_system.xml
Drop into: res/drawable/ic_theme_system.xml
```

---

## 12. Category Tile - Love

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a stylized heart motif - can be a literal heart, a flower, two intertwined leaves, or a cupid-style arrow. Soft, editorial.

Display context: shown at 56 x 56 dp in the Love category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_love.png
Drop into: res/drawable-nodpi/category_love.png
```

---

## 13. Category Tile - Wisdom

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a stack of books, an owl, an open book with light radiating, or a single open page. Suggests knowledge.

Display context: shown at 56 x 56 dp in the Wisdom category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_wisdom.png
Drop into: res/drawable-nodpi/category_wisdom.png
```

---

## 14. Category Tile - Courage

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a mountain peak, a flame, a lion's mane, or a sword. Suggests bravery.

Display context: shown at 56 x 56 dp in the Courage category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_courage.png
Drop into: res/drawable/category_courage.png
```

---

## 15. Category Tile - Humor

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a smile, a laughing face, a sun with a grin, or a balloon. Light, playful.

Display context: shown at 56 x 56 dp in the Humor category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_humor.png
Drop into: res/drawable/category_humor.png
```

---

## 16. Category Tile - Life

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a sprouting seedling, a single leaf on a stem, a tree of life, or hands holding a plant. Suggests growth.

Display context: shown at 56 x 56 dp in the Life category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_life.png
Drop into: res/drawable/category_life.png
```

---

## 17. Category Tile - Solitude

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a moon over a single figure, a window with curtains, a candle flame, or a single bird on a branch. Quiet, contemplative.

Display context: shown at 56 x 56 dp in the Solitude category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_solitude.png
Drop into: res/drawable-nodpi/category_solitude.png
```

---

## 18. Category Tile - Time

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a clock face, an hourglass, a sundial, or a snail. Suggests the passage of time.

Display context: shown at 56 x 56 dp in the Time category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_time.png
Drop into: res/drawable-nodpi/category_time.png
```

---

## 19. Category Tile - Change

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a butterfly, a caterpillar transforming, a seed becoming a sprout, or a river meandering. Suggests transformation.

Display context: shown at 56 x 56 dp in the Change category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_change.png
Drop into: res/drawable-nodpi/category_change.png
```

---

## 20. Onboarding 1 - Welcome

```
Target platform: Android (full-bleed onboarding illustration, portrait)

Generate a single PNG image at exactly 800 x 1200 pixels.
Background: a soft cream-toned backdrop is acceptable (the screen behind it will also be cream so it can blend), or fully transparent. The image will be displayed full-bleed at the top of the first onboarding screen.
Aspect ratio: 2:3 (portrait).
Format: PNG.

Subject: a wide landscape with mountains, a river, and a sunrise. Editorial watercolor feel. Leaves or trees in the foreground optional. The bottom 25 percent of the image will be covered by a text overlay (title and subtitle), so keep the bottom region relatively calm and uncluttered.

Display context: full screen width on portrait phones, 3:2 aspect ratio. Title text overlays the bottom quarter.
Output filename: ic_welcome.png
Drop into: res/drawable-nodpi/ic_welcome.png
```

---

## 21. Onboarding 2 - Daily Quote

```
Target platform: Android (full-bleed onboarding illustration, portrait)

Generate a single PNG image at exactly 800 x 1200 pixels.
Background: a soft cream-toned backdrop is acceptable, or fully transparent.
Aspect ratio: 2:3 (portrait).
Format: PNG.

Subject: a sunrise or open book on a desk with morning light, suggesting a fresh start each day. Calm, hopeful. The bottom 25 percent will be covered by text, keep that region relatively uncluttered.

Display context: full screen width on portrait phones, 3:2 aspect ratio. Title text overlays the bottom quarter.
Output filename: ic_daily_quote.png
Drop into: res/drawable-nodpi/ic_daily_quote.png
```

---

## 22. Onboarding 3 - Favorites

```
Target platform: Android (full-bleed onboarding illustration, portrait)

Generate a single PNG image at exactly 800 x 1200 pixels.
Background: a soft cream-toned backdrop is acceptable, or fully transparent.
Aspect ratio: 2:3 (portrait).
Format: PNG.

Subject: a heart made of leaves, or a hand cradling a glowing book. Suggests care and personal collection. Bottom 25 percent is reserved for text overlay.

Display context: full screen width on portrait phones, 3:2 aspect ratio. Title text overlays the bottom quarter.
Output filename: ic_favorites.png
Drop into: res/drawable-nodpi/ic_favorites.png
```

---

## 23. Onboarding 4 - Sync

```
Target platform: Android (full-bleed onboarding illustration, portrait)

Generate a single PNG image at exactly 800 x 1200 pixels.
Background: a soft cream-toned backdrop is acceptable, or fully transparent.
Aspect ratio: 2:3 (portrait).
Format: PNG.

Subject: two circular arrows, or a cloud with a sync indicator, or a phone with arrows syncing data. Suggests background data flow without alarming "loading" connotation. Bottom 25 percent is reserved for text overlay.

Display context: full screen width on portrait phones, 3:2 aspect ratio. Title text overlays the bottom quarter.
Output filename: onboard_4.png
Drop into: res/drawable-nodpi/onboard_4.png
```

---

## 24. Sync Done

```
Target platform: Android (Vector Drawable XML)

Generate a single Android Vector Drawable XML file.
Viewport: 24 x 24.
Use single color stroke or fill. The app will tint this with the success color.
Background: none.

Subject: a checkmark inside a circle, or just a clean checkmark stroke. Stroke width around 2 dp.

Display context: shown at 24 x 24 dp after sync completes (in toast or inline status).
Output filename: ic_sync_done.xml
Drop into: res/drawable/ic_sync_done.xml
```

---

## 25. Sync Pending (Static Base)

```
Target platform: Android (Vector Drawable XML - base for animated spinner)

Generate a single Android Vector Drawable XML file.
Viewport: 24 x 24.
Use single color stroke. The app will tint this.
Background: none.

Subject: an open ring or partial circle (about 270 degrees of arc), no fill, just a stroke. This is the static base layer of a rotating spinner.

Display context: shown at 24 x 24 dp while sync is in progress.
Output filename: ic_sync_pending_static.xml
Drop into: res/drawable/ic_sync_pending_static.xml
```

---

## 26. Sync Pending (Animated)

```
Target platform: Android (AnimatedVectorDrawable XML)

Generate a single Android AnimatedVectorDrawable XML file using <animated-vector> as the root.
It must reference an existing base drawable named ic_sync_pending_static.xml as its android:drawable.
Define a single <target android:name="ring"> with an <objectAnimator> that rotates 0 to 360 degrees.
Duration: 1200 ms.
Repeat count: infinite.
Interpolator: linear.

Background: none.

Display context: shown at 24 x 24 dp while sync is in progress. The rotation animator must be named "ring" to match the <target> tag.
Output filename: ic_sync_pending.xml
Drop into: res/drawable/ic_sync_pending.xml
Note: this file depends on ic_sync_pending_static.xml existing in the same folder.
```

---

## 27. About Dialog App Logo

```
Target platform: Android (small dialog-header icon)

Generate a single PNG image at exactly 256 x 256 pixels.
Background: either fully transparent OR a dark rounded-square backdrop (rounded corners, about 12 percent corner radius). The dialog will place it above the "Quote Garden" text title.
Format: PNG with alpha channel if transparent, or JPG if solid backdrop.

Subject: a refined small app mark - a Q letter, an open book, or a leaf. Single color or two-color max. Should read clearly at 72 dp.

Display context: shown at 72 x 72 dp centered at the top of the About dialog in Me.
Output filename: ic_app_logo.png
Drop into: res/drawable-nodpi/ic_app_logo.png
```

---

## 28. WeChat QR Code

```
Target platform: Android (QR code for in-app scan)

Provide a single QR code image. The QR code encodes a WeChat contact or add-friend link (the exact content is provided separately by the user; do not invent a value).

The image must be:
- Square, at least 600 x 600 pixels (export at exactly 600 x 600 or 800 x 800)
- High contrast: black modules on a pure white background
- A clear quiet zone (margin) of at least 4 modules around the code on all sides
- No compression artifacts (use lossless or high-quality JPG)

Format: PNG (preferred) or JPG. The filename extension MUST match the actual format - if the image is JPG, the filename must end in .jpg, not .png.

Display context: shown at 220 x 220 dp inside a rounded card in the Contact Us dialog on Me.
Output filename: ic_wechat_qr.png (if PNG) or ic_wechat_qr.jpg (if JPG)
Drop into: res/drawable-nodpi/
```

---

e: file:///D:/ProgramProject/Andorid_Project/app/src/main/java/com/quotegarden/feature/home/HomeScreen.kt:33:43 Unresolved reference 'matchParentSize'.


---

## 30. Category Tile - adventure

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: exploration and the open road - a compass rose with a single highlighted needle, a mountain path winding into the distance, a small sailboat on a horizon, an open trail through two trees. Choose one motif only. Inviting, not perilous.

Display context: shown at 56 x 56 dp in the adventure category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: adventure.png
Drop into: res/drawable/adventure.png
```

---

## 31. Category Tile - adversity

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: resilience through difficulty - a small green sprout pushing through a crack in stone, waves breaking against rocks with foam, a single candle flame bending in wind but not going out, a tree bent by storm but still rooted. Choose one motif only. Should feel honest about hardship but not bleak.

Display context: shown at 56 x 56 dp in the adversity category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: adversity.png
Drop into: res/drawable/adversity.png
```

---

## 32. Category Tile - advice

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: gentle guidance - an open palm extended outward, a small glowing lantern held up, an arrow pointing forward across an open page, a hand offering a single seed or small leaf. Choose one motif only. Warm, not preachy.

Display context: shown at 56 x 56 dp in the advice category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: advice.png
Drop into: res/drawable/advice.png
```

---

## 33. Category Tile - afternoon

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: the slow warm middle of the day - a low warm sun near the horizon casting long parallel shadows, a steaming cup of tea beside an open book, a hammock strung between two trees with a single cushion, a window with afternoon light spilling through half-open curtains. Choose one motif only. Suggests pause and warmth, not bedtime.

Display context: shown at 56 x 56 dp in the afternoon category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: afternoon.png
Drop into: res/drawable/afternoon.png
```

---

## 34. Category Tile - age

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: time lived and time given - a cross-section of a tree trunk showing growth rings, an hourglass resting in cupped open hands, a side-profile silhouette of an older face with one or two soft line marks (no detailed wrinkles), a wooden cane leaning against the spine of a closed book. Choose one motif only. Reverent, not melancholy.

Display context: shown at 56 x 56 dp in the age category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: age.png
Drop into: res/drawable/age.png
```

---

## 35. Category Tile - astrology

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: stars and small celestial patterns - a small constellation drawn as 5 to 7 connected dots with thin lines, a single large star with three or four smaller stars around it, a crescent moon with a sprinkle of small stars, a simple zodiac wheel with a single highlighted glyph. Choose one motif only. Quiet and a little mystical, not occult.

Display context: shown at 56 x 56 dp in the astrology category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: astrology.png
Drop into: res/drawable/astrology.png
```

---

## 36. Category Tile - autumn

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: late-season, harvest-leaning imagery - one maple leaf turning warm red or amber with visible veins, an acorn still in its textured cap, a bare branch with one last leaf clinging to the tip, a small pumpkin or gourd with a single curled stem. Choose one motif only. Warm, slightly muted, not Halloween.

Display context: shown at 56 x 56 dp in the autumn category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: autumn.png
Drop into: res/drawable/autumn.png
```

---

## 37. Category Tile - birds

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a single bird motif - a small bird in flight with wings spread (silhouette or two-tone), a small songbird perched on a thin bare branch, a single long feather drifting. Choose one motif only. Calm, observational, not ornithological chart.

Display context: shown at 56 x 56 dp in the birds category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: birds.png
Drop into: res/drawable/birds.png
```

---

## 38. Category Tile - blog-hawthorne-story-ideas

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: 19th-century writing desk - an inkwell with a quill resting across the top, a small stack of three or four manuscript pages tied with a thin cord and a single lit candle beside them, a closed leather-bound journal with a small clasp. Choose one motif only. Slightly moody, low light, very little color - cream and ink black with one warm accent.

Display context: shown at 56 x 56 dp in the blog-hawthorne-story-ideas category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: blog_hawthorne_story_ideas.png
Drop into: res/drawable/blog_hawthorne_story_ideas.png
```

---

## 39. Category Tile - book-dedications

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a small formal gesture toward a reader - an open book lying flat with a thin ribbon bookmark draped over the right-hand page, a quill resting on a single sheet of parchment with a single calligraphic flourish at the top, a closed book with a dedication plate visible on the inside cover. Choose one motif only. Quiet, ceremonial, intimate.

Display context: shown at 56 x 56 dp in the book-dedications category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: category_book-dedications.png
Drop into: res/drawable-nodpi/category_book-dedications.png
```

---

## 40. Category Tile - create-your-own-quote-contests

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: participation and recognition - a simple laurel wreath tied at the bottom with a thin ribbon, a small stage with a single warm spotlight, a podium with a small banner unfurled, a hanging medal or ribbon rosette. Choose one motif only. Inviting, not corporate.

Display context: shown at 56 x 56 dp in the create-your-own-quote-contests category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: create_your_own_quote_contests.png
Drop into: res/drawable/create_your_own_quote_contests.png
```

---

## 41. Category Tile - safari-unhand-your-pearls

```
Target platform: Android (category thumbnail)

Generate a single PNG image at exactly 128 x 128 pixels.
Background: fully transparent.
Format: PNG with alpha channel.

Subject: a single hybrid motif pairing the two halves of the collection name - a pearl resting in an open shell (the "pearls" half), a wide-brimmed safari hat with one tall ostrich plume tucked into the band (the "safari" half). Choose ONE of those two motifs only; do not try to draw both together. Slightly warm savanna palette, but very limited color - cream, sand, one warm rust accent, plus the pearl white.

Display context: shown at 56 x 56 dp in the safari-unhand-your-pearls category tile on Browse, and reused at 40 x 40 dp in the CategoryDetail screen header.
Output filename: safari_unhand_your_pearls.png
Drop into: res/drawable/safari_unhand_your_pearls.png
```

---

## Quality Checklist Before Submitting Each Asset

1. **Pixel size matches exactly** (e.g. 432 x 432 for the app icon, not 500 x 500)
2. **Format matches the filename extension** (PNG must be actual PNG, JPG must be actual JPG)
3. **Transparent background where requested** (alpha 0 - no white box around the artwork)
4. **Quiet zone preserved for QR code** (4 modules margin, no crop)
5. **Vector files use Android VectorDrawable schema** (viewport, path data, no SVG-only features)
6. **Single-color tints in vector icons** (the app will apply MaterialTheme tints at runtime)

## Final Build Step

After all 41 assets are in place, run a clean build from the repo root:

```
server/gradlew.bat :app:clean :app:assembleDebug
```

The build will fail if any `R.drawable.*` reference has no matching file. Existing composables in the codebase already reference these resources by their `R.drawable.*` name, so no code changes are required.
