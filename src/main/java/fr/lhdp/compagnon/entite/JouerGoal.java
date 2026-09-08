package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.objet.JouetLance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * On lui a lance une balle : il court apres.
 *
 * <h2>Pourquoi ce n'est pas {@link RapporterGoal}</h2>
 *
 * <p>Rapporter est du glanage : il tombe sur un objet qui traine, une fois sur
 * soixante ticks, s'il n'a rien de mieux a faire. C'est le bon comportement pour
 * un steak oublie dans l'herbe.
 *
 * <p>Une balle qu'on vient de lancer devant lui n'est pas un objet qui traine.
 * S'il l'ignorait cinquante-neuf fois sur soixante, le jeu ne marcherait pas —
 * on lance une balle a son chien et il part, tout de suite, toujours. Ce but-la
 * n'a donc aucun tirage au sort et passe avant les autres.
 *
 * <h2>Il part avant qu'elle soit posee</h2>
 *
 * <p>Il vise la balle pendant qu'elle rebondit encore. C'est la course qui est
 * belle a regarder, pas le ramassage : un compagnon qui attendrait poliment
 * qu'elle s'immobilise aurait l'air de faire une course administrative.
 *
 * <h2>Il la rend a celui qui l'a lancee</h2>
 *
 * <p>Et non a son maitre. Sur une cour a plusieurs, quelqu'un d'autre peut jouer
 * avec votre compagnon — c'est meme tout l'interet d'avoir mille joueurs et
 * mille betes au meme endroit.
 */
public class JouerGoal extends Goal {

	/** Jusqu'ou il voit une balle qu'on vient de lancer. */
	private static final double PORTEE = 26.0D;

	/** A quelle distance il l'attrape. */
	private static final double DISTANCE_PRISE = 1.5D;

	/** Et a quelle distance il la depose. */
	private static final double DISTANCE_RENDU = 2.2D;

	/** Il court, il ne se promene pas. */
	private static final double VITESSE = 1.35D;

	/**
	 * Au bout de combien de temps il abandonne.
	 *
	 * <p>Une balle lancee dans un trou ou derriere un mur infranchissable ne doit
	 * pas immobiliser une bete pour la soiree.
	 */
	private static final int PATIENCE = 20 * 45;

	private final CompagnonEntity compagnon;

	private JouetLance vise;
	private LivingEntity aQui;
	private int reste;

	public JouerGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		// Assis ou couche, il reste ou on lui a dit de rester. Meme pour une balle.
		if (this.compagnon.mode().pose() || !this.compagnon.lesMainsVides()) {
			return false;
		}
		this.vise = laPlusProche();
		if (this.vise == null) {
			return false;
		}
		this.aQui = lanceurDe(this.vise);
		return this.aQui != null;
	}

	/**
	 * La balle attrapable la plus proche, lancee par quelqu'un qu'il ecoute.
	 *
	 * <p>Quelqu'un qu'il ecoute, c'est-a-dire son proprietaire ou n'importe quel
	 * joueur : une bete qui ne joue qu'avec son maitre n'est pas un compagnon de
	 * cour, c'est un chien de garde.
	 */
	private JouetLance laPlusProche() {
		AABB zone = this.compagnon.getBoundingBox().inflate(PORTEE);
		List<JouetLance> jouets =
				this.compagnon.level().getEntitiesOfClass(JouetLance.class, zone,
						JouetLance::attrapable);
		JouetLance meilleure = null;
		double plusPres = Double.MAX_VALUE;
		for (JouetLance jouet : jouets) {
			if (lanceurDe(jouet) == null) {
				continue;
			}
			double distance = this.compagnon.distanceToSqr(jouet);
			if (distance < plusPres) {
				plusPres = distance;
				meilleure = jouet;
			}
		}
		return meilleure;
	}

	private LivingEntity lanceurDe(JouetLance jouet) {
		return jouet.getOwner() instanceof Player joueur && joueur.isAlive() ? joueur : null;
	}

	@Override
	public void start() {
		this.reste = PATIENCE;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.reste <= 0 || this.compagnon.mode().pose()) {
			return false;
		}
		if (this.compagnon.lesMainsVides()) {
			return this.vise != null && this.vise.isAlive();
		}
		return this.aQui != null && this.aQui.isAlive();
	}

	/**
	 * Il repose toujours ce qu'il portait en s'arretant.
	 *
	 * <p>Le meme filet que pour rapporter : qu'il ait rendu la balle, renonce, ou
	 * qu'on lui ait dit de s'asseoir, l'objet redescend dans le monde. Une balle
	 * qui disparait dans la gueule d'une bete est une balle perdue.
	 */
	@Override
	public void stop() {
		this.vise = null;
		this.aQui = null;
		this.compagnon.getNavigation().stop();
		if (!this.compagnon.lesMainsVides()) {
			this.compagnon.poserCeQuIlPorte();
		}
	}

	@Override
	public void tick() {
		this.reste--;
		if (this.compagnon.lesMainsVides()) {
			courirApres();
		} else {
			rapporter();
		}
	}

	private void courirApres() {
		if (this.vise == null || !this.vise.isAlive()) {
			return;
		}
		this.compagnon.getLookControl().setLookAt(this.vise, 30.0F, 30.0F);

		if (this.compagnon.distanceToSqr(this.vise) > DISTANCE_PRISE * DISTANCE_PRISE) {
			this.compagnon.getNavigation().moveTo(this.vise, VITESSE);
			return;
		}
		// Elle quitte le monde et se retrouve dans sa gueule : c'est l'ancrage de
		// bouche de son espece qui decide ou elle se voit.
		this.compagnon.prendreDansLaGueule(this.vise.ramasser());
		this.vise = null;
		Etincelles.bouffee(this.compagnon);
		Sons.jouer(this.compagnon, Sons.CONTENT, 0.7F);
	}

	private void rapporter() {
		if (this.aQui == null) {
			return;
		}
		this.compagnon.getLookControl().setLookAt(this.aQui, 30.0F, 30.0F);

		if (this.compagnon.distanceToSqr(this.aQui) > DISTANCE_RENDU * DISTANCE_RENDU) {
			this.compagnon.getNavigation().moveTo(this.aQui, VITESSE);
			return;
		}
		// Il pose, il ne donne pas : un objet qui apparait dans l'inventaire ne se
		// voit pas, un objet qui tombe devant vous est rendu.
		this.compagnon.poserCeQuIlPorte();
		this.aQui = null;
	}

	/**
	 * Il ne se met pas en pause entre deux images.
	 *
	 * <p>Sans ca, le jeu reevalue le but chaque tick et la course s'interrompt des
	 * qu'un autre but devient interessant. Une bete qui hesite en pleine course
	 * n'a pas l'air de jouer.
	 */
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
