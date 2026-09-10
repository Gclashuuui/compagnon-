# Prompt à envoyer avec les fichiers du dragonnet

Je te confie trois fichiers d'un mod Minecraft Fabric 1.21.1 utilisant GeckoLib :

- `dragonnet.geo.json` : squelette Bedrock du dragonnet, 109 os ;
- `dragonnet.animation.json` : fichier actuel, format `1.8.0`, 72 animations ;
- `dragonnet.json` : rôles utilisés par le cerveau du compagnon.

Tu es un animateur technique expert de Blockbench, des animations Bedrock JSON
et de GeckoLib. Travaille directement à partir des fichiers joints. Ne génère
pas d'image et ne redessine pas le dragon.

## Objectif

Le dragon possède déjà 72 bonnes animations. Elles sont terminées et ne doivent
être ni corrigées, ni refaites, ni remplacées, même si tu aurais animé un geste
autrement. Je veux uniquement enrichir sa vie quotidienne avec de nouvelles
micro-animations très courtes. Je ne veux pas de grandes scènes : je veux les
mouvements presque invisibles qui donnent l'impression qu'un animal respire,
observe, hésite, reconnaît les gens et revient naturellement à sa pose.

Analyse les animations existantes seulement pour retrouver leur pose de repos et
éviter les doublons. N'écris aucune modification sous un nom déjà présent.

## Travail — ajouter 30 micro-animations sans toucher aux anciennes

Crée exactement ces animations :

### Visage et attention

1. `animation.dragonnet.blink` — clignement naturel, 0,16 à 0,24 s.
2. `animation.dragonnet.blink_double` — deux clignements irréguliers, 0,40 à 0,60 s.
3. `animation.dragonnet.ear_twitch_left` — oreille gauche attirée par un son.
4. `animation.dragonnet.ear_twitch_right` — miroir réellement adapté, pas un copier-coller faux.
5. `animation.dragonnet.glance_left` — yeux puis tête, mouvement minuscule.
6. `animation.dragonnet.glance_right` — version droite.
7. `animation.dragonnet.head_micro_tilt_left` — questionnement discret.
8. `animation.dragonnet.head_micro_tilt_right` — questionnement discret.

### Corps vivant au repos

9. `animation.dragonnet.weight_shift_left` — transfère doucement son poids.
10. `animation.dragonnet.weight_shift_right` — transfert opposé.
11. `animation.dragonnet.tail_flick_left` — un petit coup de queue puis retour.
12. `animation.dragonnet.tail_flick_right` — variante non identique.
13. `animation.dragonnet.tail_settle` — la queue se replace après une action.
14. `animation.dragonnet.wing_resettle` — replace une membrane ou une aile pliée.
15. `animation.dragonnet.wing_fold_soft` — replie souplement les ailes après l'atterrissage.
16. `animation.dragonnet.look_back` — vérifie brièvement derrière lui.

### Petits comportements raccordables par le cerveau

17. `animation.dragonnet.listen_start` — passe du repos à l'écoute.
18. `animation.dragonnet.listen_end` — revient de l'écoute au repos.
19. `animation.dragonnet.inspect_ground_start` — baisse la tête vers un événement.
20. `animation.dragonnet.inspect_ground_loop` — observe 1 à 2 secondes, boucle parfaite.
21. `animation.dragonnet.inspect_ground_end` — relève la tête sans claquement.
22. `animation.dragonnet.hesitate` — un demi-pas retenu, tête et oreilles incertaines.
23. `animation.dragonnet.relax_after_alert` — souffle et relâche progressivement ailes et queue.
24. `animation.dragonnet.bored` — change d'appui, regarde ailleurs, sans caricature de chien.
25. `animation.dragonnet.friend_notice` — reconnaît un autre compagnon à distance.
26. `animation.dragonnet.affection_approach_end` — termine une approche affectueuse proprement.
27. `animation.dragonnet.morning_ritual` — réveil/étirement calme, 2 à 3,5 s.
28. `animation.dragonnet.evening_settle` — se calme et se prépare au repos, 2 à 3,5 s.
29. `animation.dragonnet.recognize_person` — reconnaît clairement une personne familière.
30. `animation.dragonnet.observe_stranger` — observe prudemment une personne inconnue.

## Contraintes techniques impératives

- Format Bedrock/GeckoLib JSON `1.8.0`, compatible Minecraft 1.21.1.
- Ne modifie jamais `dragonnet.geo.json`, les textures, les pivots ou les noms d'os.
- N'invente aucun os. Utilise uniquement ceux réellement présents dans le fichier joint.
- Ne modifie, ne renomme et ne supprime aucune animation existante.
- Aucun déplacement global de `root` ou `worldbody`. Le déplacement dans le monde
  appartient au code Minecraft, jamais au clip.
- Pas de changement d'échelle du modèle.
- Toutes les animations uniques commencent et finissent dans une pose raccordable,
  sans remise à zéro brutale.
- Toutes les boucles possèdent exactement la même pose et la même vitesse au début
  et à la fin.
- Utilise une interpolation douce, mais évite l'excès de `catmullrom` qui provoque
  des dépassements sur les ailes et les pattes.
- Les micro-animations durent idéalement de 0,16 à 1,4 seconde. Seuls les rituels
  du matin et du soir peuvent aller jusqu'à 3,5 secondes.
- Anime seulement les os utiles. Un clignement ne doit pas contenir des pistes
  pour cent os.
- En général, limite-toi à 3–8 images clés par os pour une micro-animation.
- Les ailes restent lourdes et organiques ; la queue prolonge le mouvement avec
  un léger retard ; les yeux commencent souvent le regard avant le cou.
- Le dragon est un compagnon intelligent et curieux, pas un chien : pas de queue
  qui remue frénétiquement, pas de langue pendante, pas de gestes canins.
- Ne fabrique aucune animation spectaculaire dans cette livraison.

## Livraison attendue

Ne me rends pas un énorme fichier entièrement réécrit si ce n'est pas nécessaire.
Fournis :

1. `dragonnet.animation.additions.json`, contenant sous `animations` uniquement
   les 30 nouvelles animations ;
2. `RAPPORT_ANIMATIONS_DRAGONNET.md`, avec pour chaque entrée : durée, boucle ou
   animation unique, os animés, but du geste et état de départ/arrivée ;
3. un tableau confirmant les 30 nouvelles animations et leurs usages ;
4. si tu sais fusionner sans supprimer ni reformater les animations existantes,
   ajoute aussi `dragonnet.animation.merged.json`, mais le fichier d'ajouts reste
   obligatoire.

Avant de livrer, valide automatiquement : JSON lisible, 30 nouveaux noms exacts,
aucun os inconnu, aucune animation existante modifiée, aucune durée négative,
et aucune boucle dont la pose finale diffère de la pose initiale.

Ne me pose pas de question avant de commencer : inspecte les trois fichiers,
travaille avec le squelette réel et indique clairement toute limite rencontrée.
