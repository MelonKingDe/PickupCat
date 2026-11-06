package de.melonkingde.pickupcat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Animation state machine + HUD rendering of the hanging cat. */
final class CatController {
	enum Kind { EAT, VOMIT, PARTY }

	private static final class Event {
		final Kind kind;
		final String key;
		int count;
		final ItemStack stack;

		Event(Kind kind, String key, int count, ItemStack stack) {
			this.kind = kind;
			this.key = key;
			this.count = count;
			this.stack = stack;
		}
	}

	private static final class LogLine {
		final Kind kind;
		final String key;
		int count;
		final ItemStack stack;
		long lastMs;

		LogLine(Kind kind, String key, int count, ItemStack stack, long lastMs) {
			this.kind = kind;
			this.key = key;
			this.count = count;
			this.stack = stack;
			this.lastMs = lastMs;
		}
	}

	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(PickupCatClient.MOD_ID, "textures/gui/cat.png");
	private static final int FRAME_W = 32;
	private static final int FRAME_H = 44;
	private static final int COLS = 5;
	private static final int SHEET_W = 160;
	private static final int SHEET_H = 88;

	// frame indices in cat.png
	private static final int IDLE_A = 0, IDLE_B = 1, BLINK = 2, OPEN = 3, CHEW_A = 4, CHEW_B = 5,
			HAPPY = 6, RETCH_A = 7, RETCH_B = 8, RELIEF = 9;

	private static final float MOUTH_X = 16f;
	private static final float MOUTH_Y = 21f;
	private static final float ITEM_SCALE = 0.7f; // in sprite units
	private static final float EAT_FLY = 0.40f;
	private static final float EAT_DUR = 1.15f;
	private static final float VOMIT_RETCH = 0.70f;
	private static final float VOMIT_DUR = 1.50f;
	private static final float PARTY_FALL = 0.75f;      // cat drops to the bottom of the screen
	private static final float PARTY_LAND = 0.95f;      // landing bounce ends
	private static final float PARTY_DANCE_END = 5.0f;  // then the cat climbs back up
	private static final float PARTY_DUR = 6.0f;

	private static final class Confetti {
		float x, y, vx, vy, age, life, phase;
		int size;
		int rgb;
	}

	private final CatConfig cfg;
	private final ArrayDeque<Event> queue = new ArrayDeque<>();
	private final List<LogLine> log = new ArrayList<>();

	private Event current;
	private float t;
	private boolean logged;
	private int side = 1;
	private float clock;
	private long lastNanos;

	// speech
	private final Random rng = new Random();
	private String bubble;
	private float bubbleT;
	private float bubbleLife;
	private float speechCooldown;
	private float fastCooldown;
	private float hungryTimer;
	private int hungryStage;
	private float chatterTimer;
	private float greetDelay = -1f;
	private ClientLevel lastLevel;

	// dye party
	private final List<Confetti> confetti = new ArrayList<>();
	private int[] partyPalette = {0xE0405A, 0xFFD23F, 0x3FA7FF};
	private boolean pendingBurst;
	private float rainAcc;

	CatController(CatConfig cfg) {
		this.cfg = cfg;
	}

	// ------------------------------------------------------------------ events

	void enqueue(Kind kind, String key, int count, ItemStack sample) {
		if (sample == null || count <= 0) {
			return;
		}
		if (kind == Kind.VOMIT && cfg.dyeParty && DyeColors.isDye(key)) {
			kind = Kind.PARTY;
		}
		if (kind == Kind.EAT) {
			hungryTimer = 0f;
			hungryStage = 0;
		}
		Event last = queue.peekLast();
		if (last != null && last.kind == kind && last.key.equals(key)) {
			last.count += count;
			return;
		}
		if (queue.size() >= cfg.maxQueue) {
			Event old = queue.pollFirst();
			if (old != null) {
				addLog(old.kind, old.key, old.count, old.stack);
			}
		}
		queue.addLast(new Event(kind, key, count, sample));
		if (queue.size() >= 6 && fastCooldown <= 0f) {
			fastCooldown = 20f;
			say(CatSpeech.pick(CatSpeech.FAST, rng), true);
		}
	}

