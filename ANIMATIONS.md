# Les animations du mod

**Cent onze animations**, rangees en trois familles. Chacune dit ce qui bouge,
dans quel ordre, et ce qu'on doit ressentir. Le dernier point est le plus
important : une animation techniquement correcte qui ne raconte rien est une
animation ratee.

> La meme chose en page web, avec une recherche : `ANIMATIONS.html`.

| Famille | Combien | Pour qui |
|---|---|---|
| Le socle | 50 | toute bete, ailes ou pas |
| Les ailes | 23 | celles qui volent |
| Le dragon | 38 | lui, et personne d'autre |

## Les trois regles qui valent pour les cent onze

1. **Rien ne part de zero et rien n'y revient sec.** Une patte qui passe de 0° a
   20° en une image se voit. Deux images d'amorce, deux de retombee, meme sur un
   geste bref.
2. **Le corps precede, les extremites suivent.** Le cou bouge d'abord, la tete
   deux images plus tard, le bec deux images encore apres. Ce decalage — et lui
   seul — fait la difference entre un modele articule et un etre vivant.
3. **Rien n'est parfaitement symetrique.** Une aile un peu plus haute que
   l'autre, une patte qui traine d'une image. La symetrie parfaite est le signe
   distinctif de la machine.

## Les roles que le mod appelle tout seul

Ces noms-la sont attendus par le code, via la fiche d'espece. Tout le reste va
dans la roue et se debloque par niveau.

| Role | Quand il joue |
|---|---|
| `immobile` `marche` `course` `vol` | en permanence, selon ce qu'il fait |
| `assis` `couche` | quand on le lui a dit |
| `joyeux` `triste` | a l'arret, selon son humeur |
| `caresse` | quand on le caresse |
| `ecoute` | des qu'il entend son nom |
| `ramasse` | quand il prend un objet |
| `salut` | quand il croise une bete qu'il connait |
| `reconnait` | quand il repasse a un endroit qui compte |
| `tourne` `joie` | sur « tourne » et « bravo » |

---

# Le socle

*Pour toute bete, ailes ou pas.*

Rien ici ne suppose des ailes, des pattes avant ou une queue precise. C'est le fond commun : ce qu'une creature fait quand elle respire, se deplace, mange et vous regarde.

## Repos et attente

**`idle`** — 6 s, loop
La respiration, et rien d'autre. Le torse se gonfle de 3 % sur deux secondes, retombe sur deux et demie — l'expiration est toujours plus lente que l'inspiration. La tete derive de 2° et revient. **A faire en dernier**, quand tout le reste existe : c'est l'animation qu'on voit le plus, et celle qu'on juge le mieux une fois qu'on connait la bete.

**`idle_alerte`** — 5 s, loop
Meme respiration, plus courte et plus haute dans la poitrine. La tete est levee de 8°, immobile quatre secondes puis pivote sechement de 25°. Le poids est sur les pattes avant. *Ce n'est pas la peur : c'est l'attention.*

**`idle_repos`** — 8 s, loop
Le poids retombe sur les hanches, l'echine s'arrondit de 5°. Une patte arriere decalee. La respiration descend a quatre secondes par cycle. Un tressaillement d'oreille toutes les six secondes, pas plus.

**`idle_vent`** — 6,5 s, loop
Il fait face a quelque chose qui pousse. Corrections permanentes : le corps oscille de 3° lateralement, jamais au meme rythme, la queue bat contre le vent. **Aucune image ne doit etre identique a une autre.**

**`idle_froid`** — 7 s, loop
Ramasse sur lui-meme : pattes rapprochees, echine voutee, queue enroulee le long du flanc. Un frisson toutes les trois secondes — une secousse de 2° sur tout le corps, en trois images, sans amorce.

**`idle_chaud`** — 6 s, loop
L'inverse : membres ecartes, ventre au sol, bouche entrouverte. Haletement rapide — la machoire bouge de 4° huit fois par seconde. La queue ne bouge presque plus.

**`assis`** — 6 s, loop
L'arriere-train au sol, pattes avant tendues, dos droit. Respiration visible dans les epaules plutot que dans le ventre. Un balancement de queue lent toutes les trois secondes.

**`assis_impatient`** — 4 s, loop
Assis, mais la queue bat vite et le poids passe d'une hanche a l'autre toutes les deux secondes. La tete tourne a droite, a gauche. *Il attend quelque chose de precis.*

