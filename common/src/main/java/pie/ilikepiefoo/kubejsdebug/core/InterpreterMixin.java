package pie.ilikepiefoo.kubejsdebug.core;

import dev.latvian.mods.rhino.Interpreter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pie.ilikepiefoo.kubejsdebug.RhinoHacks;

@Mixin(value = Interpreter.class, remap = false)
public class InterpreterMixin {

    @Inject(
        method = "getArgsArray([Ljava/lang/Object;[DII)[Ljava/lang/Object;",
        at = @At("RETURN")
    )
    private static void listenForArgsArray(Object[] stack, double[] sDbl, int shift, int count, CallbackInfoReturnable<Object[]> cir) {
        RhinoHacks.LAST_ARGS_ARRAY.set(cir.getReturnValue());
    }
}
