package pie.ilikepiefoo.kubejsdebug.core;

import dev.latvian.mods.rhino.Scriptable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = dev.latvian.mods.rhino.Context.class, remap = false)
public interface ContextAccessor {

    @Accessor
    Object getLastInterpreterFrame();

    @Accessor
    Scriptable getTopCallScope();

}
