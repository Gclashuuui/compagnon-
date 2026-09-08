package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur a appuye sur la touche du livre.
 *
 * <p>Il ne dit rien d'autre. Le serveur retrouve ses fiches tout seul : le client
 * ne peut pas demander a voir le livre de quelqu'un d'autre.
 *
 * @param index quel compagnon, quand le joueur en a plusieurs
 */
public record PaquetLivre(int index) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetLivre> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("livre"));

	public static final StreamCodec<ByteBuf, PaquetLivre> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PaquetLivre::index,
			PaquetLivre::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
