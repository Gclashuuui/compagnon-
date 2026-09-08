# Prompt — animations du dragonnet

> **Mode d'emploi.** Ouvre une conversation Claude neuve et colle **tout** ce fichier
> comme premier message. Le document est autonome : il contient le modèle, le format
> exact, les règles et la liste des animations. Aucun fichier à joindre.

---

Tu es animateur 3D. Tu vas écrire les animations d'un petit dragon pour un mod
Minecraft Java 1.21.1 qui utilise **GeckoLib 4.7.7**. Les animations sont du JSON
au format **Bedrock**, que j'insérerai dans `dragonnet.animation.json`.

Tu n'as pas le modèle sous les yeux. Tout ce qu'il te faut est ci-dessous : la
hiérarchie complète des 91 os avec leurs pivots, le repère, et le format exact.

---

## 1. CE QUE TU DOIS PRODUIRE

Pour chaque animation demandée, **un bloc JSON** prêt à coller dans l'objet
`"animations": { ... }`. Rien d'autre : pas de fichier complet, pas de préambule.

**Une animation à la fois.** Tu écris celle qu'on te demande, tu t'arrêtes, et tu
attends la suivante. Une animation de ce dragon fait entre 40 et 250 lignes de
JSON : tenter d'en écrire cinq d'un coup produit du bâclé.

Après chaque animation, dis en **trois lignes maximum** :
- ce que tu as fait bouger et pourquoi,
- ce dont tu n'es pas sûr (un signe de rotation, une amplitude),
- ce que tu proposes d'ajouter si l'idée t'en vient.

---

## 2. LE FORMAT EXACT

Ces clés sont vérifiées dans le code de GeckoLib. **Une clé mal orthographiée est
ignorée en silence** — l'animation se charge et il ne se passe rien.

### La structure

```json
"animation.dragonnet.NOM": {
  "loop": true,
  "animation_length": 5.0,
  "bones": {
    "nom_de_l_os": {
      "rotation": {
        "0.0":  [0, 0, 0],
        "2.5":  [-4, 0, 0],
        "5.0":  [0, 0, 0]
      },
      "position": {
        "0.0": [0, 0, 0],
        "2.5": [0, 0.4, 0]
      },
      "scale": {
        "0.0": [1, 1, 1]
      }
    }
  },
  "sound_effects": {
    "0.5": { "effect": "@content" }
  },
  "particle_effects": {
    "1.2": { "effect": "minecraft:cloud", "locator": "pattes" }
  }
}
```

### Les clés autorisées, et elles seules

| Niveau | Clés |
|---|---|
| animation | `loop`, `animation_length`, `bones`, `sound_effects`, `particle_effects`, `timeline` |
| os | `rotation`, `position`, `scale` |
| image-clé | soit `[x, y, z]` directement, soit `{ "pre": [...], "post": [...], "lerp_mode": "catmullrom", "easing": "easeInOutSine" }` |
| son | `effect` |
| particule | `effect`, `locator` |

### Les trois valeurs de `loop`

| Valeur | Pour quoi |
|---|---|
| `"loop": true` | un **état** qui dure : idle, marche, vol, sommeil |
| *(clé absente)* | un **geste** qui se joue une fois : bâillement, salut, rugissement |
| `"loop": "hold_on_last_frame"` | une **pose tenue** : la menace, le sommeil enroulé |

### `animation_length` est obligatoire

Le mod **lit cette valeur dans le fichier** pour savoir combien de temps
immobiliser la bête pendant le geste. Sans elle il applique 2,5 s par défaut :
trop court pour un rugissement, trop long pour un clin d'œil. Mets-la toujours,
même sur les boucles.

### Les unités

- **Temps** : en secondes, en clé de texte — `"0.0"`, `"1.25"`, `"3.5"`.
- **Rotation** : en degrés, `[x, y, z]`.
- **Position** : en **unités de modèle**, pas en blocs. 16 unités = 1 bloc.
  Ce dragon fait environ 21 unités de haut aux oreilles et 38 de long du museau
  à la pointe de la queue. Une translation de 2 unités est **déjà beaucoup**.
- **Échelle** : `[1,1,1]` = taille normale, `[0,0,0]` = invisible.

### Le lissage

Par défaut GeckoLib interpole en linéaire entre deux clés. Pour un mouvement
organique, ajoute `"lerp_mode": "catmullrom"` sur les clés d'un mouvement continu
(respiration, dérive, balancier de queue). Garde le linéaire pour ce qui doit
claquer (une mâchoire qui s'ouvre d'un coup, un frisson).

