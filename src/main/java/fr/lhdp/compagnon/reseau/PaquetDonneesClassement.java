package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.classement.Classement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Le classement, tel qu'un joueur le voit.
 *
 * <p>Le haut du tableau et son voisinage, jamais la liste entiere : a mille
 * joueurs, envoyer tout le monde a tout le monde ferait plusieurs centaines de
 * kilo-octets par ouverture d'ecran, pour des lignes que personne ne fait
 * defiler.
 *
 * @param lignes         ce qu'on affiche, deja trie
 * @param monRang        son rang a partir de 1, ou 0 s'il n'est pas classe
 * @param participants   combien de joueurs sont classes en tout
 * @param debutVoisinage l'indice ou commence son voisinage, ou -1
 */
public record PaquetDonneesClassement(List<Classement.Ligne> lignes, int monRang,
		int participants, int debutVoisinage) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetDonneesClassement> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("donnees_classement"));

	public static final StreamCodec<FriendlyByteBuf, PaquetDonneesClassement> CODEC =
			CustomPacketPayload.codec(PaquetDonneesClassement::ecrire,
					PaquetDonneesClassement::lire);

	/**
	 * Le plafond de lignes accepte a la lecture.
	 *
	 * <p>Un client ne fait jamais confiance a un nombre venu du reseau pour
	 * dimensionner une liste : c'est la porte ouverte a un paquet malveillant qui
	 * ferait reserver deux milliards d'entrees.
	 */
	private static final int PLAFOND = Classement.LIGNES_DU_HAUT + 2 * Classement.VOISINS + 8;

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.monRang);
		tampon.writeVarInt(this.participants);
		tampon.writeVarInt(this.debutVoisinage + 1);
		tampon.writeVarInt(Math.min(PLAFOND, this.lignes.size()));
		int ecrites = 0;
		for (Classement.Ligne ligne : this.lignes) {
			if (ecrites++ >= PLAFOND) {
				break;
			}
			tampon.writeUUID(ligne.qui());
			tampon.writeUtf(ligne.joueur(), 48);
			tampon.writeUtf(ligne.compagnon(), 48);
			tampon.writeUtf(ligne.espece(), 64);
			tampon.writeUtf(ligne.variante(), 64);
			tampon.writeVarInt(ligne.niveau());
			tampon.writeVarInt(ligne.xp());
			tampon.writeVarInt(ligne.jours());
		}
	}

	private static PaquetDonneesClassement lire(FriendlyByteBuf tampon) {
		int monRang = tampon.readVarInt();
		int participants = tampon.readVarInt();
		int debutVoisinage = tampon.readVarInt() - 1;
		int combien = Math.min(PLAFOND, Math.max(0, tampon.readVarInt()));

		List<Classement.Ligne> lignes = new ArrayList<>(combien);
		for (int i = 0; i < combien; i++) {
			lignes.add(new Classement.Ligne(
					tampon.readUUID(), tampon.readUtf(48), tampon.readUtf(48),
					tampon.readUtf(64), tampon.readUtf(64),
					tampon.readVarInt(), tampon.readVarInt(), tampon.readVarInt()));
		}
		return new PaquetDonneesClassement(List.copyOf(lignes), monRang, participants,
				debutVoisinage);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
