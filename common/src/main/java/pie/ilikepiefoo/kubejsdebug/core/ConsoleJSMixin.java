package pie.ilikepiefoo.kubejsdebug.core;

import dev.latvian.mods.kubejs.util.ConsoleJS;
import dev.latvian.mods.rhino.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ConsoleJS.class, remap = false)
public class ConsoleJSMixin {
    @Inject(
        method = "getCurrent(Ldev/latvian/mods/kubejs/util/ConsoleJS;)Ldev/latvian/mods/kubejs/util/ConsoleJS;",
        at = @At(
            value = "HEAD"
        ),
        cancellable = true
    )
    private static void getCurrent(ConsoleJS console, CallbackInfoReturnable<ConsoleJS> cir) {
        if (Context.getCurrentContext().sharedContextData == null) {
            cir.setReturnValue(console);
        }
    }
}
