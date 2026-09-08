# Prompt Gemini — la bulle de pensée

Une seule planche : les **3 petites bulles** qui montent, puis le **gros nuage**
en trois hauteurs.

## Pourquoi trois hauteurs et pas une seule

Le texte du rêve fait **1 à 4 lignes** selon la phrase. Une seule bulle obligerait
à l'étirer, et un nuage étiré se voit tout de suite : les bosses deviennent des
ovales. On demande donc trois nuages dessinés à la main dans leurs bonnes
proportions, et le code choisit celui qui va.

L'autre solution — un nuage découpable en 9 morceaux étirables — est plus souple
sur le papier, mais aucun modèle d'image ne sait produire des bords qui se
raccordent vraiment. On l'a écartée exprès.

## Format à demander

Demande le format **le plus large possible (21:9)**, comme pour les autres
planches.

## À vérifier sur le résultat

1. Les **trois petites bulles** sont bien de trois tailles différentes.
2. Le **creux intérieur** de chaque nuage est bien vide et bien plat — c'est là
   que le texte ira. S'il est bosselé à l'intérieur, redemande.
3. Aucun texte, aucun trait de grille, aucune ombre portée sur le magenta.

---

```
PIXEL ART SPRITE SHEET — follow this specification exactly.

OUTPUT
One single image, 1536 x 512 pixels, containing 6 separate sprites arranged in a
strict grid of 3 columns x 2 rows. Each grid cell is exactly 512 x 256 pixels.
The background of the ENTIRE image, including every cell and all space around and
inside the sprites, is pure flat magenta #FF00FF. Nothing else may be magenta.

PIXEL GRID (most important rule)
Every sprite is authentic pixel art shown at 8x nearest-neighbour magnification.
Every visible "pixel" is a perfectly square 8 x 8 block of ONE flat colour, snapped
to a rigid grid. No pixel may be half a block wide. Curves are built from
stair-stepped square blocks, never smoothed. Each cell is therefore a 64 x 32
pixel-art canvas.

FORBIDDEN
No anti-aliasing. No gradients. No blur. No soft or feathered edges. No noise or
grain. No drop shadows. No glow. No transparency effects. No 3D rendering, no
photorealism, no painterly brushwork. No text, no letters, no numbers, no labels,
no captions, no signature, no watermark. No grid lines, no cell borders, no frames,
no separators between cells. No tail, no pointer, no triangle attached to any cloud.

ART DIRECTION (identical for all 6 sprites)
- Subject: a comic-book THOUGHT balloon — a puffy cloud made of round bumps.
  Never a speech balloon, never a rectangle, never a rounded rectangle.
- Style: Minecraft / classic RPG interface art.
- Outline: continuous 1-pixel outline all around, in a cool dark blue-grey
  (#3A4256). Never pure black.
- Fill: off-white body (#F4F6FB) with one soft grey-blue shadow tone (#C9D2E4)
  along the bottom-right inner edge only, 1 to 2 pixels thick.
- Light source: top-left, consistent across all 6 sprites.
- Palette: exactly these 3 colours plus the magenta background. Nothing else.
- The bumps around the rim must be IRREGULAR in size, like a real thought cloud,
  never a repeating identical scallop pattern.

CELL CONTENTS — read this carefully, each cell is different.

TOP ROW — the three small trailing bubbles, one per cell, each CENTRED in its
cell with plenty of magenta all around. They are simple round puffs, not clouds.
- Cell 1 (top-left):   the SMALLEST bubble, about 4 x 4 pixel-art pixels wide.
- Cell 2 (top-middle): the MIDDLE bubble, about 7 x 7 pixel-art pixels wide.
- Cell 3 (top-right):  the LARGEST bubble, about 10 x 10 pixel-art pixels wide.
These three must be clearly three different sizes, all perfectly round, all with
the same outline and the same top-left highlight treatment.

BOTTOM ROW — the big thought cloud, THREE TIMES, in three different heights.
All three are the SAME cloud design and the SAME width, only the height changes.
Each is centred in its cell.
- Cell 4 (bottom-left):   cloud about 60 x 16 pixel-art pixels — a low, wide cloud.
- Cell 5 (bottom-middle): cloud about 60 x 22 pixel-art pixels — a medium cloud.
- Cell 6 (bottom-right):  cloud about 60 x 28 pixel-art pixels — a tall cloud.

CRITICAL for the three clouds: the INSIDE of each cloud must be a large, clean,
completely FLAT rectangular area of the off-white fill colour. All the bumpiness
lives on the outer rim only. The inside is where text will be written later, so it
must be empty, even, and free of any bump, dip, shading or decoration. Do not draw
anything inside the clouds.

The three clouds must look like the same cloud grown taller, not three different
clouds — same rim style, same bump rhythm, same outline, same shadow treatment.
```
