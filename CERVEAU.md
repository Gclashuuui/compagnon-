# Architecture du cerveau des compagnons

Le cerveau est une IA de jeu locale et déterministe. Il ne contacte aucun
service, n'utilise aucun modèle génératif et ne coûte rien par requête.

Son trajet est toujours le même :

`capteurs → mémoire courte → état intérieur → motivation → but → engagement → animation`

## Un état intérieur, sans IA coûteuse

Le cerveau entretient maintenant quatre tensions lentes entre 0 et 1 : le
stress, l'ennui, le besoin de contact et l'envie d'explorer. Elles ne sont pas
tirées au hasard à chaque décision : elles montent et redescendent
progressivement selon ce qui vient d'être perçu, l'énergie, la complicité et le
caractère.

Une menace coupe ainsi l'élan d'exploration ; l'inactivité nourrit l'ennui ; un
compagnon très attaché cherche davantage son maître après une absence ; la
présence d'un ami l'occupe. Ces valeurs pèsent actuellement la curiosité et les
initiatives affectives. Elles sont recalculées une fois par seconde près d'un
joueur, et jusqu'à une fois toutes les quatre secondes hors de vue.

Cette couche contient exactement quatre nombres. Elle ne lance aucune recherche
d'entité, ne contacte aucun service et ne grossit jamais en mémoire.

Pour les réglages, `/compagnon info` affiche la branche active, le rythme de
calcul, le nombre de souvenirs courts ainsi que les quatre tensions en
pourcentage. Rien n'est envoyé aux joueurs et aucune mesure supplémentaire ne
tourne lorsque la commande n'est pas utilisée.

## Niveau de détail permanent

Le cerveau possède trois rythmes automatiques : **proche** (moins de 24 blocs),
**moyen** (24 à 64 blocs) et **lointain**. Les ordres, le suivi, la survie, la
nourriture, l'eau et le sommeil restent immédiats. Les envies invisibles comme
flâner, observer ou chercher un ami sont espacées quand aucun joueur ne peut les
voir. Les capteurs suivent le même rythme et chaque UUID conserve son décalage :
les compagnons ne réfléchissent jamais tous sur le même tick.

La gamelle d'eau utilise un index par chunks. Chercher à boire consulte seulement
les chunks voisins au lieu de parcourir tous les blocs de la maison. Sa position
n'est gardée que tant que le monde existe et les entrées devenues invalides sont
nettoyées au passage.

## Deux mémoires, pas une sauvegarde infinie

La mémoire longue existante reste dans la fiche : personnes et compagnons
connus, quatre lieux importants, quatre heures de repas, huit mots appris,
manies, goûts et moments du livre. Chaque collection possède déjà un plafond.

La nouvelle mémoire courte sert uniquement à agir maintenant. Elle retient les
signaux suivants : maître proche ou immobile, maître en danger, événement du
monde, menace, ami proche, pluie, orage, neige, froid, obscurité et eau.

Elle est composée de tableaux fixes indexés par le type du signal. Un nouveau
souvenir remplace l'ancien du même type et expire par comparaison d'horloge :
aucune liste à nettoyer, aucune entité conservée en référence, aucune croissance
possible de la sauvegarde.

Les événements du monde possèdent aussi une habituation fixe : les deux premiers
gestes d'un chantier attirent l'attention, puis seulement un rappel sur cinq.
Changer d'endroit ou attendre dix secondes rend l'événement immédiatement neuf.
Un compagnon reste donc curieux sans courir vers chacun des cent blocs d'un mur.

## Perception optimisée

- Environnement et maître : une lecture tous les 10 ticks.
- Menaces : toutes les 3 secondes dans le noir avec le maître présent.
- Surveillance calme ou en plein jour : toutes les 10 secondes.
- Compagnons familiers : toutes les 5 secondes, seulement lorsque la créature
  est libre et sociable.
- Chaque UUID produit un décalage différent : les compagnons ne calculent pas
  tous pendant le même tick.
- Une recherche de menace ou d'ami est mémorisée puis partagée entre les
  réactions, les buts sociaux et les souvenirs.
- `/compagnon perf` mesure désormais séparément le coût « Cerveau ».

## Personnalités conservées

Les cinq personnalités et leurs cinq traits restent les mêmes : sociabilité,
attachement, vivacité, affection et curiosité. Le cerveau en déduit aussi le
courage et la patience sans ajouter de donnée aux anciennes sauvegardes.

Ainsi tous les compagnons possèdent les mêmes besoins, mais pas les mêmes
réponses : un timide cherche son maître pendant l'orage, un vif observe, un
affectueux vient plus souvent près du joueur, un jaloux ignore les scènes entre
compagnons et un calme attend davantage.

## Vie affective

Lorsqu'il est libre et que son maître reste immobile, un compagnon peut décider
rarement de venir partager un moment. Le score mélange affection, attachement,
complicité, humeur et personnalité. La scène possède ensuite 90 secondes de
repos minimum.

Deux amis proches peuvent aussi jouer une petite scène simultanée. Les rôles
`affection` et `scene_ami` sont utilisés s'ils existent. Sinon le cerveau
réutilise les animations déjà disponibles : `joie`, `joyeux`, `salut` ou
`ecoute`. On peut donc tester le comportement avant de produire de nouveaux
fichiers Blockbench.

## Engagement des animations

Chaque action possède un rang : ambiance, affectif, besoin, ordre, événement ou
urgence. Une action de rang égal ou inférieur attend la fin du geste en cours.
Un ordre peut interrompre un geste affectif ; une menace peut interrompre un
besoin ; une simple manie ne coupe jamais une animation importante.

Les transitions de marche, course, vol et plané sont également verrouillées
jusqu'à leur fin, avec une limite de sécurité de trois secondes. Une hésitation
du déplacement ne peut donc plus relancer la même transition à chaque image.

## Réactions sans animations obligatoires

La neige, l'orage et une menace choisissent une réaction selon la personnalité.
Les rôles spécialisés `surpris` et `peur` sont facultatifs et retombent sur
`ecoute`, `joyeux` ou `triste`. Pendant le sommeil, une unique particule
Minecraft est envoyée toutes les trois secondes : aucun contrôleur supplémentaire
et aucune animation réseau ne tournent en permanence.

Quand le compagnon arrive sur un événement qu'il a choisi d'examiner, il ne se
contente plus de rester planté devant. Il regarde brièvement puis joue le premier
rôle disponible parmi `surpris`, `ecoute`, `joie` et `joyeux`. Une future espèce
profite donc de cette scène dès qu'elle fournit l'un de ces rôles, sans nouveau
code et sans animation obligatoire.
