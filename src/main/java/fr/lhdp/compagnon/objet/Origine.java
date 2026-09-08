package fr.lhdp.compagnon.objet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Ce que porte l'objet remis au joueur : l'espece et la variante, choisies par
 * l'equipe au moment ou elle le donne.
 *
 * <p>C'est tout ce que le mod sait de la vente. Le reste — qui paie, qui recoit,
 * a quel prix — se passe sur Discord, et le mod n'en sait rien.
 *
 * @param espece   le nom d'un fichier de {@code assets/compagnon/especes/}
 * @param variante le nom d'une variante de cette espece
 */
public record Origine(String espece, String variante) {

	public static final Codec<Origine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("espece").forGetter(Origine::espece),
			Codec.STRING.fieldOf("variante").forGetter(Origine::variante)
	).apply(instance, Origine::new));

	public static final StreamCodec<ByteBuf, Origine> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Origine::espece,
			ByteBufCodecs.STRING_UTF8, Origine::variante,
			Origine::new);
}
