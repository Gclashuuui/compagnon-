# REPRENDRE — le mod Compagnon

Ce document remplace la lecture du code pour reprendre le projet. Il est tenu à
jour à la main : **si tu changes quelque chose d'important, change-le ici aussi.**

---

## 1. Ce qu'on fabrique

Un mod Minecraft Fabric **1.21.1** pour un serveur RP Poudlard francophone. Chaque
joueur possède un compagnon vivant — un dragonnet pour l'instant. On le nourrit,
on le soigne, on le caresse, on lui parle. Il a un caractère, il grandit en
niveaux, il se souvient de ce qui lui arrive.

**Les joueurs ne tapent jamais de commande.** Tout passe par le clic droit, le
livre et la roue. Les commandes sont réservées à l'équipe (permission 2).

Objectif d'échelle annoncé : **mille joueurs, mille compagnons** dans le château.

---

## 2. La pile — décidée, ne la rediscute pas

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loom | `net.fabricmc.fabric-loom-remap` **1.17.14** — pas `fabric-loom`, qui ne sait plus remapper la 1.21.1 |
| Mappings | Mojang officiels |
| GeckoLib | **4.8.4, comme PLANCHER** — on compile contre la plus vieille version acceptée |
| Plasmo Voice | `compileOnly` — jamais exigé, le mod tourne sans |
| Vosk + JNA | embarqués (`include`) |
| JUnit | 5, tests sans Minecraft |

`splitEnvironmentSourceSets()` : `src/main` est le serveur, `src/client` le
client. Le compilateur refuse d'appeler une classe client depuis le serveur.

**Le projet est sur OneDrive. C'est un choix assumé.** Loom affiche un
avertissement à chaque build. Ne propose jamais de le déplacer.

---

## 3. L'architecture, en trois phrases

**La fiche est la vérité.** `FicheCompagnon` est une `SavedData` : elle survit à
tout. L'entité n'est qu'un **affichage temporaire** (`shouldBeSaved() == false`)
que `Apparition` fait apparaître à 20 blocs d'un joueur et disparaître à 24.
L'UUID de l'entité est celui de la fiche.

**Le serveur décide de tout.** Chaque paquet venu du client est une *demande*,
revérifiée côté serveur.

**Le contenu est une donnée, jamais du code.** Une espèce, un aliment, un remède,
un rêve, un nom : un fichier JSON. Jamais une classe.

---

## 4. Ce qui est FAIT

**Le compagnon** — une seule classe pour toutes les espèces. Boîte de collision
en plusieurs morceaux, écrite à la main parce que Minecraft n'en connaît qu'une
par entité. Il suit à droite de son maître, s'arrête quand il s'arrête, ne le
pousse jamais.

**Le livre et la roue** — interface complète, jauges dégradées, moments datés.

**Le contenu** — 40 aliments et 30 soins, avec leurs icônes en pixel art 32×32.
Deux objets seulement sont enregistrés (`aliment`, `soin`) : la variété est une
donnée, et un **numéro de modèle** choisit la texture.

**L'onglet créatif** — les 70 objets plus l'œuf, rangés dans l'ordre des planches.

**La nourriture** — les aliments du mod **et** celle de Minecraft. La vanilla
remplit le ventre ; celle du mod rapproche. C'est la seule différence, et elle est
voulue.

**La faim** — il réclame en montrant l'objet dans une bulle, **3 minutes puis un
silence** qui se resserre de 12 à 2 minutes selon sa faim. Il veut la **même chose
toute la journée**.

**La bulle** — un nuage en pixel art avec trois petites bulles qui montent.
**Elle ne contient que l'objet réclamé, jamais de texte** : un compagnon ne parle
pas. Ce qu'il vit s'écrit dans son livre.

**La voix** — Plasmo Voice + Vosk, hors ligne. « Cacahuète assis ». Six ordres :
quatre qui changent son **mode** (viens, assis, couché, reste) et deux **gestes**
(chope, lâche). Testable sans micro : `/compagnon voix <phrase>`.

**Chope et lâche** — tu jettes un objet, « Cacahuète chope », il va le chercher,
baisse la tête et le garde dans la gueule. Il te suit avec. « Cacahuète lâche »,
il le pose. Ces deux-là **ne passent pas par l’obéissance** : un compagnon qui
boude en gardant ta pioche serait insupportable.

**Le caractère** — 5 caractères tirés à la naissance, figés à vie.

**L'obéissance** — sous 60 de complicité il fait la sourde oreille. Jamais pour
« viens », jamais deux fois de suite.

**Les amitiés** — les compagnons qui se croisent souvent se reconnaissent, se
cherchent, et se font des cadeaux.

**Il rapporte** — il ramasse ce qui traîne et vient te le poser aux pieds. Le
premier objet devient son objet préféré, pour toujours.

**Les humeurs** — l'orage, l'eau, le noir, le vide, la neige, l'ennui, la
fatigue. Chaque première fois s'écrit dans son livre.

**L'abri** — sous la pluie, les compagnons libres cherchent un toit. Ils s'y
retrouvent tous, sans que rien ne les coordonne.

---

## 5. Où vivent les données

```
data/compagnon/especes/     géométrie, animations, variantes, boîte en morceaux
data/compagnon/aliments/    40 aliments : nom, effets, numéro de modèle
data/compagnon/soins/       30 remèdes
data/compagnon/bobos/       chaque bobo nomme le remède qui le règle
data/compagnon/caracteres/  les 5 caractères
data/compagnon/niveaux.json 50 paliers + la dérive des barres
data/compagnon/reves.json   40 rêves (les phrases sont dans lang/)
data/compagnon/voix.json    les façons de dire chaque ordre
data/compagnon/noms.json    103 noms vérifiés dans le vocabulaire du moteur
assets/compagnon/           textures, modèles, geo, animations, lang
```

**Ajouter du contenu = ajouter un fichier + `/reload`.** Jamais recompiler.

### Le dossier du serveur

```
<dossier du serveur>/compagnon/especes/*.json
<dossier du serveur>/compagnon/aliments/*.json
<dossier du serveur>/compagnon/soins/*.json
<dossier du serveur>/compagnon/bobos/*.json
```

Créés vides au premier démarrage. On y dépose un fichier, on fait `/reload`, c'est
en jeu. Un fichier du même nom **remplace** celui du mod.

⚠️ **Il porte les chiffres, pas les images.** Un modèle, une texture, une
animation vivent chez le joueur. Une espèce vraiment nouvelle a donc aussi besoin
d'un **pack de ressources**, distribué par `resource-pack=` dans
`server.properties`. Aucun serveur ne peut inventer une image dans la mémoire d'un
client — c'est une règle de Minecraft, pas une limite du mod.

