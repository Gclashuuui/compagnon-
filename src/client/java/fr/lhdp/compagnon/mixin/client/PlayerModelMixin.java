package fr.lhdp.compagnon.mixin.client;

import fr.lhdp.compagnon.client.Caresses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anime le bras du joueur pendant une caresse.
 *
 * <p>C'est le seul endroit du mod qui touche a la facon dont Minecraft dessine
 * le joueur, et donc le seul qui peut entrer en conflit avec un autre mod qui
 * ferait la meme chose. Il est volontairement minuscule et additif : il
 * n'annule rien, il repose un bras a la toute fin, apres que le jeu a fini son
 * travail.
 *
 * <p>Si {@link Caresses} s'eteint, cette classe ne fait plus rien du tout.
 *
 * <p>On vise {@link PlayerModel} et pas {@link HumanoidModel} : sinon les
 * zombies et les squelettes passeraient par ici pour rien.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {

	/**
	 * La manche droite. Elle, elle appartient bien a {@link PlayerModel}.
	 *
	 * <p><b>Le chapeau, lui, n'y est pas</b> — il est declare un cran plus haut,
	 * dans {@link HumanoidModel}. On l'ombrageait ici comme s'il etait dans
	 * {@code PlayerModel}, et Mixin ne le trouvait pas :
	 *
	 * <pre>@Shadow field field_3394 was not located in the target class</pre>
	 *
	 * <p>Et un {@code @Shadow} introuvable ne rate pas qu'un champ : il fait
	 * echouer <b>tout le mixin</b>. L'animation de caresse ne s'appliquait donc
	 * plus du tout — silencieusement, avec pour seule trace une ligne d'attention
	 * dans le journal au demarrage.
	 *
	 * <p>Il n'y avait rien a ombrager : cette classe <b>etend</b> HumanoidModel,
	 * donc {@code hat}, {@code head} et {@code rightArm} sont deja a elle par
	 * heritage.
	 */
	@Shadow
	@Final
	public ModelPart rightSleeve;

	private PlayerModelMixin(ModelPart racine) {
		super(racine);
	}

	@Inject(
			method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
			at = @At("TAIL"))
	private void compagnon$animerLaCaresse(T entite, float balancier, float amplitude,
			float age, float lacet, float tangage, CallbackInfo info) {

		if (!(entite instanceof Player joueur)) {
			return;
		}

		// L'avancee dans le tick : sans elle le geste avancerait par a-coups de
		// vingt images par seconde au lieu d'etre fluide.
		float partiel = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);

		if (Caresses.animer(joueur, this.rightArm, this.head, partiel)) {
			// La manche et le chapeau suivent, sinon ils restent en arriere.
			this.rightSleeve.copyFrom(this.rightArm);
			this.hat.copyFrom(this.head);
		}
	}
}
