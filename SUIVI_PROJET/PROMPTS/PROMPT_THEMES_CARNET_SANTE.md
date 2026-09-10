# Prompt — Habillages et icônes du carnet de santé

À copier dans une nouvelle conversation avec Astra ou un outil de création
d'images. Remplace la zone `MES IDÉES` avant l'envoi.

---

Je développe l'interface d'un mod Minecraft 1.21.1 consacré à des compagnons
fantastiques. Je veux créer plusieurs habillages interchangeables pour un
**carnet de santé de compagnon**, visible en petit en haut à gauche puis ouvert
en fiche détaillée. Ce n'est ni une interface médicale moderne, ni un HUD de jeu
de tir : cela doit ressembler à un petit objet précieux appartenant à l'univers,
fait à la main, chaleureux, magique et lisible.

## Mes idées de thèmes

[MES IDÉES : écris ici les ambiances, couleurs, matières et symboles que je veux.
Exemple : cuir bordeaux d'apothicaire, herbier sylvestre, nuit étoilée, maison
douillette, bibliothèque ancienne, océan enchanté.]

## Direction artistique obligatoire

- Pixel art net inspiré de Minecraft, sans lissage, sans photoréalisme et sans
  pixels semi-flous.
- Un contour cohérent, des volumes lisibles, quelques reflets et ombres en
  escalier, mais pas de surcharge.
- Élégant, attachant, légèrement magique ; convient à un dragon, un oiseau, une
  créature à poils ou toute autre espèce.
- Aucun symbole exclusivement vétérinaire moderne, aucun stéthoscope en plastique,
  aucune croix d'hôpital omniprésente.
- Les quatre besoins doivent être reconnaissables immédiatement :
  **faim, énergie, santé, complicité**.
- Ne dessine aucun mot, aucune lettre, aucun nombre et aucun faux texte dans les
  images : le code Minecraft ajoutera le nom, les pourcentages et les phrases.
- Ne dessine jamais un damier gris et blanc. Tout ce qui entoure l'asset doit être
  réellement transparent.

## Composition à respecter

Crée d'abord une planche de présentation montrant, pour chaque thème :

1. Un panneau compact au ratio **136 × 54** : nom en haut, petite zone de
   diagnostic dessous, puis quatre petites jauges en grille 2 × 2.
2. Une fiche détaillée au ratio **300 × 206** : titre et deux petits boutons en
   haut, onglets de compagnons, quatre grandes jauges horizontales, phrase de
   conseil et bouton d'ouverture du journal.
3. Les états repos, survol et appuyé du petit bouton de thème.
4. Une famille d'icônes cohérente : faim, énergie, santé, complicité, heureux,
   calme, à surveiller, très faim, épuisé et besoin de soins.

Les cadres peuvent changer de matière et d'ornements selon le thème, mais les
zones de texte et les jauges doivent rester exactement aux mêmes places. Le
changement de thème ne doit jamais obliger à recalibrer le code.

## Contraintes de lisibilité

- Préserve une zone calme derrière chaque texte ; aucun motif détaillé sous les
  lettres.
- Fort contraste entre fond, encre, jauges vides et jauges pleines.
- Les quatre couleurs de jauge restent distinctes même pour une personne ayant
  du mal à différencier certaines couleurs ; la forme de l'icône doit suffire.
- Les alertes doivent pouvoir devenir rouges sans jurer avec le thème.
- Le panneau compact doit rester lisible à sa taille réelle, pas seulement sur
  une grande planche agrandie.
- Pas de gros dessin décoratif qui mange la place des informations.

## Exports demandés après validation de la planche

Pour **chaque thème**, exporte séparément, sans marge extérieure :

- `hud_compact.png` — fond vide du panneau, ratio exact 136 × 54 ;
- `health_card.png` — fond vide de la fiche, ratio exact 300 × 206 ;
- `theme_button_normal.png`, `theme_button_hover.png`,
  `theme_button_pressed.png` — même taille et même cadrage ;
- `visibility_button_normal.png`, `visibility_button_hover.png`,
  `visibility_button_pressed.png` — même taille et même cadrage ;
- `health_icons.png` — sprite sheet régulière des dix icônes, une seule icône par
  case, toutes les cases strictement identiques ;
- une petite palette écrite sous forme de valeurs hexadécimales : couleur de
  l'encre principale, encre secondaire, contour, creux des jauges et accent.

Les PNG peuvent être produits en ×4 pour conserver un beau pixel art, soit
544 × 216 pour le panneau et 1200 × 824 pour la fiche, à condition de garder un
pixel logique parfaitement carré et de ne jamais modifier les proportions.
Présente aussi une version à taille réelle pour vérifier la lisibilité.

## Ce qu'il ne faut surtout pas refaire

- Ne fusionne pas les icônes avec le fond : elles doivent rester remplaçables.
- Ne place pas de cœur comme symbole de santé **et** de complicité : la santé
  utilise une feuille, un pansement ou un symbole de soin ; la complicité utilise
  le cœur ou deux liens entrelacés.
- Ne change pas la position des éléments entre les thèmes.
- Ne livre pas une simple recoloration automatique : chaque thème doit avoir ses
  propres matières et quelques détails uniques, tout en gardant la même grille.
- Ne crée pas trente micro-ornements illisibles. À l'échelle Minecraft, une belle
  silhouette et trois bons détails valent mieux qu'un dessin surchargé.

Commence uniquement par la planche comparative complète. Attends ma validation
avant de produire les fichiers séparés.

