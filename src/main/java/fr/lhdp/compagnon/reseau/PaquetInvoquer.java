package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur veut faire venir un compagnon, ou le ranger.
 *
 * <p>Le client n'envoie qu'un numero et une intention. Il ne dit pas ou poser la
 * bete, ni si elle en a le droit : le serveur decide de tout, y compris de
 * ranger un autre compagnon pour faire de la place.
 *
 * @param index  quel compagnon, dans la liste de ce joueur
 * @param sortir vrai pour l'invoquer, faux pour le ranger
 */
public record PaquetInvoquer(int index, boolean sortir) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetInvoquer> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("invoquer"));

	public static final StreamCodec<ByteBuf, PaquetInvoquer> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PaquetInvoquer::index,
			ByteBufCodecs.BOOL, PaquetInvoquer::sortir,
			PaquetInvoquer::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
