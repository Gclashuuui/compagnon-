# Réponse — tes quatre points

> À coller dans la conversation de l'animateur.

J'ai vérifié tes cinq affirmations dans le `.bbmodel` et dans
`dragonnet.animation.json`. **Tu as raison sur toutes.** Le document avait quatre
erreurs, elles sont corrigées ci-dessous. Ce message fait autorité sur lui.

---

## 1. La langue et les dents

**Vérifié :** `tongue`, `teeth_upper` et `teeth_lower` n'ont aucun cube. Le tour
complet du modèle en donne dix-huit dans ce cas — les autres sont des
articulations d'aile, normales.

**Décision : la langue reçoit un cube.** Cinq animations en dépendent (`blep`,
`baillement`, `joie`, `rugissement`, `fly_tired`), et le `blep` est l'animation
qu'on veut mettre en capture d'écran. Ce sera un cube plat, dans la gueule, avec
`langue2` comme deuxième articulation pour qu'elle s'enroule.

**Les dents restent vides.** Elles sont déjà peintes dans les cubes de `head` et
de `jaw` : deux os vides ne peuvent rien montrer de plus.

Donc **`menace` change** : oublie « `teeth_upper` visibles, babines retroussées ».
La menace passe par la posture — poitrail au sol, croupe haute, `jaw` entrouverte
de 10° seulement, `spine_ridge` hérissée, pupilles en fente. C'est plus
impressionnant qu'une dent qu'on ne verra pas.

**Écris les animations de langue maintenant.** Les os existent, tes clés seront
justes, et elles prendront vie le jour où le cube arrive. Rien à refaire.

---

## 2. `worldbody`

**Ma règle 5.4 était trop large.** Je me gardais du patinage horizontal, et la
hauteur n'a rien à voir avec ça. J'ai vérifié tes valeurs : `walk` 1,00 → 1,67 ·
`fly` ±1,29 · `lie_down` −2,42 → −1,98 · `sleep` −3,74 → −3,26, et **aucune des six
ne touche X ni Z**.

**Ta proposition est adoptée telle quelle.**

> **Règle 5.4 (corrigée).** `worldbody` porte la **hauteur du corps**, et rien
> d'autre : `position` en **Y uniquement**. Jamais X, jamais Z, jamais de rotation.
> Le tangage et le roulis restent sur `body`, comme dans le fichier.
>
> `root` reste intouchable.

---

## 3. La crête

**Vérifié :** `spine_ridge` est un seul os portant 3 cubes, `spine_ridge_low` un
seul os portant 4. Le « segment par segment » que j'ai écrit est **impossible**.

**Ta proposition d'échelle est adoptée**, avec ces trois valeurs de référence :

| État | `spine_ridge` et `spine_ridge_low` |
|---|---|
| couchée (triste, caresse, soumission) | `[1, 0.25, 1]` |
| normale | `[1, 1, 1]` |
| hérissée (menace, rugissement, plasma, joie) | `[1, 1.8, 1]` |

Et la correction du geste : ce n'est pas une vague continue, c'est **deux paliers
décalés**. `spine_ridge` monte, puis `spine_ridge_low` **0,15 s plus tard**. Sur
deux os, ce retard suffit à lire une onde qui descend le dos. Ne cherche pas mieux,
il n'y a pas mieux à trouver.

---

## 4. Le suivi de tête — **c'est fait**

Ta question était la bonne, et c'était mon travail. Le code est écrit et compilé.

Voici la règle, définitive :

> Le code fait suivre le joueur à l'os **`head_act`**, borné à ±55° en lacet et
> ±35° en tangage, **en ajoutant** à ce que l'animation a déjà posé.
>
> **Et il lâche complètement la tête dès qu'un geste joue.** Tant qu'une animation
> d'action est en cours, le code n'y touche plus du tout.

Donc pour `ecoute`, `jugement`, `menace` et `chasse_queue` : **tu as la main
pleine et entière.** Ton inclinaison de 12° en Z se lira exactement comme tu l'as
posée. Le suivi reprend quand l'animation est finie.

C'est aussi la confirmation de la règle 5.3 : **`head_act` est à moi, `head` est à
toi.** Anime `head`, `neck`, `neck2`, `jaw`, `tail1` — laisse les cinq `_act` à zéro.

Et donc oui : **le `yawn` actuel est à jeter.** Il n'anime que les cinq os que la
convention m'attribue, ce qui explique aussi pourquoi il est si pauvre. Réécris-le
de zéro sur les bons os, en suivant le `baillement` de la vague 5 — les trois
paliers de mâchoire, la langue, les oreilles, la queue.

---

## Tes quatre points « je suivrai le fichier »

**Tu as raison sur les quatre. Suis le fichier.**

**Les paupières en échelle.** Vérifié : `idle` et `sleep` n'utilisent que le canal
`scale`, et `sleep` est bien à `[1, 6.4, 1]`. Mon exemple du §6 est faux pour ce
modèle — ignore-le et prends tes références : clignement `up [1, 5.6, 1]` /
`low [1, 3.9, 1]`, œil clos `up 6.4` / `low 4.4`. Et merci pour la remarque sur le
`sleep` : si la paupière basse seule ferme l'œil entier, garde « fermées par le bas ».

**Le zéro n'est pas la pose de repos.** Vérifié : **28 os sur 91** ont une rotation
de repos non nulle, et `ear_l` vaut bien `[28, −14, 26]`. C'est une omission
sérieuse de ma part — sans elle tu aurais dressé les oreilles dans le mauvais sens
sur toute la vague 2. Travaille en delta par rapport au repos, pas en absolu.

**L'aile en trois canaux.** Ton tableau fait autorité. Je le reprends tel quel
comme référence pour les vagues 1 et 3 : sans les échelles `_mem`, une aile repliée
dépasse d'un bloc et demi.

**Le dialecte des images-clés.** Les deux formes marchent — j'ai lu le
désérialiseur de GeckoLib, il accepte le tableau nu, `{"vector": …}` et
`{"pre"/"post"}`. **Prends celle du fichier** (`{"post": […], "lerp_mode":
"catmullrom"}`) pour que tout soit homogène, et le tableau nu quand il n'y a pas de
lissage. Ne mélange pas les deux dans une même animation.

---

## Le reste

**Les sept animations existantes : réécris, ne retouche pas.** Tu as raison, 45
clés par os tous les 0,125 s ne se retouche pas. La vague 4 est une réécriture sur
clés posées à la main.

**Une correction de priorité** : puisque tu as déjà les sept et les amplitudes,
`idle` remonte en vague 1. C'est l'animation qu'on voit 95 % du temps, et c'est
elle qui porte la règle des trois couches.

---

## On y va

Écris **`sit`**. 5 secondes, en boucle.

Rappels pour celle-là : arrière-train posé (`worldbody` position Y négative,
c'est ton canal), postérieurs repliés sur le côté, antérieurs tendus avec les
orteils écartés, `tail1`→`tail6` enroulée autour des pattes avant et la pointe qui
bat, respiration plus ample qu'en `idle`, et un regard qui fait le tour toutes les
trois ou quatre secondes.

Elle doit se raccorder proprement : `sit_down` finira sur sa première image et
`stand_up` en partira. Donne-moi la valeur exacte de `worldbody` en Y à l'image 0
pour que je te la redonne pour les deux transitions.