	private void addLog(Kind kind, String key, int count, ItemStack stack) {
		long now = System.currentTimeMillis();
		for (int i = 0; i < log.size(); i++) {
			LogLine l = log.get(i);
			if (l.kind == kind && l.key.equals(key) && now - l.lastMs < 3000) {
				l.count += count;
				l.lastMs = now;
				log.remove(i);
				log.add(0, l);
				return;
			}
		}
		log.add(0, new LogLine(kind, key, count, stack, now));
		while (log.size() > Math.max(1, cfg.logLines)) {
			log.remove(log.size() - 1);
		}
	}

	private void advance(float dt) {
		if (current == null) {
			current = queue.pollFirst();
			if (current != null) {
				t = 0f;
				logged = false;
				side = -side;
				if (current.kind == Kind.PARTY) {
					partyPalette = DyeColors.palette(current.key, rng);
				}
			}
		}
		if (current == null) {
			return;
		}
		float speed = cfg.animationSpeed * Math.min(4f, 1f + 0.35f * queue.size());
		t += dt * speed;
		float swallow = current.kind == Kind.EAT ? EAT_FLY : (current.kind == Kind.PARTY ? PARTY_FALL : VOMIT_RETCH);
		if (!logged && t >= swallow) {
			logged = true;
			addLog(current.kind, current.key, current.count, current.stack);
			react(current);
			if (current.kind == Kind.PARTY) {
				pendingBurst = true;
			}
		}
		float dur = current.kind == Kind.EAT ? EAT_DUR : (current.kind == Kind.PARTY ? PARTY_DUR : VOMIT_DUR);
		if (t >= dur) {
			if (!logged) {
				addLog(current.kind, current.key, current.count, current.stack);
			}
			current = null;
		}
	}

	// ------------------------------------------------------------------ speech

	private static String fmt(String line, Event e) {
		String name = ChatFormatting.stripFormatting(e.stack.getHoverName().getString());
		if (name == null || name.isEmpty()) {
			name = "that";
		}
		return line.replace("{item}", name).replace("{count}", Integer.toString(e.count));
	}

	private void say(String text, boolean force) {
		if (!cfg.speech) {
			return;
		}
		if (!force && (bubble != null || speechCooldown > 0f)) {
			return;
		}
		bubble = text;
		bubbleT = 0f;
		bubbleLife = 3.2f + text.length() * 0.045f;
		speechCooldown = 5f;
	}

	private void react(Event e) {
		if (!cfg.speech) {
			return;
		}
		if (e.kind == Kind.PARTY) {
			say(fmt(CatSpeech.pick(CatSpeech.PARTY, rng), e), true);
		} else if (e.kind == Kind.VOMIT) {
			say(fmt(CatSpeech.pick(CatSpeech.VOMIT, rng), e), true);
		} else if (e.count >= 32) {
			say(fmt(CatSpeech.pick(CatSpeech.EAT_BIG, rng), e), true);
		} else if (rng.nextFloat() < 0.3f) {
			String[] pool = rng.nextBoolean() ? CatSpeech.EAT_ITEM : CatSpeech.EAT_SHORT;
			say(fmt(CatSpeech.pick(pool, rng), e), false);
		}
	}

	/** Called when the player resized the cat with the keybinds. */
	void onResized(boolean bigger, boolean atLimit) {
		String[] pool = atLimit ? CatSpeech.LIMIT : (bigger ? CatSpeech.BIGGER : CatSpeech.SMALLER);
		say(CatSpeech.pick(pool, rng), true);
	}

	private float nextChatter() {
		return cfg.chatterSeconds * (0.6f + rng.nextFloat() * 0.8f);
	}

