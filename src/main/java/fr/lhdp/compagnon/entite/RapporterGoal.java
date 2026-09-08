package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.EnumSet;
import java.util.List;

/**
 * Il ramasse ce qui traine et vient te l'apporter.
 *
 * <p>Il prend l'objet dans sa gueule, revient vers son proprietaire, et le pose a
 * ses pieds. Ce n'est utile a rien — c'est exactement pour ca que c'est bien.
 *
 * <h2>Ce qu'il ne prend jamais</h2>
 *
 * <ul>
 *   <li>Un objet <b>que quelqu'un vient de lacher</b> : Minecraft pose un delai
 *       avant qu'on puisse le reprendre, justement parce que son proprietaire va
 *       sans doute le ramasser. On respecte ce delai.</li>
 *   <li>Un objet <b>dans l'eau ou dans la lave</b>.</li>
 *   <li>Un objet quand <b>son proprietaire n'est pas la</b> : sans personne a qui
 *       l'apporter, ramasser n'aurait aucun sens.</li>
 * </ul>
 *
 * <h2>L'objet n'est jamais perdu</h2>
 *
 * <p>C'est le point delicat. L'objet <b>quitte le monde</b> pendant qu'il le
 * porte, et l'entite du compagnon, elle, n'est pas sauvegardee. Trois filets :
 * ce but repose l'objet s'il s'interrompt, il le repose s'il met trop longtemps,
 * et {@code CompagnonEntity.remove} le repose si l'entite disparait. Un compagnon
 * qui efface la pioche en diamant d'un joueur serait imperdonnable.
 */
public class RapporterGoal extends Goal {

	/** Jusqu'ou il repere un objet au sol, en blocs. Valeur inventee. */
	private static final double PORTEE = 8.0D;

	/** Au-dela, son proprietaire est trop loin pour qu'il aille chercher. */
	private static final double PORTEE_PROPRIETAIRE = 24.0D;

	/** Il attrape a cette distance. */
	private static final double DISTANCE_PRISE = 1.4D;

	/** Il pose a cette distance de son proprietaire. */
	private static final double DISTANCE_CADEAU = 2.0D;

	private static final double VITESSE = 1.1D;

	/**
	 * Au-dela, il abandonne et repose l'objet.
	 *
	 * <p>Un chemin impossible — l'objet derriere une vitre, sur un rebord — le
	 * laisserait sinon coincé avec l'objet dans la gueule pour toujours.
	 */
	private static final int PATIENCE = 20 * 30;

	/** Une chance sur tant a chaque examen : il n'est pas ramasseur compulsif. */
	private static final int RARETE = 60;

	/** Jusqu'ou il cherche un ami a qui offrir sa trouvaille, en blocs. */
	private static final double PORTEE_AMI = 10.0D;

	/** Une fois sur tant, le cadeau va a un ami plutot qu'au maitre. */
	private static final int CHANCE_AMI = 5;

	private final CompagnonEntity compagnon;
	private ItemEntity vise;

	/** L'ami a qui il apporte sa trouvaille, ou {@code null} pour son maitre. */
	private CompagnonEntity pourUnAmi;

	private int reste;

	public RapporterGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		// Il peut rapporter en flanant comme en suivant, mais pas quand on lui a
		// dit de rester assis ou couche.
		if (this.compagnon.mode().pose() || !this.compagnon.lesMainsVides()) {
			return false;
		}
		if (proprietaireProche() == null) {
			return false;
		}
		if (this.compagnon.getRandom().nextInt(RARETE) != 0) {
			return false;
		}

