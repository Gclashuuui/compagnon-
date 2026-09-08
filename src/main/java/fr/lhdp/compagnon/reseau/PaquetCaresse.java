package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Quelqu'un caresse un compagnon.
 *
 * <p>Envoye a tous ceux qui voient la scene, y compris a celui qui caresse :
 * <b>les autres joueurs doivent le voir aussi</b>, sinon le geste n'existe que
 * pour celui qui le fait et ne fait rien vivre dans les couloirs.
 *
 * <p>Le serveur decide qui caresse et quand. Le client ne fait qu'animer.
 *
 * @param joueur    le numero d'entite de celui qui caresse
 * @param compagnon le numero d'entite du compagnon, pour savoir a quelle hauteur
 *                  poser la main
 * @param ticks     combien de temps le geste dure
 */
public record PaquetCaresse(int joueur, int compagnon, int ticks) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetCaresse> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("caresse"));

	public static final StreamCodec<ByteBuf, PaquetCaresse> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PaquetCaresse::joueur,
			ByteBufCodecs.VAR_INT, PaquetCaresse::compagnon,
			ByteBufCodecs.VAR_INT, PaquetCaresse::ticks,
			PaquetCaresse::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
