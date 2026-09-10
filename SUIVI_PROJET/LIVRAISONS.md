# Livraisons

## Treize cadres discrets du HUD

- Commit : `4680a3c` — Cadres thématiques et nouvelles jauges compactes.
- Les treize textures 448 × 152 sont rendues en 112 × 38, à l'intérieur du HUD
  RP, sans débordement ni information incorporée.
- Les jauges dynamiques gagnent un sillon, trois tons de remplissage, un reflet,
  des repères de quart et une pulsation rouge sous 25 %.
- Les pictogrammes conservent leur géométrie nette et gagnent une ombre d'un pixel.
- Les treize cadres et leurs blocs 4 × 4 sont verrouillés par les tests.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- SHA-256 : `AF0CF51967B5DCFA8E0486982593A19076E04AC5669545E4C7A2B2BF185901C5`.
- Vérification : compilation réussie, 154 tests réussis, 13 cadres présents dans le JAR.

## Recentrage de la bibliothèque

- Commit : `ba42271` — Aperçus reculés et typographie recalibrée.
- Les livres sont désormais affichés entièrement en 54 × 36 pixels, centrés et
  sans étirement ni rognage.
- « Bibliothèque des apparences » tient sur une ligne centrée dans son cartouche.
- Titre, filtres et noms utilisent la police régulière `minecraft:uniform` avec
  une échelle propre à chaque zone.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- SHA-256 : `BA753812A6FD55150B04653CFC461C6B9DF8EFF0FE7D435E316F8B30ABA81A67`.
- Vérification : compilation réussie, 153 tests réussis.

## HUD de santé RP minimal

- Commit : `bed5c25` — HUD permanent compact et galerie mieux cadrée.
- Le HUD passe de 136 × 54 pixels — ou 296 × 194 avec décor — à un format
  unique de 112 × 38 pixels placé en haut à gauche.
- Les décors illustrés ne sont plus chargés pendant le jeu normal ; ils restent
  réservés à la fiche de santé ouverte volontairement.
- Nourriture, énergie, santé et complicité utilisent quatre formes pixel nettes
  rendues par le code, sans redimensionnement flou.
- La galerie affiche quatre petites cartes par ligne et réduit les couvertures
  exactement à 50 %.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- SHA-256 : `1DD2D93F9FD05D6E633D14DFBA1CD5F6A725699088ED078A94E85AEC33246827`.
- Vérification : compilation réussie, 152 tests réussis.

## Bibliothèque illustrée des apparences

- Commit : `41a7730` — Nouvelle bibliothèque bois et laiton avec favoris.
- Douze thèmes par page, aperçus réels, noms dynamiques et infobulles complètes.
- États graphiques pour les cartes, les étoiles, les filtres et la pagination.
- Filtres directs « Tous » / « Favoris », état vide illustré et pagination
  compatible avec une collection amenée à grandir.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- SHA-256 : `99CCC24D50A23C1C3644F8AEAFE151C29A51ADEC7EB7BFC7FEF86C811C2402F1`.
- Vérification : compilation réussie, 151 tests réussis.

## Collection détaillée des carnets de santé

- Commit : `7d2ba8d` — Dix carnets enchantés pour la fiche et le HUD.
- Nouveaux habillages : cinq dragons, Phénix, Renard lunaire, Glycine,
  Œuf cosmique et Champignons.
- Total disponible dans la galerie : 13 habillages persistants.
- Le bouton d'habillage ouvre une galerie directe ; le clic droit conserve le
  raccourci vers le thème précédent.
- Les textes, diagnostics et jauges restent dynamiques et suivent le gabarit
  commun du pack.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- SHA-256 : `64163D0551813DFD5AD3CBABA657D1D19245149B9D36ECD8D2EC6F52E90A81AD`.
- Vérification : compilation réussie, 150 tests réussis.

## Compléments v6 des livres merveilleux

- Commit : `16fe1d0` — Signets, rituels, cadres, détails et animations propres aux
  cinq livres merveilleux.
- Le fallback vers Sylvestre est supprimé ; chaque thème contient 12 ressources.
- Les pages tournent en huit images à 70 ms par image.
- Le libellé du filtre des favoris n'est plus rendu avec la police d'icônes : les
  carrés blancs observés à l'écran disparaissent.
- Prompt de refonte : `PROMPTS/PROMPT_BIBLIOTHEQUE_APPARENCES_HD.md`.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- SHA-256 : `7B388A6DC5638A30AFB37798B114E4EB52096E795C51E42CA97F529CE4F0CB01`.
- Vérification : compilation réussie, 149 tests réussis.

## Bibliothèque des livres merveilleux

- Commit : `4236254` — Bibliothèque, favoris et cinq thèmes continus.
- Nouveaux thèmes : Médiéval, Bestiaire, Vitrail, Horlogerie et Porcelaine.
- Total : 27 thèmes, dont 11 thèmes haute définition.
- Les anciennes flèches superposées ne sont plus dessinées ; les coins de page
  restent cliquables de manière invisible.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- Vérification : compilation réussie, 148 tests réussis.

## Carnet de santé

- Commit : `8200a23` — Refonte du carnet de santé du compagnon.
- Le panneau compact affiche quatre besoins et un diagnostic prioritaire.
- La fiche interactive s'ouvre avec H et donne les valeurs, un conseil, l'accès
  au journal, la visibilité du HUD et le choix de l'habillage.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- Vérification : compilation réussie, 146 tests réussis.

## Dernière version du livre

- Commit : `31b0b45` — Intégrer les six journaux haute définition.
- Branche : `main`.
- Dépôt : `https://github.com/Gclashuuui/compagnon-.git`.
- JAR local : `build/libs/compagnon-0.1.0.jar`.
- Vérification : compilation réussie, 146 tests réussis.

## Jalons récents

- `ca09722` — Icônes des trente caresses.
- `2f16136` — Rituels et variantes de caresse.
- `91722b7` — Douze variantes de livre et rituels.
- `e45db19` — Quatre premiers thèmes animés.
- `3ab29ed` — Interactions et optimisation du cerveau.
- `84c2329` — Mémoire et perception.
- `fc409f0` — Cerveaux configurables.
- `19ab329` — Cerveau commun aux compagnons.
