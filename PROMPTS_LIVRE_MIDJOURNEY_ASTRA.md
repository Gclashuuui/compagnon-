# Refonte du livre — Midjourney puis Astra

Ce document sert à produire une nouvelle interface originale pour le livre du
mod Compagnon. Le travail est volontairement séparé en deux étapes :

1. Midjourney imagine la direction artistique et fournit un concept visuel.
2. Astra transforme ce concept en véritables textures pixel art, séparées,
   mesurées et prêtes à être intégrées dans Minecraft.

Le concept généré ne doit jamais remplacer directement les textures du mod :
il sert de référence artistique. Les textes, jauges, portraits, modèles 3D et
boutons restent dessinés par le code.

## Fichiers à joindre

Pour Midjourney, joindre si possible une capture du livre actuel afin de montrer
la vue de face et la quantité de contenu attendue.

Pour Astra, joindre obligatoirement :

- le concept retenu produit par Midjourney ;
- `book.png` ;
- `smudges.png` ;
- `iconbacking.png` ;
- `underline.png` ;
- `pageturnlargeleft.png` et `pageturnlargeright.png` ;
- une capture de la page Aujourd'hui avec le modèle 3D ;
- une capture des Rituels du jour ;
- une capture de la page Moments ou Liens.

Les textures actuelles se trouvent dans :

`src/main/resources/assets/compagnon/textures/gui/livre/`

## Prompt 1 — concept artistique Midjourney

Midjourney comprend généralement mieux la direction artistique en anglais.
Copier ce prompt avec une capture du livre actuel comme référence d'organisation :

```text
Front-facing orthographic game UI concept for an open magical companion journal,
authentic handcrafted Minecraft pixel art, designed for a warm fantasy creature
bonding mod. A beautiful old leather-bound book seen perfectly from above, two
large parchment pages, rich chestnut leather cover visible around the edges,
subtle embossed brass and dark-gold details, muted emerald accents, tiny original
motifs inspired by friendship, memories, nests, feathers and dragon scales.

The book must feel precious, alive, welcoming and intimate, like a journal the
player enjoys opening every day. Refined but restrained pixel decoration: worn
paper edges, a convincing central binding, a few delicate ink flourishes and very
light magical specks. Strong visual hierarchy and excellent readability. Keep the
central areas of BOTH pages broad, calm, pale and almost empty so dynamic text,
progress bars, cards and a 3D creature portrait can be drawn over them. Decorations
must stay near the outer margins and may never cross the content areas.

Integrate five elegant bookmark tabs along the outer edge for Today, History,
Bonds, Talents and Missions, using symbols only and no words. At both bottom outer
corners, design a subtle folded-paper page-turn control that clearly belongs to
the page itself: symmetrical left and right versions, small, elegant, readable,
never shaped like an animal, never floating above the book.

Exact visual constraints: horizontal 3:2 composition, perfectly centered open
book, flat 2D GUI view, crisp square pixels, limited harmonious palette, no
anti-aliasing, no perspective distortion, no cast shadow over the writing areas,
no text, no letters, no numbers, no runes, no interface labels, no progress bars,
no character, no creature, no hands, no desk, no room, no photorealism, no 3D
render, no watermark. Original design, not a copy of an existing game interface.
--ar 3:2 --style raw --stylize 125
```

Générer plusieurs propositions et retenir celle qui offre les deux pages les
plus lisibles. Le beau décor ne doit jamais réduire la place du contenu.

## Prompt 2 — planche d'animation de page Midjourney

Ce second prompt sert seulement à trouver le mouvement et les poses clés. Astra
reconstruira ensuite les images avec une géométrie stable.

```text
Pixel art animation reference sheet for the same open magical companion journal,
front-facing orthographic game UI. Show eight clearly separated chronological key
frames of ONE parchment page turning from right to left. The motion starts with a
small lifted bottom-right corner, forms a clean curved diagonal fold, passes over
the central binding, and settles perfectly flat on the left. Preserve exactly the
same book proportions, binding, palette, lighting and camera in every frame. The
page is completely blank and opaque; only the turning sheet and its soft underside
move. Crisp square pixels, limited palette, readable silhouettes, no smear, no
motion blur, no morphing book, no changing decorations, no text, letters, numbers,
runes, icons, character, hands, desk, perspective, photorealism or watermark.
Animation production reference, not a presentation mockup. --ar 3:2 --style raw
--stylize 50
```

