package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.espece.Ancrage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * L'equipe enregistre une position reglee a l'oeil.
 *
 * <h2>Il demande un droit</h2>
 *
 * <p>Comme la remise d'un oeuf, et pour la meme raison : il ecrit un fichier du
 * serveur, et ce qu'il ecrit se voit chez tout le monde. Le niveau 2 est verifie
 * <b>a l'arrivee</b>, jamais dans l'ecran.
 *
 * <p>Le nom d'espece et le nom d'os arrivent d'un client et ne sont crus sur
 * rien : le serveur verifie que l'espece existe avant d'ecrire quoi que ce soit.
 * Il ne peut pas verifier le nom de l'os — les modeles sont des fichiers du
 * client — mais un nom d'os faux ne fait qu'une chose : rien ne s'affiche.
 *
 * @param espece      la bete reglee
 * @param emplacement {@code bouche}, {@code collier}, ...
 * @param ancrage     l'os, l'ecart, l'angle et la taille
 */
public record PaquetPoserAncrage(String espece, String emplacement, Ancrage ancrage)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetPoserAncrage> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("poser_ancrage"));

	public static final StreamCodec<FriendlyByteBuf, PaquetPoserAncrage> CODEC =
			CustomPacketPayload.codec(PaquetPoserAncrage::ecrire, PaquetPoserAncrage::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeUtf(this.espece);
		tampon.writeUtf(this.emplacement);
		PaquetAncrages.ecrireUn(tampon, this.ancrage);
	}

	private static PaquetPoserAncrage lire(FriendlyByteBuf tampon) {
		return new PaquetPoserAncrage(tampon.readUtf(), tampon.readUtf(),
				PaquetAncrages.lireUn(tampon));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
