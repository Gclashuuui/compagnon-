package fr.lhdp.compagnon.objet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Index spatial minuscule des gamelles remplies d'eau.
 *
 * <p>Un compagnon ne balaie jamais des milliers de blocs autour de lui. Chaque
 * gamelle se range dans la case de son chunk ; une recherche à quatorze blocs
 * n'ouvre donc que neuf petites cases. La clef du monde est faible afin qu'un
 * serveur arrêté ne reste jamais retenu en mémoire.
 */
public final class GamellesEau {

	private static final Map<Level, Map<Long, Set<Long>>> PAR_MONDE = new WeakHashMap<>();

	private GamellesEau() {
	}

	public static synchronized void ajouter(Level monde, BlockPos position) {
		long chunk = ChunkPos.asLong(position.getX() >> 4, position.getZ() >> 4);
		PAR_MONDE.computeIfAbsent(monde, ignore -> new HashMap<>())
				.computeIfAbsent(chunk, ignore -> new HashSet<>())
				.add(position.asLong());
	}

	public static synchronized void retirer(Level monde, BlockPos position) {
		Map<Long, Set<Long>> chunks = PAR_MONDE.get(monde);
		if (chunks == null) {
			return;
		}
		long chunk = ChunkPos.asLong(position.getX() >> 4, position.getZ() >> 4);
		Set<Long> positions = chunks.get(chunk);
		if (positions != null) {
			positions.remove(position.asLong());
			if (positions.isEmpty()) {
				chunks.remove(chunk);
			}
		}
	}

	/** La plus proche dans le rayon, sans charger de chunk. */
	public static synchronized BlockPos plusProche(Level monde, BlockPos origine, int rayon) {
		Map<Long, Set<Long>> chunks = PAR_MONDE.get(monde);
		if (chunks == null) {
			return null;
		}
		int chunkX = origine.getX() >> 4;
		int chunkZ = origine.getZ() >> 4;
		int marge = (rayon + 15) >> 4;
		long limite = (long) rayon * rayon;
		long meilleureDistance = Long.MAX_VALUE;
		BlockPos meilleure = null;

		for (int x = chunkX - marge; x <= chunkX + marge; x++) {
			for (int z = chunkZ - marge; z <= chunkZ + marge; z++) {
				Set<Long> positions = chunks.get(ChunkPos.asLong(x, z));
				if (positions == null) {
					continue;
				}
				var iterateur = positions.iterator();
				while (iterateur.hasNext()) {
					BlockPos position = BlockPos.of(iterateur.next());
					if (!monde.hasChunkAt(position)) {
						continue;
					}
					if (!BlocGamelle.contientEau(monde.getBlockState(position))) {
						iterateur.remove();
						continue;
					}
					long dx = position.getX() - origine.getX();
					long dy = position.getY() - origine.getY();
					long dz = position.getZ() - origine.getZ();
					long distance = dx * dx + dy * dy + dz * dz;
					if (distance <= limite && distance < meilleureDistance) {
						meilleureDistance = distance;
						meilleure = position.immutable();
					}
				}
			}
		}
		return meilleure;
	}
}
