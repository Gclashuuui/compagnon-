package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le serveur ouvre a l'equipe l'editeur de position.
 *
 * <p>Il ne porte que l'espece a regler. Le reste — la position actuelle, la
 * liste des os du modele — le client le sait deja : il a la table des ancrages
 * et le fichier de geometrie sous la main. Envoyer ce qu'il possede deja serait
 * du bruit.
 *
 * @param espece la bete qu'on regle
 */
public record PaquetOuvrirAncrage(String espece) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetOuvrirAncrage> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("ouvrir_ancrage"));

	public static final StreamCodec<ByteBuf, PaquetOuvrirAncrage> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, PaquetOuvrirAncrage::espece,
					PaquetOuvrirAncrage::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
