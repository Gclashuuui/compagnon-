package fr.lhdp.compagnon.fiche;

import fr.lhdp.compagnon.reseau.PaquetJauges;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ce qui alimente le petit panneau du coin de l'ecran.
 *
 * <h2>La regle : on n'envoie que ce qui a change</h2>
 *
 * <p>Un panneau qui se met a jour est un paquet qui part. Le rafraichir a
 * intervalle fixe pour mille joueurs, ce serait mille paquets par seconde pour
 * afficher des barres qui n'ont pas bouge d'un pixel.
 *
 * <p>On garde donc ce qu'on a envoye en dernier, et on ne renvoie que si la
 * liste arrondie differe. Les barres perdent moins d'un point par minute : en
 * pratique, ce sont <b>quelques paquets par minute et par joueur</b>.
 *
 * <p>Ce souvenir n'est qu'une optimisation. S'il se perd — au redemarrage, ou
 * parce qu'un joueur se reconnecte — le pire qui arrive est un paquet de trop.
 */
public final class Jauges {

	/**
	 * Combien de compagnons le panneau peut montrer.
	 *
	 * <p>Trois, comme le nombre qu'on peut sortir en meme temps par defaut. Au-dela
	 * le panneau prendrait le quart de l'ecran, ce qui est exactement ce qu'on ne
	 * veut pas d'un panneau discret.
	 */
	private static final int MAXIMUM = 3;

	/** La derniere liste envoyee a chaque joueur, pour ne pas la renvoyer. */
	private static final Map<UUID, List<PaquetJauges.Jauge>> dernieres = new ConcurrentHashMap<>();

	private Jauges() {
	}

	/**
	 * Envoie l'etat a ce joueur, s'il a change.
	 *
	 * @param siennes ses fiches, deja en main par l'appelant : on ne va pas les
	 *                rechercher, ce serait payer deux fois le meme parcours
	 */
	public static void rafraichir(ServerPlayer joueur, List<FicheCompagnon> siennes) {
		List<PaquetJauges.Jauge> maintenant = new ArrayList<>(MAXIMUM);
		for (FicheCompagnon fiche : siennes) {
			if (!fiche.sorti()) {
				continue;
			}
			if (maintenant.size() >= MAXIMUM) {
				break;
			}
			maintenant.add(new PaquetJauges.Jauge(fiche.nom(),
					arrondi(fiche.barre(Barre.FAIM)),
					arrondi(fiche.barre(Barre.ENERGIE)),
					fiche.humeur().ordinal(),
					leMonte(joueur, fiche)));
		}

		List<PaquetJauges.Jauge> avant = dernieres.get(joueur.getUUID());
		if (maintenant.equals(avant)) {
			return;
		}

		// Une liste vide part quand meme, une fois : c'est elle qui dit au client
		// de ranger le panneau quand le dernier compagnon rentre.
		if (maintenant.isEmpty()) {
			dernieres.remove(joueur.getUUID());
		} else {
			dernieres.put(joueur.getUUID(), List.copyOf(maintenant));
		}
		if (avant == null && maintenant.isEmpty()) {
			return;
		}
		ServerPlayNetworking.send(joueur, new PaquetJauges(List.copyOf(maintenant)));
	}

	/** Un joueur s'en va : on oublie ce qu'on lui avait envoye. */
	public static void oublier(UUID joueur) {
		dernieres.remove(joueur);
	}

	/**
	 * De 0 a 100.
	 *
	 * <p>Arrondi volontairement grossier : c'est lui qui evite qu'un paquet parte
	 * parce qu'une barre a perdu un centieme de point.
	 */
	/**
	 * Ce joueur est-il en train de monter cette bete-la ?
	 *
	 * <p>On compare les identifiants de fiche, jamais les noms : deux betes
	 * peuvent porter le meme nom, et c'est au joueur d'en decider.
	 */
	private static boolean leMonte(ServerPlayer joueur, FicheCompagnon fiche) {
		return joueur.getVehicle() instanceof fr.lhdp.compagnon.entite.CompagnonEntity monture
			&& fiche.id().equals(monture.ficheId());
	}

	private static int arrondi(float valeur) {
		return Math.max(0, Math.min(100, Math.round(valeur)));
	}
}