	private void updateSpeech(float dt, Minecraft mc) {
		if (!cfg.speech) {
			bubble = null;
			return;
		}
		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			greetDelay = 4f;
			hungryTimer = 0f;
			hungryStage = 0;
			chatterTimer = nextChatter();
		}
		speechCooldown = Math.max(0f, speechCooldown - dt);
		fastCooldown = Math.max(0f, fastCooldown - dt);
		if (greetDelay > 0f) {
			greetDelay -= dt;
			if (greetDelay <= 0f) {
				say(CatSpeech.pick(CatSpeech.GREET, rng), false);
			}
		}
		if (bubble != null) {
			bubbleT += dt;
			if (bubbleT >= bubbleLife) {
				bubble = null;
			}
		}
		boolean busy = current != null || !queue.isEmpty();
		if (busy) {
			return;
		}
		hungryTimer += dt;
		chatterTimer -= dt;
		if (bubble != null || greetDelay > 0f) {
			return;
		}
		if (hungryTimer >= cfg.hungryAfterSeconds) {
			say(CatSpeech.HUNGRY[Math.min(hungryStage, CatSpeech.HUNGRY.length - 1)], true);
			hungryStage++;
			hungryTimer = 0f;
			chatterTimer = Math.max(chatterTimer, 20f);
		} else if (cfg.chatterSeconds > 0f && chatterTimer <= 0f) {
			say(CatSpeech.pick(CatSpeech.CHATTER, rng), false);
			chatterTimer = nextChatter();
		}
	}

	private void drawBubble(GuiGraphicsExtractor g, Minecraft mc, float pivotX, float pivotY, float s) {
		if (bubble == null) {
			return;
		}
		float in = Math.min(1f, bubbleT / 0.15f);
		float out = Math.min(1f, (bubbleLife - bubbleT) / 0.4f);
		float a = Math.max(0f, Math.min(in, out));
		int alpha = (int) (a * 255f);
		if (alpha < 6) {
			return;
		}
		Font font = mc.font;
		Component text = Component.literal(bubble);
		int w = font.width(text);
		int guiW = mc.getWindow().getGuiScaledWidth();
		int x0 = Math.round(pivotX + (FRAME_W / 2f + 2f) * s) + 5;
		int y0 = Math.round(pivotY + 8f * s + (1f - in) * 4f);
		int x1 = x0 + w + 8;
		int y1 = y0 + font.lineHeight + 8;
		if (x1 > guiW - 2) {
			int shift = x1 - (guiW - 2);
			x0 -= shift;
			x1 -= shift;
		}
		int white = (alpha << 24) | 0xFFFFFF;
		int line = (alpha << 24) | 0x3B2418;
		// rounded box: two overlapping rects, outline first
		g.fill(x0 - 1, y0, x1 + 1, y1, line);
		g.fill(x0, y0 - 1, x1, y1 + 1, line);
		g.fill(x0, y0, x1, y1, white);
		// little tail pointing at the cat
		int my = (y0 + y1) / 2;
		for (int i = 0; i < 4; i++) {
			g.fill(x0 - 2 - i, my - (3 - i) - 1, x0 - i, my + (3 - i) + 2, line);
		}
		for (int i = 0; i < 4; i++) {
			g.fill(x0 - 1 - i, my - (3 - i), x0 - i + 1, my + (3 - i) + 1, white);
		}
		g.text(font, text, x0 + 4, y0 + 4, line, false);
	}

	private int idleFrame() {
		if (clock % 4.3f < 0.12f) {
			return BLINK;
		}
		return ((int) (clock / 0.6f) % 2 == 0) ? IDLE_A : IDLE_B;
	}

	private int pickFrame() {
		if (current == null) {
			return idleFrame();
		}
		if (current.kind == Kind.EAT) {
			if (t < 0.10f) {
				return idleFrame();
			}
			if (t < EAT_FLY) {
				return OPEN;
			}
			if (t < 0.85f) {
				return ((int) ((t - EAT_FLY) / 0.11f) % 2 == 0) ? CHEW_A : CHEW_B;
			}
			return HAPPY;
		}
		if (current.kind == Kind.PARTY) {
			if (t < PARTY_FALL) {
				return OPEN;
			}
			if (t < PARTY_DANCE_END) {
				return ((int) (t * 5f) % 2 == 0) ? HAPPY : CHEW_A;
			}
			return HAPPY;
		}
		if (t < VOMIT_RETCH) {
			return ((int) (t / 0.12f) % 2 == 0) ? RETCH_A : RETCH_B;
		}
		return t < 0.95f ? RETCH_B : RELIEF;
	}

	// --------------------------------------------------------------- rendering

	void render(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (!cfg.enabled || mc.player == null || (cfg.onlyOnHypixel && !onHypixel(mc))) {
			lastNanos = 0L;
			return;
		}
		long nowNanos = System.nanoTime();
		float dt = lastNanos == 0L ? 0f : Math.min(0.1f, (nowNanos - lastNanos) / 1.0e9f);
		lastNanos = nowNanos;
		clock += dt;
		advance(dt);
		updateSpeech(dt, mc);

		float s = cfg.scale;
		int guiW = mc.getWindow().getGuiScaledWidth();
		int guiH = mc.getWindow().getGuiScaledHeight();
		float pivotX = guiW / 2f + cfg.offsetX;
		float pivotY = cfg.offsetY;
		int frame = pickFrame();
		float angle = cfg.sway ? (float) Math.sin(clock * 1.7f) * 0.035f : 0f;
		float shake = (current != null && current.kind == Kind.VOMIT && t < VOMIT_RETCH)
				? (float) Math.sin(t * 70f) * 0.9f : 0f;

		// dye party: the cat lets go, falls to the bottom, dances, then climbs back up
		boolean party = current != null && current.kind == Kind.PARTY;
		float yOff = 0f;
		if (party) {
			angle = 0f;
			float fallDist = Math.max(0f, guiH - 48f - FRAME_H * s - pivotY);
			if (t < PARTY_FALL) {
				float p = t / PARTY_FALL;
				yOff = fallDist * p * p;
			} else if (t < PARTY_LAND) {
				float p = (t - PARTY_FALL) / (PARTY_LAND - PARTY_FALL);
				yOff = fallDist - (float) Math.sin(Math.PI * p) * 5f * s;
			} else if (t < PARTY_DANCE_END) {
				float amp = Math.min(1f, (t - PARTY_LAND) / 0.3f) * Math.min(1f, (PARTY_DANCE_END - t) / 0.3f);
				float wave = (float) Math.sin(clock * 7f);
				yOff = fallDist - Math.abs(wave) * 6f * s * amp;
				angle = wave * 0.12f * amp;
			} else {
				float p = Math.min(1f, (t - PARTY_DANCE_END) / (PARTY_DUR - PARTY_DANCE_END));
				float e = p * p * (3f - 2f * p);
				yOff = fallDist * (1f - e);
			}
		}

		var pose = g.pose();
		pose.pushMatrix();
		if (party) {
			// rotate around the feet while it is dancing
			pose.translate(pivotX, pivotY + yOff + FRAME_H * s);
			pose.rotate(angle);
			pose.scale(s, s);
			pose.translate(-FRAME_W / 2f, -(float) FRAME_H);
		} else {
			pose.translate(pivotX, pivotY);
			pose.rotate(angle);
			pose.scale(s, s);
			pose.translate(-FRAME_W / 2f + shake, 0f);
		}
		float u = (frame % COLS) * FRAME_W;
		float v = (frame / COLS) * FRAME_H;
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, u, v, FRAME_W, FRAME_H, SHEET_W, SHEET_H);
		drawFlyingItem(g);
		drawPartyHat(g);
		pose.popMatrix();

		// confetti
		if (pendingBurst) {
			pendingBurst = false;
			spawnBurst(pivotX, pivotY + yOff + FRAME_H * s * 0.45f);
		}
		if (party && t >= PARTY_FALL && t < PARTY_DANCE_END) {
			rainAcc += dt * 55f;
			while (rainAcc >= 1f) {
				rainAcc -= 1f;
				spawnRain(pivotX, s);
			}
		} else {
			rainAcc = 0f;
		}
		updateConfetti(dt, guiH);
		drawConfetti(g);

		drawBubble(g, mc, pivotX, pivotY + yOff, s);
		if (cfg.showLog && cfg.logLines > 0) {
			drawLog(g, mc, pivotX, pivotY + (FRAME_H + 3) * s);
		}
	}

	// ------------------------------------------------------------- dye party

	private int confettiColor() {
		if (rng.nextFloat() < 0.12f) {
			return 0xFFFFFF;
		}
		return partyPalette[rng.nextInt(partyPalette.length)];
	}

	private void spawnBurst(float cx, float cy) {
		for (int i = 0; i < 70 && confetti.size() < 500; i++) {
			Confetti c = new Confetti();
			c.x = cx + (rng.nextFloat() - 0.5f) * 10f;
			c.y = cy;
			double ang = -Math.PI / 2 + (rng.nextFloat() - 0.5f) * 2.8f;
			float sp = 70f + rng.nextFloat() * 170f;
			c.vx = (float) Math.cos(ang) * sp;
			c.vy = (float) Math.sin(ang) * sp;
			c.life = 2.2f + rng.nextFloat() * 1.4f;
			c.phase = rng.nextFloat() * 6.28f;
			c.size = 2 + rng.nextInt(2);
			c.rgb = confettiColor();
			confetti.add(c);
		}
	}

	private void spawnRain(float cx, float s) {
		if (confetti.size() >= 500) {
			return;
		}
		Confetti c = new Confetti();
		c.x = cx + (rng.nextFloat() * 2f - 1f) * (110f + 30f * s);
		c.y = -4f;
		c.vx = (rng.nextFloat() - 0.5f) * 30f;
		c.vy = 30f + rng.nextFloat() * 40f;
		c.life = 4.5f + rng.nextFloat() * 2f;
		c.phase = rng.nextFloat() * 6.28f;
		c.size = 2 + rng.nextInt(2);
		c.rgb = confettiColor();
		confetti.add(c);
	}

	private void updateConfetti(float dt, int guiH) {
		for (int i = confetti.size() - 1; i >= 0; i--) {
			Confetti c = confetti.get(i);
			c.age += dt;
			c.vy = Math.min(c.vy + 150f * dt, 85f);
			c.vx *= Math.max(0f, 1f - 1.4f * dt);
			c.x += (c.vx + (float) Math.sin(c.age * 7f + c.phase) * 14f) * dt;
			c.y += c.vy * dt;
			if (c.age >= c.life || c.y > guiH + 4) {
				confetti.remove(i);
			}
		}
	}

	private void drawConfetti(GuiGraphicsExtractor g) {
		for (Confetti c : confetti) {
			float left = c.life - c.age;
			int alpha = left < 0.5f ? Math.max(5, (int) (left / 0.5f * 255f)) : 255;
			int w = c.size;
			int h = 1 + Math.round(Math.abs((float) Math.sin(c.age * 9f + c.phase)) * c.size);
			int x = Math.round(c.x);
			int y = Math.round(c.y);
			g.fill(x, y, x + w, y + h, (alpha << 24) | c.rgb);
		}
	}

	/** Party hat in the dye's colour (drawn in sprite space, so it scales with the cat). */
	private void drawPartyHat(GuiGraphicsExtractor g) {
		if (current == null || current.kind != Kind.PARTY || t < PARTY_FALL || t > PARTY_DUR - 0.5f) {
			return;
		}
		int main = 0xFF000000 | partyPalette[0];
		int dark = 0xFF000000 | DyeColors.shade(partyPalette[0], 0.7f);
		int out = 0xFF3B2418;
		int white = 0xFFFFFFFF;
		for (int r = 0; r < 14; r++) {
			int y = -7 + r;
			float hw = 0.8f + r * 0.4f;
			int x0 = Math.round(16f - hw);
			int x1 = Math.round(16f + hw);
			g.fill(x0 - 1, y, x1 + 1, y + 1, out);
			g.fill(x0, y, x1, y + 1, ((r / 3) % 2 == 0) ? main : dark);
		}
		g.fill(9, 7, 23, 8, out);
		g.fill(14, -10, 18, -6, out);
		g.fill(15, -9, 17, -7, white);
	}

	private void drawFlyingItem(GuiGraphicsExtractor g) {
		if (current == null) {
			return;
		}
		if (current.kind == Kind.EAT) {
			if (t >= EAT_FLY) {
				return;
			}
			float p = t / EAT_FLY;
			float e = 1f - (1f - p) * (1f - p);
			float sx = MOUTH_X + side * 34f;
			float sy = MOUTH_Y + 40f;
			float x = sx + (MOUTH_X - sx) * e;
			float y = sy + (MOUTH_Y - 3f - sy) * e - (float) Math.sin(Math.PI * p) * 12f;
			drawItem(g, current.stack, x, y, ITEM_SCALE * (1f - 0.55f * e), 0f);
		} else if (current.kind == Kind.PARTY) {
			float tp = t - PARTY_FALL;
			if (tp < 0f) {
				return;
			}
			float pop = Math.min(1f, tp / 0.25f);
			pop = pop * pop * (3f - 2f * pop);
			float out = 1f - Math.max(0f, Math.min(1f, (t - PARTY_DANCE_END) / 0.4f));
			float bob = (float) Math.sin(clock * 6f) * 3f;
			drawItem(g, current.stack, -16f, 14f + bob, ITEM_SCALE * 1.8f * pop * out, (float) Math.sin(clock * 5f) * 0.35f);
		} else {
			float tv = t - VOMIT_RETCH;
			if (tv < 0f || tv > 0.8f) {
				return;
			}
			float x = MOUTH_X + side * 30f * tv;
			float y = MOUTH_Y + 2f - 46f * tv + 0.5f * 150f * tv * tv;
			float fade = tv > 0.6f ? 1f - (tv - 0.6f) / 0.2f : 1f;
			drawItem(g, current.stack, x, y, ITEM_SCALE * fade, tv * 8f * side);
		}
	}

	private static void drawItem(GuiGraphicsExtractor g, ItemStack stack, float x, float y, float scale, float rot) {
		if (scale <= 0.02f) {
			return;
		}
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		if (rot != 0f) {
			pose.rotate(rot);
		}
		pose.scale(scale, scale);
		g.item(stack, -8, -8);
		pose.popMatrix();
	}

	private void drawLog(GuiGraphicsExtractor g, Minecraft mc, float centerX, float startY) {
		final long now = System.currentTimeMillis();
		final float life = cfg.logSeconds * 1000f;
		log.removeIf(l -> now - l.lastMs > life);
		Font font = mc.font;
		int y = Math.round(startY);
		for (LogLine l : log) {
			float age = now - l.lastMs;
			float a = age > life - 800f ? Math.max(0f, (life - age) / 800f) : 1f;
			if (a <= 0.03f) {
				continue;
			}
			int alpha = Math.max(5, (int) (a * 255f));
			boolean eat = l.kind == Kind.EAT;
			MutableComponent line = Component.literal((eat ? "+" : "-") + l.count + "x ")
					.withStyle(eat ? ChatFormatting.GREEN : (l.kind == Kind.PARTY ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
			line.append(l.stack.getHoverName());
			int w = font.width(line);
			int x = Math.round(centerX - w / 2f);
			g.fill(x - 3, y - 1, x + w + 3, y + font.lineHeight, ((int) (alpha * 0.45f) << 24));
			g.text(font, line, x, y, (alpha << 24) | 0xFFFFFF, true);
			y += font.lineHeight + 3;
		}
	}

	private static boolean onHypixel(Minecraft mc) {
		ServerData server = mc.getCurrentServer();
		return server != null && server.ip != null && server.ip.toLowerCase().contains("hypixel.net");
	}
}
