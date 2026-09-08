package fr.lhdp.compagnon.objet;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Un jouet qu'on a lance, et qui rebondit pour de vrai.
 *
 * <h2>Pourquoi elle ne disparait pas au premier mur</h2>
 *
 * <p>Tous les projectiles du jeu meurent en touchant quelque chose : c'est ce
 * qu'on attend d'une fleche. Une balle, non. Une balle qui s'arrete net contre
 * un mur n'est pas une balle, c'est une pierre — et la moitie du plaisir de la
 * lancer vient de ne pas savoir ou elle finira.
 *
 * <p>Elle rebondit donc : on inverse la vitesse le long de la face touchee, on
 * en perd une part, et on freine le reste. Trois lignes de physique qui font
 * toute la difference entre un objet et un jouet.
 *
 * <h2>Elle reste une entite jusqu'a ce qu'on la ramasse</h2>
 *
 * <p>On aurait pu la transformer en objet au sol quand elle s'arrete. On ne le
 * fait pas : le compagnon doit pouvoir <b>courir apres pendant qu'elle roule</b>,
 * et non attendre poliment qu'elle s'immobilise. C'est la course qui est jolie,
 * pas le ramassage.
 */
public class JouetLance extends ThrowableItemProjectile {

	/** Ce qu'il reste de vitesse apres un rebond. */
	private static final double RESSORT = 0.55D;

	/** Ce que le sol prend a chaque contact, sur les axes qui glissent. */
	private static final double FROTTEMENT = 0.78D;

	/** En dessous, on considere qu'elle est posee. */
	private static final double VITESSE_MORTE = 0.045D;

	/**
	 * Combien de temps elle attend qu'on vienne la chercher.
	 *
	 * <p>Cinq minutes : assez pour une partie, assez peu pour qu'un serveur ou
	 * mille joueurs lancent des balles ne se remplisse pas d'entites oubliees.
	 */
	private static final int PATIENCE = 20 * 60 * 5;

	/** Elle n'est ramassable qu'apres ce delai, comme un objet lache. */
	private static final int DELAI_AVANT_PRISE = 8;

	private static final String CLE_AGE = "Age";

	private int age;

	/** Vrai quand elle a fini de bouger. */
	private boolean posee;

	public JouetLance(EntityType<? extends JouetLance> type, Level niveau) {
		super(type, niveau);
	}

	public JouetLance(Level niveau, net.minecraft.world.entity.LivingEntity lanceur) {
		super(Objets.JOUET_LANCE, lanceur, niveau);
	}

	@Override
	protected Item getDefaultItem() {
		return Objets.BALLE;
	}

	/** Elle est posee et attrapable : c'est ce que le compagnon cherche. */
	public boolean attrapable() {
		return isAlive() && this.age > DELAI_AVANT_PRISE;
	}

	public boolean estPosee() {
		return this.posee;
	}

	@Override
	public void tick() {
		super.tick();
		this.age++;
		if (this.age > PATIENCE && !this.level().isClientSide()) {
			// Elle se rend plutot que de rester la pour toujours : on la rend au
			// monde sous forme d'objet, on ne la supprime pas.
			rendreAuSol();
		}
	}

	/**
	 * Le rebond.
	 *
	 * <p>On inverse la composante perpendiculaire a la face touchee et on garde
	 * les deux autres, freinees. Une balle qui tombe rebondit donc de moins en
	 * moins haut tout en continuant d'avancer, ce qui est exactement ce qu'on
	 * regarde quand on en lance une.
	 */
	@Override
	protected void onHitBlock(BlockHitResult touche) {
		Vec3 vitesse = getDeltaMovement();
		Direction face = touche.getDirection();

		double x = vitesse.x * (face.getAxis() == Direction.Axis.X ? -RESSORT : FROTTEMENT);
		double y = vitesse.y * (face.getAxis() == Direction.Axis.Y ? -RESSORT : FROTTEMENT);
		double z = vitesse.z * (face.getAxis() == Direction.Axis.Z ? -RESSORT : FROTTEMENT);

		// On la decolle d'un rien de la face, sinon elle rebondit a l'interieur
		// du bloc et se met a vibrer.
		setPos(touche.getLocation().add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.02D)));
		setDeltaMovement(x, y, z);

		if (Math.abs(x) + Math.abs(y) + Math.abs(z) < VITESSE_MORTE) {
			setDeltaMovement(Vec3.ZERO);
			this.posee = true;
		} else {
			this.posee = false;
		}
		if (!level().isClientSide()) {
			level().playSound(null, this, net.minecraft.sounds.SoundEvents.WOOL_STEP,
					net.minecraft.sounds.SoundSource.PLAYERS, 0.35F,
					1.4F + this.random.nextFloat() * 0.2F);
		}
	}

	/**
	 * Elle traverse tout le monde.
	 *
	 * <p>Un jouet ne doit blesser personne, ni bousculer le compagnon qui court
	 * apres. C'est le seul projectile du mod, et il est inoffensif par
	 * construction plutot que par reglage.
	 */
	@Override
	protected void onHitEntity(EntityHitResult touche) {
		// Rien, volontairement.
	}

	/** Reprise par un compagnon : elle quitte le monde sans rien laisser. */
	public net.minecraft.world.item.ItemStack ramasser() {
		net.minecraft.world.item.ItemStack pile = getItem().copy();
		if (pile.isEmpty()) {
			pile = new net.minecraft.world.item.ItemStack(getDefaultItem());
		}
		pile.setCount(1);
		discard();
		return pile;
	}

	/** Oubliee trop longtemps : elle redevient un objet ordinaire. */
	private void rendreAuSol() {
		net.minecraft.world.item.ItemStack pile = getItem().copy();
		if (pile.isEmpty()) {
			pile = new net.minecraft.world.item.ItemStack(getDefaultItem());
		}
		pile.setCount(1);
		spawnAtLocation(pile);
		discard();
	}

	@Override
	protected double getDefaultGravity() {
		// Un peu plus legere qu'une boule de neige : elle vole plus loin et se
		// regarde mieux partir.
		return 0.035D;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag balise) {
		super.addAdditionalSaveData(balise);
		balise.putInt(CLE_AGE, this.age);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag balise) {
		super.readAdditionalSaveData(balise);
		this.age = balise.getInt(CLE_AGE);
	}

	public static net.minecraft.resources.ResourceLocation identifiant() {
		return Compagnon.id("jouet_lance");
	}
}
