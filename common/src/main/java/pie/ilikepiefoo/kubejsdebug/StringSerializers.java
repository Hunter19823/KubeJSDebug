package pie.ilikepiefoo.kubejsdebug;

import com.google.common.collect.Lists;
import dev.latvian.mods.kubejs.core.AsKJS;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.kubejs.util.WithAttachedData;
import dev.latvian.mods.kubejs.util.WrappedJS;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.util.SpecialEquality;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class StringSerializers {

    private static final ThreadLocal<HashSet<Object>> seen = ThreadLocal.withInitial(HashSet::new);


    private static final String PACKAGE_REGEX = "([a-z_$0-9]+\\.)+";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(PACKAGE_REGEX);

    public static String getString(Object o) {
        if (o == null) {
            return "null";
        }
        String value = reformat(o, 0);
        seen.get().clear();
        return value;
    }

    public static String formatStack(List<RhinoHacks.FunctionCall> stack) {
        StringJoiner joiner = new StringJoiner(" -> ");
        AtomicInteger indent_level = new AtomicInteger();
        stack.forEach(
            call ->
                joiner.add(
                    "\n" + "  ".repeat((indent_level.getAndIncrement()))
                        +
                        (
                            call.sourceLine == null ? call.function_name :
                                String.format(
                                    "[%s]%s(%s)",
                                    call.sourceLine,
                                    call.function_name,
                                    call.parameters.keySet()
                                        .stream()
                                        .map(
                                            key ->
                                                String.format(
                                                    "%s=%s",
                                                    key,
                                                    getString(
                                                        call.parameters.get(key)
                                                    )
                                                )
                                        )
                                        .reduce((a, b) -> a + ", " + b)
                                        .orElse("")
                                )
                        )
                )
        );
        return joiner.toString();
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
            return number + " (" + number.getClass().getSimpleName() + ")";
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
                getClassNameWithoutPackages(o.getClass().toGenericString()),
                Arrays.toString(
                    o.getClass().getEnumConstants()
                )
            );
        }

        if (o instanceof Class<?> class_) {
            // Represent classes as "Value {ClassName}"
            return String.format(
                "(Class) %s",
                getClassNameWithoutPackages(class_.toGenericString())
            );
        }

        if (seen.get().contains(o)) {
            return String.format("[Repeated Reference] (%s) %s", getClassNameWithoutPackages(o.getClass().toGenericString()), o);
        }
        if (depth > DebugBinding.MAX_PRINT_DEPTH) {
            return String.format("(%s) %s", getClassNameWithoutPackages(o.getClass().toGenericString()), o);
        }

        String newLinePrefix = "\n" + DebugBinding.INDENT.repeat(depth);
        seen.get().add(o);

        if (o instanceof ListJS listJS) {
            // Represent ListJS as:
            // (ListJS, size=value) [
            //     value1
            //     value2
            //     ...
            // ]
            // If the list is empty, it will be represented as:
            // (ListJS, size=value) []
            StringBuilder sb = new StringBuilder();
            appendList(o, depth, sb, listJS);
            return sb.toString();
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
            sb.append(functionCall.sourceLine);
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
            return sb.toString();
        }

        if (o instanceof Map.Entry<?, ?> stringEntry) {
            return String.format(
                "%s = %s",
                reformat(stringEntry.getKey(), depth + 1),
                reformat(stringEntry.getValue(), depth + 1)
            );
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
                .append(getClassNameWithoutPackages(o.getClass().toGenericString()))
                .append(", size=")
                .append(map.size())
                .append(")");
            sb.append("{");
            if (map.isEmpty()) {
                sb.append("}");
                return sb.toString();
            }
            if (depth <= 6) {
                map.forEach(
                    (key, value) ->
                        appendEntry(sb, key, value, depth + 1)
                );
                sb.append(newLinePrefix);
                sb.append("}");
                return sb.toString();
            }
            // If this is not a top-level call, then this is a nested toString call.
            // As a result we will only print the first x elements of the map.
            // This is to prevent large maps from causing the console to lag.
            int i = 0;
            for (var entry : map.entrySet()) {
                appendEntry(sb, entry.getKey(), entry.getValue(), depth + 1);
                if (i++ >= DebugBinding.MAX_NESTED_OBJECT_LENGTH) {
                    break;
                }
            }
            if (i >= DebugBinding.MAX_NESTED_OBJECT_LENGTH) {
                appendAndIndent(sb, "...", depth + 1);
            }
            sb.append(newLinePrefix);
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

        if (
            depth <= 2 &&
                (
                    o instanceof EventJS ||
                        o instanceof SpecialEquality ||
                        o instanceof WithAttachedData ||
                        o instanceof WrappedJS ||
                        o instanceof AsKJS
                )
        ) {
            return String.format(
                "(%s) %s",
                getClassNameWithoutPackages(o.getClass().toGenericString()),
                reformat(
                    Map.ofEntries(
                        Map.entry("toString", o.toString()),
                        Map.entry("Public Fields", ReflectionAccessorTools.getPublicFields(o)),
                        Map.entry("Public Beans", ReflectionAccessorTools.getPublicAccessors(o)),
                        Map.entry("Public Attributes", ReflectionAccessorTools.getAllPublicAttributes(o))
                    ),
                    depth + 1
                )
            );
        }

        return String.format("(%s) %s", getClassNameWithoutPackages(o.getClass().toGenericString()), o);
    }

    private static void appendList(Object o, int depth, StringBuilder sb, Collection<?> collection) {
        String newLinePrefix = "\n" + DebugBinding.INDENT.repeat(depth);
        sb.append("(")
            .append(getClassNameWithoutPackages(o.getClass().toGenericString()))
            .append(", size=")
            .append(collection.size())
            .append(")");
        sb.append("[");
        if (collection.isEmpty()) {
            sb.append("]");
            return;
        }
        if (depth <= 6) {
            collection.forEach(
                value ->
                    appendAndIndent(sb, value, depth + 1)
            );
            sb.append(newLinePrefix);
            sb.append("]");
            return;
        }
        // If this is not a top-level call, then this is a nested toString call.
        // As a result we will only print the first x elements of the collection.
        // This is to prevent large collections from causing the console to lag.
        int i = 0;
        for (i = 0; i < DebugBinding.MAX_NESTED_ARRAY_LENGTH && i < collection.size(); i++) {
            appendAndIndent(sb, collection.toArray()[i], depth + 1);
        }
        if (i >= DebugBinding.MAX_NESTED_ARRAY_LENGTH) {
            appendAndIndent(sb, "...", depth + 1);
        }
        sb.append(newLinePrefix);
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

    public static String getClassNameWithoutPackages(String className) {
        var matcher = PACKAGE_PATTERN.matcher(className);
        while (matcher.find()) {
            className = className.replace(matcher.group(), "");
        }
        return className;
    }
}
