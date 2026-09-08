package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le joueur a choisi une case dans la roue.
 *
 * <p>Ce paquet est une <b>demande</b>, jamais un ordre. Le serveur revérifie
 * tout : que le compagnon est bien le sien, que l'animation est dans la table,
 * qu'elle est ouverte a son niveau, et qu'il est en etat de la faire. Un client
 * modifie qui enverrait n'importe quel nom n'obtiendrait rien.
 *
 * @param index  quel compagnon
 * @param nom    le nom d'animation demande
 * @param apercu vrai quand on ne fait que SURVOLER la case
 */
public record PaquetAction(int index, String nom, boolean apercu) implements CustomPacketPayload {

	/** Une vraie demande, celle d'un clic. */
	public static PaquetAction choisie(int index, String nom) {
		return new PaquetAction(index, nom, false);
	}

	/**
	 * Un simple survol.
	 *
	 * <p>Il montre l'animation sans rien couter : ni energie, ni niveau requis. On
	 * peut donc voir a quoi ressemble ce qu'on n'a pas encore debloque — et c'est
	 * exactement ce qui donne envie de l'avoir.
	 */
	public static PaquetAction survolee(int index, String nom) {
		return new PaquetAction(index, nom, true);
	}

	/** Longueur maximale d'un nom d'animation, bornee sur le fil. */
	public static final int LONGUEUR_MAX = 128;

	public static final CustomPacketPayload.Type<PaquetAction> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("action"));

	public static final StreamCodec<ByteBuf, PaquetAction> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PaquetAction::index,
			ByteBufCodecs.stringUtf8(LONGUEUR_MAX), PaquetAction::nom,
			ByteBufCodecs.BOOL, PaquetAction::apercu,
			PaquetAction::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
