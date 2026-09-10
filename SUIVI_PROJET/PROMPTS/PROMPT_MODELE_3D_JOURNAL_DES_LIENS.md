# Prompt — prototype 3D du Journal des Liens

Joindre à la nouvelle conversation l'archive
`journal_des_liens_references_22_themes.zip`, puis envoyer le texte ci-dessous.

```text
Tu es modeleur Blockbench spécialisé dans les objets Minecraft Java Edition.

Je développe un mod Fabric 1.21.1 consacré à des compagnons vivants. L'archive
jointe contient les 22 thèmes visuels actuels de son interface. Ces images sont
uniquement des RÉFÉRENCES ARTISTIQUES : ne les colle pas directement sur le modèle,
ne les redimensionne pas pour fabriquer l'UV et ne reproduis pas la forme du grand
livre ouvert de l'interface.

OBJECTIF DE CETTE PREMIÈRE ÉTAPE

Crée UN SEUL prototype exceptionnel d'un livre fermé tenu en main :
« Journal des Liens ». Ne crée pas encore les 22 variantes. Nous devons d'abord
valider parfaitement le modèle principal en cuir.

Le livre appartient à un univers magique chaleureux. Il conserve les souvenirs,
la progression et la relation entre un joueur et toutes sortes de compagnons.
Il ne doit pas ressembler à un jouet, à un livre générique de Minecraft ou à un
accessoire réservé aux chiens, aux chats ou aux dragons.

SILHOUETTE RECHERCHÉE

- Livre fermé, vertical et immédiatement reconnaissable dans la main.
- Cuir brun châtaigne épais, souple mais précieux, avec grain visible en pixel art.
- Couvertures avant et arrière réellement épaisses.
- Bloc de pages ivoire légèrement rentré entre les couvertures.
- Pages visibles sur les trois côtés, avec quelques lignes irrégulières discrètes.
- Tranche arrondie ou segmentée par trois nerfs de cuir.
- Quatre coins renforcés en laiton vieilli.
- Petit fermoir latéral crédible, sans pièce flottante.
- Fin marque-page bordeaux dépassant sous le livre.
- Relief central très léger représentant deux anneaux entrelacés autour d'une étoile.
- Cartouche frontal propre destiné au titre « Journal des Liens ».
- L'arrière porte une version plus discrète de l'emblème.

Le résultat doit être riche quand on le regarde de près, mais garder une silhouette
lisible dans l'inventaire. Les détails minuscules qui deviennent du bruit à 16 pixels
sont interdits.

CONTRAINTES BLOCKBENCH

- Format d'objet/bloc Minecraft Java, pas un modèle GeckoLib et pas une entité.
- Fournir le fichier source `.bbmodel` modifiable.
- Fournir un export de modèle Minecraft Java `.json` valide.
- Modèle contenu dans l'espace 16 x 16 x 16.
- Environ 10 à 20 cuboïdes propres ; pas une simple plaque plate.
- Origines et pivots nommés clairement.
- Groupes recommandés : `cover_front`, `cover_back`, `pages`, `spine`, `corners`,
  `clasp`, `bookmark`, `emblem`.
- Aucune face coplanaire, aucun chevauchement, aucun z-fighting.
- Aucun cube invisible ou inutile.
- Aucune face manquante quand on regarde l'avant, l'arrière, la tranche, le dessus ou
  le dessous.
- Identifier explicitement quelle face est l'avant.
- L'arrière ne doit pas être retourné en miroir.
- Ne pas utiliser de plans transparents pour simuler l'épaisseur du cuir.

TEXTURE DU PROTOTYPE

- Un seul PNG RGBA de 128 x 128 pixels.
- Véritable pixel art net, sans anticrénelage, flou ou texture photographique.
- Palette limitée et cohérente : cuir châtaigne, brun profond, ivoire, laiton vieilli,
  bordeaux et une très petite touche émeraude.
- Aucun éclairage directionnel ni ombre portée peinte dans la texture.
- Patron UV sans chevauchement pour les parties importantes.
- Zones UV séparées pour l'avant, l'arrière, la tranche, les pages, le fermoir,
  le marque-page et l'emblème.
- Fournir `journal_des_liens_uv.png`, annoté et lisible.
- Tous les futurs thèmes devront pouvoir remplacer cette texture sans déplacer un
  seul UV.

TEXTE ET EMBLÈME

Le titre frontal doit être lisible :

JOURNAL
DES LIENS

Si tu ne peux pas produire ces lettres parfaitement en pixel art, laisse le cartouche
entièrement propre et vide. N'invente aucune pseudo-lettre. Le titre sera ajouté après
avec une vraie police pixel. Même règle pour la petite inscription « LIENS » sur la
tranche.

VUES D'OBJET

Prépare des transformations de départ pour :
- inventaire GUI : couverture avant visible en trois quarts, centrée et non coupée ;
- première personne droite et gauche : titre tourné vers le joueur, livre bien visible ;
- troisième personne droite et gauche : livre tenu naturellement contre la paume ;
- au sol : livre posé à plat sans flotter.

Ces transformations seront recalibrées manuellement ensuite. Ne déforme jamais la
géométrie pour corriger une vue.

À NE SURTOUT PAS REFAIRE

- Pas de 22 géométries différentes.
- Pas de modèle presque invisible dans la main.
- Pas de livre gigantesque qui cache l'écran.
- Pas de couverture arrière copiée ou miroir de l'avant.
- Pas de texture 1920 x 1152 issue de l'interface.
- Pas de damier de transparence peint dans le PNG.
- Pas de lettres générées illisibles.
- Pas de métal jaune plat ressemblant à un bloc d'or Minecraft.
- Pas de pages parfaitement blanches et sans relief.
- Pas de fermoir, coins ou emblème flottant au-dessus de la couverture.
- Pas de rendu seulement présenté en image : je dois recevoir les vrais fichiers.

LIVRAISON DE LA PREMIÈRE ÉTAPE

Crée une archive ZIP contenant uniquement :
- `journal_des_liens_prototype.bbmodel`
- `journal_des_liens_prototype.json`
- `journal_des_liens_cuir.png`
- `journal_des_liens_uv.png`
- cinq aperçus PNG : avant, arrière, tranche, inventaire et première personne
- `CONTROLE.md` indiquant les dimensions, le nombre de cubes, les zones UV et les
  éventuelles limites restantes.

Avant de livrer, ouvre réellement le `.bbmodel`, vérifie toutes les faces et compare
les cinq aperçus. Ne commence les variantes qu'après validation de ce prototype.
```
