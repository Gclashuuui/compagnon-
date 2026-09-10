# État actuel — 10 septembre 2026

## Base du projet

- Mod Minecraft Fabric 1.21.1.
- Un cerveau commun, configurable par fichiers, pilote toutes les espèces.
- La mémoire, la perception, les personnalités et les intentions sont communes
  aux compagnons, avec des capacités propres à chaque espèce.
- Un état intérieur borné fait évoluer lentement stress, ennui, besoin de contact
  et envie d'explorer à partir des perceptions déjà calculées.
- La curiosité s'habitue aux événements répétés d'un même chantier et déclenche
  une vraie petite réaction d'inspection à l'arrivée.
- Deux habitudes quotidiennes décalées par UUID donnent un réveil et un retour au
  calme non synchronisés. L'approche sociale distingue désormais personne connue
  et inconnue et choisit des rôles d'animation différents.
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
- Onze thèmes HD intégrés : Sylvestre, Observatoire, Nuages, Lucioles, Marées,
  Confiserie, Médiéval, Bestiaire, Vitrail, Horlogerie et Porcelaine.
- Les nouveaux thèmes utilisent deux zones de texte fixes de 142 x 184 pixels,
  des couvertures adaptatives et leurs propres signets.
- Le carnet de sélection et le journal utilisent maintenant le même habillage HD.
- Les cinq livres merveilleux possèdent leurs propres signets, cartes de rituel,
  cadre de portrait, séparateur, détails et animations de changement de page.
- Le sélecteur ouvre une bibliothèque illustrée bois et laiton, redimensionnée
  selon l'écran, avec les vrais aperçus des livres, choix direct et pagination.
- Chaque carte possède quatre états visuels. Les favoris persistants utilisent
  une étoile PNG et deux filtres directs « Tous » / « Favoris » ; aucun caractère
  d'icône incompatible n'est utilisé.
- Les aperçus affichent maintenant le livre entier en 54 × 36 pixels, centré dans
  sa vitrine sans rognage. Le titre tient sur une seule ligne centrée.
- Le titre, les filtres et les noms de thèmes utilisent la police Unicode régulière
  du jeu avec une échelle propre, indépendante de la police décorative du pack.
- Les morceaux de page superposés ont disparu du journal et du carnet : les coins
  inférieurs sont désormais des zones cliquables invisibles de 38 × 38 pixels.
- Le HUD de santé permanent est désormais un panneau RP minimal de 112 × 38 pixels,
  placé à huit pixels du coin supérieur gauche. Il n'affiche aucun grand décor.
- Treize cadres discrets lui donnent la palette du carnet choisi sans modifier sa
  taille : Parchemin, Sylvestre, Nocturne et les dix thèmes enchantés.
- Ses quatre besoins utilisent des pictogrammes géométriques dessinés pixel par
  pixel — nourriture, éclair, croix et cœur. Les jauges ont un sillon sombre,
  trois niveaux de lumière, un reflet terminal et des repères de quart discrets.
- Sous 25 %, la jauge concernée respire lentement entre deux rouges ; le témoin
  placé devant le nom reprend aussi la couleur de l'état prioritaire.
- La touche H ouvre une fiche de santé interactive sans mettre le jeu en pause :
  choix du compagnon, valeurs exactes, conseil, accès au journal, affichage du HUD
  et sélecteur d'habillage.
- Treize habillages sont intégrés au carnet de santé : les trois palettes de
  départ et dix carnets enchantés illustrés. Le choix reste local, persiste dans
  la configuration et se fait dans une galerie directe.
- Les carnets enchantés habillent la fiche détaillée lorsqu'elle est ouverte ;
  ils ne perturbent plus l'écran de jeu pendant le RP.
- La galerie des carnets est réduite à quatre cartes par ligne. Les couvertures
  HD y sont affichées à exactement 50 %, ce qui évite le flou d'échantillonnage.

## Caresse

- Une première série de trente défis est intégrée.
- Elle doit être reprise : les défis actuels reposent sur trop peu de mécaniques
  réellement différentes et donnent une sensation répétitive.

## Vérification connue

- Dernière compilation complète réussie.
- 166 tests automatisés réussis.
- Les cinq thèmes merveilleux sont présents dans le JAR avec leurs 12 ressources
  propres chacun ; aucun de leurs éléments ne retombe sur le thème Sylvestre.
- La validation visuelle finale doit toujours être faite dans Minecraft.
