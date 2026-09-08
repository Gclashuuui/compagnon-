package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur apprend un mot a son compagnon.
 *
 * <p>Le client propose : un numero dans sa propre liste, une animation, un mot.
 * Le serveur verifie tout — que la bete est bien a lui, que l'animation est
 * ouverte a son niveau, et surtout que le mot existe dans le dictionnaire du
 * micro. Un mot que le moteur ne sait pas dire ne serait jamais entendu, et le
 * joueur croirait avoir dresse sa bete pour rien.
 *
 * <p>Un mot vide efface : c'est ainsi qu'on lui fait oublier un tour, sans avoir
 * besoin d'un second paquet pour ca.
 *
 * @param index     quel compagnon, dans la liste de ce joueur
 * @param animation le geste a declencher
 * @param mot       ce qu'on dira pour l'obtenir, ou vide pour oublier
 */
public record PaquetApprendre(int index, String animation, String mot)
		implements CustomPacketPayload {

	/**
	 * Longueur maximale du mot, cote reseau.
	 *
	 * <p>Un mot du francais depasse rarement vingt lettres, et cette borne est
	 * appliquee <b>par le serveur</b> : un client fabrique ne remplira pas la
	 * sauvegarde avec une phrase de trente mille caracteres.
	 */
	public static final int LONGUEUR_MAX = 24;

	public static final CustomPacketPayload.Type<PaquetApprendre> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("apprendre"));

	public static final StreamCodec<FriendlyByteBuf, PaquetApprendre> CODEC =
			CustomPacketPayload.codec(PaquetApprendre::ecrire, PaquetApprendre::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.index);
		tampon.writeUtf(this.animation, 128);
		tampon.writeUtf(this.mot, LONGUEUR_MAX);
	}

	private static PaquetApprendre lire(FriendlyByteBuf tampon) {
		return new PaquetApprendre(tampon.readVarInt(),
				tampon.readUtf(128), tampon.readUtf(LONGUEUR_MAX));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