**`couche`** — 8 s, loop
Le flanc au sol, la tete posee sur les pattes avant. Respiration tres lente, six secondes par cycle, cage thoracique qui se souleve nettement. Une oreille bouge parfois. Rien d'autre.

**`sommeil`** — 10 s, loop
Comme couche, mais la tete completement a plat, roulee d'un cote. Respiration a sept secondes. **Ajoute un micro-mouvement de patte toutes les huit secondes** — une bete parfaitement immobile a l'air morte, pas endormie.

## Deplacement au sol

**`marche`** — 0,8 s, loop
Quatre appuis decales d'un quart de cycle : arriere-gauche, avant-gauche, arriere-droite, avant-droite. Le corps monte et descend de 2 % — le point haut tombe quand deux pattes opposees sont au sol. La tete compense pour rester stable : c'est ce qui fait qu'un animal ne rebondit pas.

**`marche_lente`** — 1,2 s, loop
Meme cycle, plus etire, avec un temps de suspension a chaque pose. La tete plus basse. Pour la flanerie et la fatigue.

**`course`** — 0,55 s, loop
Deux temps de suspension par cycle. Le dos se creuse et se voute de 6°. La tete est projetee en avant et devient l'axe du mouvement. La queue s'allonge derriere, raide.

**`demi_tour`** — 0,9 s
Les pattes avant restent, l'arriere-train pivote de 180° en trois appuis. La tete mene d'une demi-seconde. **Une bete ne pivote jamais d'un bloc.**

**`arret_brusque`** — 0,6 s
Les pattes avant plantent, l'arriere glisse deux images, le corps bascule de 8° vers l'avant puis se retablit. Enchaine depuis course.

**`saut`** — 1 s
Accroupissement de trois images, detente, suspension, reception amortie sur les pattes avant. **Le point le plus haut tombe au tiers de l'animation, pas au milieu** — une parabole n'est pas symetrique quand on la vit.

**`saut_obstacle`** — 1,3 s
Comme le saut, mais les pattes se replient franchement sous le ventre au sommet, et le corps se plie de 15°.

**`chute`** — 1,5 s, loop
Pattes ecartees, tete en haut, queue battante. Le corps tourne lentement sur lui-meme. Doit boucler sans qu'on voie la couture.

**`reception`** — 0,8 s
Les quatre pattes touchent, le corps s'ecrase de 15 %, se releve en depassant legerement, se stabilise. Trois temps, jamais deux.

**`nage`** — 1,1 s, loop
Corps a l'horizontale, pattes en pagaie decalees deux a deux. Le corps ondule lateralement de 4°. La tete reste stable et haute — c'est elle qui doit rester hors de l'eau.

## Avec le joueur

**`ecoute`** — 0,8 s
Le geste le plus court et l'un des plus importants : il joue a chaque fois qu'on lui parle. La tete se dresse de 12° en trois images, les oreilles se braquent, tout s'immobilise. **Rien ne doit revenir en place** — le mod enchaine aussitot sur l'ordre.

**`caresse`** — 2 s
La tete pousse vers la main, s'incline de 20°, les yeux se ferment a moitie. Le corps se detend d'un cran. Sur la fin, un leger appui de tout le corps vers l'avant — *il en redemande*.

**`caresse_refus`** — 1,2 s
La tete se derobe de cote, les oreilles se couchent, un pas de recul. Pour les jours ou il n'est pas d'humeur.

**`joie`** — 2,7 s
Tout le corps. Deux petits bonds sur place, la queue bat vite, la tete part de gauche a droite. Termine en position basse sur les pattes avant, arriere-train haut — **la position de jeu**, universelle et immediatement lisible.

**`salut`** — 1,5 s
Une flexion des pattes avant, la tete qui descend puis remonte. Court, net, presque formel.

**`suit_du_regard`** — 4,6 s, loop
Le corps ne bouge pas. La tete suit lentement quelque chose de gauche a droite, avec deux arrets en chemin. **Les arrets font tout** : un regard qui balaie sans s'arreter est un regard de camera, pas d'animal.

**`quemande`** — 3 s, loop
Assis, une patte avant qui se leve et retombe. La tete penchee de 15°. Un mouvement toutes les deux secondes, jamais regulier.

**`reconnait`** — 2,3 s
Il ralentit, la tete pivote vers un point precis, s'incline de 20° d'un cote puis de l'autre. Un temps d'arret d'une seconde. Puis il repart. *C'est un souvenir, pas une decouverte* : le geste est plus lent qu'une surprise.