---

## 3. LE MODÈLE : les 91 os

Format Bedrock, texture 512×512 en espace UV 128. Le nombre entre parenthèses est
le nombre de cubes portés par l'os ; les os sans parenthèses sont des articulations
pures. Les crochets donnent le pivot `[x, y, z]`.

```
root  [0, 0, 0]
  worldbody  [0, 9.2, 0]
    body  [0, 9.2, 0]
      torso_up  [0, 10.3, -1.8]  (2)
        spine_ridge  [0, 12.8, -2]  (3)
        neck_act  [0, 11.8, -5.4]
          neck  [0, 11.8, -5.4]  (1)
            neck2_act  [0, 12.4, -8.4]
              neck2  [0, 12.4, -8.4]
                head_act  [0, 12.8, -10.2]
                  head  [0, 12.8, -10.2]  (7)
                    brow_l  [-3, 17.3, -15.4]  (1)
                    brow_r  [3, 17.3, -15.4]  (1)
                    jaw_act  [0, 12.2, -13.6]
                      jaw  [0, 12.2, -13.6]  (1)
                        tongue  [0, 11.2, -15]
                        teeth_lower  [0, 12, -16]
                    teeth_upper  [0, 12, -16]
                    eye_l  [-3, 14.6, -17.2]  (1)
                      pupil_l  [-3.3, 14.7, -18.3]  (1)
                      eyelid_up_l  [-3, 16.8, -17]  (1)
                      eyelid_low_l  [-3, 12.5, -17]  (1)
                    eye_r  [3, 14.6, -17.2]  (1)
                      pupil_r  [3.3, 14.7, -18.3]  (1)
                      eyelid_up_r  [3, 16.8, -17]  (1)
                      eyelid_low_r  [3, 12.5, -17]  (1)
                    ear_l  [-2.9, 16.8, -11.2]  (1)
                      ear_l_tip  [-2.9, 20.6, -11.4]  (1)
                    ear_r  [2.9, 16.8, -11.2]  (1)
                      ear_r_tip  [2.9, 20.6, -11.4]  (1)
                    nub_l  [-1.5, 16.9, -10.4]  (1)
                    nub_r  [1.5, 16.9, -10.4]  (1)
        wing_l  [-3.8, 11.6, -2.5]
          wing_l_bar  [-3.8, 11.6, -2.5]
          wing_l_mem  [-3.8, 11.6, -2.5]  (1)
          wing_l_fore  [-8.4, 11.6, -2.5]
            wing_l_fore_bar  [-8.4, 11.6, -2.5]
            wing_l_fore_mem  [-8.4, 11.6, -2.5]  (1)
            wing_l_f3  [-12.4, 11.6, -2.5]
              wing_l_f3_mem  [-12.4, 11.6, -2.5]  (1)
              wing_l_f2  [-12.4, 11.6, -2.5]
                wing_l_f2_mem  [-12.4, 11.6, -2.5]  (1)
                wing_l_tip  [-12.4, 11.6, -2.5]
                  wing_l_tip_mem  [-12.4, 11.6, -2.5]  (1)
        wing_r  [3.8, 11.6, -2.5]
          wing_r_bar  [3.8, 11.6, -2.5]
          wing_r_mem  [3.8, 11.6, -2.5]  (1)
          wing_r_fore  [8.4, 11.6, -2.5]
            wing_r_fore_bar  [8.4, 11.6, -2.5]
            wing_r_fore_mem  [8.4, 11.6, -2.5]  (1)
            wing_r_f3  [12.4, 11.6, -2.5]
              wing_r_f3_mem  [12.4, 11.6, -2.5]  (1)
              wing_r_f2  [12.4, 11.6, -2.5]
                wing_r_f2_mem  [12.4, 11.6, -2.5]  (1)
                wing_r_tip  [12.4, 11.6, -2.5]
                  wing_r_tip_mem  [12.4, 11.6, -2.5]  (1)
      torso_low  [0, 10.3, 1.6]  (1)
        spine_ridge_low  [0, 12.8, 2]  (4)
        tail_act  [0, 9.6, 8.2]
          tail1  [0, 9.6, 8.2]  (2)
            tail2  [0, 10, 12.8]  (2)
              tail3  [0, 10.4, 17]  (2)
                tail4  [0, 10.9, 20.8]  (2)
                  tail5  [0, 11.3, 24.2]  (1)
                    tail6  [0, 11.7, 27.2]  (1)
                      tail_fin_l  [-0.6, 12.3, 28.4]  (1)
                      tail_fin_r  [0.6, 12.3, 28.4]  (1)
      leg_fl  [-2.9, 8.6, -4.4]  (1)
        leg_fl_shin  [-2.9, 4.2, -4.5]  (2)
          foot_fl  [-2.9, 1.4, -4.6]  (1)
            toe0_fl  [-1.9, 1.2, -7]  (2)
            toe1_fl  [-3, 1.2, -7]  (2)
            toe2_fl  [-4.1, 1.2, -7]  (2)
      leg_fr  [2.9, 8.6, -4.4]  (1)
        leg_fr_shin  [2.9, 4.2, -4.5]  (2)
          foot_fr  [2.9, 1.4, -4.6]  (1)
            toe0_fr  [1.9, 1.2, -7]  (2)
            toe1_fr  [3, 1.2, -7]  (2)
            toe2_fr  [4.2, 1.2, -7]  (2)
      leg_bl  [-3.2, 9.2, 5.4]  (1)
        leg_bl_shin  [-3.4, 5, 5.6]  (2)
          foot_bl  [-3.4, 1.4, 4.8]  (1)
            toe0_bl  [-2.1, 1.2, 1.5]  (2)
            toe1_bl  [-3.3, 1.2, 1.5]  (2)
            toe2_bl  [-4.5, 1.2, 1.5]  (2)
      leg_br  [3.2, 9.2, 5.4]  (1)
        leg_br_shin  [3.4, 5, 5.6]  (2)
          foot_br  [3.4, 1.4, 4.8]  (1)
            toe0_br  [2.1, 1.2, 1.5]  (2)
            toe1_br  [3.3, 1.2, 1.5]  (2)
            toe2_br  [4.5, 1.2, 1.5]  (2)
```

