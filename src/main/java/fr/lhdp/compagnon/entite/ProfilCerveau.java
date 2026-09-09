package fr.lhdp.compagnon.entite;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumSet;
import java.util.Set;

/**
 * Les réglages du cerveau d'une famille de créatures.
 *
 * <p>Les buts restent du Java volontairement simple et prévisible. Ce fichier
 * décide lesquels existent, comment le mouvement change d'allure et à quel
 * rythme la créature réagit au monde. C'est une IA de jeu : déterministe,
 * locale, sans service externe et sans coût par requête.
 */
public record ProfilCerveau(
		String id,
		Set<CerveauComportement.Noeud> noeuds,
		float courseEntree,
		float courseSortie,
		double planeEntree,
		double planeSortie,
		int stabiliteVolTicks,
		int chanceGesteNaturel,
		float curiosite,
		float sociabilite,
		boolean reagitPluie,
		boolean reagitFroid,
		boolean nocturne) {

	public ProfilCerveau {
		noeuds = Set.copyOf(noeuds);
		if (courseSortie < 0.0F || courseEntree <= courseSortie) {
			throw new IllegalArgumentException("cerveau " + id
					+ " : course_entree doit être supérieure à course_sortie");
		}
		if (planeEntree >= planeSortie) {
			throw new IllegalArgumentException("cerveau " + id
					+ " : plane_entree doit être inférieure à plane_sortie");
		}
		if (stabiliteVolTicks < 1 || chanceGesteNaturel < 1) {
			throw new IllegalArgumentException("cerveau " + id + " : un délai doit être positif");
		}
		if (curiosite < 0.0F || sociabilite < 0.0F) {
			throw new IllegalArgumentException("cerveau " + id + " : un multiplicateur est négatif");
		}
	}

	public boolean autorise(CerveauComportement.Noeud noeud) {
		return this.noeuds.contains(noeud);
	}

	public static ProfilCerveau parDefaut(boolean vole) {
		EnumSet<CerveauComportement.Noeud> noeuds = EnumSet.allOf(
				CerveauComportement.Noeud.class);
		if (!vole) {
			noeuds.remove(CerveauComportement.Noeud.VOL_DEMANDE);
		}
		return new ProfilCerveau(vole ? "aerien" : "terrestre", noeuds,
				0.68F, 0.46F, -0.075D, -0.015D, 3,
				120, 1.0F, 1.0F, true, true, false);
	}

	static ProfilCerveau lire(String id, JsonObject json) {
		// Un profil décrit un tempérament, pas l'anatomie de l'espèce qui le
		// choisira. La capacité de voler reste portée par Espece.vole() : un même
		// profil (par exemple « nocturne ») peut donc servir à un oiseau comme à
		// une créature terrestre.
		ProfilCerveau base = parDefaut(true);
		EnumSet<CerveauComportement.Noeud> noeuds = EnumSet.copyOf(base.noeuds());
		if (json.has("noeuds_desactives")) {
			for (var element : json.getAsJsonArray("noeuds_desactives")) {
				String nom = element.getAsString().trim().toUpperCase(java.util.Locale.ROOT);
				try {
					noeuds.remove(CerveauComportement.Noeud.valueOf(nom));
				} catch (IllegalArgumentException inconnue) {
					throw new IllegalArgumentException("cerveau " + id
							+ " : nœud inconnu \"" + element.getAsString() + "\"");
				}
			}
		}

		JsonObject locomotion = objet(json, "locomotion");
		JsonObject perception = objet(json, "perception");
		JsonObject rythme = objet(json, "rythme");
		return new ProfilCerveau(id, noeuds,
				flottant(locomotion, "course_entree", base.courseEntree()),
				flottant(locomotion, "course_sortie", base.courseSortie()),
				reel(locomotion, "plane_entree", base.planeEntree()),
				reel(locomotion, "plane_sortie", base.planeSortie()),
				entier(locomotion, "stabilite_vol_ticks", base.stabiliteVolTicks()),
				entier(rythme, "geste_naturel_une_chance_sur", base.chanceGesteNaturel()),
				flottant(perception, "curiosite", base.curiosite()),
				flottant(perception, "sociabilite", base.sociabilite()),
				booleen(rythme, "reagit_pluie", base.reagitPluie()),
				booleen(rythme, "reagit_froid", base.reagitFroid()),
				booleen(rythme, "nocturne", base.nocturne()));
	}

	public void ecrire(FriendlyByteBuf tampon) {
		tampon.writeUtf(this.id);
		tampon.writeVarInt(this.noeuds.size());
		for (CerveauComportement.Noeud noeud : this.noeuds) {
			tampon.writeEnum(noeud);
		}
		tampon.writeFloat(this.courseEntree);
		tampon.writeFloat(this.courseSortie);
		tampon.writeDouble(this.planeEntree);
		tampon.writeDouble(this.planeSortie);
		tampon.writeVarInt(this.stabiliteVolTicks);
		tampon.writeVarInt(this.chanceGesteNaturel);
		tampon.writeFloat(this.curiosite);
		tampon.writeFloat(this.sociabilite);
		tampon.writeBoolean(this.reagitPluie);
		tampon.writeBoolean(this.reagitFroid);
		tampon.writeBoolean(this.nocturne);
	}

	public static ProfilCerveau lire(FriendlyByteBuf tampon) {
		String id = tampon.readUtf();
		int combien = tampon.readVarInt();
		EnumSet<CerveauComportement.Noeud> noeuds = EnumSet.noneOf(
				CerveauComportement.Noeud.class);
		for (int i = 0; i < combien; i++) {
			noeuds.add(tampon.readEnum(CerveauComportement.Noeud.class));
		}
		return new ProfilCerveau(id, noeuds,
				tampon.readFloat(), tampon.readFloat(), tampon.readDouble(), tampon.readDouble(),
				tampon.readVarInt(), tampon.readVarInt(), tampon.readFloat(), tampon.readFloat(),
				tampon.readBoolean(), tampon.readBoolean(), tampon.readBoolean());
	}

	private static JsonObject objet(JsonObject parent, String cle) {
		return parent.has(cle) ? parent.getAsJsonObject(cle) : new JsonObject();
	}

	private static float flottant(JsonObject objet, String cle, float defaut) {
		return objet.has(cle) ? objet.get(cle).getAsFloat() : defaut;
	}

	private static double reel(JsonObject objet, String cle, double defaut) {
		return objet.has(cle) ? objet.get(cle).getAsDouble() : defaut;
	}

	private static int entier(JsonObject objet, String cle, int defaut) {
		return objet.has(cle) ? objet.get(cle).getAsInt() : defaut;
	}

	private static boolean booleen(JsonObject objet, String cle, boolean defaut) {
		return objet.has(cle) ? objet.get(cle).getAsBoolean() : defaut;
	}
}
