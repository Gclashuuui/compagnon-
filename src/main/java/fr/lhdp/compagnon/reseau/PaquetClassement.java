package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * « Montre-moi le classement. »
 *
 * <p>Vide : le serveur sait deja qui demande, et il n'y a rien a preciser. Le
 * paquet existe quand meme plutot qu'un simple signal, parce que c'est le seul
 * moyen pour le serveur de <b>refuser</b> — le staff peut avoir ferme le
 * tableau, et le client ne peut pas le deviner.
 */
public record PaquetClassement() implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetClassement> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("classement"));

	public static final StreamCodec<ByteBuf, PaquetClassement> CODEC =
			StreamCodec.unit(new PaquetClassement());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
