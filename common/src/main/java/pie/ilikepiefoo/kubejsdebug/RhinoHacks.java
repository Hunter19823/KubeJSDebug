package pie.ilikepiefoo.kubejsdebug;

import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeFunction;
import dev.latvian.mods.rhino.NativeJavaMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import pie.ilikepiefoo.kubejsdebug.core.ContextAccessor;
import pie.ilikepiefoo.kubejsdebug.core.InterpreterAccessor;
import pie.ilikepiefoo.kubejsdebug.core.NativeFunctionMixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RhinoHacks {
    public static final Map<ScriptType, LinkedHashMap<String, Object>> BINDINGS = new LinkedHashMap<>();
    public static final ThreadLocal<Object[]> LAST_ARGS_ARRAY = new ThreadLocal<>();
    private static final Logger LOG = LogManager.getLogger();

    public static List<FunctionCall> getCallStack() throws ReflectiveOperationException {
        Object lastInterpreterFrame = ((ContextAccessor) Context.getCurrentContext()).getLastInterpreterFrame();
        List<FunctionCall> list = new ArrayList<>();
        Object current_frame = lastInterpreterFrame;
        // Arrays.copyOfRange(( (Interpreter.CallFrame) lastInterpreterFrame ).stack, ( (Interpreter.CallFrame) lastInterpreterFrame ).savedStackTop, ( (Interpreter.CallFrame) lastInterpreterFrame ).stack.length)
        while (current_frame != null) {
            var fnOrScript = ReflectionAccessorTools.getDeclaredField(current_frame, "fnOrScript");
            if (!(fnOrScript instanceof NativeFunction nativeFunction && fnOrScript instanceof NativeFunctionMixin mixin)) {
                throw new RuntimeException("Not a native function or mixin failed.");
            }
            String name = nativeFunction.getFunctionName();
            var stack = (Object[]) ReflectionAccessorTools.getDeclaredField(current_frame, "stack");
            var localShift = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "localShift");
            var emptyStackTop = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "emptyStackTop");
            if (list.isEmpty()) {
                // Saved stack top
                var savedStackTop = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "savedStackTop");
                // If this is the top level call, we need to use other means to get info about the call.
                var latest_method_call = stack[savedStackTop];
                String latest_method_name = latest_method_call.toString();
                if (latest_method_call instanceof NativeJavaMethod javaMethod) {
                    latest_method_name = javaMethod.getFunctionName();
                }
                var call = new FunctionCall(latest_method_name);
                call.sourceLine = getLatestLineCall();
                var latestArgs = RhinoHacks.LAST_ARGS_ARRAY.get();
                for (int i = 0; i < latestArgs.length; i++) {
                    call.parameters.put(String.format("arg%d", i), latestArgs[i]);
                }
                list.add(call);
            }

            // If this is the top level function, we don't want to include it.
            Object[] argsAndVars = getArgsAndVarsFromStack(localShift, stack);
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
                var varName = mixin.getParameterOrVariableName(i);
                var value = argsAndVars[i];
                if (Context.getCurrentContext().sharedContextData.topLevelScope.has(varName, Context.getCurrentContext().sharedContextData.topLevelScope)) {
                    value = Context.getCurrentContext().sharedContextData.topLevelScope.get(varName, Context.getCurrentContext().sharedContextData.topLevelScope);
                }
                call.localDeclarations.put(varName, value);
            }
            call.sourceLine = getScriptLineNumber(fnOrScript, current_frame);
            list.add(call);
            current_frame = ReflectionAccessorTools.getDeclaredField(current_frame, "parentFrame");
        }

        // Reverse the list so that the first call is at the top.
        // Shift all element properties down by one.
        for (int i = 0; i < list.size() - 1; i++) {
            var call = list.get(i);
            var next_call = list.get(i + 1);
            call.localDeclarations = next_call.localDeclarations;
            call.sourceLine = next_call.sourceLine;
            if (i == list.size() - 2) {
                list.remove(list.size() - 1);
            }
        }
        // Remove the last element.
        Collections.reverse(list);

        // Return all but the last element.
        return list;
    }

    public static String getLatestLineCall() {
        return Context.getSourcePositionFromStack(new int[]{0}) + ":" + ConsoleJS.getCurrent(ConsoleJS.STARTUP).getScriptLine();
    }

    @NotNull
    private static Object[] getArgsAndVarsFromStack(int localShift, Object[] stack) {
        Object[] argsAndVars = new Object[0];
//            Object[] localTemporaries = new Object[0];
//            Object[] stackData = new Object[0];
//            var stackTop = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "savedStackTop");
//            var callOperator = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "savedCallOp");
//            var frameIndex = (int) ReflectionAccessorTools.getDeclaredField(current_frame, "frameIndex");
        // stack[0 <= i < localShift]: arguments and local variables
        // stack[localShift <= i <= emptyStackTop]: used for local temporaries
        // stack[emptyStackTop < i < stack.length]: stack data
        // sDbl[i]: if stack[i] is UniqueTag.DOUBLE_MARK, sDbl[i] holds the number value
        if (0 < localShift) {
            argsAndVars = Arrays.copyOfRange(stack, 0, localShift);
        }
//            if (localShift >= 0 && localShift <= emptyStackTop) {
//                localTemporaries = Arrays.copyOfRange(stack, localShift, emptyStackTop);
//            }
//            if (emptyStackTop >= 0 && emptyStackTop < stack.length) {
//                stackData = Arrays.copyOfRange(stack, emptyStackTop, stack.length);
//            }
        return argsAndVars;
    }

    private static String getScriptLineNumber(Object functionOrScript, Object frame) throws ReflectiveOperationException {
        var idata = ReflectionAccessorTools.getDeclaredField(functionOrScript, "idata");
        var script = (String) ReflectionAccessorTools.getDeclaredField(idata, "itsSourceFile");
        return String.format("%s:%s", script, getLineNumber(idata, frame));
    }

    private static int getLineNumber(Object idata, Object frame) throws ReflectiveOperationException {
        var iCode = (byte[]) ReflectionAccessorTools.getDeclaredField(idata, "itsICode");
        var lineNo = (int) ReflectionAccessorTools.getDeclaredField(frame, "pcSourceLineStart");
        return InterpreterAccessor.callGetIndex(iCode, lineNo);
    }

    public static Object[] getLocalTemporaries(int localShift, int emptyStackTop, Object[] stack) {
        Object[] localTemporaries = new Object[0];
        if (localShift >= 0 && localShift <= emptyStackTop) {
            localTemporaries = Arrays.copyOfRange(stack, localShift, emptyStackTop);
        }
        return localTemporaries;
    }

    public static Object[] getStackData(int emptyStackTop, Object[] stack) {
        Object[] stackData = new Object[0];
        emptyStackTop = Math.max(emptyStackTop, 0);
        if (emptyStackTop < stack.length) {
            stackData = Arrays.copyOfRange(stack, emptyStackTop, stack.length);
        }
        return stackData;
    }

    public static void _registerBindings(ScriptType type, String name, Object value) {
        if (!BINDINGS.containsKey(type)) {
            BINDINGS.put(type, new LinkedHashMap<>());
        }
        BINDINGS.get(type).put(name, value);
    }

    public static LinkedHashMap<String, Object> getBindings(ScriptType type) {
        return BINDINGS.getOrDefault(type, new LinkedHashMap<>());
    }

    public static Map<String, Object> getNonBindingGlobals() throws ReflectiveOperationException {
        Context context = Context.getCurrentContext();
        Map<String, Object> globals = new TreeMap<>();
        GLOBAL_SCOPE:
        for (var id : context.sharedContextData.topLevelScope.getIds()) {
            if (!( id instanceof String name )) {
                continue GLOBAL_SCOPE;
            }
            for (var script_scope : BINDINGS.entrySet()) {
                if (script_scope.getValue().containsKey(name)) {
                    continue GLOBAL_SCOPE;
                }
            }
            globals.put(
                name,
                context.sharedContextData.topLevelScope.get(
                    name,
                    context.sharedContextData.topLevelScope
                )
            );
        }

        return globals;
    }

    public static Map<ScriptType, LinkedHashMap<String, Object>> getGlobalBindings() {
        return BINDINGS;
    }

    public static class FunctionCall {
        public String function_name;
        public String sourceLine;
        public Map<String, Object> parameters = new TreeMap<>();
        public Map<String, Object> localDeclarations = new TreeMap<>();


        public FunctionCall(String function_name) {
            this.function_name = function_name;
        }

    }
}
