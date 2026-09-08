# Les outils

Des programmes autonomes, écrits pendant le projet. Ils ne font pas partie du
mod : ils servent à préparer les images et à mesurer.

Tout se compile depuis la racine du projet.

---

## Détourer une planche de pixel art

C'est l'outil principal. Il prend une planche de Gemini (fond magenta) et en sort
des PNG à fond transparent.

```bash
javac -d outils outils/Detourer.java && java -cp outils Detourer maplanche.jpg --sortie=icones --prefixe=truc
```

| Option | À quoi ça sert |
|---|---|
| `--sortie=<dossier>` | où écrire les PNG (obligatoire) |
| `--prefixe=<nom>` | le début du nom des fichiers |
| `--bas=<N>` | coupe N pixels en bas — pour les planches où Gemini a écrit des légendes |
| `--libre` | une forme = une icône, sans grille 5×2 et sans forcer en carré 32×32 |

Sans `--libre`, il suppose la grille de 5 colonnes sur 2 lignes et rend des
carrés de 32×32. Avec, chaque forme garde ses vraies proportions — c'est ce qu'il
faut pour la bulle de pensée, où les nuages font 78 pixels de large.

**Trois choses qu'il gère et qu'on n'attend pas :**

- il **remplit depuis les bords** au lieu de tester la couleur, ce qui sauve le
  gâteau au glaçage rose et les fraises ;
- il **écarte les pixels salis par le JPEG** au moment de lire les couleurs,
  sinon chaque contour ressort bordé de violet ;
- il efface les **trous de fond enfermés** dans un objet — la boucle du fil, entre
  les lames des ciseaux — en exigeant cette fois la couleur exacte du fond.

---

## Regarder ce qu'on a détouré

```bash
javac -d outils outils/Contact.java && java -cp outils Contact icones controle.png 10
```

Assemble les PNG d'un dossier en une planche, agrandie et posée sur un **damier**.
Le damier n'est pas décoratif : sur fond uni, un halo mal détouré ne se voit pas.

Le troisième argument est le nombre de colonnes.

---

## Mesurer la grille d'une planche

```bash
javac -d outils outils/Analyse.java && java -cp outils Analyse maplanche.jpg
```

Dit la couleur du fond, le pourcentage de magenta, et surtout **la taille du bloc
de pixel art**, mesurée et non supposée — Gemini ne respecte jamais les
dimensions demandées.

Il corrige un biais : compter les changements de couleur qui tombent sur une
frontière favorise mécaniquement les petites périodes. Il compare donc à ce que
donnerait un placement au hasard.

---

## Mesurer l'intérieur d'un nuage

```bash
javac -d outils outils/Interieur.java && java -cp outils Interieur src/main/resources/assets/compagnon/textures/gui/nuage_1.png
```

Trouve le plus grand rectangle plein à l'intérieur d'une forme. C'est comme ça
qu'on sait où le texte ou l'objet tiennent dans la bulle, au lieu de deviner une
marge — les bosses du pourtour n'ont pas la même épaisseur partout.

---

## Chiffrer la boîte de collision

```bash
javac -d outils outils/BancParties.java && java -cp outils BancParties
```

Compare l'ancienne version (sinus et cosinus recalculés pour chaque partie) à la
nouvelle. **Résultat mesuré : 71 % de gagné, mais 0,026 % d'un tick au total.**
Autrement dit : c'était une vraie économie, et ça ne valait quand même pas la
peine d'y revenir.

Ce banc ne mesure **pas** la requête au monde, qui demande un serveur en marche.

---

## Vérifier les données

```bash
node outils/verifier.js .
```

Contrôle que les rêves ont leurs phrases et que les noms proposés tiennent.

⚠️ **Il fait double emploi avec les tests.** Depuis `src/test/java`, un simple
`gradlew test` vérifie la même chose et bien plus — les barres inconnues, les
bobos incurables, les textures manquantes, les numéros de modèle en double. Le
script node reste pratique pour un contrôle rapide sans lancer Gradle.

---

## Quand on reçoit une nouvelle planche de Gemini

1. `Analyse` pour voir si la grille est celle qu'on a demandée.
2. `Detourer` pour sortir les PNG.
3. `Contact` pour regarder le résultat sur un damier.
4. Renommer et déposer dans `src/main/resources/assets/compagnon/textures/item/`.
5. `gradlew test` — il refusera de passer si une texture ou un modèle manque.

## bb2geo.js — un bbmodel vers le mod

```bash
node outils/bb2geo.js <le.bbmodel> <espece> <geo.json> <animation.json> <dossier/textures>
```

Convertit un modele Blockbench en ce que le mod attend : geometrie, animations
et toutes les textures d'un coup.

Blockbench garde tout dans SON repere et ne bascule vers celui de Bedrock qu'a
l'export. L'outil refait cette bascule : le X s'inverse partout, et les
rotations autour de X et Y avec lui.

Il **renomme les animations** au passage — `animation.mount.vol` devient
`animation.<espece>.vol`. Sans ca, le mod les croirait d'une espece appelee
« mount » et les afficherait dans la roue de tout le monde. Voir
`Especes.concerne`.

Et il **verifie son propre travail** : les deux ailes doivent se retrouver
symetriques autour de zero et la tete devant. Si ces deux-la sont bonnes, la
bascule l'est aussi.
