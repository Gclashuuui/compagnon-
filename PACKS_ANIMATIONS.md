# Packs d'animations des compagnons

Ce document est le contrat entre Blockbench et le cerveau. Les identifiants sont
stables : un nouveau modèle peut les utiliser sans modifier le Java. Une animation
absente est ignorée avec un repli propre ; une animation présente devient
automatiquement disponible dès qu'elle est reliée dans `reactions` ou `locomotion`
du fichier d'espèce.

## Dragonnet — prochaines animations à créer

Le fichier v17 actuellement intégré contient déjà 72 animations, dont marche,
course, vol, plane, décollage, atterrissage, eau, sommeil et transitions. Il ne
faut pas les refaire. Voici les 30 ajouts utiles au nouveau cerveau, par priorité :

1. `animation.dragonnet.blink` — clignement simple très court.
2. `animation.dragonnet.blink_double` — double clignement curieux.
3. `animation.dragonnet.eye_track` — yeux qui suivent une cible sans tourner le corps.
4. `animation.dragonnet.ears_alert` — appendices/oreilles tournés vers un bruit.
5. `animation.dragonnet.ears_relax` — retour souple au repos.
6. `animation.dragonnet.tail_calm` — queue lente et détendue.
7. `animation.dragonnet.tail_happy` — queue vive, sans mouvement de chien.
8. `animation.dragonnet.tail_worried` — queue rapprochée du corps.
9. `animation.dragonnet.breathe` — respiration discrète du ventre et du cou.
10. `animation.dragonnet.breathe_tired` — respiration après une course ou un vol.
11. `animation.dragonnet.head_tilt_left` — questionnement à gauche.
12. `animation.dragonnet.head_tilt_right` — questionnement à droite.
13. `animation.dragonnet.listen_left` — localise un son à gauche.
14. `animation.dragonnet.listen_right` — localise un son à droite.
15. `animation.dragonnet.drink_satisfied` — relève la tête après avoir bu.
16. `animation.dragonnet.affection_nuzzle` — vient frotter doucement la tête.
17. `animation.dragonnet.comfort_owner` — se rapproche d'un maître blessé/triste.
18. `animation.dragonnet.greet_friend` — salutation propre à un autre compagnon.
19. `animation.dragonnet.wait_at_door` — attend devant un passage fermé.
20. `animation.dragonnet.watch_window` — observe au loin, corps immobile.
21. `animation.dragonnet.warm_near_fire` — replie les ailes et profite de la chaleur.
22. `animation.dragonnet.hide_from_rain` — protège sa tête et replie les ailes.
23. `animation.dragonnet.sleep_twitch` — petit mouvement pendant le sommeil.
24. `animation.dragonnet.dream_happy` — sourire/mouvement léger sans particules fortes.
25. `animation.dragonnet.tired_landing` — atterrissage lourd après effort.
26. `animation.dragonnet.bond_constellation` — **WOW**, scène de lien magique.
27. `animation.dragonnet.aerial_spiral` — **WOW**, spirale aérienne maîtrisée.
28. `animation.dragonnet.wing_shield` — **WOW**, protège son maître avec une aile.
29. `animation.dragonnet.magic_discovery` — **WOW**, découvre et suit une lueur magique.
30. `animation.dragonnet.great_reunion` — **WOW**, retrouvailles après une longue absence.

Les quinze premières sont petites et rapides à produire. Elles donnent davantage
de vie quotidienne que quinze longues cinématiques.

## Starter pack terrestre — 60 animations

### Locomotion et transitions (1–12)

1. `idle` — repos debout en boucle.
2. `walk` — marche en boucle.
3. `run` — course en boucle.
4. `swim` — nage en boucle.
5. `water_float` — flotte sans avancer.
6. `turn_left` — rotation sur place à gauche.
7. `turn_right` — rotation sur place à droite.
8. `walk_start` — départ de marche.
9. `walk_stop` — arrêt de marche.
10. `walk_to_run` — accélération.
11. `run_stop` — freinage de course.
12. `water_exit` — sortie de l'eau.

### Postures et repos (13–20)

13. `sit_down` — s'assoit.
14. `sit` — reste assis en boucle.
15. `stand_up` — se relève.
16. `lie_down` — se couche.
17. `sleep` — dort en boucle.
18. `wake_up` — se réveille.
19. `doze` — somnole sans dormir complètement.
20. `stretch` — étirement complet.

### Micro-vie individuelle (21–35)

21. `blink` — clignement simple.
22. `blink_double` — double clignement.
23. `ears_left` — oreilles/appendices vers la gauche.
24. `ears_right` — oreilles/appendices vers la droite.
25. `ears_alert` — oreilles/appendices en alerte.
26. `ears_relax` — retour au calme.
27. `head_tilt_left` — tête penchée à gauche.
28. `head_tilt_right` — tête penchée à droite.
29. `sniff` — renifle le sol ou l'air.
30. `breathe` — respiration légère en boucle.
31. `tail_calm` — queue calme.
32. `tail_happy` — queue heureuse propre à l'espèce.
33. `tail_worried` — queue inquiète.
34. `shake` — secoue tout le corps.
35. `scratch` — se gratte.

### Besoins et environnement (36–42)

36. `eat` — mange.
37. `drink` — boit dans une gamelle.
38. `favorite_food` — réaction à son aliment préféré.
39. `ask_food` — signale discrètement sa faim.
40. `tired` — fatigue lisible.
41. `groom` — toilette adaptée à l'anatomie.
42. `rain_shake` — chasse l'eau après la pluie.

### Vie sociale et voix (43–50)

