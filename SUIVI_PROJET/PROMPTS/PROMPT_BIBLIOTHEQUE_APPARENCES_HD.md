# Prompt — bibliothèque des apparences du Journal des Liens

Je développe une interface Minecraft Fabric 1.21.1 appelée **Bibliothèque des
apparences**. Elle permet de choisir l'apparence du Journal des Liens parmi plus
de 27 livres, d'ajouter des thèmes aux favoris et de filtrer le catalogue.

Je vais te fournir deux captures de l'interface actuelle et plusieurs textures
de livres. Les captures servent uniquement à comprendre les fonctions et les
proportions : **ne recopie pas leur grand panneau violet ni leurs boutons**.

## Direction artistique recherchée

Refais entièrement cette interface comme une petite bibliothèque magique de
collectionneur, élégante, chaleureuse et très lisible : cuir brun-prune sombre,
bois fin, laiton vieilli, papier ivoire et quelques reflets dorés. L'ensemble
doit être raffiné et merveilleux, mais rester discret pour que les couvertures
très colorées soient les éléments principaux.

Le panneau doit évoquer un meuble-bibliothèque ou un écrin de bibliothécaire,
pas une fenêtre informatique plate. Chaque livre est présenté dans une petite
fiche de catalogue avec une vraie profondeur, des angles travaillés et une zone
de nom claire. Les ornements restent fins : aucune grosse décoration ne doit
réduire les aperçus ou gêner le texte.

Style : pixel art Minecraft premium, pixels nets, détails peints à la main,
aucun anti-aliasing flou, aucune 3D photoréaliste, aucune texture moderne ou
plastique. L'interface doit rester belle en mouvement devant le monde Minecraft.

## Composition du nouvel écran

- En haut à gauche : emplacement vide pour le titre « Bibliothèque des
  apparences ».
- En haut à droite : un sélecteur en deux onglets illustrés, « Tous les livres »
  et « Favoris ». Utilise une pile de livres et une étoile en métal doré. Les
  icônes doivent être de vrais PNG : **aucun emoji ni caractère Unicode**.
- Au centre : grille de quatre colonnes et trois lignes. Chaque carte contient
  un grand aperçu horizontal du livre et une bande basse réservée à son nom.
- Sur chaque carte : un petit bouton favori en haut à droite de l'aperçu. Il
  ressemble à un marque-page ou un médaillon étoilé, pas à un signe « + ».
- État sélectionné : coins dorés fins, légère lumière chaude et petit sceau avec
  coche. Ne couvre jamais l'aperçu du livre.
- État survolé : relief et lumière légèrement renforcés, sans déplacement de la
  mise en page.
- En bas : pagination centrale élégante avec points ou petits signets, flèche
  gauche et flèche droite intégrées au cadre.
- Prévoir un état « aucun favori » avec un petit ex-libris étoilé, sans texte
  dessiné dans l'image.

## États à dessiner

Pour les cartes : normal, survolé, sélectionné et sélectionné + favori.

Pour le bouton favori : vide, vide survolé, favori actif, favori actif survolé.

Pour le filtre : tous normal, tous survolé, favoris normal, favoris survolé.

Pour la pagination : précédent normal/survolé, suivant normal/survolé, point de
page normal et point de page actif.

## Contraintes essentielles

- Ne dessine **aucun mot, aucune lettre et aucun chiffre** dans les PNG. Le mod
  ajoutera tous les textes avec sa vraie police, afin d'éviter les faux mots et
  les carrés illisibles.
- Ne mets aucun aperçu de livre définitif dans les cartes exportées : leur
  centre doit être transparent, car le mod y dessinera les thèmes réels.
- Fonds transparents RGBA partout en dehors des formes.
- Conserve de grandes zones calmes pour les noms français parfois longs.
- Les étoiles, coches et flèches doivent rester parfaitement reconnaissables à
  petite taille.
- Palette cohérente sur tous les fichiers, sans bordure violette plate comme
  l'ancienne interface.
- Tous les éléments doivent pouvoir être assemblés sans couture visible.

## Fichiers attendus dans un ZIP

Crée d'abord un mockup complet `preview_bibliotheque.png` en 1664 × 1136 px,
correspondant à une interface logique de 416 × 284 px affichée à l'échelle 4.

Puis fournis les éléments fonctionnels séparés :

1. `library_panel_hd.png` — 1664 × 1136 px. Fond complet du panneau, sans texte,
   sans cartes et sans boutons, avec transparence autour de la silhouette.
2. `library_card_states_hd.png` — 1504 × 272 px. Bande horizontale de 4 images
   de 376 × 272 px : normal, survolé, sélectionné, sélectionné + favori. La
   fenêtre d'aperçu est transparente.
3. `favorite_button_states_hd.png` — 256 × 64 px. Bande de 4 images de 64 × 64
   px : vide, vide survolé, actif, actif survolé.
4. `filter_tabs_states_hd.png` — 1664 × 88 px. Bande de 4 images de 416 × 88 px :
   tous normal, tous survolé, favoris normal, favoris survolé. Prévois une zone
   vide à droite de l'icône pour le texte du mod.
5. `pagination_states_hd.png` — 432 × 72 px. Bande de 6 images de 72 × 72 px :
   précédent normal, précédent survolé, suivant normal, suivant survolé, point
   normal, point actif.
6. `empty_favorites_hd.png` — 256 × 256 px. Ex-libris étoilé centré, sans texte.
7. `library_details_overlay.png` — 1664 × 1136 px. Petites lumières et détails
   décoratifs uniquement, centre des cartes et zones de texte transparents.
8. `layout.json` — dimensions, ordre exact des états, marges transparentes et
   zones sûres de chaque fichier.

Ajoute une planche `states_preview.png` qui montre tous les états sur un fond
gris neutre, à leur taille native et à l'échelle 4, ainsi qu'un
`verification.json` confirmant les dimensions, le mode RGBA, la transparence des
fenêtres et l'absence de texte incorporé.

Le résultat doit donner envie de collectionner les livres : clair au premier
regard, luxueux sans être chargé, et assez neutre pour convenir aussi bien à un
livre médiéval, marin, céleste, sombre, sakura ou porcelaine.
