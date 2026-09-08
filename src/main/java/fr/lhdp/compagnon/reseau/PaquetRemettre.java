package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * L'equipe remet un oeuf de compagnon a quelqu'un.
 *
 * <h2>Le seul paquet du mod qui demande un droit</h2>
 *
 * <p>Tous les autres ne peuvent toucher que les betes de celui qui les envoie.
 * Celui-ci donne, donc il faut le niveau 2 — et le serveur le verifie a
 * l'arrivee, jamais l'ecran. Un client fabrique qui l'enverrait sans le droit
 * repart les mains vides.
 *
 * <p>Rien de ce qu'il contient n'est cru : le nom du joueur est cherche parmi
 * ceux qui sont en ligne, l'espece et la variante sont cherchees dans les
 * fiches chargees. Trois chaines libres, trois verifications.
 *
 * @param joueur   le nom de celui qui recoit, pas son identifiant — voir
 *                 {@code SecuriteTest}, aucun paquet montant n'en porte
 * @param espece   la cle d'espece, verifiee a l'arrivee
 * @param variante la couleur, verifiee elle aussi
 */
public record PaquetRemettre(String joueur, String espece, String variante)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetRemettre> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("remettre"));

	public static final StreamCodec<ByteBuf, PaquetRemettre> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, PaquetRemettre::joueur,
			ByteBufCodecs.STRING_UTF8, PaquetRemettre::espece,
			ByteBufCodecs.STRING_UTF8, PaquetRemettre::variante,
			PaquetRemettre::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
