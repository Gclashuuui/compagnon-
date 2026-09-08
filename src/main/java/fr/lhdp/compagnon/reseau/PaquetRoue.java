package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur demande a ouvrir la roue.
 *
 * @param index quel compagnon, quand il en a plusieurs
 */
public record PaquetRoue(int index) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetRoue> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("roue"));

	public static final StreamCodec<ByteBuf, PaquetRoue> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PaquetRoue::index,
			PaquetRoue::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
