package pie.ilikepiefoo.kubejsdebug;

import com.google.common.collect.Lists;
import dev.latvian.mods.rhino.Undefined;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class StringSerializers {

    private static final ThreadLocal<HashSet<Object>> seen = ThreadLocal.withInitial(HashSet::new);


    public static String getString(Object o, String prefix) {
        if (o == null) {
            return "null";
        }
        String value = reformat(o, 0);
        seen.get().clear();
        return value.replace("\n", "\n" + prefix);
    }

    private static String reformat(Object o, int depth) {
        if (o == null) {
            return "null";
        }

        if (o instanceof Undefined) {
            // Represent undefined as "Value"
            return "undefined";
        }

        if (o instanceof String string) {
            // Represent strings as "Value"
            return "\"" + (string) + "\"";
        }

        if (o instanceof Number number) {
            // Represent numbers as "Value"
            return number.toString();
        }

        if (o instanceof Boolean bool) {
            // Represent booleans as "Value"
            return bool.toString();
        }

        if (o instanceof Character character) {
            // Represent characters as "Value"
            return "'" + character + "'";
        }

        if (o instanceof Enum<?> enum_) {
            // Represent enums as "Value {EnumClass={PossibleValues}}"
            return String.format(
                    "(Enum) %s (Ordinal=%d) {%s=%s}",
                    enum_.name(),
                    enum_.ordinal(),
                    enum_.getClass().toGenericString(),
                    Arrays.toString(
                            o.getClass().getEnumConstants()
                    )
            );
        }

        if (o instanceof Class<?> class_) {
            // Represent classes as "Value {ClassName}"
            return String.format(
                    "(Class) %s",
                    class_.toGenericString()
            );
        }

        if (seen.get().contains(o)) {
            return String.format("[Circular Reference] (%s) %s", o.getClass().toGenericString(), o);
        }
        if (depth > DebugBinding.MAX_PRINT_DEPTH) {
            return String.format("(%s) %s", o.getClass().toGenericString(), o);
        }

        String newLinePrefix = "\n" + DebugBinding.INDENT.repeat(depth);
        seen.get().add(o);

        if (o instanceof Map.Entry<?, ?> stringEntry) {
            return String.format(
                    "%s = %s",
                    reformat(stringEntry.getKey(), depth + 1),
                    reformat(stringEntry.getValue(), depth + 1)
            );
        }

        if (o instanceof RhinoHacks.FunctionCall functionCall) {
            // Represent function calls as:
            // FunctionName (
            //     paramName1 = paramValue1
            //     paramName2 = paramValue2
            //     ...
            // ) {
            //    localName1 = localValue1
            //    localName2 = localValue2
            //    ...
            // }
            // If the function has no parameters or local variables, it will be represented as:
            // FunctionName ()
            // If the function has no local variables, it will be represented as:
            // FunctionName (
            //     paramName1 = paramValue1
            //     paramName2 = paramValue2
            // )

            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(functionCall.getClass().toGenericString());
            sb.append(") ");
            sb.append(functionCall.function_name);
            sb.append(" (");
            if (!functionCall.parameters.isEmpty()) {
                functionCall.parameters.forEach(
                        (key, value) ->
                                appendEntry(sb, key, value, depth + 1)
                );
                sb.append(newLinePrefix);
            }
            sb.append(")");
            if (!functionCall.localDeclarations.isEmpty()) {
                sb.append(" {");
                functionCall.localDeclarations.forEach(
                        (key, value) ->
                                appendEntry(sb, key, value, depth + 1)
                );
                sb.append(newLinePrefix).append("}");
            }
        }

        if (o instanceof Map<?, ?> map) {
            // Represent maps as:
            // (Type, size=value) = {
            //     key1 = value1
            //     key2 = value2
            //     ...
            // }
            // If the map is empty, it will be represented as:
            // {}
            StringBuilder sb = new StringBuilder();
            sb.append("(")
                    .append(o.getClass().toGenericString())
                    .append(", size=")
                    .append(map.size())
                    .append(")");
            sb.append("{");
            if (!map.isEmpty()) {
                map.forEach(
                        (key, value) ->
                                appendEntry(sb, key, value, depth + 1)
                );
                sb.append(newLinePrefix);
            }
            sb.append("}");
            return sb.toString();
        }

        if (o instanceof Collection<?> collection) {
            // Represent collections as:
            // [
            //     value1
            //     value2
            //     ...
            // ]
            // If the collection is empty, it will be represented as:
            // []
            StringBuilder sb = new StringBuilder();
            appendList(o, depth, sb, collection);
            return sb.toString();
        }

        if (o instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            var collection = Lists.newArrayList(iterable);
            appendList(o, depth, sb, collection);
            return sb.toString();
        }

        if (o instanceof Object[] array) {
            StringBuilder sb = new StringBuilder();
            var collection = Lists.newArrayList(array);
            appendList(o, depth, sb, collection);
            return sb.toString();
        }

        return String.format("(%s) %s", o.getClass().toGenericString(), o);
    }

    private static void appendList(Object o, int depth, StringBuilder sb, Collection<?> collection) {
        String newLinePrefix = "\n" + DebugBinding.INDENT.repeat(depth);
        sb.append("(")
                .append(o.getClass().toGenericString())
                .append(", size=")
                .append(collection.size())
                .append(")");
        sb.append("[");
        if (!collection.isEmpty()) {
            collection.forEach(
                    value ->
                            appendAndIndent(sb, value, depth + 1)
            );
            sb.append(newLinePrefix);
        }
        sb.append("]");
    }

    private static void appendEntry(StringBuilder sb, Object key, Object value, int depth) {
        String newLinePrefix = "\n" + DebugBinding.INDENT.repeat(depth);
        sb
                .append(newLinePrefix)
                .append(
                        reformat(
                                Map.entry(key, value),
                                depth + 1
                        )
                );
    }

    private static void appendAndIndent(StringBuilder sb, Object value, int depth) {
        String newLinePrefix = "\n" + DebugBinding.INDENT.repeat(depth);
        sb
                .append(newLinePrefix)
                .append(reformat(value, depth + 1));
    }
}
