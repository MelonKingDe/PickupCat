package de.melonkingde.pickupcat;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Which items count as dyes, and which confetti colours they get. Colours are approximate - edit freely. */
final class DyeColors {
	private DyeColors() {}

	private static final Map<String, Integer> SKYBLOCK = new HashMap<>();
	private static final Map<String, int[]> MULTI = new HashMap<>();
	private static final Map<String, Integer> VANILLA = new HashMap<>();

	static {
		Object[][] sb = {
				{"AQUAMARINE", 0x7FFFD4}, {"BINGO_BLUE", 0x002FA7}, {"BONE", 0xE3DAC9}, {"BRICK_RED", 0xCB4154},
				{"BYZANTIUM", 0x702963}, {"CARMINE", 0x960018}, {"CELADON", 0xACE1AF}, {"CELESTE", 0xB2FFFF},
				{"CHOCOLATE", 0x7B3F00}, {"COPPER", 0xB87333}, {"CYCLAMEN", 0xF56FA1}, {"DARK_PURPLE", 0x301934},
				{"DUNG", 0x5C4033}, {"EMERALD", 0x50C878}, {"FLAME", 0xE25822}, {"FOSSIL", 0xC2B280},
				{"FROSTBITTEN", 0x9FD8DF}, {"HOLLY", 0x0B6623}, {"ICEBERG", 0x71A6D2}, {"JADE", 0x00A86B},
				{"LAVA", 0xCF1020}, {"LIVID", 0x6699CC}, {"MANGO", 0xFF8243}, {"MATCHA", 0x7B9F35},
				{"MIDNIGHT", 0x191970}, {"MOCHA", 0x4E3B31}, {"NADESHIKO", 0xF6ADC6}, {"NECRON", 0x3D3D3D},
				{"NYANZA", 0xE9FFDB}, {"PELT", 0x6F4E37}, {"PERIWINKLE", 0xCCCCFF}, {"PURE_BLACK", 0x222222},
				{"PURE_BLUE", 0x0000FF}, {"PURE_WHITE", 0xFFFFFF}, {"PURE_YELLOW", 0xFFFF00}, {"ROSE", 0xFF007F},
				{"SANGRIA", 0x92000A}, {"TENTACLE", 0x6A0DAD}, {"TREASURE", 0xFFD700}, {"WARDEN", 0x0D6064},
				{"WILD_STRAWBERRY", 0xFF43A4},
		};
		for (Object[] e : sb) {
			SKYBLOCK.put((String) e[0], (Integer) e[1]);
		}
		MULTI.put("SUNSET", new int[] {0xFF8C42, 0xFF5E7E, 0xB05BD0});
		// PEARLESCENT, SECRET and every unknown dye get rainbow confetti

		Object[][] vn = {
				{"white", 0xF9FFFE}, {"orange", 0xF9801D}, {"magenta", 0xC74EBD}, {"light_blue", 0x3AB3DA},
				{"yellow", 0xFED83D}, {"lime", 0x80C71F}, {"pink", 0xF38BAA}, {"gray", 0x474F52},
				{"light_gray", 0x9D9D97}, {"cyan", 0x169C9C}, {"purple", 0x8932B8}, {"blue", 0x3C44AA},
				{"brown", 0x835432}, {"green", 0x5E7C16}, {"red", 0xB02E26}, {"black", 0x1D1D21},
		};
		for (Object[] e : vn) {
			VANILLA.put((String) e[0], (Integer) e[1]);
		}
	}

	private static String stripVariant(String key) {
		int bar = key.indexOf('|');
		return bar >= 0 ? key.substring(0, bar) : key;
	}

	/** SkyBlock dyes have ids like DYE_NYANZA; vanilla dyes are minecraft:*_dye. */
	static boolean isDye(String key) {
		String base = stripVariant(key);
		return base.startsWith("DYE_") || (base.startsWith("minecraft:") && base.endsWith("_dye"));
	}

	/** Confetti colours (RGB, no alpha); at least one entry. */
	static int[] palette(String key, Random rng) {
		String base = stripVariant(key);
		Integer rgb = null;
		if (base.startsWith("minecraft:")) {
			String name = base.substring("minecraft:".length());
			if (name.endsWith("_dye")) {
				name = name.substring(0, name.length() - 4);
			}
			rgb = VANILLA.get(name);
		} else if (base.startsWith("DYE_")) {
			String name = base.substring(4);
			int[] multi = MULTI.get(name);
			if (multi != null) {
				return multi;
			}
			rgb = SKYBLOCK.get(name);
		}
		if (rgb != null) {
			return new int[] {rgb, mix(rgb, 0xFFFFFF, 0.4f), shade(rgb, 0.7f)};
		}
		int[] rainbow = new int[6];
		float h0 = rng.nextFloat();
		for (int i = 0; i < rainbow.length; i++) {
			rainbow[i] = hsv((h0 + i / 6f) % 1f, 0.8f, 1f);
		}
		return rainbow;
	}

	static int shade(int rgb, float f) {
		return mix(rgb, 0x000000, 1f - f);
	}

	static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t);
		int g = Math.round(((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1f - t) + (b & 255) * t);
		return (r << 16) | (g << 8) | bl;
	}

	static int hsv(float h, float s, float v) {
		int i = (int) (h * 6f);
		float f = h * 6f - i;
		float p = v * (1f - s);
		float q = v * (1f - f * s);
		float u = v * (1f - (1f - f) * s);
		float r;
		float g;
		float b;
		switch (i % 6) {
			case 0: r = v; g = u; b = p; break;
			case 1: r = q; g = v; b = p; break;
			case 2: r = p; g = v; b = u; break;
			case 3: r = p; g = q; b = v; break;
			case 4: r = u; g = p; b = v; break;
			default: r = v; g = p; b = q; break;
		}
		return ((int) (r * 255f) << 16) | ((int) (g * 255f) << 8) | (int) (b * 255f);
	}
}
