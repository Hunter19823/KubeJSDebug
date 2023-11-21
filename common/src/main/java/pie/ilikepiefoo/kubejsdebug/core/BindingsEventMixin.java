package pie.ilikepiefoo.kubejsdebug.core;

import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pie.ilikepiefoo.kubejsdebug.RhinoHacks;

@Mixin(value = BindingsEvent.class, remap = false)
public abstract class BindingsEventMixin {

    @Inject(
            method = "add(Ljava/lang/String;Ljava/lang/Object;)V",
            at = @At("HEAD")
    )
    private void onAdd(String name, Object value, CallbackInfo ci) {
        if (value == null) {
            return;
        }
        RhinoHacks._registerBindings(this.getType(), name, value);
    }

    @Shadow
    public abstract ScriptType getType();
}
