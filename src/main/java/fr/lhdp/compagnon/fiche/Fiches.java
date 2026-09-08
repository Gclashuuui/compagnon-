package fr.lhdp.compagnon.fiche;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Toutes les fiches du monde, dans les donnees de sauvegarde du serveur.
 *
 * <p>Pas en NBT sur l'entite : une entite qui n'existe pas ne peut rien
 * sauvegarder, et l'interet de la fiche est justement de survivre a l'absence de
 * l'entite.
 *
 * <p>Le stockage est global au serveur (il vit dans l'Overworld), pas par
 * dimension : la fiche porte sa dimension, un compagnon peut donc etre range
 * n'importe ou sans qu'on ait a le chercher monde par monde.
 *
 * <p><b>Un index par proprietaire est tenu a jour</b> a cote de la carte
 * principale. Sans lui, ouvrir le livre couterait un balayage de toutes les
 * fiches du serveur — ce qui, dans un chateau a mille compagnons, se paierait a
 * chaque appui sur la touche.
 */
public class Fiches extends SavedData {

	/** Nom du fichier, dans {@code data/} du monde. */
	public static final String NOM_FICHIER = "compagnon_fiches";

	private static final String CLE_LISTE = "Fiches";

	public static final SavedData.Factory<Fiches> FABRIQUE =
			new SavedData.Factory<>(Fiches::new, Fiches::charger, DataFixTypes.LEVEL);

	private final Map<UUID, FicheCompagnon> parId = new LinkedHashMap<>();

	/** Proprietaire vers ses fiches. Deduit de {@link #parId}, jamais sauvegarde. */
	private final Map<UUID, List<FicheCompagnon>> parProprietaire = new LinkedHashMap<>();

	public Fiches() {
	}

	private static Fiches charger(CompoundTag balise, HolderLookup.Provider registres) {
		Fiches fiches = new Fiches();
		ListTag liste = balise.getList(CLE_LISTE, Tag.TAG_COMPOUND);
		for (int i = 0; i < liste.size(); i++) {
			fiches.poser(FicheCompagnon.charger(liste.getCompound(i)));
		}
		return fiches;
	}

	@Override
	public CompoundTag save(CompoundTag balise, HolderLookup.Provider registres) {
		ListTag liste = new ListTag();
		for (FicheCompagnon fiche : this.parId.values()) {
			liste.add(fiche.sauver());
		}
		balise.put(CLE_LISTE, liste);
		return balise;
	}

	/** Le magasin de fiches du serveur. Le cree au premier appel. */
	public static Fiches de(MinecraftServer serveur) {
		return serveur.overworld().getDataStorage().computeIfAbsent(FABRIQUE, NOM_FICHIER);
	}

	// --- Ecriture ---------------------------------------------------------------

	public void ajouter(FicheCompagnon fiche) {
		poser(fiche);
		setDirty();
	}

	/** Range la fiche dans les deux index. Ne marque pas la sauvegarde. */
	private void poser(FicheCompagnon fiche) {
		this.parId.put(fiche.id(), fiche);
		this.parProprietaire
				.computeIfAbsent(fiche.proprietaire(), proprietaire -> new ArrayList<>())
				.add(fiche);
	}

	public boolean supprimer(UUID id) {
		FicheCompagnon retiree = this.parId.remove(id);
		if (retiree == null) {
			return false;
		}

		List<FicheCompagnon> siennes = this.parProprietaire.get(retiree.proprietaire());
		if (siennes != null) {
			siennes.remove(retiree);
			if (siennes.isEmpty()) {
				this.parProprietaire.remove(retiree.proprietaire());
			}
		}

		setDirty();
		return true;
	}

	// --- Lecture ----------------------------------------------------------------

	public FicheCompagnon get(UUID id) {
		return this.parId.get(id);
	}

	/**
	 * Une <b>copie</b>, volontairement.
	 *
	 * <p>Les commandes de suppression parcourent cette liste tout en retirant des
	 * fiches. Renvoyer une vue ferait planter le parcours. Ne pas transformer en
	 * {@code values()} pour economiser une allocation : ce n'est pas le meme
	 * comportement.
	 */
	public Collection<FicheCompagnon> toutes() {
		return new ArrayList<>(this.parId.values());
	}

	/**
	 * Les fiches d'un joueur, sans balayer le serveur.
	 *
	 * <p>La liste renvoyee est une copie : les appelants la parcourent parfois en
	 * supprimant dedans.
	 */
	public List<FicheCompagnon> duProprietaire(UUID proprietaire) {
		List<FicheCompagnon> siennes = this.parProprietaire.get(proprietaire);
		return siennes == null ? List.of() : new ArrayList<>(siennes);
	}

	/** Sans copie : pour les parcours en lecture seule, appeles souvent. */
	public List<FicheCompagnon> duProprietaireSansCopie(UUID proprietaire) {
		List<FicheCompagnon> siennes = this.parProprietaire.get(proprietaire);
		return siennes == null ? List.of() : siennes;
	}

	public int nombre() {
		return this.parId.size();
	}
}