**`tourne`** — 1,6 s
Un tour complet sur place, pattes en pas chasse, queue a l'horizontale par la force centrifuge. Finit exactement dans l'axe de depart.

**`fait_le_beau`** — 2,5 s, hold
Se dresse sur les pattes arriere, pattes avant repliees contre la poitrine. Tient trois secondes en oscillant legerement — **l'oscillation est ce qui rend la pose credible**.

## Nourriture et soins

**`mange`** — 3,4 s
La tete descend, trois prises rapides avec un mouvement de machoire, puis se releve en machant. Le cou fait le travail, pas le corps.

**`mange_vite`** — 1 s
La meme chose, ramassee. Pour les fois ou il a tres faim.

**`avale`** — 1,9 s
La tete part en arriere, trois poussees le long du cou de bas en haut. Tres lisible sur un long cou, a raccourcir sur les autres.

**`boit`** — 3,1 s
La langue ou le bec descend, quatre lapements, puis la tete se releve et s'egoutte d'un mouvement lateral.

**`refuse_nourriture`** — 1,4 s
La tete se detourne de 40°, un pas de cote. Le regard revient une seconde plus tard — *la curiosite l'emporte toujours sur le refus*.

**`sent`** — 2,2 s
Le museau au sol, il avance de trois pas tres lents en balayant de gauche a droite. Les narines bougent si le modele le permet.

**`se_soigne`** — 4 s
Il leche ou frotte une patte, la tete revenant deux ou trois fois au meme endroit. Le reste du corps immobile et bas.

**`malade`** — 6 s, loop
Tete basse, echine voutee, respiration irreguliere — deux cycles courts, un long. Un tremblement occasionnel. **Aucun mouvement ample** : c'est l'absence de mouvement qui dit la maladie.

## Emotions

**`content`** — 5 s, loop
Une version d'idle avec la queue qui bat et la tete plus haute. Sert d'attente quand l'humeur est au plus haut.

**`triste`** — 7 s, loop
Tete basse, queue immobile et pendante, respiration lente. Le regard fixe le sol. Sert d'attente quand l'humeur est au plus bas.

**`peur`** — 1,5 s, hold
Recul brusque de deux images, corps ramasse, tete basse mais yeux en avant. Tient la pose.

**`sursaut`** — 0,8 s
Tout le corps se contracte en deux images, puis se detend en six. Doit pouvoir interrompre n'importe quoi.

**`curieux`** — 2,4 s
La tete s'incline de 25° d'un cote, marque un temps, s'incline de l'autre. **Le temps d'arret au milieu est le geste** ; sans lui c'est un dodelinement.

**`agace`** — 2 s, loop
La queue bat sechement, les oreilles se couchent, un ebrouement toutes les deux secondes.

**`fier`** — 3 s
La poitrine se gonfle, le menton rentre, la queue se leve. Tient deux secondes, puis se relache d'un cran — pas completement.

**`bailler`** — 2,6 s
La machoire s'ouvre lentement sur huit images, tient trois images au maximum, se referme en quatre. Le cou s'etire vers l'arriere, tout le corps s'allonge. **Enchaine bien sur couche ou sommeil.**

## Menus gestes

**`ebroue`** — 1,2 s
La secousse part de la tete et descend jusqu'a la queue en quatre images decalees. **Le decalage est toute l'animation.**

**`gratte`** — 2,9 s
Une patte arriere remonte derriere l'oreille, six grattements rapides, la patte redescend. Le corps penche pour compenser l'equilibre.

**`etire`** — 3,2 s
Pattes avant tendues loin devant, arriere-train haut, dos creuse, un temps de tenue, puis il se ramasse et etire les pattes arriere une par une.

**`regarde_ciel`** — 2,6 s
La tete se leve a 45°, tient, redescend. Simple, et etonnamment evocatrice quand elle survient toute seule.

---

# Les ailes

*Pour toute bete qui vole.*

Ces vingt-trois valent pour n'importe quelle creature ailee — mouette, dragon, ce qui viendra apres. Elles etaient rangees avec le dragon par erreur : rien dedans ne lui appartient. Sans « vol », une espece qui decolle se fige dans sa pose de geometrie : c'est un role obligatoire.

## Le battement de base

**`vol`** — 0,68 s, loop
Descente rapide sur deux cinquiemes du cycle, remontee lente sur trois. La membrane se deforme — l'aile n'est pas une planche. **Le corps monte au moment ou l'aile descend**, jamais l'inverse.

**`vol_lent`** — 1 s, loop
Le meme, etire, avec un temps de plane entre chaque battement. Pour la croisiere economique.

