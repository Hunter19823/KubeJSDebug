package pie.ilikepiefoo.kubejsdebug;

import dev.latvian.mods.kubejs.util.ConsoleJS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;

import static pie.ilikepiefoo.kubejsdebug.StringSerializers.formatStack;
import static pie.ilikepiefoo.kubejsdebug.StringSerializers.getString;

public class DebugBinding {
    private static final Logger LOG = LogManager.getLogger();
    public static String INDENT = "\t";
    public static int MAX_PRINT_DEPTH = 10;

    /**
     * This function is used to list all the variables, their types and their values,
     * in the current scope.
     */
    public void logAll(Object... parameters) throws ReflectiveOperationException {
        // Get the current line number and file name.
        String prefix = String.format("[%s] [%s] debug.logAll(Object...): ", KubeJSDebug.MOD_NAME, getScriptLine());
        consoleLogMultiline(getString(Map.entry("parameters", parameters)), prefix);
        consoleLogMultiline(getString(Map.entry("Script Scope", RhinoHacks.getNonBindingGlobals())), prefix);
        consoleLogMultiline(getString(Map.entry("ScriptCallStack", RhinoHacks.getCallStack())), prefix);
    }

    /**
     * Get the current file name and line number of the script. <br>
     * format: file:line
     *
     * @return The current file name and line number of the script.
     */
    public String getScriptLine() {
        return RhinoHacks.getLatestLineCall();
    }

    private void consoleLogMultiline(String message, String prefix) {
        Arrays.stream(message.split("\n")).forEachOrdered(line -> consoleLog(line, prefix));
    }

    private void consoleLog(Object message, String prefix) {
        ConsoleJS log = ConsoleJS.getCurrent(ConsoleJS.STARTUP);
        log.info(prefix + message);
    }

    public void trace() throws ReflectiveOperationException {
        String prefix = String.format("[%s] [%s] ", KubeJSDebug.MOD_NAME, getScriptLine());
        var stack = RhinoHacks.getCallStack();
        consoleLogMultiline(getString(stack), prefix);
        consoleLogMultiline(formatStack(stack), prefix);
    }

    public void log(Object... parameters) {
        String prefix = String.format("[%s] [%s] debug.log(): ", KubeJSDebug.MOD_NAME, getScriptLine());
        consoleLogMultiline(getString(parameters), prefix);
    }

    private void consoleLogMultiline(String message) {
        Arrays.stream(message.split("\n")).forEachOrdered(this::consoleLog);
    }

    private void consoleLog(Object message) {
        ConsoleJS log = ConsoleJS.getCurrent(ConsoleJS.STARTUP);
        log.info(message);
    }
}
