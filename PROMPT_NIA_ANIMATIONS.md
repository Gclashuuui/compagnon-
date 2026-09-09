# Prompt de production pour Nia — animations d'un compagnon

Copier ce prompt dans la discussion qui réalise les animations, joindre le
fichier `.bbmodel`, puis remplacer les champs entre crochets. Le starter pack à
utiliser se trouve dans `PACKS_ANIMATIONS.md`.

---

Tu travailles sur un compagnon Minecraft 1.21.1 animé avec GeckoLib et créé dans
Blockbench.

Compagnon : **[nom de l'espèce]**  
Type : **[terrestre / volant]**  
Tempérament principal : **[calme, curieux, fier, timide, joueur, affectueux…]**  
Anatomie remarquable : **[oreilles, crête, antennes, ailes, longue queue,
nageoires, cornes, plusieurs têtes…]**  
Univers et rôle : **[origine, magie, habitat, façon de vivre]**

## Règles obligatoires

1. Analyse d'abord tous les os du `.bbmodel` et indique ceux que tu utiliseras.
2. Ne renomme, ne supprime et ne déplace aucun os existant.
3. Ne modifie ni le modèle, ni les cubes, ni les pivots, ni les UV, ni les textures.
4. Travaille uniquement dans les animations.
5. Conserve exactement le préfixe `animation.[identifiant_espece].`.
6. Les boucles doivent revenir exactement à leur première pose.
7. Les transitions commencent dans la dernière pose de l'état de départ et se
   terminent dans la première pose de l'état d'arrivée.
8. Aucun déplacement continu de la racine : le mouvement dans le monde appartient
   au code, pas à l'animation.
9. Utilise une interpolation douce et teste le résultat à 20 ticks/seconde.
10. Ne transforme pas automatiquement le compagnon en chien : ses gestes sociaux
    doivent venir de son anatomie, de son tempérament et de son univers.

## Travail demandé

Réalise le starter pack **[terrestre 60 / volant 70]** décrit dans le document
joint. Commence par un premier lot de 10 animations pour validation avant de
continuer afin qu'une erreur de pivot ne se répète pas sur tout le pack.

Ajoute ensuite **8 à 12 animations signatures** que ce compagnon est le seul à
pouvoir avoir. Avant de les créer, propose-les sous forme de tableau avec :

- identifiant exact ;
- durée ;
- os utilisés ;
- déclencheur possible dans son cerveau ;
- raison pour laquelle cette animation correspond précisément à cette espèce.

Parmi elles, propose au moins :

- une façon unique de saluer son maître ;
- une réaction unique à son lieu ou élément préféré ;
- une petite scène avec un autre compagnon ;
- une réaction de peur ou de courage propre à son anatomie ;
- une scène spectaculaire de lien ;
- une scène spectaculaire utilisant sa capacité physique ou magique principale.

## Micro-animations

Crée aussi de petites animations de 0,15 à 1,5 seconde que le cerveau pourra
réutiliser très souvent sans interrompre la locomotion :

- clignement simple et double ;
- mouvement gauche/droite de l'organe d'écoute (oreilles, crête, antennes…) ;
- orientation du regard ;
- respiration calme et fatiguée ;
- tête légèrement penchée à gauche et à droite ;
- réaction très courte à un bruit ;
- mouvement calme, joyeux et inquiet de la queue ou de son équivalent ;
- ouverture/repli discret des ailes si l'espèce en possède ;
- petit mouvement de sommeil.

Si l'anatomie ne permet pas une animation demandée, remplace-la par un geste qui
transmet la même émotion avec les os réellement présents. Explique le remplacement.

## Livraison

- Rends un `.bbmodel` conservant le modèle original intact et contenant les
  nouvelles animations.
- Rends aussi le fichier `.animation.json` exporté pour GeckoLib.
- Donne la liste exacte des animations ajoutées.
- Signale toute animation impossible à réaliser proprement avec les os présents.
- Pour chaque scène spectaculaire, précise le moment où le modèle revient dans
  une pose compatible avec `idle`, `walk` ou `fly`.

---

## Organisation conseillée des lots

1. Micro-vie et regard.
2. Locomotion et transitions.
3. Besoins : nourriture, eau, repos, soin.
4. Voix et vie sociale.
5. Animations signatures.
6. Dix scènes spectaculaires.

Cette organisation permet de tester le cerveau dès le premier lot au lieu
d'attendre les soixante ou soixante-dix animations.