---

## 6. Les commandes (équipe, permission 2)

```
/compagnon donner <joueur> <espece> <variante>   remet un œuf
/compagnon objet  <joueur> <genre> <variete>     remet un aliment ou un soin
/compagnon creer | supprimer | fiches | info | table
/compagnon anim <nom> | variante | espece | barre | xp | niveau | bobo
/compagnon envie <aliment>                       lui fait réclamer
/compagnon envie objet <item>                    ... ou n'importe quel objet du jeu
/compagnon envie rien                            arrête
/compagnon voix <phrase>                         parle, sans micro
/compagnon voix etat                             pourquoi la voix ne répond pas
```

`/compagnon table` et `/compagnon voix etat` sont les outils de diagnostic : ils
disent ce que le serveur a **vraiment** chargé.

---

## 7. Ce qui reste à faire

- **Les animations** — l'auteur les fait lui-même. Cases vides dans
  `especes/dragonnet.json` : `assis`, `reactions.caresse`.
- **Le geste de caresse** — à refaire quand les animations existeront.
- **Le perchoir est fait** pour les petites espèces : assis puis clic droit les
  fait monter sur l'épaule, un second clic les pose sur la tête, un troisième
  les fait redescendre. Le point d'accroche est synchronisé pour que tous les
  joueurs voient la même position.
- **La table compte 50 niveaux** et se prolonge ensuite sans plafond. Une montée
  affiche un titre, joue un son et entoure la bête d'un double halo lumineux qui
  monte, adapté à sa taille. Elle déclenche aussi le rôle optionnel
  `reactions.niveau` — ou `reactions.joie` en repli. Le sous-titre annonce les
  gestes, points de compétence et la monte nouvellement débloqués. Le livre
  affiche le niveau sans faux maximum et la jauge d'XP indique au survol ce
  qu'il reste avant le suivant.
- **L’animation `ramasse`** : le rôle `@ramasse` est appelé quand il baisse la
  tête pour prendre un objet. Case à remplir dans `reactions` du fichier
  d’espèce ; sans elle il ramasse quand même, sans le geste.
- **Les métadonnées du mod sont renseignées** : auteur `Gclashuuui`, dépôt et
  suivi des problèmes sur GitHub, licence actuelle `All-Rights-Reserved`.
- **La rubrique « Il connaît » est faite** dans la page d'histoire du livre.
  Elle affiche les joueurs et les compagnons devenus familiers ; les UUID sont
  résolus côté serveur avant l'envoi au client.
- **Le jar pèse 29,7 Mo** à cause de Vosk et JNA, que seul le serveur utilise. La
  seule vraie sortie est de scinder en deux jars. Pas fait : le gain ne vaut pas
  le risque de casser le build aujourd'hui.
- **La chaîne audio n'a jamais tourné** — pas de micro pendant le développement.

---

## 8. Les pièges déjà payés

**L'index spatial.** Faire apparaître les compagnons coûte *fiches × joueurs*.
Ça ressemble à un problème à mille contre mille. **Ce n'en est pas un** : mesuré à
0,5 ms dans le pire cas. Un index par tronçon a été écrit puis mesuré **5 à 200
fois plus lent**. Ne pas le réintroduire sans mesurer d'abord.

**La boîte de collision.** Mesurée aussi (`outils/BancParties.java`) : **0,026 %
d'un tick** pour 200 compagnons des deux côtés. Ne pas l'optimiser davantage. Ce
qui n'a *pas* été mesuré, c'est le `getEntities()` qui suit.

**`Map.copyOf` ne conserve pas l'ordre.** Utiliser `Collections.unmodifiableMap`
partout où l'ordre du fichier doit tenir.

**Les barres qui n'existent pas.** Il n'y a que `faim`, `energie`, `complicite`,
`sante`. L'humeur se **calcule** à partir d'elles. Un fichier qui nomme `humeur`
ou `proprete` est **écarté en silence** — 36 aliments l'ont été. Un test l'attrape
maintenant.

**Un mot absent du vocabulaire français** est retiré de la grammaire vocale **en
silence**. `Lexique` lit les 135 774 mots du modèle pour n'inscrire que les noms
prononçables, et rend au passage les accents non tapés (« Cacahuete » →
« cacahuète »).

**Le fil audio ne doit jamais lire une entité.** Le son arrive sur le fil réseau
de Plasmo, le monde appartient au fil principal. `Voix` prépare la liste des mots
côté serveur ; le fil audio ne fait que la lire, et toute action repart par
`serveur.execute`.

**Autour de Vosk : attraper `Throwable`, pas `Exception`.** Une faute native
arrive en `Error`. Et `close()` doit être `synchronized` comme les méthodes de
travail, sinon un fil libère la mémoire pendant qu'un autre s'en sert — et la voix
meurt pour tout le serveur jusqu'au redémarrage.

**Dans le repère de la bulle, 1 unité = 0,025 bloc.** Un objet dessiné à l'échelle
1 y occupe **une unité**, pas un bloc. Diviser par 40 rend une pomme invisible.

**La profondeur dans un repère tourné vers la caméra.** Le sens de l'axe Z n'est
pas devinable. Trois tentatives ont échoué avant qu'on renonce à deviner : le
texte s'écrivait derrière le nuage.

---

## 9. Comment l'auteur veut travailler

- **Ne devine pas. Mesure.** Et jamais « c'est corrigé » sans chiffre.
- **Demande-toi ce que ton test NE mesure PAS.**
- **Le code doit compiler** avant d'annoncer une étape finie.
- **Messages courts, phrases simples.** L'auteur dicte à la voix.
- **Ne rien écrire sur le disque sans le dire.**
- **Pas de fichiers non demandés.**
- **Pas de sous-agents ni de workflows** sans qu'il l'ait demandé : ça consomme
  énormément, et il l'a dit deux fois.
- Éditer le Java avec l'outil d'édition, jamais avec des scripts perl multilignes
  — ils ont corrompu des fichiers trois fois.

---

## 10. Vérifier que tout tient

```bash
gradlew test
```

20 tests, sans Minecraft. Ils attrapent les trois pires défauts du projet, qui
étaient tous dans les **fichiers** et ne levaient **aucune erreur** : les barres
inconnues, les bobos devenus incurables, les textures manquantes.

`outils/README.md` explique les programmes de détourage et de mesure.

---

## La voix : quatre pannes trouvees et corrigees

Le vocabulaire n'y etait pour rien. Les 31 mots d'ordre ont ete verifies un par
un contre le vrai modele (`graph/Gr.fst`, 135 774 mots) : **tous presents**,
`chope` et `lache` compris. Les pannes etaient dans le tuyau.

