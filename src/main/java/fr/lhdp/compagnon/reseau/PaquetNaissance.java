package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur a tape un nom dans l'ecran d'accueil : il demande la naissance de son
 * compagnon.
 *
 * <p>Le client ne decide de rien. Il envoie un nom, et c'est tout. C'est le
 * serveur qui verifie que le joueur tient bien un certificat, qui y lit l'espece
 * et la variante, qui cree la fiche et qui consomme l'objet.
 *
 * @param nom le nom choisi, deja borne a {@value #LONGUEUR_MAX} caracteres sur le
 *            fil pour qu'un client bricole ne puisse pas envoyer un roman
 */
public record PaquetNaissance(String nom) implements CustomPacketPayload {

	/** Longueur maximale d'un nom de compagnon. Valeur inventee. */
	public static final int LONGUEUR_MAX = 24;

	public static final CustomPacketPayload.Type<PaquetNaissance> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("naissance"));

	public static final StreamCodec<ByteBuf, PaquetNaissance> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(LONGUEUR_MAX), PaquetNaissance::nom,
			PaquetNaissance::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