		// LE PLUS PROCHE DE LUI, et non un au hasard.
		//
		// C'etait un tirage au sort, et ca se voyait tout de suite : pose quatre
		// steaks devant lui et il partait chercher celui du fond. Personne ne
		// comprend un animal qui ignore ce qu'il a sous le nez.
		this.vise = leplusProche(this.compagnon, PORTEE, this::ramassable);
		return this.vise != null;
	}

	/**
	 * Un objet est ramassable s'il est pose la, tranquille, et que personne
	 * n'est en train de venir le chercher.
	 */
	private boolean ramassable(ItemEntity objet) {
		return objet.isAlive()
				&& !objet.hasPickUpDelay()
				&& !objet.isInWater()
				&& !objet.isInLava()
				&& objet.onGround();
	}

	/**
	 * L'objet retenu le plus proche du compagnon.
	 *
	 * <p>Du COMPAGNON et non du joueur : c'est lui qui fait le trajet, et c'est ce
	 * qu'on a sous les yeux quand on le regarde partir.
	 */
	public static ItemEntity leplusProche(CompagnonEntity compagnon, double portee,
			java.util.function.Predicate<ItemEntity> retenu) {

		List<ItemEntity> objets = compagnon.level().getEntitiesOfClass(ItemEntity.class,
				compagnon.getBoundingBox().inflate(portee), retenu::test);

		ItemEntity trouve = null;
		double meilleure = Double.MAX_VALUE;
		for (ItemEntity objet : objets) {
			double distance = compagnon.distanceToSqr(objet);
			if (distance < meilleure) {
				meilleure = distance;
				trouve = objet;
			}
		}
		return trouve;
	}

	private LivingEntity proprietaireProche() {
		LivingEntity maitre = this.compagnon.getOwner();
		if (maitre == null || maitre.isRemoved() || maitre.level() != this.compagnon.level()) {
			return null;
		}
		return this.compagnon.distanceToSqr(maitre) <= PORTEE_PROPRIETAIRE * PORTEE_PROPRIETAIRE
				? maitre : null;
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
			// Phase aller : l'objet doit toujours exister.
			return this.vise != null && this.vise.isAlive();
		}
		// Phase retour : il faut quelqu'un a qui l'apporter — l'ami, ou le maitre.
		return (this.pourUnAmi != null && this.pourUnAmi.isAlive()) || proprietaireProche() != null;
	}

	/**
	 * Il repose toujours ce qu'il portait en s'arretant.
	 *
	 * <p>Que ce soit parce qu'il a livre, parce qu'on lui a dit de s'asseoir ou
	 * parce qu'il a renonce : l'objet redescend au sol. C'est le filet qui garantit
	 * qu'un objet ramasse revient toujours dans le monde.
	 */
	@Override
	public void stop() {
		this.vise = null;
		this.pourUnAmi = null;
		this.compagnon.getNavigation().stop();
		if (!this.compagnon.lesMainsVides()) {
			ecrireDansLeJournal();
			this.compagnon.poserCeQuIlPorte();
		}
	}

	@Override
	public void tick() {
		this.reste--;

		if (this.compagnon.lesMainsVides()) {
			allerChercher();
		} else {
			rapporter();
		}
	}

	private void allerChercher() {
		if (this.vise == null || !this.vise.isAlive()) {
			return;
		}
		this.compagnon.getLookControl().setLookAt(this.vise, 30.0F, 30.0F);

		if (this.compagnon.distanceToSqr(this.vise) > DISTANCE_PRISE * DISTANCE_PRISE) {
			this.compagnon.getNavigation().moveTo(this.vise, VITESSE);
			return;
		}

		// Il prend la pile entiere et l'objet quitte le monde. C'est ce qui rend
		// les filets de stop() et de remove() indispensables.
		this.compagnon.prendreDansLaGueule(this.vise.getItem());
		this.vise.discard();
		this.vise = null;
		this.pourUnAmi = choisirUnAmi();
	}

	/**
	 * A qui il apporte sa trouvaille : parfois un ami plutot que son maitre.
	 *
	 * <p>Deux compagnons qui se connaissent assez pour se chercher peuvent aussi se
	 * faire des cadeaux. Ca ne sert a rien, personne ne le demande, et c'est
	 * exactement le genre de chose qu'on a envie de voir arriver dans une cour.
	 *
	 * <p>Une fois sur cinq seulement : le reste du temps il revient vers son
	 * proprietaire, qui reste la personne a qui l'on rapporte les choses.
	 */
	private CompagnonEntity choisirUnAmi() {
		if (this.compagnon.getRandom().nextInt(CHANCE_AMI) != 0) {
			return null;
		}
		List<CompagnonEntity> amis = this.compagnon.level().getEntitiesOfClass(
				CompagnonEntity.class,
				this.compagnon.getBoundingBox().inflate(PORTEE_AMI),
				autre -> autre != this.compagnon
						&& autre.ficheId() != null
						&& this.compagnon.connait(autre.ficheId()));
		return amis.isEmpty() ? null
				: amis.get(this.compagnon.getRandom().nextInt(amis.size()));
	}

	private void rapporter() {
		// L'ami s'il est toujours la, sinon le maitre : un ami qui s'en va ne doit
		// pas laisser le compagnon plante avec son cadeau dans la gueule.
		LivingEntity vers = this.pourUnAmi != null && this.pourUnAmi.isAlive()
				? this.pourUnAmi
				: proprietaireProche();
		if (vers == null) {
			return;
		}
		this.compagnon.getLookControl().setLookAt(vers, 30.0F, 30.0F);

		if (this.compagnon.distanceToSqr(vers) > DISTANCE_CADEAU * DISTANCE_CADEAU) {
			this.compagnon.getNavigation().moveTo(vers, VITESSE);
			return;
		}
		// Arrive : stop() posera l'objet a ses pieds.
		this.reste = 0;
	}

	/**
	 * Le cadeau entre dans son histoire.
	 *
	 * <p>Le premier objet qu'il rapporte devient son <b>objet prefere</b>, pour
	 * toujours. On le range dans la liste des moments, sous une cle qui porte le
	 * nom de l'objet : {@code objet_prefere.minecraft:feather}. Pas de nouveau champ
	 * a sauvegarder, pas de format a faire evoluer — et {@code marquer} refuse deja
	 * d'ecraser un moment existant, ce qui est exactement la regle voulue.
	 */
	private void ecrireDansLeJournal() {
		if (this.compagnon.ficheId() == null
				|| !(this.compagnon.level() instanceof ServerLevel niveau)) {
			return;
		}
		Fiches fiches = Fiches.de(niveau.getServer());
		FicheCompagnon fiche = fiches.get(this.compagnon.ficheId());
		if (fiche == null) {
			return;
		}
		long maintenant = System.currentTimeMillis();
		ResourceLocation quoi = BuiltInRegistries.ITEM.getKey(this.compagnon.porte().getItem());

		boolean neuf = fiche.marquer("premier_cadeau", maintenant);
		neuf |= fiche.marquer("objet_prefere." + quoi, maintenant);
		if (neuf) {
			fiches.setDirty();
		}
	}

}
