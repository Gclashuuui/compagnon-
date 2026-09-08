package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.livre.EntreeMission;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.Moment;
import fr.lhdp.compagnon.livre.DonneesLivre;
import fr.lhdp.compagnon.livre.EntreeCompetence;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La reponse du serveur : le contenu du livre.
 *
 * @param combien le nombre de compagnons du joueur, pour qu'il puisse passer de
 *                l'un a l'autre
 * @param index   celui qui est affiche
 * @param donnees tout ce qu'il y a a montrer
 */
public record PaquetDonneesLivre(int combien, int index, DonneesLivre donnees,
		java.util.List<String> compagnons)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetDonneesLivre> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("donnees_livre"));

	public static final StreamCodec<FriendlyByteBuf, PaquetDonneesLivre> CODEC =
			CustomPacketPayload.codec(PaquetDonneesLivre::ecrire, PaquetDonneesLivre::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.combien);
		tampon.writeVarInt(this.index);
		// Les noms de toutes ses betes : c'est ce qui permet au livre d'afficher
		// un onglet par compagnon, comme la roue, sans un aller-retour de plus.
		tampon.writeVarInt(this.compagnons.size());
		for (String compagnon : this.compagnons) {
			tampon.writeUtf(compagnon, 64);
		}

		DonneesLivre d = this.donnees;
		tampon.writeUtf(d.nom());
		tampon.writeUtf(d.espece());
		tampon.writeUtf(d.variante());

		tampon.writeVarInt(d.niveau());
		tampon.writeVarInt(d.niveauMax());
		tampon.writeVarInt(d.xp());
		tampon.writeVarInt(d.xpDuNiveau());
		tampon.writeInt(d.xpDuSuivant());
		tampon.writeByte(d.rituelsDuJour());

		for (Barre barre : Barre.values()) {
			tampon.writeFloat(d.barres().getOrDefault(barre, 0.0F));
		}

		tampon.writeUtf(d.humeurLibelle());
		tampon.writeUtf(d.humeurBouille());
		tampon.writeUtf(d.mode());

		tampon.writeUtf(d.boboNom());
		tampon.writeUtf(d.boboDescription());
		tampon.writeUtf(d.boboRemede());

		tampon.writeLong(d.dateObtention());
		tampon.writeLong(d.ticksEnsemble());

		tampon.writeVarInt(d.moments().size());
		for (Moment moment : d.moments()) {
			tampon.writeUtf(moment.cle());
			tampon.writeLong(moment.date());
		}

		tampon.writeVarInt(d.motsAppris().size());
		for (Map.Entry<String, String> appris : d.motsAppris().entrySet()) {
			tampon.writeUtf(appris.getKey(), 64);
			tampon.writeUtf(appris.getValue(), 128);
		}

		// Les competences : ce qu'il a, et ce qu'il pourrait avoir. Les EFFETS ne
		// partent pas — le client n'a pas a les connaitre pour dessiner une page,
		// et les lui envoyer ne servirait qu'a lui permettre de mentir dessus.
		tampon.writeVarInt(d.pointsDeCompetence());
		tampon.writeVarInt(d.competences().size());
		for (EntreeCompetence competence : d.competences()) {
			tampon.writeUtf(competence.id(), 64);
			tampon.writeUtf(competence.nom(), 128);
			tampon.writeUtf(competence.description(), 256);
			tampon.writeVarInt(competence.niveauRequis());
			tampon.writeBoolean(competence.prise());
		}

		tampon.writeVarInt(d.compteurs().size());
		d.compteurs().forEach((cle, valeur) -> {
			tampon.writeUtf(cle);
			tampon.writeVarInt(valeur);
		});

		tampon.writeVarInt(d.connait().size());
		for (String nom : d.connait()) {
			tampon.writeUtf(nom);
		}

		tampon.writeVarInt(d.missions().size());
		for (EntreeMission mission : d.missions()) {
			tampon.writeUtf(mission.texte(), 128);
			tampon.writeVarInt(mission.quantite());
			tampon.writeVarInt(mission.faits());
			tampon.writeBoolean(mission.finie());
			tampon.writeBoolean(mission.longue());
		}
		tampon.writeBoolean(d.peutEcarter());
		tampon.writeVarInt(d.monterAuNiveau());
	}

	private static PaquetDonneesLivre lire(FriendlyByteBuf tampon) {
		int combien = tampon.readVarInt();
		int index = tampon.readVarInt();

		int nombreDeCompagnons = tampon.readVarInt();
		List<String> compagnons = new java.util.ArrayList<>(nombreDeCompagnons);
		for (int i = 0; i < nombreDeCompagnons; i++) {
			compagnons.add(tampon.readUtf(64));
		}

		String nom = tampon.readUtf();
		String espece = tampon.readUtf();
		String variante = tampon.readUtf();

		int niveau = tampon.readVarInt();
		int niveauMax = tampon.readVarInt();
		int xp = tampon.readVarInt();
		int xpDuNiveau = tampon.readVarInt();
		int xpDuSuivant = tampon.readInt();
		int rituelsDuJour = tampon.readUnsignedByte();

		Map<Barre, Float> barres = new EnumMap<>(Barre.class);
		for (Barre barre : Barre.values()) {
			barres.put(barre, tampon.readFloat());
		}

		String humeurLibelle = tampon.readUtf();
		String humeurBouille = tampon.readUtf();
		String mode = tampon.readUtf();

		String boboNom = tampon.readUtf();
		String boboDescription = tampon.readUtf();
		String boboRemede = tampon.readUtf();

		long dateObtention = tampon.readLong();
		long ticksEnsemble = tampon.readLong();

		int combienMoments = tampon.readVarInt();
		List<Moment> moments = new ArrayList<>(combienMoments);
		for (int i = 0; i < combienMoments; i++) {
			moments.add(new Moment(tampon.readUtf(), tampon.readLong()));
		}

		int combienMots = tampon.readVarInt();
		Map<String, String> motsAppris = new LinkedHashMap<>();
		for (int i = 0; i < combienMots; i++) {
			motsAppris.put(tampon.readUtf(64), tampon.readUtf(128));
		}

		int points = tampon.readVarInt();
		int combienCompetences = tampon.readVarInt();
		List<EntreeCompetence> competences = new ArrayList<>(combienCompetences);
		for (int i = 0; i < combienCompetences; i++) {
			competences.add(new EntreeCompetence(
					tampon.readUtf(64), tampon.readUtf(128), tampon.readUtf(256),
					tampon.readVarInt(), tampon.readBoolean()));
		}

		int combienCompteurs = tampon.readVarInt();
		Map<String, Integer> compteurs = new LinkedHashMap<>();
		for (int i = 0; i < combienCompteurs; i++) {
			compteurs.put(tampon.readUtf(), tampon.readVarInt());
		}

		int combienConnus = tampon.readVarInt();
		List<String> connait = new ArrayList<>(combienConnus);
		for (int i = 0; i < combienConnus; i++) {
			connait.add(tampon.readUtf());
		}

		// Huit au plus : trois courtes, une longue, et de la marge. Le plafond
		// est fixe ici et jamais pris du reseau — un client ne dimensionne pas une
		// liste avec un nombre venu d'ailleurs.
		int combienMissions = Math.min(8, Math.max(0, tampon.readVarInt()));
		List<EntreeMission> missions = new ArrayList<>(combienMissions);
		for (int i = 0; i < combienMissions; i++) {
			missions.add(new EntreeMission(tampon.readUtf(128), tampon.readVarInt(),
					tampon.readVarInt(), tampon.readBoolean(), tampon.readBoolean()));
		}
		boolean peutEcarter = tampon.readBoolean();
		int monterAuNiveau = tampon.readVarInt();

		return new PaquetDonneesLivre(combien, index, new DonneesLivre(
				nom, espece, variante,
				niveau, niveauMax, xp, xpDuNiveau, xpDuSuivant, rituelsDuJour,
				Map.copyOf(barres),
				humeurLibelle, humeurBouille, mode,
				boboNom, boboDescription, boboRemede,
				dateObtention, ticksEnsemble,
				List.copyOf(moments), Map.copyOf(compteurs), Map.copyOf(motsAppris),
				List.copyOf(competences), points,
				List.copyOf(connait),
				List.copyOf(missions), peutEcarter, monterAuNiveau),
				List.copyOf(compagnons));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
