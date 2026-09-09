# Animations à produire pour le cerveau

Cette liste est la feuille de route Blockbench. Les noms à gauche sont des
**rôles** stables : chaque espèce les relie à ses propres noms d'animations dans
son fichier `data/compagnon/especes/<espece>.json`.

Une créature sans ailes n'a jamais besoin des rôles aériens. Le cerveau
`terrestre.json` désactive aussi le nœud de vol : ajouter une nouvelle espèce
terrestre ne pourra donc pas lui faire ouvrir les ailes par erreur.

## Priorité 1 — locomotion fluide

| Rôle | Type | Utilisation |
|---|---|---|
| `immobile` | boucle | respiration permanente |
| `marche` | boucle | déplacement normal |
| `course` | boucle | déplacement rapide, avec hystérésis |
| `transition_depart_marche` | unique | repos vers marche |
| `transition_arret_marche` | unique | marche vers repos |
| `transition_course` | unique | marche vers course |
| `transition_arret_course` | unique | course vers repos |
| `transition_assis` | unique | descend en position assise |
| `transition_debout` | unique | se relève avant de repartir |
| `transition_couche` | unique | descend au sol |
| `transition_reveil` | unique | se relève après sommeil |

Les trois premières sont obligatoires pour toutes les espèces. Les transitions
sont facultatives : absentes, le moteur conserve un fondu court et sûr.

## Priorité 2 — créatures volantes

| Rôle | Type | Utilisation |
|---|---|---|
| `vol` | boucle | battements actifs, obligatoire si `vole: true` |
| `plane` | boucle | descente douce sans battement |
| `transition_decollage` | unique | pattes au sol vers ailes ouvertes |
| `transition_atterrissage` | unique | freinage puis contact au sol |
| `transition_vol_plane` | unique | battement vers plane |
| `transition_plane_vol` | unique | plane vers reprise des battements |
| `vol_stationnaire` | boucle | futur nœud d'observation aérienne |
| `virage_gauche` | boucle courte | futur guidage selon le cap |
| `virage_droit` | boucle courte | futur guidage selon le cap |
| `montee` | boucle | futur état vertical positif |
| `descente` | boucle | futur état vertical négatif |

À créer en premier : `vol`, `plane`, `transition_decollage`,
`transition_atterrissage`. Le nouveau cerveau attend quatre ticks stables avant
de croire un changement de terrain, mais un vrai décollage reste immédiat.

## Priorité 3 — besoins et réactions lisibles

| Rôle | Type | Comportement |
|---|---|---|
| `ecoute` | unique court | entend le joueur |
| `caresse` | unique | reçoit une caresse |
| `mange` | unique | mange à la main ou à la gamelle |
| `ramasse` | unique | saisit un objet |
| `curieux` | unique | inspecte un bloc ou événement |
| `reconnait` | unique | retrouve un lieu mémorable |
| `salut` | unique | rencontre un compagnon connu |
| `joie` | unique | rituel ou réussite |
| `joyeux` | boucle calme | humeur très bonne |
| `triste` | boucle calme | humeur basse, froid ou pluie |
| `tourne` | unique | s'ébroue ou manie simple |
| `niveau` | unique | montée de niveau |

## Priorité 4 — prochaine couche du cerveau

Ces rôles sont prévus pour les prochains comportements. Ils ne doivent pas être
ajoutés à la roue : ils seront choisis automatiquement par un futur nœud.

| Rôle proposé | Déclencheur futur |
|---|---|
| `surpris` | bruit ou apparition soudaine proche |
| `peur` | menace vue, selon courage/personnalité |
| `cherche` | objet ou maître perdu de vue |
| `renifle` | nourriture ou nouvel objet proche |
| `invite_jeu` | complicité haute et joueur immobile |
| `attend_ordre` | vient de finir `ecoute` |
| `fatigue` | énergie basse pendant la marche ou le vol |
| `reve` | mouvement rare pendant le sommeil |
| `protege` | se place entre son maître et une menace |
| `retrouvailles` | maître absent depuis longtemps |

## Règles Blockbench

1. Les boucles commencent et finissent sur la même pose et la même vitesse.
2. Une transition termine sur la première pose exacte de la boucle suivante.
3. Le contact des pattes et le battement d'aile reçoivent un repère sonore.
4. Aucun rôle ne déplace l'origine du modèle : le déplacement appartient au jeu.
5. Tester marche → course → arrêt et vol → plane → atterrissage sans autre
   animation avant de produire les réactions.
