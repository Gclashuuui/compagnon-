package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.livre.EntreeRoue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Le contenu de la roue, envoye par le serveur.
 *
 * <p>Tout y est, ouvert ou non : c'est le serveur qui dit ce qui est ouvert, et
 * c'est lui qui le reverifiera au moment de jouer l'animation. Le client ne fait
 * qu'afficher des cadenas.
 *
 * @param index   quel compagnon
 * @param nom     son nom, pour le titre de la roue
 * @param niveau  son niveau, pour expliquer les cadenas
 * @param entrees toutes les cases
 * @param compagnons les noms de toutes ses betes, pour les onglets du haut
 * @param present est-il réellement sorti dans ce monde
 * @param fatigue est-il trop fatigué pour un geste volontaire
 * @param occupe était-il déjà au milieu d'une action à l'ouverture
 */
public record PaquetDonneesRoue(int index, String nom, int niveau,
		List<EntreeRoue> entrees, List<String> compagnons,
		boolean present, boolean fatigue, boolean occupe)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetDonneesRoue> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("donnees_roue"));

	public static final StreamCodec<FriendlyByteBuf, PaquetDonneesRoue> CODEC =
			CustomPacketPayload.codec(PaquetDonneesRoue::ecrire, PaquetDonneesRoue::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.index);
		tampon.writeUtf(this.nom);
		tampon.writeVarInt(this.niveau);
		tampon.writeVarInt(this.compagnons.size());
		for (String compagnon : this.compagnons) {
			tampon.writeUtf(compagnon, 64);
		}
		tampon.writeVarInt(this.entrees.size());
		for (EntreeRoue entree : this.entrees) {
			tampon.writeUtf(entree.nom());
			tampon.writeVarInt(entree.niveauRequis());
			tampon.writeBoolean(entree.debloque());
			tampon.writeUtf(entree.mot(), 64);
		}
		tampon.writeBoolean(this.present);
		tampon.writeBoolean(this.fatigue);
		tampon.writeBoolean(this.occupe);
	}

	private static PaquetDonneesRoue lire(FriendlyByteBuf tampon) {
		int index = tampon.readVarInt();
		String nom = tampon.readUtf();
		int niveau = tampon.readVarInt();
		int nombreDeCompagnons = tampon.readVarInt();
		List<String> compagnons = new ArrayList<>(nombreDeCompagnons);
		for (int i = 0; i < nombreDeCompagnons; i++) {
			compagnons.add(tampon.readUtf(64));
		}

		int combien = tampon.readVarInt();
		List<EntreeRoue> entrees = new ArrayList<>(combien);
		for (int i = 0; i < combien; i++) {
			entrees.add(new EntreeRoue(tampon.readUtf(), tampon.readVarInt(),
					tampon.readBoolean(), tampon.readUtf(64)));
		}

		return new PaquetDonneesRoue(index, nom, niveau,
				List.copyOf(entrees), List.copyOf(compagnons),
				tampon.readBoolean(), tampon.readBoolean(), tampon.readBoolean());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