**`vol_rapide`** — 0,45 s, loop
Battements courts et profonds, corps allonge, cou tendu, pattes plaquees.

**`vol_plane`** — 4 s, loop
Ailes deployees, presque immobiles. Des corrections de 3° a intervalles irreguliers, et un tres lent roulis de 5° d'un bord a l'autre. **La plus difficile a reussir, parce qu'il ne s'y passe presque rien.**

**`vol_stationnaire`** — 0,8 s, loop
Battements rapides et courts, corps presque vertical, queue pendante en gouvernail. Couteux pour la bete : a ne pas tenir longtemps.

**`vol_fatigue`** — 1,1 s, loop
Deux battements normaux, un plus faible, un temps de plane trop long. Le corps descend a chaque cycle et se rattrape au suivant. **Rien ne doit etre regulier** — c'est l'irregularite seule qui dit l'epuisement.

## Partir et se poser

**`decollage`** — 1,5 s, hold
Accroupissement, poussee des quatre pattes, **premier battement pile au moment ou les pattes quittent le sol**, puis les pattes se replient. C'est la synchronisation qui vend le decollage.

**`decolle_de_l_eau`** — 1,8 s
Deux ou trois battements dans l'eau avec les extremites qui frappent la surface, les pattes qui courent en arriere, puis l'arrachement. **Le corps reste presque horizontal** — pas d'accroupissement possible sur l'eau, toute la poussee vient des ailes.

**`atterrissage`** — 1,85 s
Les ailes se cabrent en frein, le corps se redresse a la verticale, les pattes arriere touchent d'abord, puis les avant, puis les ailes se replient en deux temps. **Quatre etapes distinctes.**

**`se_pose_perchoir`** — 1,6 s
Approche cabree, battements de freinage, **les pattes descendent en premier et cherchent** — deux petits ajustements avant de se refermer. Les ailes restent ouvertes une demi-seconde apres le contact. Bien plus juste qu'un atterrissage au sol quand la cible est etroite.

**`freine_en_vol`** — 0,9 s
Il s'arrete en l'air sans se poser. Les ailes se cabrent d'un coup a 70°, la queue s'etale et se baisse, le corps se redresse de 40° en trois images puis retombe en six. **Trois images pour freiner, six pour se retablir** — l'inverse aurait l'air d'une marche arriere.

## Manoeuvres

**`vol_cercle`** — 3 s, loop
Roulis constant de 20° vers l'interieur, une aile plus repliee que l'autre, la queue en gouvernail. **La tete regarde le centre du cercle, pas devant** — ce seul detail dit qu'il tourne autour de toi et pas au hasard. C'est celle que le mod joue sur « monte ». *Fais-la en premier.*

**`monte_spirale`** — 2,5 s, loop
La montee le long d'une paroi. Meme roulis, plus un cabrage de 15°, et des battements **deux fois plus frequents et plus profonds** — monter coute. La tete vise le haut, jamais l'horizon. C'est ce qu'il fait quand tu es sur une tour.

**`virage_gauche`** — 1,2 s, loop
Roulis de 30°, l'aile interieure se replie a moitie, la queue s'incline. La tete regarde a l'interieur du virage, jamais devant.

**`virage_droit`** — 1,2 s, loop
Le miroir, avec un ou deux degres d'ecart — **ne le copie pas exactement**.

**`piquer`** — 1,4 s
Les ailes se plaquent contre le corps, le nez pointe vers le bas, tout s'allonge. Une seule pose, tenue, plus deux images d'entree.

**`ressource`** — 1,6 s
Sortie de pique : les ailes se deploient d'un coup, le corps se cabre de 60°, un battement puissant. **Trois images pour l'ouverture, pas plus** — c'est brutal.

**`descente_douce`** — 2,8 s, loop
Ailes ouvertes mais a demi repliees au poignet, comme un parachute qu'on referme. Aucun battement. Queue etalee au maximum, une correction toutes les huit images. **Il ne tombe pas, il se laisse descendre** — la difference tient dans la queue.

**`vol_rase`** — 0,7 s, loop
A un metre du sol ou de l'eau. Battements courts et rapides, corps horizontal, tete tendue. Les extremites descendent presque au niveau du ventre. **Une oscillation verticale de 3 %** a chaque battement donne la sensation de vitesse, bien plus que la vitesse reelle.

## Au sol et en l'air

**`ailes_deploie`** — 2 s, hold
Debout au sol, il ouvre lentement les ailes en grand et tient. La pose d'intimidation, et la plus belle du lot.

