package fr.lhdp.compagnon.entite;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Ce que le compagnon te montre, et pour combien de temps.
 *
 * <h2>Pourquoi une classe entiere</h2>
 *
 * <p>Faire briller une creature est facile : une ligne. L'<b>eteindre</b> ne
 * l'est pas. Ce n'est pas notre entite : on ne peut pas lui ajouter un compteur,
 * et si on l'oublie allumee, elle brille pour le reste de la partie. Un zombie
 * qui reste dore a vie parce qu'un compagnon l'a signale une fois, c'est un bug
 * qu'on met des semaines a comprendre.
 *
 * <p>D'ou ce petit registre : ce qu'on a allume, et combien de ticks il reste.
 *
 * <h2>On n'eteint que ce qu'on a allume</h2>
 *
 * <p>Une creature qui brillait deja — une fleche de reperage, un effet de
 * potion, une commande du staff — n'entre pas dans le registre. Sans ce garde,
 * un compagnon qui signale une cible deja marquee par un professeur l'eteindrait
 * six secondes plus tard, au pire moment.
 *
 * <h2>Ce que ca ne coute pas</h2>
 *
 * <p>La table est vide la quasi-totalite du temps, et le tour de tick sort a la
 * premiere ligne quand elle l'est. Rien a sauvegarder non plus : une revelation
 * dure six secondes, elle n'a aucune raison de survivre a un redemarrage.
 */
public final class Reveles {

	private Reveles() {
	}

	/**
	 * Ce qui brille par notre faute, et le temps qui lui reste.
	 *
	 * <p>Une table ordinaire : tout se passe sur le fil du serveur — la creature
	 * est allumee depuis le tick d'un compagnon, et eteinte depuis le tick du
	 * serveur.
	 */
	private static final Map<Entity, Integer> montres = new HashMap<>();

	public static void enregistrer() {
		ServerTickEvents.END_SERVER_TICK.register(Reveles::tick);
	}

	/**
	 * Fais briller ca, le temps qu'on le trouve.
	 *
	 * <p>Sans effet sur ce qui brillait deja : ce n'est pas a nous de l'eteindre.
	 */
	public static void pendant(Entity quoi, int ticks) {
		if (quoi == null || ticks <= 0) {
			return;
		}
		if (quoi.hasGlowingTag() && !montres.containsKey(quoi)) {
			return;
		}
		quoi.setGlowingTag(true);
		montres.merge(quoi, ticks, Math::max);
	}

	/** Combien de creatures brillent en ce moment par notre faute. */
	public static int combien() {
		return montres.size();
	}

	private static void tick(MinecraftServer serveur) {
		if (montres.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<Entity, Integer>> parcours = montres.entrySet().iterator();
		while (parcours.hasNext()) {
			Map.Entry<Entity, Integer> entree = parcours.next();
			Entity quoi = entree.getKey();

			// Morte ou dechargee : on la lache, sinon la table garderait une
			// creature qui n'existe plus jusqu'a l'arret du serveur.
			if (quoi.isRemoved()) {
				parcours.remove();
				continue;
			}
			int reste = entree.getValue() - 1;
			if (reste <= 0) {
				quoi.setGlowingTag(false);
				parcours.remove();
			} else {
				entree.setValue(reste);
			}
		}
	}
}