## Prompt 3 — production complète par Astra

Créer une nouvelle tâche avec Astra, lui joindre les fichiers listés plus haut,
puis copier intégralement ce prompt :

```text
Tu es responsable de la direction artistique ET de la production technique de
l'interface du livre d'un mod Minecraft Fabric 1.21.1 nommé Compagnon.

Je te joins :
- un concept Midjourney qui définit l'ambiance recherchée ;
- les textures actuellement utilisées par le mod ;
- plusieurs captures en jeu qui montrent la densité réelle du contenu et les
  problèmes actuels.

Le concept Midjourney est une référence, pas un fichier à redimensionner ni à
copier aveuglément. Reconstruis une interface originale en véritable pixel art,
pixel par pixel, adaptée à un écran Minecraft. Ne réutilise aucune œuvre protégée
ni aucun élément reconnaissable provenant d'un autre jeu.

OBJECTIF ÉMOTIONNEL

Le joueur doit avoir envie d'ouvrir ce livre chaque jour pour s'occuper de son
compagnon. Il doit sembler précieux, chaleureux, vivant, un peu magique et très
lisible. L'effet « waouh » doit venir de la finition, des détails et de la cohérence,
pas d'un décor envahissant.

CONTRAINTES TECHNIQUES ABSOLUES

- Texture principale exacte : 384 × 256 pixels, PNG RGBA.
- Vue strictement de face, orthographique, sans perspective.
- Extérieur du livre totalement transparent.
- Pixel art net : aucun anti-aliasing, aucun pixel semi-transparent sur les
  contours, aucune texture floue, aucun filtrage lissé.
- Aucun texte, lettre, chiffre, rune, jauge, bouton libellé, compagnon ou modèle
  3D ne doit être peint dans le fond.
- Conserver deux zones de contenu calmes, contrastées et libres :
  page gauche x=34..175, page droite x=208..349, y=28..211.
- La reliure centrale ne doit pas empiéter sur ces zones.
- Les ornements doivent rester principalement dans les marges extérieures.
- Le bas des pages doit rester suffisamment dégagé pour les contrôles de page.
- Palette limitée et cohérente : parchemin crème chaud, ombres brun noisette,
  cuir châtaigne, détails or vieilli et accents émeraude discrets.
- Le rendu doit rester lisible avec la police pixel de Minecraft et dans les
  résolutions d'interface petites.

DIRECTION ARTISTIQUE

- Livre ancien mais entretenu, reliure en cuir épais, tranche et couture visibles.
- Papier chaleureux avec variation très légère, jamais assez forte pour gêner le
  texte gris/brun.
- Motifs originaux très discrets liés aux compagnons : lien, plume, petite écaille,
  nid ou souvenir. Ne pas transformer le livre en interface exclusivement dragon,
  car il accueillera plusieurs espèces terrestres et volantes.
- Les deux pages doivent être visuellement équilibrées et symétriques sans être
  copiées au pixel près.
- Prévoir cinq signets cohérents pour Aujourd'hui, Histoire, Liens, Talents et
  Missions. Utiliser de petits symboles compréhensibles, sans mots incrustés.

LIVRABLES OBLIGATOIRES, TOUS SÉPARÉS

1. `book_astra_v2.png` — 384 × 256 : livre seul, sans texte ni contrôles actifs.
2. `book_details_overlay.png` — 384 × 256 : détails facultatifs sur fond
   transparent. Ils ne doivent jamais recouvrir les zones de contenu.
3. `portrait_frame.png` — 142 × 76 : cadre très discret et transparent pour la
   vue 3D du compagnon. L'intérieur doit être entièrement dégagé. Le cadre ne doit
   couper ni les ailes ni le dessous du modèle.
4. `underline_astra.png` — 159 × 11 : séparateur/soulignement étirable, aux bords
   propres.
5. `bookmark_tabs.png` — planche transparente des cinq signets, avec pour chacun
   les états normal, survolé et sélectionné. Fournir une grille régulière et
   indiquer la taille exacte d'une cellule.
6. `page_turn_left.png` et `page_turn_right.png` — 29 × 28 : deux contrôles
   parfaitement symétriques représentant un coin de feuille que l'on peut saisir.
   Ils doivent être intégrables aux coins bas extérieurs, sans animal, sans gros
   triangle blanc, sans partie coupée et sans dépasser leur cadre.
7. Les variantes `normal`, `hover` et `pressed` des deux contrôles, mêmes
   dimensions et même point d'ancrage.
8. `page_turn_rtl_strip.png` — bande horizontale transparente de 8 images pour
   tourner une page de droite vers la gauche.
9. `page_turn_ltr_strip.png` — la séquence inverse, redessinée correctement et
   pas simplement retournée si la lumière ou la reliure l'interdisent.
10. Pour chaque animation : 8 cellules de 384 × 256, donc une bande de
    3072 × 256. Le livre, les bords, la reliure et la caméra restent parfaitement
    immobiles entre toutes les images. Seule la feuille tourne.
11. `book_layout_spec.md` — tableau des coordonnées exactes de toutes les zones,
    ancres, cellules de sprites, signets, boutons et hitboxes recommandées.
12. Une maquette 4× de trois écrans remplis avec du faux contenu clairement
    identifié comme maquette : Aujourd'hui, Liens et Missions. Ces maquettes servent
    uniquement à valider la lisibilité ; aucun faux texte ne doit être présent dans
    les textures de production.

ANIMATION DE PAGE

- Mouvement de 8 images lisible entre 90 et 140 ms par image.
- Départ discret au coin extérieur, soulèvement, pli diagonal, passage au-dessus
  de la reliure, puis pose naturelle sur l'autre page.
- La face avant et le dessous de la feuille doivent être distinguables par une
  nuance légère, sans ombre opaque massive.
- Aucun scintillement des pixels fixes entre les images.
- Aucun changement de taille du livre, de reliure, d'éclairage ou de palette.
- L'animation ne doit jamais cacher durablement les deux pages à la fois.
- Prévoir une version accessibilité où l'animation peut être désactivée et
  remplacée par un fondu très court ; lister les recommandations dans le fichier
  de spécification, sans peindre le fondu dans les PNG.

PROBLÈMES ACTUELS À ÉLIMINER

- Les coins de page actuels ressemblent à des formes isolées et ne donnent pas
  envie de cliquer.
- Les contrôles gauche et droite doivent être traités avec la même qualité.
- Aucun contrôle ne doit sortir du livre ou être coupé par le bord de l'écran.
- La fenêtre 3D doit offrir assez de marge en haut ET en bas pour les compagnons
  avec ailes, cornes ou longue queue.
- Les décorations ne doivent pas traverser les textes des Rituels du jour, des
  Moments, des jauges, des cartes ou de la page Liens.
- Les signets doivent rester lisibles sans voler l'attention au compagnon.

MÉTHODE ET CONTRÔLE QUALITÉ

Avant de produire les fichiers finaux :

1. Analyse les pièces jointes et résume en dix lignes maximum les problèmes de
   lisibilité et de cohérence.
2. Propose trois directions cohérentes, chacune avec palette et niveau de décor.
3. Recommande celle qui sert le mieux le mod et attends ma validation de la
   direction avant de finaliser tous les sprites.

Après validation :

- Produis réellement chaque PNG demandé, séparément, à sa taille exacte.
- Affiche aussi chaque asset agrandi à 400 % avec interpolation nearest-neighbor
  pour vérification visuelle.
- Vérifie les transparences, dimensions, symétries et zones sûres.
- Fournis un inventaire final des fichiers et signale toute contrainte que le code
  devra adapter.
- Ne prétends pas qu'un fichier est prêt s'il n'est qu'une image de présentation.

Critère de réussite : les textures doivent pouvoir être déposées dans le dossier
du mod, puis le code Java doit pouvoir être recalibré à partir de
`book_layout_spec.md` sans devoir deviner une seule coordonnée.
```

## Retour dans le projet

Une fois Astra terminé, récupérer tous les PNG originaux et
`book_layout_spec.md`, sans captures d'écran ni images recompressées. Les déposer
dans un dossier ou une archive ZIP, puis les joindre à la tâche du mod. On pourra
alors :

- remplacer les textures de manière contrôlée ;
- adapter les coordonnées et les hitboxes dans `EcranLivre` ;
- animer les changements de page sans bloquer le jeu ;
- vérifier que le modèle 3D n'est coupé ni en haut ni en bas ;
- compiler et effectuer la validation finale en jeu.

