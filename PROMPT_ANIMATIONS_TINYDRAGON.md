# Prompt — animations du petit dragon

> **Mode d'emploi.** Ouvre une conversation Claude neuve et colle **tout** ce
> fichier comme premier message. Il est autonome : rien à joindre.

---

Tu es animateur 3D. Tu vas écrire les animations d'un **petit dragon** pour un mod
Minecraft Java 1.21.1 qui utilise **GeckoLib 4.7.7**. Le format est du JSON
**Bedrock**, à coller dans `tinydragon.animation.json`.

Il en a déjà sept. Il devient un **compagnon de joueur**, et il lui en manque
vingt-cinq.

---

## 1. CE QUE TU DOIS PRODUIRE

Pour chaque animation demandée, **un bloc JSON** prêt à coller dans l'objet
`"animations": { … }`. Rien d'autre.

**Une animation à la fois.** Tu écris celle qu'on te demande, tu t'arrêtes, tu
attends la suivante. Après chacune, dis en **trois lignes** :
- ce que tu as fait bouger et pourquoi,
- ce dont tu n'es pas sûr (un signe de rotation, une amplitude),
- ce que tu proposerais d'ajouter.

---

## 2. LE MODÈLE : 19 os

Texture 32×32. C'est une **petite vouivre** : deux pattes, deux ailes, une longue
queue fine, un museau allongé.

```
all               [0, 7, -1]           ← la racine du modèle
  Head            [0, 10.7, -3.1]  (1)
    boca1         [0, 10.5, -4]   (11)   ← le museau, mâchoire supérieure
    boca2         [0, 10.5, -4]    (2)   ← la mâchoire inférieure
  allbody         [0, 5.8, -0.5]
    Body          [0, 5.8, -0.5]   (2)
    Calda         [0, 2, 2]        (4)   ← la queue, en un seul os
  Hands           [1.5, 7, -2.5]
    Left_Hand     [1.5, 7, -2.5]   (3)
    Right_Hand    [-1.5, 7, -2.5]  (3)
  legs            [0, 3, 0]
    Left_Leg      [1.5, 3, 0]      (3)
    Right_Leg     [-1.5, 3, 0]     (3)
  LeftWing        [2, 9, -0.7]
    LeftWingPt1   [2, 9, -0.7]     (3)   ← bras de l'aile
    LeftWingPt2   [8.5, 9, -0.7]   (4)   ← main de l'aile, la grande membrane
  RightWing       [-2, 9, -0.7]
    RightWingPt1  [-2, 9, -0.7]    (3)
    RightWingPt2  [-8.5, 9, -0.7]  (4)
```

### Ce qu'il faut comprendre de ce corps

**Il n'a ni yeux, ni paupières, ni oreilles, ni doigts.** Sa tête est un bloc et
un museau. **Aucune animation ne doit reposer sur son visage** — tout se joue en
silhouette.

**Ses ailes sont énormes.** Envergure 2,19 blocs pour un corps de 0,89 de long et
1,22 de haut. Quand elles s'ouvrent, elles font trois fois la bête. C'est son
outil d'expression principal, très loin devant tout le reste.

**Sa queue est longue, fine, et d'un seul tenant.** `Calda` est **un seul os** :
elle ne peut pas onduler segment par segment. Elle pivote en bloc — un balancier,
pas un fouet. N'écris jamais « une vague qui parcourt la queue ».

**Chaque aile n'a que deux segments.** `Pt1` est le bras, `Pt2` la grande
membrane. Le décalage entre les deux est tout ce que tu as pour donner de la
souplesse : `Pt2` doit toujours être **en retard de deux ou trois images** sur
`Pt1`. C'est ce retard qui fait une aile plutôt qu'une planche.

**`Hands` est séparé des ailes.** Deux petites mains sous le poitrail, qui peuvent
saisir, se frotter, se poser au sol.

**`boca1` et `boca2`** sont les deux mâchoires. Ouvrir la gueule = faire pivoter
`boca2` vers le bas.

### Le repère

| | |
|---|---|
| **Il regarde vers −Z** | la tête est à `z = −3,1`, la queue à `z = +2` |
| **+Y est le haut** | les pattes touchent le sol vers `y = −5`, la tête monte à `y = 14` |
| **Attention à la gauche** | `Left_Leg` et `LeftWing` sont à **x positif**, `Right_*` à x négatif. C'est l'inverse de l'habitude — **fie-toi aux noms, pas aux signes** |

