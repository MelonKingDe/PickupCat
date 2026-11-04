package de.melonkingde.pickupcat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Watches the player's inventory (client side only, no mixins).
 * <ul>
 *   <li>Total of an item goes up   -> the cat eats it.</li>
 *   <li>Total goes down AND a fresh item entity appears right at the player's
 *       drop position within ~1 second -> it was dropped, the cat throws it up.</li>
 * </ul>
 * Items are identified by their SkyBlock id (custom_data "id") so changing
 * lore/stats doesn't count as a pickup.
 */
final class InventoryTracker {
	private static final int SETTLE_TICKS = 60;      // ignore inventory reloads after world switches
	private static final int DROP_WINDOW_TICKS = 24; // max delay between inventory change and item entity
	private static final Set<String> VARIANT_IDS = Set.of(
			"ENCHANTED_BOOK", "PET", "POTION", "RUNE", "UNIQUE_RUNE", "ATTRIBUTE_SHARD", "NEW_BOOK");

	private static final class Pending {
		final String key;
		int count;
		final int tick;
		final ItemStack sample;

		Pending(String key, int count, int tick, ItemStack sample) {
			this.key = key;
			this.count = count;
			this.tick = tick;
			this.sample = sample;
		}
	}

	private final CatConfig cfg;
	private final CatController cat;

	private Map<String, Integer> stable = new HashMap<>();
	private Map<String, Integer> candidate;
	private final Map<String, ItemStack> samples = new HashMap<>();
	private final Set<Integer> knownEntities = new HashSet<>();
	private final List<Pending> decreases = new ArrayList<>();
	private final List<Pending> dropEntities = new ArrayList<>();

	private ClientLevel lastLevel;
	private int settle;
	private int tick;

	InventoryTracker(CatConfig cfg, CatController cat) {
		this.cfg = cfg;
		this.cat = cat;
	}

	void tick(Minecraft mc) {
		if (!cfg.enabled) {
			return;
		}
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null) {
			lastLevel = null;
			reset();
			return;
		}
		tick++;
		if (level != lastLevel) {
			lastLevel = level;
			reset();
			settle = SETTLE_TICKS;
		}

		Map<String, Integer> now = snapshot(player);
		scanEntities(player, level);

		if (settle > 0) {
			settle--;
			stable = now;
			candidate = null;
			decreases.clear();
			dropEntities.clear();
			return;
		}

		// 1-tick debounce: a change must be seen on two consecutive ticks
		if (now.equals(stable)) {
			candidate = null;
		} else if (now.equals(candidate)) {
			diff(now, player);
			stable = now;
			candidate = null;
		} else {
			candidate = now;
		}
		matchDrops();
	}

	private void reset() {
		stable = new HashMap<>();
		candidate = null;
		samples.clear();
		knownEntities.clear();
		decreases.clear();
		dropEntities.clear();
	}

	private Map<String, Integer> snapshot(LocalPlayer player) {
		Map<String, Integer> map = new HashMap<>();
		Inventory inv = player.getInventory();
		int size = inv.getContainerSize();
		for (int i = 0; i < size; i++) {
			add(map, inv.getItem(i));
		}
		// the stack on the mouse cursor still belongs to the player
		add(map, player.containerMenu.getCarried());
		return map;
	}

	private void add(Map<String, Integer> map, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		String key = keyOf(stack);
		map.merge(key, stack.getCount(), Integer::sum);
		if (samples.size() < 1024) {
			samples.putIfAbsent(key, stack.copyWithCount(1));
		}
	}

	private void diff(Map<String, Integer> now, LocalPlayer player) {
		Set<String> keys = new HashSet<>(stable.keySet());
		keys.addAll(now.keySet());
		boolean inGui = player.containerMenu != player.inventoryMenu;
		for (String key : keys) {
			int delta = now.getOrDefault(key, 0) - stable.getOrDefault(key, 0);
			ItemStack sample = samples.get(key);
			if (sample == null || delta == 0) {
				continue;
			}
			if (delta > 0) {
				if (inGui && !cfg.eatFromGuis) {
					continue;
				}
				cat.enqueue(CatController.Kind.EAT, key, delta, sample);
			} else {
				decreases.add(new Pending(key, -delta, tick, sample));
			}
		}
	}

	/** Thrown items spawn at (player x, eyeY - 0.3, player z). */
	private void scanEntities(LocalPlayer player, ClientLevel level) {
		AABB box = player.getBoundingBox().inflate(8.0);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);
		Set<Integer> present = new HashSet<>();
		boolean allow = settle == 0;
		double px = player.getX();
		double py = player.getEyeY() - 0.3;
		double pz = player.getZ();
		for (ItemEntity entity : items) {
			int id = entity.getId();
			present.add(id);
			if (knownEntities.add(id) && allow) {
				double dx = entity.getX() - px;
				double dy = entity.getY() - py;
				double dz = entity.getZ() - pz;
				if (dx * dx + dz * dz <= 6.25 && Math.abs(dy) <= 1.0) {
					ItemStack stack = entity.getItem();
					if (!stack.isEmpty()) {
						dropEntities.add(new Pending(keyOf(stack), stack.getCount(), tick, stack.copyWithCount(1)));
					}
				}
			}
		}
		knownEntities.retainAll(present);
	}

	private void matchDrops() {
		for (Pending dec : decreases) {
			for (Pending ent : dropEntities) {
				if (dec.count <= 0) {
					break;
				}
				if (ent.count > 0 && dec.key.equals(ent.key)) {
					int m = Math.min(dec.count, ent.count);
					dec.count -= m;
					ent.count -= m;
					cat.enqueue(CatController.Kind.VOMIT, dec.key, m, dec.sample);
				}
			}
		}
		decreases.removeIf(p -> p.count <= 0 || tick - p.tick > DROP_WINDOW_TICKS);
		dropEntities.removeIf(p -> p.count <= 0 || tick - p.tick > DROP_WINDOW_TICKS);
	}

	/** SkyBlock id if present, otherwise registry id (+ custom name). */
	static String keyOf(ItemStack stack) {
		String sbId = "";
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data != null) {
			sbId = data.copyTag().getStringOr("id", "");
		}
		if (!sbId.isEmpty()) {
			if (VARIANT_IDS.contains(sbId)) {
				return sbId + "|" + ChatFormatting.stripFormatting(stack.getHoverName().getString());
			}
			return sbId;
		}
		String reg = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		if (stack.has(DataComponents.CUSTOM_NAME)) {
			return reg + "|" + ChatFormatting.stripFormatting(stack.getHoverName().getString());
		}
		return reg;
	}
}
