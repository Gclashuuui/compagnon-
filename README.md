# Compagnon

Compagnon est un mod Minecraft Fabric 1.21.1 pour un serveur RP francophone.
Chaque joueur peut faire grandir une creature vivante, la nourrir, la soigner,
la caresser, lui parler et construire une relation qui evolue avec le temps.

Le compagnon possede notamment :

- une personnalite, une humeur et des souvenirs ;
- des besoins, des soins et une progression sur 50 niveaux ;
- des competences et des missions liees a la vie commune ;
- des interactions avec les joueurs et les autres compagnons ;
- des ordres vocaux hors ligne avec Plasmo Voice et Vosk ;
- un livre vivant, une roue d'actions contextuelle et plusieurs especes configurees en JSON ;
- un cerveau commun visible par un petit bandeau discret, avec diagnostic F8
  pour relier intention, locomotion et animation pendant la creation d'une espece.

Les profils de cerveau sont dans `data/compagnon/cerveaux/`. Une espèce utilise
automatiquement `aerien` ou `terrestre`, ou choisit un autre profil avec la clé
`"cerveau"`. La liste de production Blockbench est dans
`ANIMATIONS_CERVEAU.md`.

## Prerequis

- Java 21
- Minecraft 1.21.1
- Fabric Loader et Fabric API
- GeckoLib 4.8.4 ou plus recent
- Plasmo Voice, facultatif, pour les ordres vocaux

## Compiler et verifier

Sous Windows :

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Le mod compile se trouve ensuite dans `build/libs/`.

## Documentation

- `REPRENDRE.md` decrit l'architecture et l'etat actuel du projet.
- `ANIMATIONS.md` rassemble les roles et intentions des animations.
- `CONCEPTION.html` presente les principes et les idees de conception.

## Etat du projet

Le projet est en cours de developpement. Certaines animations et validations en
conditions reelles, notamment la chaine vocale complete, restent a terminer.

## Licence

Tous droits reserves. Le code est consultable publiquement, mais sa copie, sa
modification et sa redistribution demandent l'autorisation de l'auteur.