En rotation : **X** = tangage (baisser la tête, plier une patte), **Y** = lacet
(tourner la tête, la queue qui balaie), **Z** = roulis (pencher la tête, l'aile
qui bascule).

---

## 3. LE FORMAT EXACT

Ces clés sont vérifiées dans le code de GeckoLib. **Une clé mal orthographiée est
ignorée en silence** : l'animation se charge et il ne se passe rien.

```json
"animation.tinydragon.NOM": {
  "loop": true,
  "animation_length": 0.8,
  "bones": {
    "LeftWing": {
      "rotation": {
        "0.0": [0, 0, 0],
        "0.4": { "post": [0, 0, -35], "lerp_mode": "catmullrom" },
        "0.8": [0, 0, 0]
      }
    },
    "Calda": {
      "rotation": {
        "0.0": [0, 6, 0],
        "0.4": [0, -6, 0],
        "0.8": [0, 6, 0]
      }
    }
  },
  "sound_effects": {
    "0.1": { "effect": "@envol" }
  },
  "particle_effects": {
    "0.2": { "effect": "minecraft:cloud", "locator": "pattes" }
  }
}
```

**Les clés autorisées, et elles seules :**

| Niveau | Clés |
|---|---|
| animation | `loop`, `animation_length`, `bones`, `sound_effects`, `particle_effects`, `timeline` |
| os | `rotation`, `position`, `scale` |
| image-clé | `[x, y, z]`, ou `{ "post": […], "lerp_mode": "catmullrom" }` |
| son | `effect` |
| particule | `effect`, `locator` |

**`loop`** : `true` pour un état qui dure · **clé absente** pour un geste qui se
joue une fois · `"hold_on_last_frame"` pour une pose tenue.

**`animation_length` est obligatoire, même sur les boucles.** Le mod lit cette
valeur dans le fichier pour savoir combien de temps immobiliser la bête. Sans
elle, il applique 2,5 s par défaut.

**Unités** : temps en secondes, en clés de texte (`"0.0"`, `"0.45"`). Rotation en
degrés. Position en **unités de modèle** — 16 unités = 1 bloc, et cette bête fait
19 unités de haut. Une translation de 2 unités est **déjà beaucoup**.

**Sons** : `@ambiance`, `@content`, `@mal`, `@envol`, `@alerte` prennent la voix
de l'espèce. Un identifiant complet (`minecraft:block.sand.step`) joue ce son-là.

**Particules** : un identifiant simple du jeu (`minecraft:cloud`, `smoke`,
`happy_villager`, `heart`, `crit`, `splash`), plus un mot de position :
`"pattes"`, `"tete"`, ou rien pour le milieu du corps.

---

## 4. TROIS RÈGLES

**1. Les pattes ne glissent jamais.** Un pied posé garde **exactement** la même
position tant qu'il est au sol : une clé au contact, **la même valeur** au
décollage, rien entre les deux.

