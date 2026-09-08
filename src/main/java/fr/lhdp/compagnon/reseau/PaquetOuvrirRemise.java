package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Le serveur ouvre a l'equipe l'ecran qui remet un compagnon.
 *
 * <h2>Pourquoi le serveur envoie la liste des joueurs</h2>
 *
 * <p>Le client connait les joueurs de sa liste de tabulation, mais elle peut
 * etre filtree, triee, ou vide selon les mods installes. La liste vient donc
 * d'en haut, une fois, au moment de l'ouverture : c'est la seule qui fasse
 * autorite.
 *
 * <p>Elle n'est qu'un confort d'affichage. Au retour, le serveur cherchera le
 * joueur par son nom et refusera s'il n'est plus la — la liste peut vieillir
 * pendant que l'ecran est ouvert, et c'est sans consequence.
 */
public record PaquetOuvrirRemise(List<String> joueurs) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetOuvrirRemise> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("ouvrir_remise"));

	public static final StreamCodec<FriendlyByteBuf, PaquetOuvrirRemise> CODEC =
			CustomPacketPayload.codec(PaquetOuvrirRemise::ecrire, PaquetOuvrirRemise::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.joueurs.size());
		for (String joueur : this.joueurs) {
			tampon.writeUtf(joueur);
		}
	}

	private static PaquetOuvrirRemise lire(FriendlyByteBuf tampon) {
		int combien = tampon.readVarInt();
		List<String> joueurs = new ArrayList<>(combien);
		for (int i = 0; i < combien; i++) {
			joueurs.add(tampon.readUtf());
		}
		return new PaquetOuvrirRemise(List.copyOf(joueurs));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
