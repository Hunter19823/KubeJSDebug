package pie.ilikepiefoo.kubejsdebug.core;

import dev.latvian.mods.kubejs.script.BindingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BindingsEvent.class, remap = false)
public class BindingsEventMixin {

    @Inject(
            method = "add(Ljava/lang/String;Ljava/lang/Object;)V",
            at = @At("HEAD")
    )
    private void onAdd(String name, Object value, CallbackInfo ci) {
        if (value == null) {
        }

    }
}