**2. Rien n'est jamais complètement immobile.** Trois couches se superposent :
la **respiration** (`Body`, 1 à 2°, cycle de 4 à 6 s), une **dérive** (`Head` et
`Calda`, sur un cycle plus long et **non multiple** du premier), et des
**micro-gestes** (la mâchoire, une main, une pointe d'aile). Trois périodes
premières entre elles — 5 s, 7 s, 11 s — donnent une boucle qu'on ne reconnaît
pas ; 4, 8 et 12 se resynchronisent et l'œil le voit.

**3. Sur une boucle, la première et la dernière clé sont identiques**, sinon elle
saute à chaque tour.

---

## 5. CE QU'IL A DÉJÀ

`idle` (1 s ⟳) · `fly` (1 s ⟳) · `sitting` (1 s ⟳) · `shoulder1` (1 s ⟳) ·
`shoulder2` (1 s ⟳) · `attack` (1 s) · `trick` (3 s ⟳)

Ne les touche pas pour l'instant.

---

## 6. LES QUINZE PRATIQUES

Fais-les dans cet ordre. La première est un vrai trou : **il n'a aucune marche**,
donc un compagnon qui suit son maître glisse aujourd'hui en position d'attente.

### 1. `walk` — 0,8 s — boucle
Marche de bipède. Les pattes en opposition, le corps qui monte et descend d'une
demi-période par rapport aux pas. Ailes repliées contre le corps, mais pas
figées : elles absorbent le tangage d'un ou deux degrés. **La queue est le
contrepoids** — elle balaie à l'opposé du pas, avec un quart de cycle de retard.
La tête reste à peu près à hauteur constante, comme chez un oiseau qui marche.
Son `minecraft:entity.parrot.step` à chaque contact.

### 2. `run` — 0,45 s — boucle
Foulée plus longue, buste penché vers l'avant, **une phase de suspension** où les
deux pattes quittent le sol. Ailes entrouvertes à 25° pour l'équilibre, sans
battre. La queue tendue presque à l'horizontale.

### 3. `hop` — 0,6 s — boucle
Les **deux pattes ensemble**, comme un corbeau. Le corps se ramasse, pousse,
atterrit. Un petit battement d'aile au sommet de chaque bond. Pour les courtes
distances : c'est plus charmant qu'une marche pour une bête de cette taille.

### 4. `takeoff` — 1,2 s
**Aujourd'hui il se téléporte en vol.** Trois temps : il s'accroupit (0 → 0,4 s,
pattes pliées, ailes fermées) ; il détend tout d'un coup (0,4 → 0,6 s, pattes qui
poussent, ailes qui s'ouvrent) ; deux battements amples où les ailes descendent
**plus bas que le corps** (0,6 → 1,2 s).
Particule `minecraft:cloud` sur `pattes` à 0,45 s. Son `@envol` à 0,4 s.

### 5. `land` — 1,4 s
Il cabre : ailes en cuillère vers l'avant pour freiner, corps redressé à 40°,
pattes tendues vers le sol. Contact, les genoux amortissent, puis les ailes se
replient **en deux temps** (`Pt2` d'abord, `Pt1` ensuite). Il s'ébroue une fois.
Particule `minecraft:cloud` sur `pattes` au contact.

### 6. `glide` — 3 s — boucle
Ailes tendues, immobiles. Tout le mouvement est dans les **micro-corrections** :
`Pt2` qui monte et descend de 2°, la queue en gouvernail, le corps qui roule très
légèrement. **La tête reste parfaitement stable** pendant que le corps oscille —
c'est ce qui donne l'impression de maîtrise.

### 7. `hover` — 1,2 s — boucle
Battements rapides et courts, sur place, corps redressé à la verticale, pattes qui
pendent, queue basse. **Un gros dragon ne peut pas faire ça** — c'est un privilège
de sa taille, et ça doit se voir.

### 8. `fly_fast` — 0,6 s — boucle
Battements serrés, corps allongé, cou tendu vers l'avant, queue droite comme une
flèche, pattes plaquées sous le ventre.

### 9. `sit_down` — 0,8 s
Il recule d'un demi-pas, plie les pattes, laisse tomber l'arrière-train, ramène la
queue. **Doit finir exactement sur la première image de `sitting`**, qui existe
déjà — donne-moi la pose de départ que tu lis, je vérifierai le raccord.

### 10. `stand_up` — 0,7 s
L'inverse. Il pousse sur les pattes, l'arrière monte en dernier, et il s'ébroue en
finissant. **Doit partir de la première image de `sitting`.**

### 11. `lie_down` — 5 s — boucle
Couché sur le ventre, pattes repliées de part et d'autre, **les ailes rabattues
sur les flancs comme un manteau**, la queue enroulée le long du corps — pas tendue
derrière. La tête haute ou posée sur les mains selon le moment. Respiration lente.

### 12. `sleep` — 8 s — boucle
**La tête glissée sous une aile.** C'est la pose, et elle vaut à elle seule
l'animation. Respiration très lente et très ample (7 s par cycle), la queue
enroulée jusqu'au museau. Et **une fois par boucle, un soubresaut minuscule** : il
rêve.

### 13. `look_around` — 3 s
La tête balaie de gauche à droite en trois temps inégaux, avec des arrêts. Une
aile se hausse et retombe. La queue s'immobilise pendant qu'il regarde — un animal
qui écoute cesse de bouger le reste.

### 14. `preen` — 4 s
Il tourne la tête vers son aile gauche et la nettoie au museau, **de l'épaule vers
la pointe**, en trois passages. Puis il secoue la membrane. Puis il se redresse et
recommence brièvement de l'autre côté.

### 15. `shake` — 1,2 s
Une onde de torsion à haute fréquence : la tête tourne trois fois d'un côté puis
de l'autre, le corps suit avec un tour de retard, la queue deux. Les ailes
claquent. **À jouer après l'eau, après le sommeil, après une frayeur.**
Particule `minecraft:splash` au milieu du corps.

---

## 7. LES DIX SPÉCIALES

Ce sont les récompenses : le joueur les débloque en montant de niveau, et il les
lance depuis une roue.

**Le ton n'est pas le même que le reste.** Cette bête est petite et ronde ; sa
force n'est pas d'impressionner, c'est d'être **attachante**. Vise le calme, le
drôle et le tendre — pas le spectaculaire.

Le critère : **est-ce que le joueur peut la raconter à quelqu'un qui ne l'a pas
vue ?** « Il s'emballe dans ses ailes comme dans une couverture » se raconte.
« Il lève la patte » ne se raconte pas.

### 16. `colis` — 6 s — `hold_on_last_frame`
Il ramène ses deux ailes **autour de lui** comme une cape, ne laisse dépasser que
le bout du museau, et ne bouge plus. Un petit paquet posé là. Seule la respiration
continue, et une fois toutes les deux secondes le paquet se réajuste d'un degré.
> C'est la seule qui ne s'arrête pas toute seule. On peut le laisser comme ça dans
> un coin.

### 17. `tete_a_l_envers` — 5 s
Il incline la tête sur le côté à 45°… puis continue jusqu'à **180°, complètement
à l'envers**, et te regarde comme ça pendant trois secondes. Le corps ne bouge
pas. Puis il se redresse d'un coup, et secoue la tête une fois.
> Personne ne s'y attend. Et c'est exactement ce que fait une chauve-souris.

### 18. `ronronnement` — 6 s — boucle
Couché, la queue enroulée en spirale contre lui, les ailes qui pendent mollement,
et **tout le corps qui vibre** : une oscillation de moins d'un degré sur `Body`,
mais rapide — six fois par seconde. La tête à moitié posée.
> Le mouvement le plus petit de toute la liste, et le plus tendre.

### 19. `grand_etirement` — 5 s
Une aile tendue à fond vers l'arrière, tenue une seconde. Puis l'autre. Puis
**les deux en même temps**, avec les pattes avant qui poussent en avant et le dos
qui se creuse. Un bâillement (`boca2` en trois paliers) pour finir, et tout
retombe d'un coup.

### 20. `pas_de_danse` — 4 s
Deux pas de côté à gauche, deux à droite, ailes ouvertes à demi qui battent en
rythme, la queue qui balance à contretemps. La tête suit le mouvement avec un
temps de retard. **Bête et joyeux** — c'est tout l'intérêt.
Son `@content` au début.

### 21. `vol_stationnaire` — 7 s
Il décolle d'un bloc (`all` en position Y jusqu'à +16 unités), se stabilise face
au joueur, **penche la tête de 20°** comme s'il posait une question, tient trois
secondes en battant, puis redescend et se pose.
> **Attention** : `all` doit revenir exactement à sa valeur de départ à la
> dernière image, sinon la bête reste en l'air.

### 22. `nid` — 6 s
Il tourne trois fois sur lui-même en regardant le sol, gratte deux fois avec une
patte, se laisse tomber, se réajuste deux fois, et ramène la queue tout autour de
lui. Le rituel exact d'un chien qui se couche.

### 23. `gargouille` — 8 s — `hold_on_last_frame`
Il se ramasse, ailes **à demi déployées et figées**, tête basse et rentrée dans
les épaules, et il ne bouge **plus du tout**. Pas de respiration, pas de dérive,
rien — pendant huit secondes.
> C'est l'inverse d'une animation, et c'est pour ça que ça marche. Sur un mur de
> château, on ne sait plus si c'est une statue.

### 24. `rot_de_feu` — 3 s
Il hoquette : tout le corps se contracte d'un coup, la tête part en avant, la
gueule s'ouvre — et il n'en sort qu'une **petite bouffée de fumée**. Il referme la
gueule, regarde à gauche, regarde à droite, comme si personne n'avait vu.
Particule `minecraft:smoke` sur `tete` à 1,4 s.
> Le regard d'après. C'est la gêne qui est drôle, pas le rot.

### 25. `reverence` — 4 s
Les deux ailes s'ouvrent **à l'envergure complète, très lentement** (deux
secondes rien que pour ça), puis la tête descend jusqu'au sol et il tient la pose
deux secondes. Puis tout se replie.
> La lenteur est le sujet. Tout le reste du jeu est rapide ; deux secondes pour
> ouvrir une aile, ça force à regarder.

---

## 8. POUR COMMENCER

Réponds d'abord par **une seule chose** : dis-moi si quelque chose te manque pour
travailler — un os, une convention, une amplitude de référence.

Puis attends que je te demande la première. On commencera par **`walk`**, parce
que c'est le seul vrai trou.
