package de.melonkingde.pickupcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config: .minecraft/config/pickupcat.json (read on game start). */
public final class CatConfig {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Master switch. */
	public boolean enabled = true;
	/** Only show the cat while connected to a *.hypixel.net server. */
	public boolean onlyOnHypixel = false;
	/** Integer pixel scale of the cat sprite (1-4). */
	public int scale = 1;
	/** Shift the cat horizontally / vertically (GUI pixels). */
	public int offsetX = 0;
	public int offsetY = 0;
	/** Gentle swinging of the hanging cat. */
	public boolean sway = true;
	/** Show the fading "+12x Item" log under the cat. */
	public boolean showLog = true;
	public int logLines = 5;
	public float logSeconds = 5.0f;
	/** 1.0 = normal. The cat also speeds up on its own when many items are queued. */
	public float animationSpeed = 1.0f;
	public int maxQueue = 40;
	/** Speech bubbles: random reactions and chatter. */
	public boolean speech = true;
	/** Seconds without eating before the cat says it is hungry (repeats, getting more dramatic). */
	public float hungryAfterSeconds = 60f;
	/** Average seconds between random chatter lines. 0 = no random chatter. */
	public float chatterSeconds = 75f;
	/** Items moved into your inventory from chests, sacks, bazaar, auctions... also get eaten. */
	public boolean eatFromGuis = true;

	static CatConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("pickupcat.json");
		CatConfig cfg = new CatConfig();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				CatConfig loaded = GSON.fromJson(reader, CatConfig.class);
				if (loaded != null) {
					cfg = loaded;
				}
			} catch (Exception e) {
				LOGGER.warn("[PickupCat] Could not read config, using defaults", e);
			}
		}
		cfg.sanitize();
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(cfg, writer);
		} catch (Exception e) {
			LOGGER.warn("[PickupCat] Could not write config", e);
		}
		return cfg;
	}

	private void sanitize() {
		scale = Math.max(1, Math.min(4, scale));
		logLines = Math.max(0, Math.min(10, logLines));
		logSeconds = Math.max(1f, Math.min(30f, logSeconds));
		animationSpeed = Math.max(0.25f, Math.min(3f, animationSpeed));
		maxQueue = Math.max(5, Math.min(200, maxQueue));
		hungryAfterSeconds = Math.max(10f, Math.min(600f, hungryAfterSeconds));
		chatterSeconds = chatterSeconds <= 0f ? 0f : Math.max(20f, Math.min(600f, chatterSeconds));
	}
}
