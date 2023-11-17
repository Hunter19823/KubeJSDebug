package pie.ilikepiefoo.kubejsdebug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import dev.latvian.mods.rhino.Context;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;

import static pie.ilikepiefoo.kubejsdebug.StringSerializers.getString;

public class DebugBinding {
    private static final Logger LOG = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeAdapter(
                    ResourceLocation.class,
                    (JsonSerializer<ResourceLocation>)
                            (src, typeOfSrc, context) ->
                                    new JsonPrimitive("[" + src.getNamespace() + ":" + src.getPath() + "]")
            )
            .registerTypeAdapter(
                    Class.class,
                    (JsonSerializer<Class<?>>)
                            (src, typeOfSrc, context) ->
                                    new JsonPrimitive(src.getName())
            )
            .registerTypeAdapter(
                    Enum.class,
                    (JsonSerializer<Enum<?>>)
                            (src, typeOfSrc, context) ->
                                    new JsonPrimitive(src.name() + " (" + Arrays.toString(src.getClass().getEnumConstants()) + ")")
            )
            .registerTypeAdapter(
                    dev.latvian.mods.rhino.NativeFunction.class,
                    (JsonSerializer<dev.latvian.mods.rhino.NativeFunction>)
                            (src, typeOfSrc, context) -> {
                                StringBuilder sb = new StringBuilder();
                                sb.append(src.getFunctionName());
                                sb.append("(");
                                var ids = src.getIds();
                                for (var i = 0; i < ids.length; i++) {
                                    sb.append(ids[i]);
                                    if (i < ids.length - 1) {
                                        sb.append(", ");
                                    }
                                }
                                sb.append(")");

                                return new JsonPrimitive(sb.toString());
                            }
            )
            .registerTypeAdapter(
                    RhinoHacks.FunctionCall.class,
                    (JsonSerializer<RhinoHacks.FunctionCall>)
                            (src, typeOfSrc, context) -> {
                                JsonObject obj = new JsonObject();
                                obj.addProperty("name", src.function_name);
                                obj.add("parameters", context.serialize(src.parameters));
                                obj.add("local_declarations", context.serialize(src.localDeclarations));
                                return obj;
                            }
            )
            .enableComplexMapKeySerialization()
            .create();
    public static String INDENT = "\t";
    public static int MAX_PRINT_DEPTH = 10;

    /**
     * This function is used to list all the variables, their types and their values,
     * in the current scope.
     */
    public void logAll(Object... parameters) throws ReflectiveOperationException {
        // Get the current line number and file name.
        String prefix = String.format("[%s] [%s] debug.logAll() -> ", KubeJSDebug.MOD_NAME, getScriptLine());
        // Get the current scope.
        Context context = Context.getCurrentContext();
        logMultiline(getString(Map.entry("ScriptCallStack", RhinoHacks.getCallStack()), prefix));
        logMultiline(getString(Map.entry("Provided Parameters", parameters), prefix));
        if (context.sharedContextData.topLevelScope instanceof Map<?, ?> map) {
            logMultiline(getString(Map.entry("Global Scope", map), prefix));
        }
    }

    /**
     * Get the current file name and line number of the script. <br>
     * format: file:line
     *
     * @return The current file name and line number of the script.
     */
    public String getScriptLine() {
        return Context.getSourcePositionFromStack(new int[]{0}) + ":" + ConsoleJS.getCurrent(ConsoleJS.STARTUP).getScriptLine();
    }

    private <T> T log(T a) {
        ConsoleJS log = ConsoleJS.getCurrent(ConsoleJS.STARTUP);
        log.info(a);
        return a;
    }

    private void logMultiline(String a) {
        ConsoleJS log = ConsoleJS.getCurrent(ConsoleJS.STARTUP);
        Arrays.stream(a.split("\n")).forEachOrdered(log::info);
    }
}
