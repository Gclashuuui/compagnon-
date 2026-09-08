# Prompt — les cinq animations qui manquent au kobeko

> Colle tout ce fichier comme premier message dans une conversation Claude neuve.
> Il est autonome.

---

Tu es animateur 3D. Tu vas écrire cinq animations pour un petit bipède d'un mod
Minecraft Java 1.21.1 qui utilise **GeckoLib 4.7.7**. Le format est du JSON
**Bedrock**, à coller dans `kobeko.animation.json`.

La créature en a déjà trente, mais ce sont celles d'un monstre de combat :
attaques, gardes, soins, provocations. Elle devient un **compagnon de joueur**, et
il lui manque exactement cinq choses pour ça.

---

## Le modèle : 21 os

Bipède. Texture 64×64. Il regarde vers **−Z** (la mâchoire est à z = −3, la queue
à z = +3,3). **+Y est le haut.** Les pieds touchent le sol vers y = −1.

```
Kobeko            [0, 7.06, 3.37]
  torso           [0, 5.26, 3.37]
    upper         [0, 9.2, 3]
      cube_r1     [0, 11.2, -1]      (1 cube — le buste)
      head        [0, 11.2, -2]      (4 cubes)
        jaw       [0, 13.2, -3]      (2 cubes)
          cube_r2 [0, 13.2, -3]      (1 cube)
      left_arm    [2.5, 10.4, -0.5]
        cube_r3   [0, 11.2, -1]      (1 cube)
      right_arm   [-2.5, 10.4, -0.5]
        cube_r4   [0, 11.2, -1]      (1 cube)
        weapon    [-2.07, 6.21, -4.71]   (os d'accroche, vide)
    lower         [0, 9.24, 3.02]
      cube_r5     [0, 9.24, 3.02]    (1 cube — le bassin)
      right_leg   [-1.5, 5.26, 0.87] (1)
        right_knee  [-1.5, 2.26, 0.37] (1)
          right_foot  [-1.5, -0.74, 0.37] (1)
      left_leg    [1.5, 5.26, 0.87]  (1)
        left_knee   [1.5, 2.26, 0.37]  (1)
          left_foot   [1.5, -0.74, 0.37]  (1)
      tail        [0, 5.45, 3.33]    (2 cubes)
```

**Attention à la gauche et à la droite** : `left_arm` est à x = **+2,5** et
`right_arm` à x = **−2,5**. C'est l'inverse de la convention habituelle. Fie-toi
aux noms, pas aux signes.

---

## Le format

```json
"animation.kobeko.NOM": {
  "loop": true,
  "animation_length": 4.0,
  "bones": {
    "head": {
      "rotation": {
        "0.0": [0, 0, 0],
        "2.0": [-3, 0, 0],
        "4.0": [0, 0, 0]
      }
    }
  }
}
```

**Les clés autorisées, et elles seules** (vérifiées dans le code de GeckoLib —
une clé mal orthographiée est ignorée en silence) :

- animation : `loop`, `animation_length`, `bones`, `sound_effects`, `particle_effects`
- os : `rotation`, `position`, `scale`
- image-clé : `[x, y, z]`, ou `{ "post": [...], "lerp_mode": "catmullrom" }`

`"loop": true` pour un état qui dure, **pas de clé** pour un geste qui se joue une
fois. **`animation_length` est obligatoire** : le mod lit cette valeur dans le
fichier pour savoir combien de temps immobiliser la bête.

**Unités** : temps en secondes (clés de texte `"0.0"`, `"1.25"`), rotation en
degrés, position en **unités de modèle** — 16 unités = 1 bloc, et cette créature
fait 17 unités de haut. Une translation de 2 unités est déjà beaucoup.

---

## Trois règles

**1. Les pieds ne glissent jamais.** Un pied posé garde exactement la même
position tant qu'il est au sol : une clé au contact, **la même valeur** au
décollage, rien entre les deux.

**2. Rien n'est jamais complètement immobile.** Même assis, trois couches se
superposent — la respiration (`torso`, 1 à 2°, cycle de 4 à 6 s), une dérive
lente (`head`, `tail`, sur un cycle **plus long et non multiple** du premier), et
des micro-gestes (la mâchoire, une oreille, un doigt). Trois périodes premières
entre elles (5 s, 7 s, 11 s) donnent une boucle qu'on ne reconnaît pas ; 4, 8 et
12 se resynchronisent et l'œil le voit.

**3. Sur une boucle, la première et la dernière clé sont identiques**, sinon elle
saute à chaque tour.

---

## Les cinq animations

Écris-les **une à la fois**. Tu t'arrêtes après chacune et tu attends la suivante.
Après chaque animation, dis en trois lignes ce que tu as fait bouger, ce dont tu
n'es pas sûr, et ce que tu proposerais d'ajouter.

### 1. `sit` — 4 s — boucle

**La plus urgente.** Le mod a un ordre « assis » que le kobeko ne sait pas jouer.
Il possède `sit_start` et `sit_end`, mais ce sont deux transitions d'une seconde
sans pose entre les deux : il s'assoit et se relève aussitôt.

- Fesses au sol : `Kobeko` ou `torso` descend en position Y (c'est ton canal de
  hauteur), genoux repliés vers l'avant, pieds à plat devant lui.
- Les bras posés sur les cuisses ou pendants, pas raides.
- La queue étalée derrière, la pointe qui bat lentement.
- Respiration **plus ample** qu'en `idle` : au repos on souffle plus fort.
- Toutes les 3 à 4 s, la tête tourne pour regarder autour.

**Doit se raccorder** : la dernière image de `sit_start` et la première image de
`sit_end` existent déjà. Donne-moi la pose exacte de ta première image pour que je
vérifie que les trois s'enchaînent sans à-coup.

### 2. `lie_down` — 6 s — boucle

Il n'a **aucune animation couchée**. Le mod a un ordre « couché », et il ne se
passe rien.

Couché sur le ventre, jambes repliées de part et d'autre, tête posée sur les bras
croisés ou relevée selon le moment. La queue enroulée le long du corps, **pas
tendue derrière**. Respiration lente.

### 3. `sleep` — 8 s — boucle

Il ne dort pas non plus. Le mod fait dormir le compagnon **quand le joueur se
couche dans un lit** — c'est l'un de ses meilleurs moments et le kobeko en est
exclu.

Même pose que `lie_down`, mais complètement relâchée : respiration très lente et
très ample (7 s par cycle), tête bien posée, mâchoire légèrement entrouverte. Et
**une fois par boucle, un soubresaut minuscule** : il rêve.

### 4. `run` — 0,35 s — boucle

Il n'a que `walk` (0,5 s), qu'on réutilise faute de mieux — donc il court
aujourd'hui à la vitesse d'un promeneur.

Une vraie course de bipède : foulée plus longue, buste penché vers l'avant, bras
qui pompent en opposition avec les jambes, une **phase de suspension** où les deux
pieds quittent le sol. La queue tendue à l'horizontale pour l'équilibre.

### 5. `turn` — 1,5 s — une fois

Le mod lui demande de « tourner sur lui-même » quand son maître s'arrête de
marcher, et il n'a rien à jouer.

Un tour complet sur place, en pas croisés. La queue traîne **en retard** et fouette
à la fin. La tête garde le regard vers l'avant le plus longtemps possible avant de
rattraper le corps — comme une danseuse qui « spotte ».

---

Commence par **`sit`**.