1. **Le moteur etait jete en pleine phrase.** `getEntitiesOfClass` ne promet
   aucun ordre ; le fil audio comparait la nouvelle liste de mots a l'ancienne
   avec `equals`, qui tient compte de l'ordre. Un simple reclassement des
   entites suffisait a faire croire que la liste avait change. Tout le son deja
   avale partait avec le moteur. → `Voix.preparer` trie ; et un moteur ne se
   refait plus tant qu'une phrase est en cours (`Ecoute.aRefaire`).

2. **Le debut de chaque phrase etait jete.** Un fragment arrive avant que le
   profil soit pret etait supprime, avec un commentaire rassurant sur « vingt
   millisecondes que personne n'entend passer ». C'etait les vingt premieres :
   celles du **d** de « dragon ». → `OreilleCompagnon.enAttenteDeProfil` garde
   jusqu'a une seconde de son et le donne au moteur des qu'il existe.

3. **Le profil mourait apres quinze secondes de silence**, donc chaque reprise
   de parole repayait le defaut ci-dessus. → deux minutes.

4. **Tout echouait en silence.** Micro coupe, mot mal entendu, bete qui boude,
   gueule deja pleine : quatre pannes, un seul symptome — un dragon immobile.
   → `EcouteVocale.repondre` affiche toujours quelque chose dans la barre
   d'action, y compris **ce qui a ete entendu** quand rien n'a marche.

Accessibilite, en plus du seuil de silence deja passe a 900 ms : le nom est
retenu 30 s (« Dragon... » puis « assis »), et un ordre sans nom attend 8 s
(« Chope ! » puis « Dragon ! »). Les deux sens marchent.

`/compagnon voix etat` dit maintenant **les mots exactement guettes** pour celui
qui tape la commande. C'est le premier endroit ou regarder.

## La mouette

Ajoutee sans une ligne de code : `especes/mouette.json` + trois fichiers dans
`assets/`. 52 animations, 16 des plus expressives posees dans la roue entre les
niveaux 1 et 42. Son os `beak` est trouve tout seul par `Bouches`.

**Un defaut est apparu avec elle et a ete corrige** : la roue envoyait *tous* les
deblocages de la table, sans regarder l'espece. Le dragonnet voyait donc les
cases de la mouette, qui ne faisaient rien. `Especes.concerne` tranche sur la
convention `animation.<espece>.<geste>`, et un nom hors convention reste visible
par tout le monde. Trois tests le gardent (`EspecesTest`).

## Plusieurs compagnons

Les touches ouvraient toujours le premier. `Touches.actif` retient le dernier
ouvert ; **Tab** change de bete dans le livre **et** maintenant dans la roue.

## Ce qui reste a remplir

- `especes/dragonnet.json` : `assis` et `reactions.caresse`/`ramasse` sont vides.
  La mouette, elle, a `sit`, `settle_feathers` et `peck`.
- La mouette n'a qu'une variante (`blanche`). Ajouter un `.png` suffit.

---

## Le nom n'est plus obligatoire — la correction qui compte

Le diagnostic en jeu a tranche : `sabre` etait bien dans la grammaire, et le
moteur a quand meme rendu `assis` tout seul. Le nom est le mot le plus fragile
de la phrase — un mot rare, souvent invente, au milieu de mots courants — et
c'est precisement celui dont on n'a pas besoin.

`EcouteVocale` cherche maintenant le destinataire dans cet ordre :

1. le nom prononce ;
2. celui qu'on vient de nommer (30 s) ;
3. **le seul qui ecoute** — une seule bete a portee, ou une seule qui soit a
   vous parmi plusieurs. Aucun doute, donc aucun nom necessaire ;
4. deux betes a vous : la seule fois ou il faut choisir. Le message les
   **nomme** (« lequel ? Sabre ou Mouette ? ») au lieu du « J'attends de savoir
   a qui » qui n'aidait personne.

Une seule recherche dans le monde par phrase, au lieu de trois.

## Soixante-dix tournures

`voix.json` est passe de 20 a 70 facons de dire les six ordres. Chacun des 56
mots a ete verifie dans le vrai modele : **aucun absent**. Deux ont ete refuses
a la verification et ne sont donc pas dans le fichier — `assieds` et `libere`
sans accent.

`TournuresTest` verifie qu'aucune tournure n'en masque une autre, dans les deux
sens, et qu'un nom place devant ne casse rien. C'est le genre de defaut qui ne
se decouvre qu'au micro, par hasard, des semaines plus tard.

## Le vol

`especes/*.json` accepte `"vole": true`. Absent, la bete ne vole pas — une
espece ecrite avant ce champ ne se met pas a decoller.

`Vol` n'est pas une navigation volante : c'est un **coup d'ailes** demande par
un but, tick par tick. Il n'y a aucun etat a eteindre — si plus personne ne
demande, la gravite revient d'elle-meme au tick suivant. C'est volontairement
plus bete qu'un drapeau : un drapeau qu'on oublie de baisser, c'est une mouette
qui flotte pour toujours au milieu de la cour.

Branche sur « viens » (apres un echec de chemin a pied) et sur le suivi. Une
bete qui vole ne se teleporte plus : elle monte, et on la voit monter.

## Le carnet

Nouvelle touche **C**. Une page pour celui qu'on regarde (modele 3D, niveau,
etat), une page pour la liste. Un bouton **Faire venir** / **Le ranger**.

`FicheCompagnon.sorti` dit s'il a le droit d'exister dans le monde. Faux ne veut
pas dire mort : la fiche vit, ses barres tournent, son livre s'ouvre. C'est
`Apparition` qui refuse de le faire apparaitre.

`reglages.json` — nouveau fichier — porte `sortis_en_meme_temps` (1 par defaut,
`0` pour aucune limite). En sortir un de plus **range le precedent** au lieu de
refuser : un refus obligerait a deux gestes pour une seule intention.

## Ce qui est retenu, et ce qui ne l'est pas

Aucune phrase entendue n'est ecrite sur le disque. `Voix.derniereEntendue` est
**une** chaine, globale, ecrasee a chaque fois, jamais sauvegardee.

En memoire, tout est indexe par joueur et efface a la deconnexion — sur
`ServerPlayConnectionEvents.DISCONNECT`, qui se declenche toujours, et non plus
sur l'evenement de Plasmo, qui manquait les plantages et les joueurs n'ayant
jamais parle.

Sur le disque, par fiche : moments (bornes), compteurs (bornes par le code),
connaissances (plafonnees a 64). Rien ne grandit sans fin.

## La camera

Le mod ne touche plus a F5. Elle passait en troisieme personne pendant chaque
caresse : joli une fois, penible les cent suivantes.

---

## La voix etait muette, et le diagnostic le disait

