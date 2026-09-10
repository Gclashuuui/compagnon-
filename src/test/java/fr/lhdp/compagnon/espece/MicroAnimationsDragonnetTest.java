package fr.lhdp.compagnon.espece;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroAnimationsDragonnetTest {

	private static final Path FICHIER = Path.of(
			"src/main/resources/assets/compagnon/animations/dragonnet.animation.json");

	private static final Set<String> AJOUTS = Set.of(
			"blink", "blink_double", "ear_twitch_left", "ear_twitch_right",
			"glance_left", "glance_right", "head_micro_tilt_left",
			"head_micro_tilt_right", "weight_shift_left", "weight_shift_right",
			"tail_flick_left", "tail_flick_right", "tail_settle", "wing_resettle",
			"wing_fold_soft", "look_back", "listen_start", "listen_end",
			"inspect_ground_start", "inspect_ground_loop", "inspect_ground_end",
			"hesitate", "relax_after_alert", "bored", "friend_notice",
			"affection_approach_end", "morning_ritual", "evening_settle",
			"recognize_person", "observe_stranger");

	@Test
	void lesTrenteMicroAnimationsSontPresentesEtNeBougentPasLesRacines()
			throws Exception {
		JsonObject animations = JsonParser.parseString(Files.readString(FICHIER))
				.getAsJsonObject().getAsJsonObject("animations");
		assertEquals(102, animations.size());

		for (String ajout : AJOUTS) {
			String nom = "animation.dragonnet." + ajout;
			assertTrue(animations.has(nom), () -> "animation absente : " + nom);
			JsonObject os = animations.getAsJsonObject(nom).getAsJsonObject("bones");
			assertTrue(os == null || !os.has("root"), () -> nom + " anime root");
			assertTrue(os == null || !os.has("worldbody"), () -> nom + " anime worldbody");
		}
	}
}
