package fr.lhdp.compagnon.espece;

import fr.lhdp.compagnon.entite.ProfilCerveau;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Une espece de compagnon, telle qu'elle est ecrite dans
 * {@code assets/compagnon/especes/<nom>.json}.
 *
 * <p>Le code ne connait que les <b>roles</b> ci-dessous. Les noms d'animation ne
 * sont jamais ecrits en dur : c'est le fichier qui fait la correspondance role
 * → nom. Ajouter une espece, c'est ajouter un fichier.
 *
 * <p>Quatre roles sont obligatoires, les autres non. Un role optionnel absent
 * retombe sur {@link #IMMOBILE} : on peut donc livrer une espece avec le strict
 * minimum et l'enrichir plus tard sans rien casser.
 *
 * <p>Une variante ne change que la texture — meme geometrie, memes animations.
 *
 * <p>La <b>taille</b> est celle de la boite de collision, pas celle du dessin.
 * C'est ce qui empeche de traverser le compagnon, et ce qui dit ou poser la main
 * quand on le caresse. Elle vit dans le fichier : ajouter une espece plus grande
 * ne demande aucune ligne de code.
 */
public record Espece(
		String nom,

		/**
		 * Son nom tel qu'on le montre, qui n'est pas son identifiant.
		 *
		 * <p>{@code nom} est une cle : il vit dans les fichiers, dans les
		 * sauvegardes et dans les chemins d'assets, et le renommer casserait
		 * les compagnons deja apprivoises. On ne le montre donc plus a
		 * personne — c'est ce titre-la qui s'affiche.
		 *
		 * <p>La distinction devient necessaire le jour ou une meme bete existe
		 * a deux ages : {@code atroxiia} et {@code atroxiia_adulte} sont deux
		 * especes pour le mod, mais « Bebe atroxiia » et « Atroxiia adulte »
		 * pour le joueur, qui n'a pas a lire un tiret bas.
		 *
		 * <p>Une espece qui n'en declare pas en recoit un correct : son
		 * identifiant, les tirets bas rendus a l'espace et la premiere lettre
		 * en capitale. Aucun fichier existant n'a donc besoin d'etre touche.
		 */
		String titre,
		ResourceLocation geometrie,
		ResourceLocation animations,
		String varianteParDefaut,
		Map<String, ResourceLocation> variantes,
		Map<String, String> locomotion,
		Map<String, String> reactions,
		ProfilCerveau cerveau,

		/**
		 * Sa voix : un son du jeu par role.
		 *
		 * <p>Des sons du jeu, jamais des fichiers a nous : le jeu en a des
		 * centaines, dont des dizaines de cris d'animaux. Une mouette qui
		 * emprunte la voix du perroquet sonne tres bien, et le mod ne grossit
		 * pas d'un octet.
		 *
		 * <p>Vide = muette, comme toutes les especes l'etaient avant. On remplit
		 * les fiches a son rythme, rien ne casse en attendant.
		 */
		Map<String, String> sons,
		float largeur,
		float hauteur,
		List<Partie> parties,
		boolean vole,
		String nomVocal,

		/**
		 * Ou le cavalier s'assoit, en blocs depuis les pattes de la bete.
		 *
		 * <p>Dans le meme ordre que les morceaux de collision : droite, haut,
		 * avant. Une espece qui ne le dit pas se monte au milieu de son dos, ce
		 * qui est rarement juste et toujours visible.
		 */
		Partie selle,

		/**
		 * Le niveau a partir duquel elle se laisse monter, ou {@code 0}.
		 *
		 * <p>Zero veut dire jamais, et c'est le defaut : une espece qui ne declare
		 * pas cette cle ne se monte pas. On n'ajoute pas une facon de se deplacer
		 * a tout un serveur par omission.
		 */
		int monterAuNiveau,

		/**
		 * L'espece qu'il deviendra en grandissant, ou une chaine vide.
		 *
		 * <p>Un bebe raevyx et un raevyx adulte sont deux especes pour le mod :
		 * deux modeles, deux jeux d'animations, deux tailles. Ce champ est le
		 * fil qui les relie, et il ne va que dans un sens.
		 *
		 * <p>Vide, la bete ne grandit jamais — c'est le cas de la mouette, du
		 * kobeko, et de tous les adultes.
		 */
		String devient,

		/**
		 * A quel niveau il grandit. Zero veut dire jamais.
		 *
		 * <p>Volontairement tard. Un adulte qu'on distribue n'est qu'un gros
		 * modele ; un adulte qu'on a eleve depuis l'oeuf est une histoire. C'est
		 * le meme fichier de geometrie et ce n'est pas la meme chose.
		 */
		int devientAuNiveau) {


	/** Le son de ce role, ou {@code null} si l'espece n'en declare pas. */
	public String son(String role) {
		return this.sons.get(role);
	}

	/** Largeur par defaut d'une boite de collision, en blocs. */
	public static final float LARGEUR_PAR_DEFAUT = 0.9F;

	/** Hauteur par defaut d'une boite de collision, en blocs. */
	public static final float HAUTEUR_PAR_DEFAUT = 0.9F;

	// --- Roles obligatoires ---
	public static final String IMMOBILE = "immobile";
	public static final String MARCHE = "marche";
	public static final String COURSE = "course";
	public static final String VOL = "vol";
	/** Vol sans battement, quand il perd doucement de l'altitude. */
	public static final String PLANE = "plane";
	/** Deplacement volontaire dans l'eau. */
	public static final String NAGE = "nage";
	/** Attente a la surface de l'eau. */
	public static final String FLOTTE = "flotte";

	// --- Roles optionnels : les poses sur ordre ---
	public static final String ASSIS = "assis";
	public static final String COUCHE = "couche";

	// --- Roles optionnels : les reactions ---
	public static final String CARESSE = "caresse";

	/**
	 * Les deux poses dattente qui montrent son humeur.
	 *
	 * <p>Absentes, il garde sa pose dattente ordinaire : une espece qui na
	 * quune seule animation immobile se comporte exactement comme avant.
	 */
	public static final String JOYEUX = "joyeux";
	public static final String TRISTE = "triste";

	/** Les rôles réellement communs, y compris aux créatures sans ailes. */
	public static final List<String> ROLES = List.of(IMMOBILE, MARCHE, COURSE);

	/** Une espèce déclarée volante doit posséder celui-ci. */
	public static final List<String> ROLES_AERIENS_OBLIGATOIRES = List.of(VOL);

	/**
	 * Les roles de locomotion facultatifs. Les poses retombent sur immobile ;
	 * le vol plane et l'eau choisissent eux-memes leur repli dans l'entite.
	 */
	public static final List<String> ROLES_OPTIONNELS =
			List.of(ASSIS, COUCHE, PLANE, NAGE, FLOTTE);

	/** Une joie ordinaire, reutilisee si l'espece n'a pas de geste de niveau. */
	public static final String JOIE = "joie";

	/** La celebration propre a une montee de niveau, entierement facultative. */
	public static final String NIVEAU = "niveau";

	// --- Roles optionnels : les transitions de locomotion ---
	public static final String DEPART_MARCHE = "transition_depart_marche";
	public static final String ARRET_MARCHE = "transition_arret_marche";
	public static final String PASSAGE_COURSE = "transition_course";
	public static final String ARRET_COURSE = "transition_arret_course";
	public static final String VERS_ASSIS = "transition_assis";
	public static final String VERS_DEBOUT = "transition_debout";
	public static final String VERS_COUCHE = "transition_couche";
	public static final String REVEIL = "transition_reveil";
	public static final String DECOLLAGE = "transition_decollage";
	public static final String ATTERRISSAGE = "transition_atterrissage";
	public static final String VOL_VERS_PLANE = "transition_vol_plane";
	public static final String PLANE_VERS_VOL = "transition_plane_vol";

	/** Vrai si cette espece peut porter quelqu'un, a un niveau ou a un autre. */
	public boolean seMonte() {
		return this.monterAuNiveau > 0;
	}

	/** La texture d'une variante ; celle par defaut si la variante est inconnue. */
	public ResourceLocation texture(String variante) {
		ResourceLocation trouvee = this.variantes.get(variante);
		return trouvee != null ? trouvee : this.variantes.get(this.varianteParDefaut);
	}

	/** Le nom d'animation d'un role de locomotion, ou {@code null}. */
	public String animation(String role) {
		return this.locomotion.get(role);
	}

	/**
	 * Le nom d'animation d'un role, avec repli sur {@link #IMMOBILE} si le fichier
	 * ne decrit pas ce role.
	 */
	public String animationOuImmobile(String role) {
		String trouvee = this.locomotion.get(role);
		return trouvee != null ? trouvee : this.locomotion.get(IMMOBILE);
	}

	/**
	 * Le nom d'animation d'une reaction, ou {@code null} si le fichier n'en decrit
	 * pas. C'est volontaire : tant que la liste est vide, la mecanique marche mais
	 * rien ne s'anime.
	 */
	public String reaction(String role) {
		return this.reactions.get(role);
	}

	public boolean connaitVariante(String variante) {
		return this.variantes.containsKey(variante);
	}
}
