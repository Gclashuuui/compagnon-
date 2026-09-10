# Décisions conservées

- Le nom de l'objet physique est **Journal des Liens**.
- Un seul modèle 3D de livre sert à tous les thèmes.
- Les variantes changent la texture, pas la géométrie.
- Le livre doit convenir à toutes les espèces : aucun symbole exclusivement chien,
  chat ou dragon.
- Le titre est ajouté avec une vraie police pixel si l'outil artistique ne sait pas
  produire des lettres impeccables.
- Les textures d'interface servent de références de direction artistique ; elles ne
  sont pas des textures UV prêtes à coller sur le modèle 3D.
- Les boutons de changement de page visibles doivent disparaître. Les coins des pages
  deviennent les zones cliquables.
- Les favoris de livres et le thème sélectionné sont des préférences locales au
  joueur : ils ne sont jamais envoyés au serveur.
- Avec beaucoup de variantes, le clic sur le nuancier ouvre une bibliothèque avec
  aperçus ; le clic droit sert de raccourci pour ajouter le thème courant aux favoris.
- Le panneau de santé doit toujours montrer quatre besoins distincts : faim, énergie,
  santé et complicité. Un cœur ne représente pas à la fois la santé et le lien.
- Les thèmes du carnet de santé changent le cadre et la palette, jamais la position
  des textes et des jauges. Le choix reste purement client.
- Le code, les documents de suivi et les livraisons restent versionnés sur GitHub.
- Le dossier local `output/` n'est pas ajouté automatiquement au dépôt.
- Toute nouvelle espèce réutilise le cerveau commun, la mémoire, la perception et les
  règles d'optimisation, avec des animations et capacités adaptées à son corps.
