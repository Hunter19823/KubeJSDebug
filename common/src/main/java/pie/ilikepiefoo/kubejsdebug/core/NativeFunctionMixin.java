package pie.ilikepiefoo.kubejsdebug.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = dev.latvian.mods.rhino.NativeFunction.class, remap = false)
public interface NativeFunctionMixin {
    @Invoker("getParamCount")
    int getParameterCount();

    @Invoker("getParamAndVarCount")
    int getParameterAndVariableCount();

    @Invoker("getParamOrVarName")
    String getParameterOrVariableName( int index );
}
