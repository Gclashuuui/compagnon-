# HUD RP — cadres discrets

## Source

- Archive reçue : `output/HUD_cadres_discrets_13_variantes.zip`.
- Treize fichiers `hud_frame_hd.png`, un pour chaque valeur de `ThemeSante`.
- Format source : 448 × 152 RGBA, affiché en 112 × 38 pixels logiques.
- Chaque pixel logique est un bloc uniforme de 4 × 4 pixels source.

## Zones sûres logiques

- Nom : x 10, y 3, largeur 94, hauteur 9.
- Nourriture : x 5, y 14, largeur 48, hauteur 10.
- Énergie : x 58, y 14, largeur 49, hauteur 10.
- Santé : x 5, y 25, largeur 48, hauteur 10.
- Complicité : x 58, y 25, largeur 49, hauteur 10.

## Ordre de rendu

1. Cadre du thème avec son alpha d'origine.
2. Témoin de diagnostic devant le nom.
3. Nom dynamique du compagnon.
4. Ombres puis couleurs des quatre pictogrammes.
5. Sillons, remplissages et repères des quatre jauges.

Le fond illustré ancien et les variantes `hud_compact_overflow` ne doivent jamais
revenir dans le HUD permanent. Ils restent uniquement des sources historiques.