Une ligne du `/compagnon voix etat`, en jeu, a tout explique :

> Mots guettes pour vous — **rien : aucun compagnon a portee ne le connait**

Le joueur etait colle a sa bete. La grammaire etait vide, donc **aucun moteur
n'etait allume**, donc rien n'a jamais pu etre entendu.

La cause : `Voix.preparer` rendait une liste vide quand aucun nom ne survivait
au dictionnaire, avec ce raisonnement — « un ordre sans nom ne declenchera
jamais rien ». Un compagnon baptise **Dargon** (un mot qui n'existe pas en
francais) rendait donc la voix entierement muette pour son proprietaire. Pas
seulement son nom : `assis`, `viens ici`, `chope`, tout.

Ce raisonnement etait devenu faux le jour ou le nom est devenu facultatif. Les
ordres partent maintenant **toujours**, des qu'une bete est a portee.

## Trois facons d'etre appele, en plus de son nom

- **Les variantes** — `Lexique.variantes` cherche dans les 135 774 mots du
  modele ceux qui ont la meme racine (`dragon` → `dragons`, `dragonne`) ou deux
  lettres voisines inversees (`dargon` → **`dragon`**). Uniquement de vrais
  mots : un mot absent du dictionnaire ne peut de toute facon jamais sortir du
  moteur.

  **Une distance d'edition avait ete essayee d'abord, et elle etait dangereuse** :
  « Braise » ramenait *baiser*, « Mouette » ramenait *maquette* et *mazette*.
  `VariantesTest` empeche ce retour.

- **L'espece** — `"nom_a_la_voix"` dans la fiche d'espece. « dragonnet »
  n'existe pas en francais, « dragon » si. Absente, le nom de l'espece sert.

- **`/compagnon renommer <nom>`** — il manquait, et son absence rendait une
  faute de frappe au bapteme definitive.

## Le vol : un debordement d'entier

`volDemande()` comparait `tickCount - volDemandeA <= 1` en partant de
`Integer.MIN_VALUE`. **La soustraction debordait** : la condition etait vraie
des la premiere seconde. Toute bete ailee perdait sa gravite et battait des
ailes en permanence, posee dans l'herbe.

Remplace par un compte a rebours, qui ne peut pas deborder.

Le vol sait maintenant **regarder ou il va** (le corps pivote, pas seulement la
tete) et **passer par-dessus** : un mur devant lui le fait grimper au lieu
d'insister. Inutile de comprendre la forme d'une tour pour la franchir.

## Il ne recule plus a reculons

`SuivreProprietaireGoal` posait le regard sur le maitre a **chaque tick**, y
compris en marchant. Le corps suit la tete : la bete gardait le nez sur son
maitre et se deplacait de cote ou a reculons pour se recaler.

Le regard n'est plus pose qu'une fois arrete. Il marche en regardant devant, et
il te regarde quand il s'arrete.

## Trois familiers a la fois

`reglages.json` — `sortis_en_meme_temps` passe de 1 a **3**. `0` enleve la
limite.

---

## Les deux niveaux, verifies

`SecuriteTest` lit le code source et tient six regles. C'est inhabituel, et
c'est assume : ces regles vivent dans la **forme** du code — un `requires` pose
au bon endroit, un paquet enregistre dans le bon sens, un import qui ne doit pas
exister. Les verifier autrement demanderait de lancer un serveur, un client et
un joueur malveillant.

**Niveau 1, les eleves** — aucune commande. Une seule racine `/compagnon`, et
sa garde `hasPermission(2)` est posee **avant** la premiere branche, donc
Brigadier ferme tout l'arbre : ils ne peuvent pas l'executer et ne la voient
meme pas dans l'autocompletion. Aucune autre commande n'est enregistree ailleurs
dans le mod.

**Niveau 2, l'equipe** — tout.

### Les paquets, qui sont la vraie porte d'entree

Un joueur n'a pas besoin de commande pour parler au serveur : son client envoie
des paquets. Six montent, six descendent, aucun dans les deux sens, et aucun
paquet de donnees ne remonte.

**Aucun paquet montant ne transporte l'identifiant d'un compagnon.** Le client
envoie un *numero dans sa propre liste*, que le serveur resout dans les fiches
de ce joueur-la et ramene dans les bornes. Un client fabrique ne peut donc pas
designer la bete de quelqu'un d'autre : le pire qu'il puisse faire est de
choisir une des siennes.

`naissance` exige un oeuf **en main** portant son espece, et le consomme : on ne
s'invente pas un compagnon. Le nom est borne cote serveur, au bapteme comme au
renommage — un nom sans fin finirait dans la sauvegarde de tout le monde.

### Les deux cotes

- `environment: "*"`, point d'entree commun non client, mixins marques `client`.
- **Zero import client dans `src/main`** — Gradle l'empeche deja par les sources
  separees, le test le redit et explique pourquoi : un serveur dedie tomberait
  au chargement.
- Les donnees sont lues sur `PackType.SERVER_DATA` : especes, niveaux, reglages,
  contenu et vocabulaire arrivent bien sur un serveur sans client.

---

## Lui apprendre un mot

Dans la roue : **« Lui apprendre un mot »**, on choisit le geste, on ecrit le
mot. Ensuite il se dit a voix haute. Le mot appris s'affiche sous son geste dans
la roue — c'est le seul endroit ou l'on voit d'un coup d'oeil ce que sa bete
sait deja.

**On ne peut pas apprendre un mot en le disant**, et ce n'est pas un choix : le
moteur travaille en liste fermee, il ne peut entendre que des mots qu'il guette
deja. Un mot qu'on veut lui apprendre n'y est, par definition, pas encore.

Le serveur verifie tout — la bete est a lui, le geste est ouvert a son niveau,
le mot n'est ni un ordre du mod ni son nom, et surtout **le micro sait le dire**.
Sinon le mot serait retire de la liste du moteur en silence, et le joueur
crierait pendant des semaines un mot qui ne peut pas etre entendu. Un refus dit
pourquoi et propose des mots qui marcheraient.

Huit mots par bete au maximum, et c'est deja beaucoup : chaque mot appris entre
dans la liste que le moteur guette, et plus elle est longue, moins il entend
bien chacun d'eux.

## Il ne te comprend pas ? Il te propose autre chose

Trois echecs de suite, et le mod suggere plus court — et rappelle que le nom
n'est pas obligatoire, ce qui debloque le plus de monde.

Un entier par joueur qui parle, en memoire, efface a la deconnexion. Rien sur le
disque, aucune phrase gardee, aucun travail par tick. A mille joueurs, quatre
kilo-octets.

## Il se souvient d'ici