**`ailes_replie`** — 1,4 s
L'inverse, en deux temps : les extremites d'abord, puis le bras de l'aile contre le flanc.

**`secoue_ailes_vol`** — 1,1 s
En plein vol, il s'ebroue. Un battement asymetrique : une aile s'agite plus vite que l'autre sur trois images, tout le corps oscille de 5°, puis le rythme se retablit. **Il perd un peu d'altitude pendant** — c'est ce qui rend le geste credible plutot que decoratif.

**`attrape_en_vol`** — 1,3 s
Un coup de frein d'aile, le cou se detend vers l'avant et le bas, la gueule se ferme, puis il se cabre pour repartir. **Le moment de la prise tombe juste apres le point le plus bas du corps**, pas dessus — il attrape en remontant, comme tous les oiseaux.

---

# Le dragon

*Pour lui, et personne d'autre.*

Le principe qui gouverne les trente-huit : un dragon est lourd, et tout ce qu'il fait doit le rappeler. Chaque mouvement met du temps a demarrer et du temps a s'arreter. C'est ce qui le distingue d'un gros oiseau.

## Le souffle

**`plasma_charge`** — 1,8 s, hold
Le cou se replie en S, la tete recule vers les epaules, la gorge se gonfle visiblement. Les pattes avant s'ecartent et s'ancrent. **La lueur monte le long du cou** de la poitrine vers la gueule — sans partie lumineuse, fais-la avec l'echelle d'un os interne. Tient la pose sur la derniere image.

**`plasma_tir`** — 1,2 s
Detente : le cou se deploie violemment, la gueule s'ouvre en grand, tout le corps recule de 15 % sous le recul. Trois images de projection, huit de retour. **Le recul est ce qui donne la puissance** — sans lui, il crache, il ne tire pas.

**`plasma_balayage`** — 2,4 s
Comme le tir, mais la tete balaie horizontalement de 90° pendant l'emission. Le corps pivote d'un tiers de cet angle, en retard sur la tete.

**`plasma_rate`** — 1,6 s
La charge se fait, la gorge se gonfle, et rien ne sort. Deux toux, la tete qui se secoue. **Une animation d'echec est une animation de personnage** : elle en dit plus long sur lui que dix reussites.

**`souffle_court`** — 0,9 s
Une bouffee breve, sans charge. La tete bouge a peine. Pour ponctuer, pour saluer, pour prevenir.

**`fumee_naseaux`** — 3 s, loop
Rien ne bouge sauf deux petites poussees au niveau des naseaux, toutes les secondes et demie. **A jouer en fond pendant l'attente** si ton systeme le permet.

**`rugissement`** — 2,8 s
La tete part vers le haut a 50°, la gueule s'ouvre, tout le corps se raidit et gonfle. Les ailes s'ecartent d'un tiers. Trois secousses pendant l'emission — un rugissement n'est pas une note tenue.

**`grondement`** — 4 s, loop
Gueule fermee, la poitrine vibre a basse frequence — une oscillation de 1 % a huit hertz. La tete basse et fixe. *La menace, sans le spectacle.*

## Menace et combat

**`menace`** — 2,5 s, hold
Tete basse au niveau des epaules, cou en avant, ailes a demi ouvertes, queue levee. **Toute la silhouette doit grandir.**

**`intimide`** — 3 s
Comme la menace, plus un pas lent en avant et un grondement. Le poids passe visiblement d'une patte avant a l'autre.

**`coup_de_griffe`** — 0,9 s
Amorce du poids sur l'arriere, patte avant qui part en arc, corps qui suit avec deux images de retard, retour en garde.

**`coup_de_queue`** — 1,1 s
Le mouvement part des hanches, remonte le long de la queue, et l'extremite arrive en dernier — trois images apres la base. **C'est un fouet, pas une barre.**

**`morsure`** — 0,8 s
La tete part droit devant, la gueule se ferme, le cou tire vers l'arriere. Court et sec.

**`esquive`** — 0,7 s
Un pas de cote rapide, corps incline de 20°, tete qui reste orientee vers la menace.

**`encaisse`** — 0,9 s
Le corps recule de 20 %, la tete se detourne, une patte glisse. **Le retour doit etre trois fois plus long que l'impact.**

**`blesse`** — 5 s, loop
Une patte evite le sol, l'echine est de travers, la tete basse. Toute la locomotion est asymetrique.

