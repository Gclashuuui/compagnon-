package fr.lhdp.compagnon.objet;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Un jouet qu'on lance a son compagnon.
 *
 * <h2>Il ne se consomme pas</h2>
 *
 * <p>La balle part de la main, vole, rebondit, et le compagnon la rapporte. Elle
 * ne disparait jamais de l'inventaire : on ne joue pas a la balle avec un stock
 * de balles. C'est la meme, indefiniment, et c'est ce qui fait qu'on y tient.
 *
 * <p>En creatif comme en survie : rien a gerer, rien a fabriquer, rien a perdre.
 */
public class ObjetJouet extends Item {

	/** La force du lancer. Assez pour traverser une cour, pas un chateau. */
	private static final float ELAN = 0.85F;

	/** L'imprecision, en degres : un lancer parfait deux fois de suite ennuie. */
	private static final float DISPERSION = 1.2F;

	/** Entre deux lancers, pour ne pas vider une brouette de balles d'un clic. */
	private static final int REPOS = 12;

	public ObjetJouet(Properties proprietes) {
		super(proprietes);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level niveau, Player joueur, InteractionHand main) {
		ItemStack pile = joueur.getItemInHand(main);

		niveau.playSound(null, joueur.getX(), joueur.getY(), joueur.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F,
				0.4F / (niveau.getRandom().nextFloat() * 0.4F + 0.8F));

		if (!niveau.isClientSide()) {
			JouetLance jouet = new JouetLance(niveau, joueur);
			// L'objet lance est celui qu'on tient : la balle vole comme une balle,
			// l'os comme un os, sans une ligne de plus.
			jouet.setItem(pile.copyWithCount(1));
			jouet.shootFromRotation(joueur, joueur.getXRot(), joueur.getYRot(),
					0.0F, ELAN, DISPERSION);
			niveau.addFreshEntity(jouet);
		}

		joueur.getCooldowns().addCooldown(this, REPOS);
		joueur.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
		// La pile reste entiere : SUCCESS et non CONSUME, et rien n'est retire.
		return InteractionResultHolder.sidedSuccess(pile, niveau.isClientSide());
	}
}