`FicheCompagnon.Lieu` — quatre endroits par bete : la ou on l'a soigne, la ou il
mange. Il y repasse, il s'arrete, il regarde, il repart. Rien n'est ecrit :
c'est au joueur de se souvenir de ce qui s'y est passe.

`SouvenirDuLieuGoal` est l'avant-dernier but de la liste et n'interrompt jamais
rien. Quatre comparaisons de distance toutes les trois secondes, et seulement
quand il ne fait rien d'autre. Quatre-vingt-dix secondes avant de refaire le
meme geste au meme endroit — sinon une bete qui vit dans sa chambre s'arreterait
en boucle, et le souvenir deviendrait un tic.

## Le test qui manquait

`FichiersDEspeceTest` verifie que chaque animation citee par une fiche d'espece
existe vraiment dans son fichier.

Il vient d'attraper une faute reelle : la mouette citait
`animation.mouette.head_tilt` alors qu'elle s'appelle `amb_head_tilt`. Rien
n'aurait plante — le geste n'aurait simplement jamais joue, et on aurait cherche
le defaut dans le code. **Verifie en le cassant expres :** le test echoue bien,
avec le nom du role fautif.

---

## Un onglet par compagnon, en haut de la roue

Tab marchait deja, mais il fallait le savoir : rien a l'ecran ne disait qu'on
pouvait changer de bete, ni combien on en avait. Il y a maintenant une rangee
d'onglets au-dessus de la roue, un par compagnon, l'actif en vert.

Cliquer sur un onglet **redemande la roue au serveur** au lieu de changer
l'affichage : lui seul sait le niveau de cette bete-la, ce qu'elle a debloque,
et les mots qu'on lui a appris.

Rien du tout quand on n'en a qu'un : un onglet unique n'apprend rien a personne.

**Les deux rangees sont bornees a l'ecran.** La roue fait 232 pixels de haut ;
a l'echelle d'interface 4 sur un ecran ordinaire, la hauteur utile tombe a 270,
et les onglets se seraient dessines au-dessus du bord — invisibles et
incliquables. Le bouton « apprendre » avait le meme defaut vers le bas.

## Pour plus tard

Une boutique ou les joueurs achetent leur compagnon avec une monnaie du jeu.
Note ici pour ne pas l'oublier ; rien n'est commence.

---

## Il t'attend

Nourri trois fois sur quatre a peu pres a la meme heure, il finit par le savoir.
Ce jour-la, quand l'heure approche, il va s'asseoir la ou on le nourrit
d'habitude — le lieu `repas`, deja retenu par l'idee precedente — et il attend.

Le joueur arrive. Il est deja la. Personne ne lui a rien demande.

**Une habitude, ou rien.** Il faut trois repas sur quatre a deux heures pres.
Une moyenne aurait toujours rendu un chiffre, meme pour quelqu'un qui nourrit sa
bete n'importe quand : elle serait allee attendre a une heure qui ne veut rien
dire, et l'illusion serait tombee du premier coup. Mieux vaut qu'il n'attende
jamais que de l'apercevoir attendre au hasard.

**Il n'attend pas quand tu es la** : le but ne se declenche que loin de son
maitre, ou en son absence — c'est justement pendant qu'on n'est pas la qu'il
faut que quelque chose se passe. Et il n'interrompt jamais un ordre : un
compagnon qu'on a fait asseoir reste assis, meme a l'heure du diner.

**L'heure tourne en rond**, et c'est tout le piege : 23 h et 1 h sont voisines
de deux heures, pas de vingt-deux. Une soustraction ordinaire dit le contraire,
et un compagnon nourri chaque soir n'aurait jamais eu d'habitude — ses repas
auraient paru disperses sur tout le cadran. `HeuresTest` tient cette regle ;
c'est une faute qu'on ne voit pas en jouant l'apres-midi.

Cout : quatre entiers par fiche, et un coup d'oeil a l'heure toutes les dix
secondes quand il ne fait rien d'autre.

---

## Le petit panneau

En haut a gauche, quand un compagnon est dehors : son nom, la bouille de son
humeur, et deux barres — faim et energie. Elles passent au rouge en dessous d'un
quart. C'est le seul moment ou le panneau attire l'oeil ; le reste du temps il
doit se faire oublier.

Touche **H** pour le replier. Un panneau qu'on ne peut pas faire taire n'est pas
discret, c'est un panneau de plus.

Rien du tout quand aucune bete n'est dehors — pas un cadre vide, pas un titre.
Rien non plus par-dessus un ecran ouvert, ni sur une capture sans interface.

### Ce qu'il coute sur le reseau

**Trois octets par compagnon**, et le paquet ne part que si les valeurs
<b>arrondies</b> ont change. Les barres perdent moins d'un point par minute : en
pratique, quelques paquets par minute et par joueur, pas vingt par seconde.

L'envoi se greffe sur le parcours qu'`Apparition` fait deja chaque seconde — les
fiches y sont deja en main, les reparcourir ailleurs aurait ete payer deux fois
le meme travail.

Une liste vide part une fois quand le dernier compagnon rentre : c'est elle qui
dit au client de ranger le panneau.

## Le carnet dit ou il est

Il disait « il est range » ou « il est avec toi », et rien entre les deux. Or
entre les deux se trouve le cas le plus frequent : il est dehors, quelque part.

Maintenant : **« A 340 blocs, au nord »**, « Dans un autre monde », ou —
lorsqu'il est a son coin des repas a l'heure ou on le nourrit — **« Il t'attend,
a 120 blocs »**. C'est le seul cas ou l'on dit ce qu'il FAIT et pas seulement ou
il est, et c'est justement celui qu'on a envie de lire.

Distance et direction en toutes lettres plutot qu'en coordonnees : ca se comprend
sans carte, et ca ne transforme pas le carnet en tableau de bord.

---

## Le vol en dents de scie

Il montait, retombait d'un cran, remontait. Trois causes, toutes la meme faute
de fond : **le vol dependait d'une demande a chaque tick**, et il suffisait d'un
seul tick sans demande pour que la gravite revienne.

1. **La gravite etait posee trop tard.** `tenirLeVol()` tournait apres
   `super.tick()`, donc apres que le jeu ait deja applique le mouvement. Le
   premier tick de chaque envol subissait la gravite quand meme. Il tourne
   maintenant en premier.

2. **Deux ticks de battement, c'etait trop court.** Un but qui reflechit, une
   condition qui vacille une image, un chemin momentanement trouve : un tick
   saute, et la bete perdait un cran. A huit ticks — quatre dixiemes de seconde,
   comme un oiseau qui plane entre deux battements — un trou passe inapercu.

