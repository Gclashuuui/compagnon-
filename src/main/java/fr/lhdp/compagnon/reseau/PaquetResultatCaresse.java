package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Résultat proposé par le petit jeu de caresse, toujours vérifié par le serveur. */
public record PaquetResultatCaresse(int compagnon, int reussites)
		implements CustomPacketPayload {

	public static final Type<PaquetResultatCaresse> TYPE =
			new Type<>(Compagnon.id("resultat_caresse"));

	public static final StreamCodec<ByteBuf, PaquetResultatCaresse> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, PaquetResultatCaresse::compagnon,
					ByteBufCodecs.VAR_INT, PaquetResultatCaresse::reussites,
					PaquetResultatCaresse::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
