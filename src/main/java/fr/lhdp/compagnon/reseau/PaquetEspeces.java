package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Partie;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Le catalogue des especes, envoye au client quand il se connecte et apres
 * chaque {@code /reload}.
 *
 * <p>Le client ne lit aucun fichier d'espece : c'est le serveur qui fait
 * autorite, ici comme ailleurs. Sans ce paquet, le client ne saurait ni quelle
 * geometrie dessiner, ni quelle animation jouer.
 */
public record PaquetEspeces(List<Espece> especes) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetEspeces> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("especes"));

	public static final StreamCodec<FriendlyByteBuf, PaquetEspeces> CODEC =
			CustomPacketPayload.codec(PaquetEspeces::ecrire, PaquetEspeces::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.especes.size());
		for (Espece espece : this.especes) {
			tampon.writeUtf(espece.nom());
			tampon.writeUtf(espece.titre());
			tampon.writeUtf(espece.geometrie().toString());
			tampon.writeUtf(espece.animations().toString());
			tampon.writeUtf(espece.varianteParDefaut());
			ecrireVariantes(tampon, espece.variantes());
			tampon.writeMap(espece.locomotion(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
			tampon.writeMap(espece.reactions(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
			tampon.writeMap(espece.sons(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
			tampon.writeFloat(espece.largeur());
			tampon.writeFloat(espece.hauteur());
			tampon.writeBoolean(espece.vole());
			tampon.writeUtf(espece.nomVocal());
			// La selle voyage : c'est le CLIENT qui dessine le cavalier dessus, et il
			// ne peut pas deviner ou elle est sur un modele qu'il ne lit pas.
			tampon.writeVarInt(espece.monterAuNiveau());
			tampon.writeUtf(espece.devient());
			tampon.writeVarInt(espece.devientAuNiveau());
			tampon.writeBoolean(espece.selle() != null);
			if (espece.selle() != null) {
				tampon.writeDouble(espece.selle().droite());
				tampon.writeDouble(espece.selle().haut());
				tampon.writeDouble(espece.selle().avant());
			}

			// Les parties partent aussi : le client en a besoin pour prevoir ses
			// propres deplacements. Sans elles, il traverserait puis serait remis
			// en place par le serveur — l'effet elastique.
			tampon.writeVarInt(espece.parties().size());
			for (Partie partie : espece.parties()) {
				tampon.writeUtf(partie.nom());
				tampon.writeDouble(partie.droite());
				tampon.writeDouble(partie.haut());
				tampon.writeDouble(partie.avant());
				tampon.writeDouble(partie.largeur());
				tampon.writeDouble(partie.hauteur());
			}
		}
	}

	private static PaquetEspeces lire(FriendlyByteBuf tampon) {
		int combien = tampon.readVarInt();
		List<Espece> especes = new ArrayList<>(combien);

		for (int i = 0; i < combien; i++) {
			String nom = tampon.readUtf();
			String titre = tampon.readUtf();
			ResourceLocation geometrie = ResourceLocation.parse(tampon.readUtf());
			ResourceLocation animations = ResourceLocation.parse(tampon.readUtf());
			String varianteParDefaut = tampon.readUtf();
			Map<String, ResourceLocation> variantes = lireVariantes(tampon);
			Map<String, String> locomotion =
					tampon.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);
			Map<String, String> reactions =
					tampon.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);
			Map<String, String> sons =
					tampon.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);

			float largeur = tampon.readFloat();
			float hauteur = tampon.readFloat();
			boolean vole = tampon.readBoolean();
			String nomVocal = tampon.readUtf();
			int monterAuNiveau = tampon.readVarInt();
			String devient = tampon.readUtf();
			int devientAuNiveau = tampon.readVarInt();
			Partie selle = tampon.readBoolean()
					? new Partie("selle", tampon.readDouble(), tampon.readDouble(),
						tampon.readDouble(), 0.0D, 0.0D)
					: null;

			int combienParties = tampon.readVarInt();
			List<Partie> parties = new ArrayList<>(combienParties);
			for (int p = 0; p < combienParties; p++) {
				parties.add(new Partie(
						tampon.readUtf(),
						tampon.readDouble(), tampon.readDouble(), tampon.readDouble(),
						tampon.readDouble(), tampon.readDouble()));
			}

			especes.add(new Espece(nom, titre, geometrie, animations, varianteParDefaut,
					java.util.Collections.unmodifiableMap(variantes),
					Map.copyOf(locomotion), Map.copyOf(reactions), Map.copyOf(sons), largeur, hauteur,
					List.copyOf(parties), vole, nomVocal, selle, monterAuNiveau,
				devient, devientAuNiveau));
		}

		return new PaquetEspeces(List.copyOf(especes));
	}

	private static void ecrireVariantes(FriendlyByteBuf tampon, Map<String, ResourceLocation> variantes) {
		tampon.writeVarInt(variantes.size());
		variantes.forEach((nom, texture) -> {
			tampon.writeUtf(nom);
			tampon.writeUtf(texture.toString());
		});
	}

	private static Map<String, ResourceLocation> lireVariantes(FriendlyByteBuf tampon) {
		int combien = tampon.readVarInt();
		Map<String, ResourceLocation> variantes = new java.util.LinkedHashMap<>(combien);
		for (int i = 0; i < combien; i++) {
			String nom = tampon.readUtf();
			variantes.put(nom, ResourceLocation.parse(tampon.readUtf()));
		}
		return variantes;
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