---

## 4. LE REPÈRE

| | |
|---|---|
| **Il regarde vers −Z** | le museau est à `z = −17`, la pointe de la queue à `z = +28` |
| **+Y est le haut** | les pattes touchent le sol vers `y = 1`, les oreilles montent à `y = 21` |
| **Sa gauche est −X** | `ear_l` est à `x = −2.9`, `ear_r` à `x = +2.9` |

Donc, en rotation :

- **X** = le tangage. Baisser la tête, creuser le dos, plier une patte.
- **Y** = le lacet. Tourner la tête à gauche ou à droite, la queue qui balaie.
- **Z** = le roulis. Pencher la tête sur le côté (l'air interrogateur), l'aile qui bascule.

**Vérifie tes signes.** Il est très facile de faire lever une tête qui devrait
descendre. En cas de doute, dis-le dans ton commentaire de fin plutôt que de
deviner en silence.

---

## 5. LES RÈGLES

### 5.1 Les pattes ne glissent jamais

C'est la règle qui sépare une animation d'amateur d'une vraie. Un pied posé au sol
garde **exactement** la même position tant qu'il y reste : une clé au moment du
contact, **la même valeur** au moment du décollage, et rien entre les deux.

Sur une marche à 2,7 s, ça veut dire quatre appuis dont chacun tient une
demi-seconde **sans bouger d'un pixel**.

### 5.2 Rien n'est jamais complètement immobile

Même dans un idle, trois couches se superposent :

1. **La respiration** — `torso_up` et `torso_low`, 1 à 2°, cycle de 4 à 6 s.
2. **La dérive** — `neck`, `neck2`, `tail1→6`, sur un cycle **plus long et non
   multiple du premier**.
3. **Les micro-gestes** — une oreille qui pivote, un clignement, un orteil.

Trois périodes premières entre elles (par exemple 5 s, 7 s, 11 s) donnent une
boucle qu'on ne reconnaît pas. Si tu utilises 4 s, 8 s et 12 s, tout se resynchronise
toutes les 12 secondes et l'œil le voit.

### 5.3 Les os `_act` sont réservés au code

`neck_act`, `neck2_act`, `head_act`, `jaw_act`, `tail_act` ont **le même pivot que
leur jumeau sans suffixe**. Ils existent pour que le programme puisse tourner la
tête vers le joueur *par-dessus* ton animation.

**Anime toujours le jumeau sans suffixe** (`neck`, `neck2`, `head`, `jaw`, `tail1`)
et laisse les `_act` à zéro. Si tu les animes, le code et toi vous battrez pour le
même os.

### 5.4 On ne touche pas à `root` ni `worldbody`

C'est le jeu qui déplace la bête dans le monde. Une translation sur ces deux os la
fait patiner sur le sol.

### 5.5 Les décalages font la vie

Rien ne bouge en même temps sur un être vivant :

- Sur une **aile**, le coude part, le poignet suit deux images plus tard, la pointe
  encore deux après. C'est ce retard qui fait une aile plutôt qu'une planche.
- Sur la **queue**, chaque segment est en retard sur le précédent. Six segments,
  six retards.
- Sur les **paupières**, l'œil gauche se ferme une image avant le droit. Personne ne
  le remarque, tout le monde le sent.

### 5.6 Les sons et les particules se posent dans l'animation

Le mod écoute les images-clés. Ça ne coûte **rien sur le réseau** — chaque joueur
calcule déjà l'animation de son côté.

**Sons** — deux formes possibles :

| Écrire | Effet |
|---|---|
| `"@ambiance"` `"@content"` `"@mal"` `"@envol"` `"@alerte"` | joue la voix de l'espèce pour ce rôle |
| `"minecraft:entity.polar_bear.step"` | joue exactement ce son du jeu |

**Particules** — un identifiant du jeu et un mot de position :

| `locator` | Où ça sort |
|---|---|
| `"pattes"` | au ras du sol |
| `"tete"` | à hauteur de tête |
| *(absent)* | au milieu du corps |

Les particules simples marchent : `minecraft:cloud`, `minecraft:smoke`,
`minecraft:happy_villager`, `minecraft:heart`, `minecraft:end_rod`,
`minecraft:splash`, `minecraft:crit`. Une particule qui demande un argument (un
bloc, un objet, une couleur) ne marche pas.

---

## 6. UN EXEMPLE COMPLET

Voici à quoi doit ressembler ta sortie. C'est un `idle` très réduit — deux couches
seulement — pour que tu voies la forme, pas pour que tu le recopies.

```json
"animation.dragonnet.exemple": {
  "loop": true,
  "animation_length": 6.0,
  "bones": {
    "torso_up": {
      "rotation": {
        "0.0": { "vector": [0, 0, 0], "lerp_mode": "catmullrom" },
        "2.5": { "vector": [-1.5, 0, 0], "lerp_mode": "catmullrom" },
        "5.0": { "vector": [0, 0, 0], "lerp_mode": "catmullrom" },
        "6.0": { "vector": [0, 0, 0], "lerp_mode": "catmullrom" }
      }
    },
    "neck": {
      "rotation": {
        "0.0": [0, 0, 0],
        "1.75": [2, -3, 0],
        "4.25": [-1, 2, 0],
        "6.0": [0, 0, 0]
      }
    },
    "ear_l": {
      "rotation": {
        "0.0": [0, 0, 0],
        "2.1": [0, 0, 0],
        "2.3": [-14, 0, 0],
        "2.6": [0, 0, 0],
        "6.0": [0, 0, 0]
      }
    },
    "eyelid_up_l": {
      "rotation": {
        "0.0": [0, 0, 0],
        "3.40": [0, 0, 0],
        "3.48": [-72, 0, 0],
        "3.56": [0, 0, 0],
        "6.0": [0, 0, 0]
      }
    },
    "eyelid_up_r": {
      "rotation": {
        "0.0": [0, 0, 0],
        "3.44": [0, 0, 0],
        "3.52": [-72, 0, 0],
        "3.60": [0, 0, 0],
        "6.0": [0, 0, 0]
      }
    }
  }
}
```

Remarque trois choses :

- La **première et la dernière clé sont identiques** sur une boucle, sinon elle
  saute à chaque tour.
- La **respiration** (6 s) et la **dérive du cou** (deux temps inégaux) ne
  retombent jamais ensemble.
- La paupière **gauche cligne 0,04 s avant la droite**. C'est ça, la règle 5.5.

---

## 7. LES ANIMATIONS À ÉCRIRE

44 animations, en six vagues. **Fais-les dans cet ordre** : chaque vague se voit
plus que la suivante.

Toutes les durées sont indicatives — si le mouvement demande plus, dis-le.

---

### VAGUE 1 — Les trous qui se voient tout de suite (5)

#### `sit` — 5 s — boucle
**Le mod a un mode « assis » que le dragon ne sait pas jouer.** C'est la case la
plus urgente du modèle.
- Arrière-train posé au sol, `leg_bl`/`leg_br` repliés sur le côté, jarrets à plat.
- Antérieurs tendus, `toe0/1/2` écartés et posés.
- `tail1→6` enroulée autour des pattes avant, la pointe (`tail6`) qui bat lentement.
- Respiration **plus ample** qu'en idle : au repos on souffle plus fort.
- Toutes les 3-4 s, la tête tourne de 20° pour regarder autour, oreilles indépendantes.

#### `sit_down` — 1,2 s — une fois
Il recule d'un demi-pas, plie les postérieurs, laisse tomber la croupe (le poids
arrive **après** le mouvement, pas pendant), puis ramène la queue autour.
**Doit finir exactement sur la première image de `sit`.**

