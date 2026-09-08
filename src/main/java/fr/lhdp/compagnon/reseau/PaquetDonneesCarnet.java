package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.livre.EntreeCarnet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Tous les compagnons d'un joueur, envoyes d'un coup.
 *
 * <p>D'un coup, et pas page par page : un joueur en a quelques-uns, pas mille.
 * Tourner les pages devient alors instantane et ne coute pas un aller-retour
 * reseau — ce qui compte quand on cherche lequel sortir.
 *
 * @param choisi  celui qui est montre en grand
 * @param entrees tous les autres, dans l'ordre du joueur
 */
public record PaquetDonneesCarnet(int choisi, List<EntreeCarnet> entrees)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetDonneesCarnet> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("donnees_carnet"));

	public static final StreamCodec<FriendlyByteBuf, PaquetDonneesCarnet> CODEC =
			CustomPacketPayload.codec(PaquetDonneesCarnet::ecrire, PaquetDonneesCarnet::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.choisi);
		tampon.writeVarInt(this.entrees.size());
		for (EntreeCarnet entree : this.entrees) {
			tampon.writeUtf(entree.nom());
			tampon.writeUtf(entree.espece());
			tampon.writeUtf(entree.variante());
			tampon.writeVarInt(entree.niveau());
			tampon.writeBoolean(entree.sorti());
			tampon.writeUtf(entree.ou(), 128);
		}
	}

	private static PaquetDonneesCarnet lire(FriendlyByteBuf tampon) {
		int choisi = tampon.readVarInt();
		int combien = tampon.readVarInt();

		List<EntreeCarnet> entrees = new ArrayList<>(combien);
		for (int i = 0; i < combien; i++) {
			entrees.add(new EntreeCarnet(
					tampon.readUtf(), tampon.readUtf(), tampon.readUtf(),
					tampon.readVarInt(), tampon.readBoolean(), tampon.readUtf(128)));
		}
		return new PaquetDonneesCarnet(choisi, List.copyOf(entrees));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
