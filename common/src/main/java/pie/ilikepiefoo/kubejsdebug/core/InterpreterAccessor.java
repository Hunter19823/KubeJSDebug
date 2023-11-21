package pie.ilikepiefoo.kubejsdebug.core;

import dev.latvian.mods.rhino.Interpreter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Interpreter.class, remap = false)
public interface InterpreterAccessor {

    @Invoker
    static int callGetIndex(byte[] bytes, int i) {
        throw new UnsupportedOperationException("Mixin failed to apply.");
    }
}
