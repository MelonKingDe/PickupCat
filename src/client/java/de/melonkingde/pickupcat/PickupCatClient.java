package de.melonkingde.pickupcat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class PickupCatClient implements ClientModInitializer {
	public static final String MOD_ID = "pickupcat";

	@Override
	public void onInitializeClient() {
		CatConfig config = CatConfig.load();
		CatController cat = new CatController(config);
		InventoryTracker tracker = new InventoryTracker(config, cat);

		ClientTickEvents.END_CLIENT_TICK.register(tracker::tick);
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MOD_ID, "cat"),
				(graphics, deltaTracker) -> cat.render(graphics));
	}
}
