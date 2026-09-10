# État actuel — 10 septembre 2026

## Base du projet

- Mod Minecraft Fabric 1.21.1.
- Un cerveau commun, configurable par fichiers, pilote toutes les espèces.
- La mémoire, la perception, les personnalités et les intentions sont communes
  aux compagnons, avec des capacités propres à chaque espèce.
- Les animations sont choisies par rôle afin qu'une espèce puisse remplacer ses
  gestes sans modifier le code Java.
- La voix fonctionne hors ligne avec Plasmo Voice, Vosk et un mode de diagnostic.

## Compagnons et vie quotidienne

- Dragonnet et autres espèces déclarées par fichiers de données.
- Faim, énergie, santé, complicité, goûts alimentaires et souvenirs persistants.
- Gamelle, gamelle d'eau, coussin et perchoir intégrés au coin personnel.
- Rituels quotidiens, missions, progression et interactions vocales.
- Les systèmes coûteux sont temporisés et limités pour rester adaptés à un serveur.

## Interfaces

- Journal complet : Aujourd'hui, Histoire, Liens, Talents et Missions.
- Carnet de sélection des compagnons.
- 27 thèmes visuels disponibles.
- Six thèmes HD intégrés : Sylvestre, Observatoire, Nuages, Lucioles, Marées et
  Confiserie.
- Les nouveaux thèmes utilisent deux zones de texte fixes de 142 x 184 pixels,
  des couvertures adaptatives et leurs propres signets.
- Le carnet de sélection et le journal utilisent maintenant le même habillage HD.
- Cinq livres continus supplémentaires sont intégrés : Médiéval, Bestiaire,
  Vitrail, Horlogerie et Porcelaine.
- Le sélecteur ouvre une bibliothèque adaptative avec aperçu, choix direct,
  pagination, favoris persistants et filtre « Mes favoris ».
- Les morceaux de page superposés ont disparu du journal et du carnet : les coins
  inférieurs sont désormais des zones cliquables invisibles de 38 × 38 pixels.
- Le panneau de santé compact montre désormais faim, énergie, santé et complicité,
  avec un diagnostic prioritaire en toutes lettres.
- La touche H ouvre une fiche de santé interactive sans mettre le jeu en pause :
  choix du compagnon, valeurs exactes, conseil, accès au journal, affichage du HUD
  et sélecteur d'habillage.
- Trois palettes de départ sont intégrées au carnet de santé : Parchemin,
  Sylvestre et Nocturne. Le choix reste local et persiste dans la configuration.

## Caresse

- Une première série de trente défis est intégrée.
- Elle doit être reprise : les défis actuels reposent sur trop peu de mécaniques
  réellement différentes et donnent une sensation répétitive.

## Vérification connue

- Dernière compilation complète réussie.
- 146 tests automatisés réussis.
- Les six thèmes HD sont présents dans le JAR avec 24 ressources chacun.
- La validation visuelle finale doit toujours être faite dans Minecraft.