#### `stand_up` — 1,0 s — une fois
L'inverse : il pousse sur les antérieurs, la croupe monte **en dernier**, et il
s'ébroue une fois en finissant. **Doit partir de la première image de `sit`.**

#### `takeoff` — 1,6 s — une fois
Aujourd'hui il passe du sol au vol sans un battement d'aile. Trois temps :
- **0 → 0,5 s** : il s'accroupit sur les quatre pattes, griffes serrées, crête baissée, ailes repliées.
- **0,5 → 0,8 s** : détente. Les pattes poussent, les ailes s'ouvrent d'un coup.
- **0,8 → 1,6 s** : deux battements puissants, les ailes descendant **plus bas que le corps**.
- Particule `minecraft:cloud` sur `pattes` à 0,55 s. Son `@envol` à 0,5 s.

#### `land` — 1,8 s — une fois
Il cabre : ailes en cuillère vers l'avant pour freiner, corps redressé à 40°, pattes
tendues vers le sol. Contact des **postérieurs d'abord**, puis des antérieurs. Les
jarrets amortissent. Les ailes se replient en trois temps (avant-bras, doigts,
pointe). Il s'ébroue une fois.
- Particule `minecraft:cloud` sur `pattes` au contact. Son `minecraft:entity.horse.land` au même instant.

