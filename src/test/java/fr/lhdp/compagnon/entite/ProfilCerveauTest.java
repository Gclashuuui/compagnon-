package fr.lhdp.compagnon.entite;

import com.google.gson.JsonParser;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.fiche.Humeur;
import fr.lhdp.compagnon.fiche.Mode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilCerveauTest {

	@Test
	void leFichierDesactiveUnNoeudEtRegleLaPerception() {
		ProfilCerveau profil = ProfilCerveau.lire("test",
				JsonParser.parseString("""
						{
						  "noeuds_desactives": ["vol_demande", "cadeau_spontane"],
						  "perception": {"curiosite": 1.4},
						  "rythme": {"nocturne": true}
						}
						""").getAsJsonObject());

		assertFalse(profil.autorise(CerveauComportement.Noeud.VOL_DEMANDE));
		assertFalse(profil.autorise(CerveauComportement.Noeud.CADEAU_SPONTANE));
		assertEquals(1.4F, profil.curiosite());
		assertTrue(profil.nocturne());
	}

	@Test
	void lesSeuilsInvalidesSontRefusesAuChargement() {
		assertThrows(IllegalArgumentException.class, () -> ProfilCerveau.lire("casse",
				JsonParser.parseString("""
						{"locomotion":{"course_entree":0.3,"course_sortie":0.5}}
						""").getAsJsonObject()));
	}

	@Test
	void leSeuilDuFichierChangeVraimentLAnimationChoisie() {
		ProfilCerveau lent = new ProfilCerveau("lent",
				Set.of(CerveauComportement.Noeud.values()),
				0.9F, 0.7F, -0.075D, -0.015D, 3,
				120, 1.0F, 1.0F, true, true, false);
		CerveauAnimation cerveau = new CerveauAnimation();
		CerveauAnimation.Observation mouvement = new CerveauAnimation.Observation(
				false, false, false, Mode.RESTE, true, 0.8F, 0.0D, Humeur.MOYEN);

		assertEquals(Espece.MARCHE, cerveau.choisir(mouvement,
				Set.of(Espece.IMMOBILE, Espece.MARCHE, Espece.COURSE)::contains, lent).role());
	}
}
