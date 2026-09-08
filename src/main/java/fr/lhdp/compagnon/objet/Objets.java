package fr.lhdp.compagnon.objet;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.contenu.Donnable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Les objets du mod, et le composant qu'ils portent.
 *
 * <p>Il y en a trois, et un seul de chaque : l'oeuf qui donne un
 * compagnon, l'aliment, l'objet de soin. Leur <b>variete</b> — quel aliment,
 * quel remede — est une donnee portee par l'objet et decrite en JSON. Ajouter un
 * aliment, c'est ajouter un fichier : jamais une classe, jamais une compilation.
 */
public final class Objets {

	/**
	 * L'espece et la variante que porte un oeuf.
	 */
	public static final DataComponentType<Origine> ORIGINE = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Compagnon.id("origine"),
			DataComponentType.<Origine>builder()
					.persistent(Origine.CODEC)
					.networkSynchronized(Origine.STREAM_CODEC)
					.build());

	/**
	 * Le nom du fichier qui decrit un aliment ou un objet de soin.
	 */
	public static final DataComponentType<String> VARIETE = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Compagnon.id("variete"),
			DataComponentType.<String>builder()
					.persistent(com.mojang.serialization.Codec.STRING)
					.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8)
					.build());

	/** L'oeuf : le joueur l'utilise ou il veut, quand il veut. */
	public static final Item OEUF = enregistrer("oeuf", new ObjetOeuf(
			new Item.Properties().stacksTo(1)));

	/** Un aliment. Ce qu'il remplit est decrit dans {@code data/compagnon/aliments/}. */
	public static final Item ALIMENT = enregistrer("aliment", new ObjetDonne(
			new Item.Properties().stacksTo(16), ObjetDonne.Genre.ALIMENT));

	/** Un objet de soin. Decrit dans {@code data/compagnon/soins/}. */
	public static final Item SOIN = enregistrer("soin", new ObjetDonne(
			new Item.Properties().stacksTo(16), ObjetDonne.Genre.SOIN));

	/**
	 * La balle. On la lance, il la rapporte, on recommence.
	 *
	 * <p>Une seule par joueur suffit : elle ne se consomme pas et revient
	 * toujours. Un stock de balles n'aurait aucun sens.
	 */
	public static final Item BALLE = enregistrer("balle", new ObjetJouet(
			new Item.Properties().stacksTo(1)));

	/** L'os a macher. Se lance aussi : c'est le geste, pas l'objet, qui compte. */
	public static final Item OS_A_MACHER = enregistrer("os_a_macher", new ObjetJouet(
			new Item.Properties().stacksTo(1)));

	/**
	 * Le jouet pendant qu'il vole.
	 *
	 * <p>Enregistre ici et non avec le compagnon : ce n'est pas une creature,
	 * c'est un objet en l'air. Il porte la pile qu'on a lancee, donc une balle
	 * vole comme une balle et un os comme un os, sans deux entites.
	 *
	 * <p>Suivi de pres seulement — quatre troncons : personne n'a besoin de voir
	 * rebondir une balle a soixante blocs.
	 */
	public static final net.minecraft.world.entity.EntityType<JouetLance> JOUET_LANCE =
			Registry.register(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE,
				Compagnon.id("jouet_lance"),
				net.minecraft.world.entity.EntityType.Builder
					.<JouetLance>of(JouetLance::new,
						net.minecraft.world.entity.MobCategory.MISC)
					.sized(0.28F, 0.28F)
					.clientTrackingRange(4)
					.updateInterval(10)
					.build("jouet_lance"));

	private Objets() {
	}

	/**
	 * Un exemplaire d'un aliment ou d'un soin, pret a etre donne.
	 *
	 * <p>Deux donnees, et il faut les deux : la <b>variete</b>, dont le serveur se
	 * sert pour savoir ce que l'objet fait, et le <b>numero de modele</b>, que le
	 * client regarde pour choisir la texture. Poser la premiere sans la seconde
	 * donne un objet qui marche mais qui s'affiche comme tous les autres.
	 *
	 * @param donnable la variete, ou {@code null} : on rend alors l'objet nu
	 */
	public static ItemStack exemplaire(Item objet, Donnable donnable) {
		ItemStack pile = new ItemStack(objet);
		if (donnable == null) {
			return pile;
		}
		pile.set(VARIETE, donnable.id());
		if (donnable.modele() > 0) {
			pile.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(donnable.modele()));
		}
		return pile;
	}

	/**
	 * Ce qu'on envoie au client pour qu'il dessine l'objet dont le compagnon a envie.
	 *
	 * <p>Un aliment du mod n'a pas d'objet a lui : les soixante-dix varietes
	 * partagent le meme, et seule la donnee de modele les distingue. Le client, lui,
	 * ne recoit qu'une chaine. On y colle donc le numero apres une barre —
	 * {@code compagnon:aliment|12} — et il reconstitue la bonne image.
	 *
	 * <p>Un aliment qui nomme un vrai objet du jeu dans son fichier garde la
	 * priorite : c'est ce qui permet de faire reclamer une pomme de Minecraft.
	 */
	public static String envie(Donnable donnable) {
		if (donnable == null) {
			return "";
		}
		if (donnable.aUnObjet()) {
			return donnable.objet();
		}
		return Compagnon.id("aliment") + "|" + donnable.modele();
	}

	private static Item enregistrer(String nom, Item objet) {
		return Registry.register(BuiltInRegistries.ITEM, Compagnon.id(nom), objet);
	}

	/** Force le chargement de la classe, donc les enregistrements ci-dessus. */
	public static void enregistrer() {
		// Volontairement vide : les champs statiques font le travail.
	}
}
