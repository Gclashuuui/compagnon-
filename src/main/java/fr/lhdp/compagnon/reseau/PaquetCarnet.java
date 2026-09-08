package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur demande a ouvrir le carnet des compagnons.
 *
 * @param choisi celui qu'il regardait, pour rouvrir dessus
 */
public record PaquetCarnet(int choisi) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetCarnet> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("carnet"));

	public static final StreamCodec<ByteBuf, PaquetCarnet> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PaquetCarnet::choisi,
			PaquetCarnet::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
