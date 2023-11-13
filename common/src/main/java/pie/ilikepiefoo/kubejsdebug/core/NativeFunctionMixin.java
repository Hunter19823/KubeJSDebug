package pie.ilikepiefoo.kubejsdebug.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(dev.latvian.mods.rhino.NativeFunction.class)
public interface NativeFunctionMixin {
    @Invoker("getParamCount")
    int getParameterCount();

    @Invoker("getParamAndVarCount")
    int getParameterAndVariableCount();

    @Invoker("getParamOrVarName")
    String getParameterOrVariableName( int index );
}
