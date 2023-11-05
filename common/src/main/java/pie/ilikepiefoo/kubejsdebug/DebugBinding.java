package pie.ilikepiefoo.kubejsdebug;

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaMethod;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

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
                    NativeJavaMethod.class,
                    (JsonSerializer<NativeJavaMethod>)
                            (src, typeOfSrc, context) ->
                                    new JsonPrimitive(src.getFunctionName() + src)
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
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(com.google.gson.FieldAttributes f) {
                    return !f.hasModifier(Modifier.PUBLIC);
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .enableComplexMapKeySerialization()
            .create();


    /**
     * This function is used to list all the variables, their types and their values,
     * in the current scope.
     */
    public void logAll() {
        // Get the current line number and file name.
        String prefix = String.format("[%s] [%s] ", KubeJSDebug.MOD_NAME, getScriptLine());
        if (Context.getCurrentContext().sharedContextData.topLevelScope instanceof Map<?, ?> map) {
            LOG.info(Context.getCurrentContext().sharedContextData.topLevelScope);

            map.forEach((key, value) -> log(
                    String.format(
                            "%s%s: %s",
                            prefix,
                            key,
                            getString(value)
                    )
            ));
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

    private String getString(Object o) {
        if (o == null) {
            return "null";
        }

        if (o instanceof String) {
            return (String) o;
        }

        if (o instanceof Class) {
            return ((Class<?>) o).getName();
        }

        return String.format("%s (%s)", GSON.toJson(o), o.getClass());
    }
}
