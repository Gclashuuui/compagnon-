package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.espece.Ancrage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Le serveur envoie ou se posent les objets, espece par espece.
 *
 * <h2>Pourquoi ca voyage</h2>
 *
 * <p>C'est le client qui dessine la balle dans la gueule, et il ne peut pas le
 * deviner : les reglages vivent dans un fichier du <b>serveur</b>, que l'equipe
 * modifie en jeu. Sans cet envoi, regler une position en direct n'aurait aucun
 * effet chez les joueurs jusqu'au prochain redemarrage.
 *
 * <p>La table entiere plutot qu'une ligne : elle tient en quelques centaines
 * d'octets, part une fois a la connexion et une fois apres chaque reglage. Un
 * envoi differentiel couterait plus de code que de reseau.
 */
public record PaquetAncrages(Map<String, Map<String, Ancrage>> ancrages)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PaquetAncrages> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("ancrages"));

	public static final StreamCodec<FriendlyByteBuf, PaquetAncrages> CODEC =
			CustomPacketPayload.codec(PaquetAncrages::ecrire, PaquetAncrages::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeVarInt(this.ancrages.size());
		for (Map.Entry<String, Map<String, Ancrage>> espece : this.ancrages.entrySet()) {
			tampon.writeUtf(espece.getKey());
			tampon.writeVarInt(espece.getValue().size());
			for (Map.Entry<String, Ancrage> emplacement : espece.getValue().entrySet()) {
				tampon.writeUtf(emplacement.getKey());
				ecrireUn(tampon, emplacement.getValue());
			}
		}
	}

	private static PaquetAncrages lire(FriendlyByteBuf tampon) {
		int combienDEspeces = tampon.readVarInt();
		Map<String, Map<String, Ancrage>> tout = new LinkedHashMap<>(combienDEspeces);
		for (int i = 0; i < combienDEspeces; i++) {
			String espece = tampon.readUtf();
			int combien = tampon.readVarInt();
			Map<String, Ancrage> pourElle = new LinkedHashMap<>(combien);
			for (int j = 0; j < combien; j++) {
				pourElle.put(tampon.readUtf(), lireUn(tampon));
			}
			tout.put(espece, pourElle);
		}
		return new PaquetAncrages(tout);
	}

	static void ecrireUn(FriendlyByteBuf tampon, Ancrage ancrage) {
		tampon.writeUtf(ancrage.os());
		tampon.writeDouble(ancrage.x());
		tampon.writeDouble(ancrage.y());
		tampon.writeDouble(ancrage.z());
		tampon.writeFloat(ancrage.tangage());
		tampon.writeFloat(ancrage.lacet());
		tampon.writeFloat(ancrage.roulis());
		tampon.writeFloat(ancrage.echelle());
	}

	static Ancrage lireUn(FriendlyByteBuf tampon) {
		return new Ancrage(tampon.readUtf(),
				tampon.readDouble(), tampon.readDouble(), tampon.readDouble(),
				tampon.readFloat(), tampon.readFloat(), tampon.readFloat(),
				tampon.readFloat());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
