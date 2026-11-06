package de.melonkingde.pickupcat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class PickupCatClient implements ClientModInitializer {
	public static final String MOD_ID = "pickupcat";
	private static final float MIN_SCALE = 0.5f;
	private static final float MAX_SCALE = 6f;
	private static final float STEP = 0.25f;

	@Override
	public void onInitializeClient() {
		CatConfig config = CatConfig.load();
		CatController cat = new CatController(config);
		InventoryTracker tracker = new InventoryTracker(config, cat);

		// Resize keys (rebindable under Options > Controls > Key Binds > Pickup Cat)
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		KeyMapping biggerKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.pickupcat.bigger", InputConstants.KEY_RBRACKET, category));
		KeyMapping smallerKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.pickupcat.smaller", InputConstants.KEY_LBRACKET, category));

		ClientTickEvents.END_CLIENT_TICK.register(tracker::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (biggerKey.consumeClick()) {
				resize(config, cat, STEP);
			}
			while (smallerKey.consumeClick()) {
				resize(config, cat, -STEP);
			}
			config.pollReload();
		});
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MOD_ID, "cat"),
				(graphics, deltaTracker) -> cat.render(graphics));
	}

	private static void resize(CatConfig config, CatController cat, float delta) {
		float target = Math.round((config.scale + delta) * 4f) / 4f;
		target = Math.max(MIN_SCALE, Math.min(MAX_SCALE, target));
		boolean atLimit = target == config.scale;
		config.scale = target;
		if (!atLimit) {
			config.save();
		}
		cat.onResized(delta > 0f, atLimit);
	}
}
