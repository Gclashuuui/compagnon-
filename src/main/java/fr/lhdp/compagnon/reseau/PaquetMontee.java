package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** Les quelques donnees dont le client a besoin pour la petite annonce de niveau. */
public record PaquetMontee(String nom, int niveau, List<String> gestes,
		int points, boolean monte) implements CustomPacketPayload {

	private static final int GESTES_MAXIMUM = 2;

	public PaquetMontee {
		gestes = List.copyOf(gestes.subList(0, Math.min(GESTES_MAXIMUM, gestes.size())));
	}

	public static final CustomPacketPayload.Type<PaquetMontee> TYPE =
			new CustomPacketPayload.Type<>(Compagnon.id("montee"));

	public static final StreamCodec<FriendlyByteBuf, PaquetMontee> CODEC =
			CustomPacketPayload.codec(PaquetMontee::ecrire, PaquetMontee::lire);

	private void ecrire(FriendlyByteBuf tampon) {
		tampon.writeUtf(this.nom, 24);
		tampon.writeVarInt(this.niveau);
		tampon.writeVarInt(this.gestes.size());
		for (String geste : this.gestes) {
			tampon.writeUtf(geste, 128);
		}
		tampon.writeVarInt(this.points);
		tampon.writeBoolean(this.monte);
	}

	private static PaquetMontee lire(FriendlyByteBuf tampon) {
		String nom = tampon.readUtf(24);
		int niveau = tampon.readVarInt();
		int combien = Math.min(tampon.readVarInt(), GESTES_MAXIMUM);
		List<String> gestes = new ArrayList<>(combien);
		for (int i = 0; i < combien; i++) {
			gestes.add(tampon.readUtf(128));
		}
		return new PaquetMontee(nom, niveau, gestes,
				tampon.readVarInt(), tampon.readBoolean());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
