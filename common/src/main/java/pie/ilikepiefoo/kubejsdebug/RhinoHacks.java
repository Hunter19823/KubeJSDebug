package pie.ilikepiefoo.kubejsdebug;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pie.ilikepiefoo.kubejsdebug.core.ContextAccessor;
import pie.ilikepiefoo.kubejsdebug.core.NativeFunctionMixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RhinoHacks {
    public static final LinkedHashMap<String, Object> BINDINGS = new LinkedHashMap<>();
    private static final Logger LOG = LogManager.getLogger();

    public static List<FunctionCall> getCallStack() throws ReflectiveOperationException {
        Object lastInterpreterFrame = ((ContextAccessor) Context.getCurrentContext()).getLastInterpreterFrame();
        List<FunctionCall> list = new ArrayList<>();
        Object current_frame = lastInterpreterFrame;
        int depth = 0;
        while (current_frame != null) {
            var fnOrScript = ReflectionAccessorTools.getDeclaredField(current_frame, "fnOrScript");
            if (!(fnOrScript instanceof NativeFunction nativeFunction && fnOrScript instanceof NativeFunctionMixin mixin)) {
                throw new RuntimeException("Not a native function or mixin failed.");
            }
            String name = nativeFunction.getFunctionName();
            // If this is the top level function, we don't want to include it.
            if (name.isBlank()) {
                break;
            }
            var stack = (Object[]) ReflectionAccessorTools.getDeclaredField(current_frame, "stack");
            var localShift = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "localShift");
            var emptyStackTop = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "emptyStackTop");
            var stackTop = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "savedStackTop");
            var callOperator = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "savedCallOp");
            var frameIndex = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "frameIndex");
            // stack[0 <= i < localShift]: arguments and local variables
            // stack[localShift <= i <= emptyStackTop]: used for local temporaries
            // stack[emptyStackTop < i < stack.length]: stack data
            // sDbl[i]: if stack[i] is UniqueTag.DOUBLE_MARK, sDbl[i] holds the number value
            Object[] argsAndVars = new Object[0];
            Object[] localTemporaries = new Object[0];
            Object[] stackData = new Object[0];
            if (0 < localShift) {
                argsAndVars = Arrays.copyOfRange(stack, 0, localShift);
            }
            if (localShift >= 0 && localShift <= emptyStackTop) {
                localTemporaries = Arrays.copyOfRange(stack, localShift, emptyStackTop);
            }
            if (emptyStackTop >= 0 && emptyStackTop < stack.length) {
                stackData = Arrays.copyOfRange(stack, emptyStackTop, stack.length);
            }
            int totalVariableCount = mixin.getParameterAndVariableCount();
            int parameterCount = mixin.getParameterCount();
            if (totalVariableCount > argsAndVars.length) {
                LOG.error("Parameter count mismatch! Expected {}, got {}", totalVariableCount, argsAndVars.length);
                throw new ReflectiveOperationException("Parameter count mismatch!");
            }
            FunctionCall call = new FunctionCall(name);
            for (int i = 0; i < parameterCount; i++) {
                call.parameters.put(mixin.getParameterOrVariableName(i), argsAndVars[i]);
            }
            for (int i = parameterCount; i < totalVariableCount; i++) {
                call.localDeclarations.put(mixin.getParameterOrVariableName(i), argsAndVars[i]);
            }
            list.add(call);
//            // This is the top level function, so we don't want to include it.
//            if (name.isBlank()) {
//                break;
//            }
//            Object[] functionArgs = (Object[]) args[depth++];
//            int totalVariableCount = mixin.getParameterAndVariableCount();
//            int parameterCount = mixin.getParameterCount();
//            if (parameterCount != functionArgs.length) {
//                LOG.error("Parameter count mismatch! Expected {}, got {}", parameterCount, functionArgs.length);
//                throw new ReflectiveOperationException("Parameter count mismatch!");
//            }
//            FunctionCall call = new FunctionCall(name, args[depth++]);
//
//            list.add(call);
            current_frame = ReflectionAccessorTools.getDeclaredField(current_frame, "parentFrame");
        }

        // Return all but the last element.
        return list;
    }

    public static class FunctionCall {
        public String function_name;
        public Map<String, Object> parameters = new TreeMap<>();
        public Map<String, Object> localDeclarations = new TreeMap<>();


        public FunctionCall(String function_name) {
            this.function_name = function_name;
        }

    }
}
