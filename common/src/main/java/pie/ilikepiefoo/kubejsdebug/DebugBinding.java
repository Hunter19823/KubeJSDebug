package pie.ilikepiefoo.kubejsdebug;

import dev.latvian.mods.kubejs.util.ConsoleJS;
import dev.latvian.mods.rhino.Context;

import java.util.Map;

public class DebugBinding {
    /**
     * This function is used to list all the variables, their types and their values,
     * in the current scope.
     */
    public void logAll() {
        ConsoleJS log = ConsoleJS.getCurrent(ConsoleJS.STARTUP);


        // Get the current line number and file name.
        String prefix = "[KubeJS Debug] [" +
                Context.getSourcePositionFromStack(new int[]{0}) +
                ":" +
                log.getScriptLine() +
                "] ";
        if (Context.getCurrentContext().sharedContextData.topLevelScope instanceof Map<?, ?> map) {
            map.forEach((key, value) -> log.log(
                    String.format(
                            "%s%s (%s): %s",
                            prefix,
                            key,
                            (value != null) ?
                                    ((value instanceof Class) ? value : value.getClass())
                                    : "null",
                            value
                    )
            ));
        }
    }
}
