package pie.ilikepiefoo.kubejsdebug.fabric;

import pie.ilikepiefoo.kubejsdebug.KubeJSDebug;
import net.fabricmc.api.ModInitializer;

public class KubeJSDebugFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KubeJSDebug.init();
    }
}