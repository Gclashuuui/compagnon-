# Bibliothèque des apparences — référence d'intégration

## Source

- Archive reçue : `output/bibliotheque_apparences.zip`.
- Direction artistique : bois sombre, laiton, papier ivoire et étoiles dorées.
- Format logique de l'écran : 416 × 284 pixels, textures livrées en ×4.

## Ressources fonctionnelles

- `library_panel_hd.png` : panneau principal, 1664 × 1136.
- `library_card_states_hd.png` : normal, survolé, sélectionné et
  sélectionné-favori, 1504 × 272.
- `favorite_button_states_hd.png` : étoile vide/active et leurs survols,
  256 × 64.
- `filter_tabs_states_hd.png` : filtres Tous/Favoris et leurs survols,
  1664 × 88.
- `pagination_states_hd.png` : précédent, suivant et points de page,
  432 × 72.
- `empty_favorites_hd.png` : illustration de la liste vide, 256 × 256.
- `library_details_overlay.png` : détails de premier plan, 1664 × 1136.

## Règles conservées dans le code

- Grille fixe de 4 × 3 cartes, soit 12 livres par page.
- Le panneau entier est centré et réduit uniformément si la résolution manque.
- Les aperçus et tous les textes sont rendus dynamiquement par Minecraft.
- Chaque aperçu complet est ramené à 54 × 36 pixels et centré en (20, 12) dans
  sa carte ; aucune partie du livre n'est coupée.
- Le titre complet est centré sur une ligne dans son cartouche. Les textes utilisent
  la police régulière `minecraft:uniform` avec une échelle adaptée à leur zone.
- Les noms trop longs sont abrégés visuellement et restent complets en infobulle.
- Clic gauche sur une carte : appliquer le thème. Clic sur l'étoile ou clic droit :
  ajouter ou retirer le favori sans fermer la bibliothèque.
- Les favoris et le thème choisi restent des préférences locales persistantes.
- Les PNG ne contiennent ni texte final, ni couverture de livre, ni glyphe Unicode.
