package pie.ilikepiefoo.kubejsdebug.forge;

import dev.architectury.platform.forge.EventBuses;
import pie.ilikepiefoo.kubejsdebug.KubeJSDebug;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KubeJSDebug.MOD_ID)
public class KubeJSDebugForge {
    public KubeJSDebugForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(KubeJSDebug.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        KubeJSDebug.init();
    }
}