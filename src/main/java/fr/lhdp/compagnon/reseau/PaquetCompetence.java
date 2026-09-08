package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur depense un point de competence.
 *
 * <p>Le client propose : un numero dans sa propre liste, et un identifiant. Le
 * serveur verifie tout — que la bete est bien a lui, que la competence existe,
 * qu'elle concerne son espece, que son niveau l'ouvre, et surtout <b>qu'il lui
 * reste un point</b>.
 *
 * <p>Un choix ne se reprend pas. C'est ce qui lui donne son poids : une
 * competence qu'on peut annuler n'est pas un choix, c'est un reglage.
 *
 * @param index quel compagnon, dans la liste de ce joueur
 * @param id    la competence demandee
 */
public record PaquetCompetence(int index, String id) implements CustomPacketPayload {

	/** Longueur maximale d'un identifiant, cote reseau. */
	public static final int LONGUEUR_MAX = 64;

	public static final CustomPacketPayload.Type<PaquetCompetence> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("competence"));

	public static final StreamCodec<FriendlyByteBuf, PaquetCompetence> CODEC =
			CustomPacketPayload.codec(PaquetCompetence::ecrire, PaquetCompetence::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.index);
		tampon.writeUtf(this.id, LONGUEUR_MAX);
	}

	private static PaquetCompetence lire(FriendlyByteBuf tampon) {
		return new PaquetCompetence(tampon.readVarInt(), tampon.readUtf(LONGUEUR_MAX));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
