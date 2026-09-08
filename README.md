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
- un livre, une roue d'actions et plusieurs especes configurees en JSON.

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

