package fr.lhdp.compagnon.contenu;

/**
 * Un petit bobo.
 *
 * <p>La sante ne baisse pas au combat : elle baisse par petits accidents,
 * quelques fois par semaine. Il s'est fait mal a la patte, il a mal au ventre.
 *
 * <p><b>Ca ne le tue jamais.</b> Tant que ce n'est pas soigne, la sante reste
 * basse et l'humeur descend — c'est ce qui donne une raison de venir le voir. Ce
 * n'est pas du farm, c'est de l'attention.
 *
 * @param id          le nom du fichier, sans son extension
 * @param nom         ce qui s'affiche dans le livre
 * @param description une phrase, pour que le joueur comprenne ce qu'il a
 * @param soignePar   l'identifiant de l'objet de soin qui le regle. Chaque bobo
 *                    nomme lui-meme son remede
 * @param sante       ce qu'il retire a la sante en apparaissant ; un nombre
 *                    negatif
 */
public record Bobo(String id, String nom, String description, String soignePar, float sante) {
}
