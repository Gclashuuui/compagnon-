package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le cavalier veut monter ou descendre en vol.
 *
 * <h2>Pourquoi ce paquet existe</h2>
 *
 * <p>Le serveur connait deja l'avant, l'arriere et les cotes du cavalier :
 * {@code xxa} et {@code zza} sont publics et synchronises pour celui qui pilote.
 * <b>Le saut, lui, ne l'est pas</b> — {@code jumping} est protege, et aucune
 * monture du jeu ne le lit depuis l'exterieur.
 *
 * <p>On envoie donc la seule chose qui manque : l'intention verticale. Et
 * uniquement <b>quand elle change</b> — trois fois par vol, pas vingt fois par
 * seconde.
 *
 * @param vers {@code 1} pour monter, {@code -1} pour descendre, {@code 0} pour
 *             tenir l'altitude
 */
public record PaquetChevaucher(int vers) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetChevaucher> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("chevaucher"));

	public static final StreamCodec<ByteBuf, PaquetChevaucher> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PaquetChevaucher::vers,
			PaquetChevaucher::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