---

### VAGUE 2 — Les onze réactions que le mod appelle dans le vide (11)

Le mod déclenche déjà ces rôles et il ne se passe rien, uniquement parce que la
case est vide. Elles sont **courtes** — 1 à 3 s — et ce sont elles qui font la
différence entre un modèle et un animal.

#### `caresse` — 2 s — une fois
Il pousse la tête **contre** la main au lieu de la subir.
- `neck` avance de 2 unités (position `[0, 0, -2]`), `head` s'incline de 15°.
- `eyelid_up_l/r` descendent à mi-hauteur et **y restent**.
- `spine_ridge` se couche complètement.
- `tail1` s'enroule d'un cran vers l'avant.
- Souffle par les narines à la fin : particule `minecraft:smoke` sur `tete` à 1,7 s.

#### `ecoute` — 1,5 s — `hold_on_last_frame`
- `ear_l` et `ear_r` se dressent et pivotent vers l'avant (une image d'écart).
- `pupil_l/r` se braquent : `scale` à `[1.3, 1.3, 1.3]`.
- `brow_l` monte de 15°.
- `head` s'incline de 12° en **Z** (le roulis, l'air interrogateur).
- **Le corps ne bouge pas.** Il n'a pas encore tourné la tête et on sait déjà qu'il écoute.

#### `salut` — 2 s — une fois
Une inclinaison de tout l'avant-corps : `leg_fl`/`leg_fr` plient, le poitrail
descend, la croupe reste haute, `head` bas — **et `pupil_l/r` restent levées vers
le joueur**. Une aile s'entrouvre de 20°. Le salut d'un chat qui s'étire et d'un
chevalier à la fois.

#### `joie` — 2,5 s — une fois
Deux bonds sur place : `body` monte de 2 unités, les quatre pattes quittent le sol.
`spine_ridge` dressée, ailes ouvertes à 50 % qui claquent, `tail1`→`tail6` qui fouettent,
`jaw` ouverte, `tongue` sortie.
- Particule `minecraft:happy_villager` à 0,3 s et 1,1 s. Son `@content` à 0,2 s.

#### `joyeux` — 3 s — boucle
Ce n'est pas un geste, c'est une **humeur** : elle remplace l'idle quand tout va
bien. Un idle plus vif — queue plus rapide, `spine_ridge` à moitié dressée,
oreilles en avant, tête plus haute de 8°.

#### `triste` — 4 s — boucle
L'inverse **exact**, os par os : `head` bas, `neck` fléchi, `spine_ridge`
complètement couchée, oreilles en arrière, `tail1`→`tail6` au sol qui ne bougent presque plus,
paupières à mi-hauteur, ailes qui pendent d'un cran. Respiration plus lente (6 s).

