package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.objet.Objets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Il regarde ce que tu tiens.
 *
 * <h2>Ce que ca change</h2>
 *
 * <p>Tu sors un morceau de viande, il tourne la tete. Tu le ranges, il repart a
 * ses affaires. C'est un tout petit geste, et c'est peut-etre celui qui fait le
 * plus pour l'impression qu'une bete <b>te voit</b>.
 *
 * <p>Rien de neuf n'est stocke : ce que le joueur tient en main, le serveur le
 * sait deja. Ce but ne fait que le regarder.
 *
 * <h2>Il ne mendie pas</h2>
 *
 * <p>Il ne s'approche pas, il ne reclame pas, il ne bloque rien. Il <b>regarde</b>,
 * et il continue ce qu'il faisait — le but ne prend que le controle du regard,
 * jamais celui des pattes.
 *
 * <p>C'est la difference entre une bete attentive et une bete collante. La
 * seconde devient vite insupportable, et personne ne saurait dire pourquoi.
 */
public class RegarderCeQueTuTiensGoal extends Goal {

	/** Jusqu'ou il remarque ce que tu tiens, en blocs. */
	private static final double PORTEE = 8.0D;

	/** Combien de temps il regarde avant de se lasser, en ticks. */
	private static final int DUREE = 60;

	/** Il ne verifie pas a chaque tick : une demi-seconde suffit. */
	private static final int ENTRE_DEUX_COUPS_D_OEIL = 10;

	private final CompagnonEntity compagnon;

	private LivingEntity celuiQuiTient;
	private int reste;
	private int avantDeRegarder;

	public RegarderCeQueTuTiensGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		// LOOK seulement : il tourne la tete, il ne quitte pas sa place. C'est ce
		// qui permet a ce but de cohabiter avec tous les autres sans jamais rien
		// interrompre.
		setFlags(EnumSet.of(Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (--this.avantDeRegarder > 0) {
			return false;
		}
		this.avantDeRegarder = ENTRE_DEUX_COUPS_D_OEIL;

		LivingEntity maitre = this.compagnon.getOwner();
		if (maitre == null || maitre.level() != this.compagnon.level()
				|| this.compagnon.distanceToSqr(maitre) > PORTEE * PORTEE) {
			return false;
		}
		if (!luiPlait(maitre.getItemInHand(InteractionHand.MAIN_HAND))) {
			return false;
		}
		this.celuiQuiTient = maitre;
		return true;
	}

	@Override
	public void start() {
		this.reste = DUREE;
	}

	@Override
	public boolean canContinueToUse() {
		return this.reste > 0
				&& this.celuiQuiTient != null
				&& this.celuiQuiTient.isAlive()
				// Il range l'objet : le charme est rompu tout de suite, et c'est
				// justement ce qui rend le geste lisible.
				&& luiPlait(this.celuiQuiTient.getItemInHand(InteractionHand.MAIN_HAND));
	}

	@Override
	public void stop() {
		this.celuiQuiTient = null;
	}

	@Override
	public void tick() {
		this.reste--;
		if (this.celuiQuiTient != null) {
			this.compagnon.getLookControl().setLookAt(this.celuiQuiTient, 30.0F, 30.0F);
		}
	}

	/**
	 * Ce qui l'interesse.
	 *
	 * <p>De la nourriture — la sienne ou celle du jeu — ou un remede. Le reste ne
	 * lui dit rien : une pioche n'a jamais fait tourner la tete a personne.
	 */
	private static boolean luiPlait(ItemStack pile) {
		return !pile.isEmpty()
				&& (pile.is(Objets.ALIMENT)
						|| pile.is(Objets.SOIN)
						|| pile.has(DataComponents.FOOD));
	}
}