43. `petted` — reçoit une caresse.
44. `affection` — réclame ou offre de l'affection.
45. `greet_owner` — salue son maître.
46. `greet_companion` — salue un compagnon connu.
47. `listen` — écoute un ordre vocal.
48. `understand` — confirme qu'il a compris.
49. `refuse` — refuse selon sa personnalité.
50. `praised` — reçoit une félicitation.

### Dix scènes spectaculaires (51–60)

51. `wow_bond_celebration` — **WOW**, célèbre un nouveau lien.
52. `wow_great_reunion` — **WOW**, retrouvailles après longue absence.
53. `wow_defend_owner` — **WOW**, s'interpose face à une menace.
54. `wow_courage_pose` — **WOW**, surmonte une peur importante.
55. `wow_comfort_scene` — **WOW**, longue scène de réconfort.
56. `wow_duo_scene` — **WOW**, chorégraphie avec un ami.
57. `wow_level_up` — **WOW**, passage de niveau propre à l'espèce.
58. `wow_discover_place` — **WOW**, découvre son lieu favori.
59. `wow_dream_scene` — **WOW**, rêve visible et très léger.
60. `wow_species_signature` — **WOW**, scène signature impossible à confondre.

## Starter pack volant — 70 animations

### Sol, repos et eau (1–15)

1. `idle` — repos debout en boucle.
2. `walk` — marche.
3. `run` — course au sol.
4. `turn_left` — rotation gauche.
5. `turn_right` — rotation droite.
6. `walk_start` — départ de marche.
7. `walk_stop` — arrêt de marche.
8. `sit_down` — s'assoit.
9. `sit` — assis en boucle.
10. `stand_up` — se relève.
11. `lie_down` — se couche.
12. `sleep` — dort.
13. `wake_up` — se réveille.
14. `stretch` — étire corps et ailes.
15. `doze` — somnole.

### Vol et transitions (16–33)

16. `takeoff` — décollage depuis le sol.
17. `fly` — battement de croisière.
18. `fly_fast` — vol rapide.
19. `glide` — plane.
20. `landing` — atterrit.
21. `fly_to_glide` — passe du battement au plané.
22. `glide_to_fly` — reprend ses battements.
23. `bank_left` — virage incliné gauche.
24. `bank_right` — virage incliné droit.
25. `climb` — prend de l'altitude.
26. `descend` — perd de l'altitude.
27. `air_brake` — freinage aérien.
28. `hover` — vol stationnaire.
29. `hover_turn` — tourne en stationnaire.
30. `landing_abort` — renonce à se poser.
31. `water_float` — flotte.
32. `swim` — nage.
33. `water_takeoff` — décolle depuis l'eau.

### Micro-vie individuelle (34–47)

34. `blink` — clignement simple.
35. `blink_double` — double clignement.
36. `ears_alert` — oreilles/crête en alerte.
37. `ears_relax` — retour au calme.
38. `head_tilt_left` — tête penchée gauche.
39. `head_tilt_right` — tête penchée droite.
40. `sniff` — renifle.
41. `breathe` — respiration calme.
42. `wing_fold` — replie les ailes.
43. `wing_unfold` — déploie les ailes.
44. `wing_groom` — entretient ailes ou plumes.
45. `tail_calm` — queue calme.
46. `tail_happy` — queue heureuse.
47. `tail_worried` — queue inquiète.

### Besoins et environnement (48–54)

48. `eat` — mange.
49. `drink` — boit.
50. `favorite_food` — adore un aliment.
51. `ask_food` — signale sa faim.
52. `tired_landing` — revient se poser fatigué.
53. `rain_shake` — chasse l'eau des ailes.
54. `groom` — toilette générale.

### Vie sociale et voix (55–60)

55. `petted` — reçoit une caresse.
56. `affection` — geste affectif.
57. `greet_owner` — salue le maître.
58. `greet_companion` — salue un ami.
59. `listen` — écoute la voix.
60. `understand` — confirme l'ordre.

### Dix scènes spectaculaires (61–70)

61. `wow_aerial_spiral` — **WOW**, spirale aérienne autour d'un point.
62. `wow_dive_recovery` — **WOW**, piqué puis ressource au dernier instant.
63. `wow_sky_loop` — **WOW**, boucle complète adaptée à l'anatomie.
64. `wow_circle_owner` — **WOW**, cercle protecteur autour du maître.
65. `wow_wing_shield` — **WOW**, protège quelqu'un sous son aile.
66. `wow_air_duo` — **WOW**, chorégraphie avec un autre volant.
67. `wow_storm_courage` — **WOW**, affronte l'orage puis revient.
68. `wow_royal_landing` — **WOW**, atterrissage signature et salut.
69. `wow_level_up_sky` — **WOW**, passage de niveau en vol.
70. `wow_species_signature` — **WOW**, pouvoir/scène unique de l'espèce.

## Règles de livraison Blockbench

- Les boucles (`idle`, `walk`, `run`, `fly`, `glide`, `sleep`, `breathe`) doivent
  revenir exactement à leur première pose.
- Les transitions commencent sur la dernière pose de l'état précédent et
  terminent sur la première pose de l'état suivant.
- Les micro-animations ne déplacent jamais la racine du modèle : elles pourront
  être superposées sans glissement.
- Les scènes **WOW** durent idéalement de 3 à 7 secondes, pas quinze secondes.
- Chaque animation doit être testée à 20 ticks/seconde et avec interpolation.
- Une espèce sans oreilles remplace `ears_*` par crête, antennes, nageoires ou
  regard : le rôle émotionnel reste le même, l'anatomie change.

