package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * « Celle-la ne me dit rien, donne m'en une autre. »
 *
 * <p>Une seule par renouvellement : sans cette limite, on retirerait jusqu'a
 * tomber sur la plus facile, et le carnet ne voudrait plus rien dire. C'est le
 * serveur qui la fait respecter — le client ne fait que demander.
 *
 * @param index   quel compagnon, dans l'ordre du joueur
 * @param place   quelle mission, de 0 a 2 ; la longue ne s'ecarte pas
 */
public record PaquetEcarter(int index, int place) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetEcarter> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("ecarter"));

	public static final StreamCodec<ByteBuf, PaquetEcarter> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PaquetEcarter::index,
			ByteBufCodecs.VAR_INT, PaquetEcarter::place,
			PaquetEcarter::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
