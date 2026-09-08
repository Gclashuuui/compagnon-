package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * L'etat des compagnons sortis, pour le petit panneau du coin de l'ecran.
 *
 * <h2>Pourquoi c'est si petit</h2>
 *
 * <p>Ce paquet peut partir vers mille joueurs. Chaque jauge tient donc en
 * <b>trois octets</b> : les barres sont ramenees de 0 a 100 dans un octet, et
 * l'humeur est un simple numero. On perd les decimales — et on s'en moque, la
 * barre fait vingt pixels de large.
 *
 * <h2>Et pourquoi il part si rarement</h2>
 *
 * <p>Il n'est envoye que lorsque quelque chose a <b>vraiment</b> change, une
 * fois arrondi. Les barres bougent de moins d'un point par minute : en pratique
 * ce paquet part quelques fois par minute et par joueur, pas vingt fois par
 * seconde. Voir {@code Jauges}.
 *
 * <p>Une liste vide veut dire « range le panneau » : c'est ce qu'on envoie quand
 * le dernier compagnon rentre.
 *
 * @param jauges une entree par compagnon dehors
 */
public record PaquetJauges(List<Jauge> jauges) implements CustomPacketPayload {

	/**
	 * Ce qu'on montre d'un compagnon.
	 *
	 * @param nom     son nom
	 * @param faim    de 0 a 100
	 * @param energie de 0 a 100
	 * @param humeur  le numero de l'humeur, dans l'ordre de l'enumeration
	 */
	/**
	 * Ce qu'on montre d'un compagnon dans le panneau.
	 *
	 * @param monte vrai si quelqu'un est sur son dos. Le panneau cesse alors de
	 *              s'effacer : quand on vole sur une bete, son energie est la
	 *              seule chose qui compte, et c'est le pire moment pour la
	 *              rendre discrete.
	 */
	public record Jauge(String nom, int faim, int energie, int humeur, boolean monte) {
	}

	public static final CustomPacketPayload.Type<PaquetJauges> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("jauges"));

	public static final StreamCodec<FriendlyByteBuf, PaquetJauges> CODEC =
			CustomPacketPayload.codec(PaquetJauges::ecrire, PaquetJauges::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.jauges.size());
		for (Jauge jauge : this.jauges) {
			tampon.writeUtf(jauge.nom(), 64);
			tampon.writeByte(jauge.faim());
			tampon.writeByte(jauge.energie());
			tampon.writeByte(jauge.humeur());
			tampon.writeBoolean(jauge.monte());
		}
	}

	private static PaquetJauges lire(FriendlyByteBuf tampon) {
		int combien = tampon.readVarInt();
		List<Jauge> jauges = new ArrayList<>(combien);
		for (int i = 0; i < combien; i++) {
			jauges.add(new Jauge(tampon.readUtf(64),
				tampon.readByte(), tampon.readByte(), tampon.readByte(),
					tampon.readBoolean()));
		}
		return new PaquetJauges(List.copyOf(jauges));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
