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
	/** Size of the cat (0.5 - 6). Whole numbers (1, 2, 3) look the crispest. Also changeable in-game with the resize keys. */
	public float scale = 1f;
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
	/** Receiving a dye makes the cat fall down and throw a party with it. */
	public boolean dyeParty = true;
	/** Where the cat lands during the party: GUI pixels above the bottom edge of the screen (raise it to clear your HUD). */
	public int partyHeight = 120;
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
		cfg.save();
		return cfg;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("pickupcat.json");
	}

	private static long mtime(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (Exception e) {
			return 0L;
		}
	}

	private transient long lastModified;
	private transient int pollCounter;

	/** Writes the config to disk. */
	void save() {
		Path path = path();
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(this, writer);
		} catch (Exception e) {
			LOGGER.warn("[PickupCat] Could not write config", e);
		}
		lastModified = mtime(path);
	}

	/** Every ~2 seconds: if the file was edited, apply the changes live (no restart needed). */
	void pollReload() {
		if (++pollCounter < 40) {
			return;
		}
		pollCounter = 0;
		Path path = path();
		long m = mtime(path);
		if (m == 0L || m == lastModified) {
			return;
		}
		lastModified = m;
		try (Reader reader = Files.newBufferedReader(path)) {
			CatConfig loaded = GSON.fromJson(reader, CatConfig.class);
			if (loaded != null) {
				copyFrom(loaded);
				sanitize();
			}
		} catch (Exception e) {
			LOGGER.warn("[PickupCat] Could not reload config", e);
		}
	}

	private void copyFrom(CatConfig o) {
		enabled = o.enabled;
		onlyOnHypixel = o.onlyOnHypixel;
		scale = o.scale;
		offsetX = o.offsetX;
		offsetY = o.offsetY;
		sway = o.sway;
		showLog = o.showLog;
		logLines = o.logLines;
		logSeconds = o.logSeconds;
		animationSpeed = o.animationSpeed;
		maxQueue = o.maxQueue;
		speech = o.speech;
		hungryAfterSeconds = o.hungryAfterSeconds;
		chatterSeconds = o.chatterSeconds;
		eatFromGuis = o.eatFromGuis;
		dyeParty = o.dyeParty;
		partyHeight = o.partyHeight;
	}

	private void sanitize() {
		scale = Math.max(0.5f, Math.min(6f, scale));
		logLines = Math.max(0, Math.min(10, logLines));
		logSeconds = Math.max(1f, Math.min(30f, logSeconds));
		animationSpeed = Math.max(0.25f, Math.min(3f, animationSpeed));
		maxQueue = Math.max(5, Math.min(200, maxQueue));
		partyHeight = Math.max(0, Math.min(600, partyHeight));
		hungryAfterSeconds = Math.max(10f, Math.min(600f, hungryAfterSeconds));
		chatterSeconds = chatterSeconds <= 0f ? 0f : Math.max(20f, Math.min(600f, chatterSeconds));
	}
}
