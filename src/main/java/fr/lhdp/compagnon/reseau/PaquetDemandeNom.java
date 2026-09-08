package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le serveur a verifie le certificat et demande au client d'ouvrir l'ecran de nom.
 *
 * <p>Il porte l'espece et la variante uniquement pour que l'ecran puisse dire ce
 * qu'on est en train de nommer. Rien de ce qu'il contient ne sera cru au retour :
 * le serveur relira le certificat.
 */
public record PaquetDemandeNom(String espece, String variante) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetDemandeNom> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("demande_nom"));

	public static final StreamCodec<ByteBuf, PaquetDemandeNom> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, PaquetDemandeNom::espece,
			ByteBufCodecs.STRING_UTF8, PaquetDemandeNom::variante,
			PaquetDemandeNom::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