#### `tourne` — 1,8 s — une fois
Un tour complet de `body` en Y, en pas croisés. La queue traîne **en retard** et
fouette à la fin. `head` garde le regard sur le joueur le plus longtemps possible
avant de rattraper le corps — comme une danseuse qui « spotte ».

#### `reconnait` — 2,5 s — une fois
Il s'arrête, renifle le sol **deux fois** (tête qui descend, oreilles qui pivotent
vers le bas), puis relève la tête et regarde autour de lui, lentement. **Rien
d'autre.** C'est au joueur de se souvenir de ce qui s'est passé ici.

#### `ramasse` — 1,5 s — une fois
Tête et cou descendent au sol, `jaw` s'ouvre puis se referme, la tête remonte avec
un petit mouvement sec pour caler l'objet. Le mod accroche l'objet à l'os de la
gueule : il suivra tout seul.

#### `reveil` — 2,5 s — une fois
Le mod le joue **quand le joueur sort du lit**. Il ouvre un œil, puis l'autre
(0,3 s d'écart), bâille à moitié, s'étire sur place, secoue la tête, se relève.

#### `gotosleep` — 1,5 s — une fois
Il tourne une fois sur lui-même (le geste du chien qui se couche), plie les quatre
pattes, pose le menton, ramène la queue. **Doit finir sur la première image de `sleep`.**

---

### VAGUE 3 — Le vol et la course (6)

#### `run` — 1,1 s — boucle
La troisième allure, celle qui manque. Un **galop**, pas un trot rapide : les deux
antérieurs ensemble, puis les deux postérieurs, avec un **temps de suspension** où
les quatre pattes quittent le sol. Le dos se creuse et s'arrondit alternativement
(`torso_up` et `torso_low` en opposition). Les ailes s'entrouvrent pour l'équilibre,
**sans battre**.

#### `glide` — 4 s — boucle
Ailes tendues, immobiles. Tout le mouvement est dans les **micro-corrections** :
`wing_*_tip` qui montent et descendent de 2°, une membrane qui ondule, la queue en
gouvernail. **`head` reste parfaitement stable** pendant que le corps oscille —
c'est ce qui donne l'impression de maîtrise.

#### `fly_fast` — 1,4 s — boucle
Battements courts et serrés, corps allongé, `neck` tendu vers l'avant, queue droite
comme une flèche, oreilles plaquées en arrière par la vitesse.

#### `fly_up` — 2 s — boucle
Battements plus amples et plus lents, corps redressé de 25°, tête vers le haut,
pattes qui pendent.

#### `fly_down` — 2 s — boucle
Ailes à demi repliées, corps piqué de 30° vers l'avant, la queue qui remonte pour
équilibrer.

#### `fly_tired` — 2,8 s — boucle
À jouer quand son énergie tombe bas. Battements **irréguliers**, un temps mort
entre deux, le corps qui perd de l'altitude entre chaque poussée. Tête basse,
langue à moitié sortie. **C'est la seule information de santé qu'on lit sur la bête
et pas dans un menu.**

---

### VAGUE 4 — Reprendre les sept existantes (7)

Elles existent, elles marchent, mais elles ne respectent pas les règles 5.1 et 5.2.
Je te les fournirai une par une quand on y sera.

`idle` · `walk` · `walk_fast` · `fly` · `sleep` · `lie_down` · `yawn`

Notes principales :
- **`walk`** : allure diagonale (avant-gauche + arrière-droit ensemble, décalés
  d'une demi-période des deux autres). **Jamais l'amble** — les deux pattes d'un
  même côté ensemble donne un chameau. Les orteils s'écartent à la pose, se
  referment au décollage. Son `minecraft:entity.polar_bear.step` à chaque contact
  d'antérieur.
- **`fly`** : la descente est **rapide et puissante**, la remontée lente et repliée.
  Les quatre articulations ne bougent pas ensemble (règle 5.5). Les membranes se
  tendent à la descente, se creusent à la remontée. Pattes repliées sous le corps.
- **`yawn`** : n'utilise que **5 os sur 91**. À refaire complètement — voir la
  vague 5.
- **`lie_down`** : la queue doit être **enroulée**, pas tendue derrière.
- **`sleep`** : respiration très lente (7 s), paupières fermées **par le bas**
  (`eyelid_low_l` et `eyelid_low_r`), une oreille qui frémit de temps en temps, et **un soubresaut
  minuscule une fois par boucle** — il rêve.

---

### VAGUE 5 — Les treize spéciales sans ajout de modèle (13)

Ce sont les récompenses : le joueur les débloque en montant de niveau. Le critère
est simple — **est-ce que le joueur peut la raconter à quelqu'un qui ne l'a pas
vue ?** « Il tire la langue et il l'oublie » se raconte. « Il lève la patte » ne se
raconte pas.

#### `blep` — 8 s — une fois
Il bâille, referme la bouche… **et la langue reste dehors.** Elle reste sortie six
secondes pendant qu'il regarde ailleurs, cligne, bouge une oreille — l'air
parfaitement normal. Puis il la rentre d'un coup à 7,4 s, avec un petit sursaut de
la tête, l'air surpris.
> Deux os seulement (`tongue`, `jaw`), et c'est la chose la plus attachante que ce
> modèle puisse produire.

#### `jugement` — 4 s — `hold_on_last_frame`
`brow_l` monte de 20°, `brow_r` descend de 5°. `pupil_l/r` rétrécissent
(`scale` `[0.6, 0.6, 0.6]`). `head` s'incline de 15° en Z. Il regarde. **Rien
d'autre ne bouge pendant quatre secondes** — même la queue s'arrête. Puis un seul
clignement très lent, et il détourne la tête.
> La première fois qu'un compagnon a un avis sur toi.

#### `etirement` — 4,5 s — une fois
Pattes avant tendues loin devant, griffes qui labourent le sol, dos creusé au
maximum, croupe en l'air, queue droite vers le ciel — **et un frisson qui part de
la nuque et descend jusqu'à `tail6`, segment par segment, avec deux images de
décalage entre chaque**. Long soupir sonore à la fin.
> Un dragon qui s'étire comme un chat : c'est le décalage qui fait qu'on s'attache.

#### `baillement` — 5 s — une fois
La mâchoire s'ouvre. Elle s'ouvre **encore**. Puis **encore** — trois paliers,
chacun plus lent que le précédent. `tongue` s'enroule vers le haut, les oreilles se
replient complètement en arrière, les yeux se plissent, la queue tremble jusqu'au
bout, tout le corps se tasse. Puis la gueule claque, il cligne, il secoue la tête.

#### `tresor` — 6 s — une fois
Il pousse du museau un objet invisible, le fait rouler sur deux unités, le renifle,
s'assoit dessus, enroule sa queue tout autour — **puis relève la tête et regarde le
joueur**, sourcils bas, l'air de dire que c'est à lui.
> Il défend quelque chose qui n'existe pas, et le regard final te désigne comme la menace.

#### `vrombissement` — 4 s — une fois
Les membranes vibrent comme celles d'une libellule, de plus en plus vite. **Seuls
les os `_mem` bougent** — l'armature (`wing_l_fore`, `_f2`, `_f3`, `_tip`) reste
immobile. Les pattes s'ancrent. **Il ne décolle jamais.**
- Particule `minecraft:cloud` sur `pattes` en continu à partir de 2 s.
> Tout dit qu'il va décoller. Il ne décolle pas.

#### `mue` — 7 s — une fois
Il déplie l'aile gauche en grand et l'examine **articulation par articulation** :
l'avant-bras, puis `wing_l_f3`, puis `wing_l_f2`, puis `wing_l_tip` — **et la tête
suit le regard à chaque fois**. Il trouve une membrane qui ne va pas, la secoue, la
mordille. Puis il replie en trois temps.
> Personne n'a jamais vu une aile de dragon être *vérifiée*.

#### `rugissement` — 4,5 s — une fois
Il inspire en reculant la tête, puis projette tout l'avant-corps vers l'avant :
`jaw` grande ouverte, `tongue` plaquée, `spine_ridge` et `spine_ridge_low`
hérissées d'un bout à l'autre, oreilles rabattues, ailes ouvertes à 70 %, les quatre
pattes ancrées. Le cou vibre (deux petites oscillations rapides en X). À la fin la
tête retombe et il souffle.
- Son `@alerte` à 1,2 s.

#### `chasse_queue` — 6 s — une fois
Il aperçoit sa propre queue, se **fige**, pupilles dilatées. Puis il tourne — deux
tours de plus en plus vite, la tête qui essaie de rattraper `tail6`, les orteils qui
dérapent. Au troisième il perd l'équilibre, s'assoit lourdement, secoue la tête, et
regarde ailleurs comme si de rien n'était.
> Le regard d'après. C'est la gêne qui est drôle, pas la chute.

#### `frisson` — 2,5 s — une fois
Une onde de torsion à haute fréquence qui part de la tête et descend jusqu'à
`tail6` : la tête tourne trois fois d'un côté puis de l'autre, `neck` suit avec un
tour de retard, `torso_up` deux, la queue trois. Les ailes claquent.
- Particule `minecraft:splash` sur le corps. À jouer après l'eau, après le sommeil,
  après une frayeur.

#### `menace` — 5 s — `hold_on_last_frame`
Corps très bas, poitrail presque au sol, croupe haute. `spine_ridge` hérissée.
Ailes à demi ouvertes pour paraître plus gros. Les babines retroussées —
**`teeth_upper` visibles sans ouvrir la gueule**. `pupil_l/r` en fente
(`scale` `[0.4, 1, 1]`). La queue qui bat au sol, lentement, comme un chat.
> Il ne fait rien. C'est une posture, et elle est plus impressionnante qu'une attaque.

#### `decollage_vertical` — 2 s — une fois
Il se ramasse complètement — les quatre jarrets pliés au maximum, le corps à
quelques unités du sol, ailes fermées contre le corps — puis **tout se détend en
trois images** : les pattes poussent, les ailes s'ouvrent d'un coup et frappent vers
le bas. Il monte droit.

#### `sommeil_dragon` — 6 s — `hold_on_last_frame`
Il s'enroule complètement : la queue fait **un tour entier** autour du corps, la tête
vient se poser sur sa propre queue, et **une aile se rabat par-dessus comme une
couverture**. Puis plus rien qu'une respiration très lente et une pointe d'oreille
qui frémit.
> La seule qui ne s'arrête pas toute seule. On peut le laisser dormir dans un coin
> et revenir demain.

---

### VAGUE 6 — Le plasma (2, après ajout au modèle)

**Ne les écris pas encore.** Elles demandent sept os qui n'existent pas dans le
modèle actuel. Je te dirai quand ils seront ajoutés.

Les os à venir : `gorge_lueur` (3 cubes émissifs sur `torso_up`, `neck2`, `neck`),
`bouche_lueur` (derrière `teeth_lower`), `boule` (au bout de `jaw`, échelle 0),
`naseau_l`/`naseau_r` (os vides sur `head`), `voile_l`/`voile_r` (membrane
nictitante), `langue2` (enfant de `tongue`).

#### `braise` — 4 s
Même charge que le plasma, mais au lieu du tir : il expire un nuage d'étincelles qui
retombe et s'éteint au sol. Rien ne brûle. **La première fois qu'on voit quelque
chose se passer à l'intérieur de la bête.**

#### `plasma` — 6 s
En cinq temps :
- **0 → 1,2 s — l'ancrage.** Les quatre pattes s'écartent et se plantent, les douze
  orteils se serrent au sol, la croupe s'abaisse. `spine_ridge` puis
  `spine_ridge_low` se hérissent, segment par segment, de la nuque à la queue.
- **1,2 → 2,8 s — la charge.** Il inspire, `torso_up` se gonfle. La lueur monte
  **cube par cube** : `gorge_lueur` sur `torso_up`, puis sur `neck2`, puis sur
  `neck`, chacun passant de l'échelle 0 à 1. `pupil_l/r` se réduisent à un trait.
- **2,8 → 3,1 s — l'ouverture.** `jaw` s'ouvre en grand d'un seul coup, `tongue` se
  plaque au fond, `bouche_lueur` passe à l'échelle 1.
- **3,1 → 4,3 s — le tir.** `boule` passe de l'échelle 0 à 1,5 puis part droit devant
  (position en Z négatif, jusqu'à −48 unités = 3 blocs) en grossissant puis en
  disparaissant. `head` recule de 8° sous le choc, les pattes avant reculent d'une
  demi-unité, les ailes claquent en arrière.
- **4,3 → 6,0 s — l'après.** La gueule reste ouverte, de la fumée sort des narines
  pendant deux secondes (particule `minecraft:smoke`, locator `tete`), il cligne,
  referme, se lèche les babines, `spine_ridge` retombe.

> **La limite à connaître.** Une animation ne déplace un os que *par rapport au
> corps*. La boule reste donc accrochée à la tête : si le dragon tourne pendant le
> tir, elle tourne avec. C'est parfait pour un crachat court et droit — trois blocs,
> une seconde. Un vrai projectile qui part loin et explose ailleurs, ce n'est plus
> de l'animation, c'est du code.

---

## 8. POUR COMMENCER

Réponds d'abord par **une seule chose** : dis-moi si quelque chose dans ce document
te manque pour travailler — un os, une convention, une amplitude de référence.

Puis attends que je te demande la première animation. On commencera par `sit`.