3. **`VenirIciGoal` remettait son compteur d'echecs a zero.** Depuis les airs,
   le chemin vers le sol se trouve souvent : le compteur retombait a zero, le
   vol s'arretait, la bete tombait, le chemin echouait de nouveau, elle
   redecollait. Un va-et-vient qui pouvait durer indefiniment. Le compteur ne
   se remet plus a zero tant qu'elle est en l'air.

Deux corrections de plus, du meme esprit : la condition sur la navigation dans
le suivi a saute (elle sautait le premier tick de chaque envol), et **on ne
teleporte plus une bete en vol** — on la voit arriver, la faire reapparaitre au
dernier moment gachait tout le trajet.

## Des onglets dans le livre aussi

Le bas de page annoncait « compagnon 1 sur 2 », mais rien ne disait comment
passer au second. Une information qu'il faut deviner n'est pas une information.

Memes mesures et memes couleurs que dans la roue : ce sont les memes betes, et
passer d'un ecran a l'autre ne doit rien deplacer.

---

## Deux defauts trouves dans le journal

**Le mixin de la caresse ne s'appliquait pas du tout.**

```
@Shadow field field_3394 was not located in the target class net.minecraft.class_591
```

`hat` n'appartient pas a `PlayerModel` mais a `HumanoidModel`, un cran plus
haut. On l'ombrageait comme s'il etait dans `PlayerModel`, et Mixin ne le
trouvait pas — or **un `@Shadow` introuvable fait echouer tout le mixin**.
L'animation du bras pendant une caresse etait donc morte, silencieusement,
depuis le debut. Il n'y avait rien a ombrager : la classe etend deja
`HumanoidModel`.

**L'avertissement `com_alphacephei_vosk` en double** est cosmetique : le jar
n'en contient qu'un, verifie. Plasmo Voice a exactement le meme.

## Une bete qui ecoute, l'autre qui semble sourde

Avec deux compagnons dehors, un seul repondait. Ce n'etait pas le micro : c'etait
**l'ordre des questions**.

```
« Mouette, viens »  -> elle vient, et on retient son nom 30 secondes.
« Prince, viens »   -> le moteur rend « princes » au lieu de « prince ».
                       Le nom exact ne collait pas, on passait au SOUVENIR,
                       et c'est la mouette qui repondait.
```

Ce qui vient d'etre **dit** l'emporte desormais toujours sur ce dont on se
**souvient** : nom exact, puis variante du nom, puis espece, puis seulement le
souvenir, puis « le seul qui ecoute ».

## Les quatre idees

**L'age en toutes lettres.** « 47 jours » est un chiffre ; « vous vous
connaissez bien maintenant » est une phrase, et c'est elle qu'on relit. Six
paliers, aux endroits ou l'on reconnait la semaine, le mois, la saison et
l'annee. La bete ne grandit pas — le temps se voit ailleurs.

**Il regarde ce que tu tiens.** Nourriture ou remede en main a moins de huit
blocs : il tourne la tete. Sur la couche du **regard** uniquement — il ne
s'approche pas, ne reclame pas, ne bloque rien. C'est la difference entre une
bete attentive et une bete collante.