**`epuise`** — 6 s, loop
Debout mais bas, respiration ample et bruyante, tete qui dodeline. Les ailes pendent legerement au lieu d'etre plaquees.

**`victoire`** — 3,2 s
Il se redresse au maximum, ailes ouvertes, tete haute, un battement d'ailes, et retombe en position normale. Rapide, et sans exces.

## Majeste et presence

**`pose_royale`** — 6 s, loop
Assis tres droit, ailes repliees haut, queue enroulee autour des pattes avant. La tete legerement au-dessus de l'horizontale. Presque rien ne bouge. *La retenue est le sujet.*

**`inspecte`** — 4,5 s
La tete descend lentement vers un point, s'approche jusqu'a quinze centimetres, tourne de 20° d'un cote puis de l'autre, se recule. Il examine.

**`scrute_horizon`** — 5,2 s
Tete haute, balayage tres lent de 120°, avec trois arrets d'une demi-seconde. Le corps ne bouge pas du tout.

**`deploie_collerette`** — 1,8 s, hold
Si le modele a des piquants ou une collerette : ils se dressent de la nuque vers l'arriere, en vague, sur six images.

**`secoue_tete`** — 1 s
Un mouvement rapide gauche-droite-gauche, amplitude decroissante. Trois oscillations, pas quatre.

**`s_ebroue_entier`** — 1,8 s
Comme l'ebrouement commun, mais les ailes participent. La vague part de la tete et met huit images a atteindre le bout de la queue.

**`etire_ailes`** — 3,4 s
Une aile s'etire seule vers l'arriere et le haut, avec la patte du meme cote. Puis l'autre. **Jamais les deux ensemble** — c'est ce qui differencie l'etirement du deploiement.

**`lisse_ecailles`** — 4,2 s
La tete vient frotter l'epaule, puis le flanc, puis la base de l'aile. Trois zones, trois temps, avec un retour au centre entre chaque.

## Avec son maitre

**`baisse_tete`** — 2,2 s, hold
Il abaisse la tete et le cou jusqu'au niveau du sol devant lui, et tient. Pour qu'on le caresse, ou pour qu'on monte. **La pose la plus utile du lot.**

**`presente_dos`** — 2,8 s, hold
Position basse, l'aile du cote du joueur legerement relevee pour degager le passage. Tient.

**`pousse_du_museau`** — 1,6 s
Un coup de museau doux vers l'avant, deux fois, avec un temps entre les deux. Il reclame.

**`enroule_queue`** — 3 s, hold
La queue vient s'enrouler autour de quelque chose devant lui. Le mouvement part de la base et se propage.

**`ronronne`** — 5 s, loop
Yeux mi-clos, tete posee, une vibration de 0,5 % a basse frequence sur toute la cage thoracique. Presque invisible, et on la sent.

**`garde`** — 6 s, loop
Debout entre le joueur et le reste du monde. Balayage lent de la tete, poids reparti, ailes legerement ecartees. *Une vigilance calme, pas une menace.*

**`reveille_maitre`** — 2,5 s
Museau qui pousse, un pas en arriere, tete inclinee. Recommence une fois.

**`accueille`** — 3,5 s
Il se dresse, ouvre les ailes a demi, baisse la tete, et fait deux pas en avant. La sequence complete de la retrouvaille.

## Le reste

**`dort_enroule`** — 12 s, loop
Enroule sur lui-meme, queue ramenee sur le museau, une aile en couverture. Respiration tres lente et tres ample — huit secondes par cycle. **La plus longue de la liste, et celle qu'on regardera le plus longtemps.**

**`reve`** — 4 s, loop
Pendant le sommeil : les pattes tressaillent, la queue bouge d'un coup, un fremissement de machoire. Irregulier, rare, jamais rythme.

**`reveil`** — 3,3 s
La tete se leve d'abord, les yeux s'ouvrent, un baillement, l'aile se deplie, le corps se deroule, il se leve. **Cinq etapes, dans cet ordre, sans en sauter une.**

**`creuse`** — 3,8 s, loop
Les pattes avant grattent en alternance, la terre part vers l'arriere, le museau descend verifier toutes les deux secondes.

---

## Par ou commencer

Si tu n'en fais que dix, fais celles-ci : ce sont celles que le mod appelle le
plus souvent, et donc celles qu'on verra le plus. Les cent autres enrichissent —
ces dix decident de ce que la bete a l'air d'etre.

`idle` · `marche` · `course` · `vol` · `assis` · `couche` · `ecoute` ·
`caresse` · `joie` · `mange`
