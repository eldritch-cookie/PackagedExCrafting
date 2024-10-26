package thelm.packagedexcrafting;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import thelm.packagedexcrafting.event.CommonEventHandler;

@Mod(PackagedExCrafting.MOD_ID)
public class PackagedExCrafting {

	public static final String MOD_ID = "packagedexcrafting";

	public PackagedExCrafting(IEventBus modEventBus) {
		CommonEventHandler.getInstance().onConstruct(modEventBus);
	}
}