**Deux familiers se saluent.** Cinq rencontres suffisent a devenir familiers
(`Voisinages` le comptait deja sans s'en servir). Quand ils se croisent, ils
s'arretent, se regardent, jouent `@salut`, repartent. Deux minutes avant de
resaluer le meme : sans ce delai, deux betes d'une meme chambre passeraient leur
vie a se dire bonjour.

**Le livre dit quoi faire.** Sous quarante, chaque barre porte sa ligne :
« Donne-lui a manger », « Caresse-le, promene-le, parle-lui ». Le mod savait
deja ce qui fait monter quoi ; il ne le disait a personne. Rien au-dessus du
seuil — une page couverte de conseils ne serait plus une fiche de sante, ce
serait un mode d'emploi.

---

## Le panneau, refait

La premiere version etait un rectangle sombre a coins droits, avec deux traits
pleins dedans. Sur un ciel bleu ca virait au gris-bleu, et ca avait l'air de ce
que c'etait : un affichage de mise au point colle par-dessus le jeu.

Maintenant c'est un **bout de parchemin**. Memes couleurs que le livre, meme
encre, **memes jauges creusees dans le papier**, memes teintes pour la faim et
l'energie — orange et jaune, exactement comme dans le livre. Le joueur n'a donc
rien de neuf a apprendre : il reconnait ses barres parce qu'il les a deja vues
ailleurs.

Trois details qui font le reste : une ombre portee d'un pixel, qui pose le
parchemin sur le monde au lieu de l'y coller ; les quatre pixels de coin
retires, ce qui suffit a arrondir l'oeil a cette taille ; un filet clair sous le
cadre, qui donne au papier l'air d'etre legerement bombe.

**Et il s'efface quand tout va bien** — deux tiers d'opacite au repos, franc des
qu'une barre passe sous le quart, avec la barre en rouge. La promesse d'un
affichage discret, tenue par le dessin plutot que par une phrase dans la
documentation.

## Les textes qui se chevauchaient

Chaque ligne a deux colonnes du livre etait ecrite deux fois : le libelle depuis
la marge gauche, la valeur calee sur la marge droite. Tant que le libelle etait
court, tout allait bien — « Il a passe un moment avec un autre compagnon » ne
l'est pas, et la date se dessinait **par-dessus la phrase**.

`deuxColonnes` coupe le libelle et pose des points de suspension, en gardant
toujours quatre pixels entre les deux. La valeur est prioritaire : une date
tronquee ne veut plus rien dire.

**Les quatre lignes concernees y passent** — le niveau, les barres, les
compteurs, les moments — et il ne reste plus un seul calage a droite ailleurs
dans le fichier. C'est ce qui fait que le defaut ne peut pas revenir a un
endroit qu'on aurait oublie.

---

## Quatre ordres de plus

`voix.json` compte maintenant **94 tournures pour 84 mots**, tous verifies dans
le modele.

**« Monte »** — il decolle et tourne au-dessus de toi. C'est le seul ordre qui
ne mene nulle part, et c'est pour ca qu'il vaut la peine : un dragon qui tourne
dans le ciel pendant qu'on fait autre chose, c'est ce qu'on a envie de regarder.
`PlanerGoal` decrit un cercle autour d'un centre **fige au depart** — le suivre
en continu ferait un vol nerveux qui se recale sur les pas du joueur.

Il redescend **toujours** au bout de quinze secondes : une bete qu'on aurait
laissee en l'air et oubliee serait une bete perdue.

**« Descends »** — il se pose. **« Tourne »** — il fait son tour, ca ne sert a
rien, c'est le but.

**« Bravo »** — le premier moyen de lui dire qu'on est content de lui <b>sans le
toucher</b>. Jusqu'ici il fallait s'approcher et caresser ; on peut maintenant
le feliciter de loin, en pleine action, au moment ou ca compte. Une minute entre
deux bravos qui comptent : on felicite un geste, pas une syllabe. Repete, il est
content quand meme — mais la complicite ne bouge plus.

Aucun de ces quatre ne touche a un objet : n'importe qui de connu peut donc dire
bravo a une bete, et c'est tres bien ainsi.

## Il montre qu'il a entendu

Entre le moment ou l'on parle et celui ou la bete agit, il se passe une bonne
seconde — silence constate, phrase conclue, ordre reconnu. Pendant cette
seconde, rien ne se passait, et on ne savait pas si on avait ete entendu.

Le role `@ecoute` comble ce trou : il dresse la tete, il attend. Court exprès —
l'animation de l'ordre le remplace aussitot.

## L'humeur se voit sur la bete

Elle vivait dans la fiche, donc cote serveur, donc **uniquement dans le livre**.
On pouvait avoir un compagnon au plus mal sans s'en douter en le regardant.

Un octet synchronise, qui ne change qu'a la minute, et deux roles d'attente :
`joyeux` et `triste`. Une bete qui ne fait rien est une bete qu'on regarde —
c'est le meilleur moment pour qu'elle raconte comment elle va, sans un mot et
sans ouvrir de page.

Absents de la fiche d'espece, ces roles retombent sur l'attente ordinaire : une
espece qui n'a qu'une pose se comporte exactement comme avant.

---

## Crier

`Ecoute.avaler` mesure desormais la force de chaque fragment — la moyenne
quadratique de ses echantillons, c'est-a-dire son energie. On garde **le plus
fort** de la phrase et non la moyenne : une phrase commence et finit toujours
doucement, et une moyenne dirait de tout le monde qu'il parle bas.

**Le cri se mesure par rapport a soi, jamais dans l'absolu.** C'est le point qui
decide de tout : un seuil fixe recompenserait ceux qui ont un bon micro, un
preampli genereux, une voix qui porte — et priverait les autres d'une fonction
du jeu pour une raison qui n'a rien a voir avec le jeu. `Voix.aCrie` compare
chaque phrase a la moyenne de **ce joueur-la**, moyenne qui suit doucement et
qu'un cri ne tire pas vers le haut.

`TonTest` tient cette regle avec deux micros dont les valeurs different d'un
facteur dix : les deux joueurs peuvent crier, et le cri de l'un reste une voix
ordinaire pour l'autre.

Crier fait deux choses, et deux seulement : **la voix porte a 26 blocs au lieu
de 16**, et **la bete ne peut pas faire la sourde oreille**. Rien de plus. Crier
ne doit jamais devenir une facon de jouer — c'est une facon de se faire entendre
de loin, et d'insister quand la bete boude.

## Ce qu'il sait faire

Le livre liste les mots appris, sous leur geste. La roue les montrait un par un ;
il manquait la vue d'ensemble — et c'est aussi ce qui donne envie de lui en
apprendre d'autres. Rien du tout quand il n'en connait aucun.

`etiquette()` est remontee dans `EcranCompagnon` : la roue et le livre nomment
desormais les memes gestes, et deux facons de les nommer donneraient deux noms
differents pour la meme chose.

## Les competences

**Le niveau ne debloquait que des gestes.** Joli, mais ca ne change rien a ce
que la bete <i>est</i> : deux dragons de niveau 50 etaient rigoureusement
identiques, et le niveau 30 n'apportait rien s'il n'y avait pas d'animation a ce
palier.

`data/compagnon/competences/` — un fichier par competence, comme les aliments.
Neuf pour commencer. **Un point tous les cinq niveaux**, soit dix points au
maximum pour neuf competences : presque toutes, mais pas tout de suite, et pas
dans le meme ordre que le voisin. C'est la rarete des points qui fait le choix,
et le choix qui fait la difference entre deux compagnons.

Cinq effets sont reellement branches : la faim et la fatigue ralentissent, les
bobos se rarefient, l'experience monte plus vite, l'oreille porte plus loin.
Les facteurs se **multiplient** — deux fois moins 20 % font moins 36 %, pas
moins 40 % — ce qui evite qu'une pile de bonus finisse par ramener une barre a
zero.

Troisieme page du livre, sur les deux pages a la fois : on y prend une decision
qu'on ne pourra pas reprendre, et les serrer dans une colonne les rendrait
illisibles. Trois etats, trois couleurs, aucun texte pour les expliquer.

**Un choix ne se reprend pas.** C'est ce qui lui donne son poids : une
competence qu'on peut annuler n'est pas un choix, c'est un reglage.

`CompetencesTest` verifie que chaque effet existe, qu'aucune competence
n'<b>empire</b> la vie (0,7 et non 1,3), qu'aucune n'est hors d'atteinte, et
qu'aucun nom n'est en double. **Verifie en cassant un fichier exprès** : les
deux fautes sont nommees, fichier par fichier.

---

## Le vol saccade

Trois causes, et la premiere n'avait rien a voir avec le vol.

**L'animation racontait autre chose.** Les poses passaient avant le vol dans
`locomotion()`. Une bete a qui on avait dit « assis », puis « monte », volait
donc <b>en position assise</b> — pattes repliees, ailes fermees, glissant dans
le ciel. On croyait a un probleme de vol alors que le vol marchait. Une creature
en l'air n'est assise sous aucun pretexte : le vol passe devant.

**La cible etait rattrapable.** `PlanerGoal` faisait tourner un point a vitesse
fixe et demandait a la bete de le suivre. Elle volait plus vite : elle le
rattrapait, s'arretait, attendait qu'il reprenne de l'avance, repartait. Vingt
fois par seconde — d'ou l'impression d'une image par seconde alors que le jeu
tournait normalement. La cible se calcule maintenant depuis **l'angle ou la bete
se trouve vraiment**, un quart de tour devant elle : c'est un cap, pas un
rendez-vous.

**La vitesse etait imposee, pas atteinte.** `setDeltaMovement` ecrivait la
vitesse voulue a chaque tick. En ligne droite ca ne se voit pas ; en cercle, la
direction change a chaque tick — et une vitesse qui change vingt fois par
seconde n'est pas un vol, c'est un tremblement. On glisse desormais d'un
cinquieme vers la vitesse voulue, ce qui lisse tout et donne au passage
l'inertie d'une bete qui pese quelque chose.

## Chuchoter

L'inverse du cri, et gratuit une fois qu'on mesure le volume. Sous 55 % de sa
voix ordinaire, la portee tombe a six blocs : on peut donner un ordre discret au
milieu d'une salle pleine sans que trois autres compagnons repondent.

Meme regle que pour le cri, et pour la meme raison : **tout se mesure par
rapport a sa propre voix.** Un chuchotement n'est pas un volume, c'est un ecart.

## Des competences que l'autre ne verra jamais

Le champ `especes` existait depuis le debut et ne servait a rien. Quatre
competences l'utilisent maintenant : **Vol soutenu** et **Oeil de mouette** pour
la mouette, **Cuir epais** et **Estomac de dragon** pour le dragonnet. Treize au
total, et **zero ligne de code** — juste des fichiers.

## Deux betes qui se ressemblent se saluent plus longuement

Meme espece, et des choix de vie en commun : elles ont plus a se dire. Une
nuance de trois quarts de seconde que personne ne remarquera consciemment, et
qui fait qu'un couloir plein de compagnons n'a pas l'air de repeter le meme
geste en boucle.

## Le livre garde une trace

« Le jour ou il est devenu quelqu'un » — un moment, marque a la premiere
competence prise. Une seule fois : c'est le premier choix qui marque, pas le
neuvieme.

## ANIMATIONS.md

Cent animations decrites une par une — cinquante communes, cinquante pour le
dragon. Chaque entree dit ce qui bouge, dans quel ordre, et ce qu'on doit
ressentir. A garder ouvert a cote de Blockbench.

---

## Le petit oiseau bleu

Converti depuis `courierbirdmount.geo_4.bbmodel` par `outils/bb2geo.js` : **32
os, 54 cubes, 52 animations, 7 textures**. Aucune retouche a la main.

La conversion se verifie elle-meme — les ailes doivent ressortir symetriques
autour de zero, la droite en +X, et la tete en -Z. Les trois sont bonnes, donc
la bascule de repere l'est aussi.

Les animations sont **renommees** au passage : `animation.mount.*` devient
`animation.oiseau_bleu.*`. Sans ca le mod les aurait crues d'une espece appelee
« mount », et les aurait affichees dans la roue de tout le monde.

Son os `jaw` est trouve tout seul par `Bouches` : ce qu'il porte ira dans son
bec.

## On monte dessus

**Clic droit les mains vides, au niveau 50.** Espace pour monter, accroupi pour
descendre, et il avance ou tu regardes.

Monter sa bete doit etre l'aboutissement de tout le reste, pas une chose qu'on
fait le premier jour. C'est la seule recompense du mod qui change la facon de se
deplacer sur le serveur.

**Les autres especes ne se montent pas** : elles ne declarent pas
`monter_au_niveau`, et l'absence vaut zero. On n'ajoute pas une facon de se
deplacer a tout un serveur par omission.

### Ce qui se regle sans recompiler

Dans `especes/oiseau_bleu.json` :

| | |
|---|---|
| `selle` | `[droite, haut, avant]` en blocs — ou le cavalier s'assoit |
| `monter_au_niveau` | `0` interdit la monte |

### Le saut, et pourquoi il a fallu un paquet

Le serveur connait deja l'avant, l'arriere et les cotes du cavalier :
`xxa` et `zza` sont publics et synchronises. **Le saut, lui, ne l'est pas** —
`jumping` est protege, et aucune monture du jeu ne le lit depuis l'exterieur.

`PaquetChevaucher` n'envoie donc que ce qui manque : l'intention verticale, et
**seulement quand elle change**. Trois fois par vol, pas vingt fois par seconde.
Il n'y a rien a y voler : monter et descendre sont deja ce qu'un cavalier a le
droit de faire.

### Monte, il n'a plus d'avis

Les buts continuaient de demander des chemins, et le controle de deplacement
faisait pivoter la bete pendant que le cavalier la faisait pivoter aussi — les
deux se disputant le cap a chaque tick. On coupe la navigation : sans chemin,
les buts ne bougent rien. Et `estLibre()` est faux quand il est monte, donc les
buts de flanerie ne demarrent meme pas.

Il refuse aussi de porter quelqu'un sous 20 d'energie.

## Monter : trois corrections

**Il repoussait celui qui voulait le monter.** Ses morceaux de collision
ecartaient son proprietaire avant qu'il ait pu le toucher : il fallait trouver
le seul angle ou le clic passait encore. Sur une bete de trois blocs de haut,
ca revenait a chercher un point precis.

Une monture n'ecarte plus son maitre. Elle continue d'ecarter tout le monde —
on ne traverse pas un oiseau — mais pas la personne qui doit pouvoir s'en
approcher pour s'asseoir dessus.

**L'accroupi n'est pas a nous.** Je l'avais pris pour descendre en altitude ;
c'est la touche que Minecraft reserve pour mettre pied a terre, et elle le fait
avant que notre paquet ne parte. On descendait de l'oiseau au lieu de descendre
avec lui.

Il ne reste donc **qu'une touche a connaitre** : la barre d'espace monte, la
lacher fait planer jusqu'au sol. C'est aussi une touche de moins a expliquer.

**Il ne battait des ailes que pendant qu'on appuyait.** Des qu'on lachait, il
redevenait une bete qui tombe — ailes repliees, pattes tendues — au lieu de
planer. C'est ce qui donnait un oiseau sans animation de vol alors que
l'animation etait la.

Il plane maintenant tant qu'il est en l'air, et perd doucement de l'altitude :
un oiseau qui plane, pas un ballon qui flotte.

## Le livre vivant

Les interfaces suivent maintenant une meme idee : le compagnon n'est plus une
fiche de statistiques, c'est quelqu'un dont on a envie de s'occuper.

Le livre s'ouvre sur **Aujourd'hui**. Il montre en grand le vrai modele 3D du
compagnon, son humeur, son mode, ses quatre besoins, la priorite du moment, deux
petites choses a faire ensemble et la prochaine recompense de progression. Les
onglets sur la tranche menent directement a Aujourd'hui, l'histoire, les
talents et les missions. Les pages glissent et se revelent comme de l'encre.

La toute premiere ouverture est une rencontre speciale : le compagnon apparait
en grand avec « Votre histoire commence ici ». Cette decouverte n'est jouee
qu'une fois sur le client et laisse une marque dans le dossier de configuration.

Le papier prend une nuance chaude quand un besoin devient urgent et une nuance
verte quand la complicite est forte. Les souvenirs forment une petite frise
chronologique, afin que le livre ressemble davantage a une histoire partagee.

Le carnet est devenu une collection de cartes avec etat, espece et niveau. Le
portrait s'adapte a la taille reelle de chaque espece et glisse a l'arrivee.
La roue garde son apercu du geste en jeu, mais son centre explique maintenant le
geste survole, le mot vocal appris ou le niveau encore necessaire.

Les sons, les boutons animes, les parchemins et les apercus 3D deja presents ont
ete conserves : ils font partie de cette nouvelle identite plutot que d'etre
remplaces.
