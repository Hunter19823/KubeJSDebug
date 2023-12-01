package pie.ilikepiefoo.kubejsdebug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionAccessorTools {
    public static final Logger LOG = LogManager.getLogger();

    public static Object getDeclaredField(Object target, String name) throws ReflectiveOperationException {
        Field field;
        try {
            field = target.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new ReflectiveOperationException("Unable to get field: " + name + " from: " + target.getClass().getName(), e);
        }
        if (!field.trySetAccessible()) {
            throw new ReflectiveOperationException("Unable to set field: " + name + " from: " + target.getClass().getName() + " accessible!");
        }
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new ReflectiveOperationException("Unable to get field: " + name + " from: " + target.getClass().getName(), e);
        }
    }

    public static Object getAccessor(Object target, String accessorName) throws ReflectiveOperationException {
        Method method;
        try {
            method = target.getClass().getMethod(accessorName);
        } catch (NoSuchMethodException e) {
            throw new ReflectiveOperationException("Unable to get accessor: " + accessorName + " from: " + target.getClass().getName(), e);
        }
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new ReflectiveOperationException("Unable to invoke accessor: " + accessorName + " from: " + target.getClass().getName(), e);
        }
    }

    public static String getCallingMethod(int i) {
        var callee = Thread.currentThread().getStackTrace()[i];
        return String.format(
            "%s.%s",
            StringSerializers.getClassNameWithoutPackages(callee.getClassName()),
            callee.getMethodName()
        );
    }

    public static Map<String, Object> getPublicAccessors(Object target) {
        if (target == null) {
            return Map.of();
        }

        return Arrays.stream(target.getClass().getMethods())
            .filter(
                (method) -> method.getParameterCount() == 0 && !method.getReturnType().equals(Void.TYPE)
            ).filter(
                (method) -> method.getName().startsWith("get") || method.getName().startsWith("is")
            ).filter(
                (method) -> Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
            ).filter(
                (method) -> !method.getReturnType().isAssignableFrom(method.getDeclaringClass())
            ).sorted(
                Comparator.comparing(Method::toGenericString)
            ).map(
                (method) -> {
                    try {
                        Object value = Modifier.isStatic(method.getModifiers()) ? method.invoke(null) : method.invoke(target);
                        if (value == null) {
                            value = "null";
                        }
                        return Map.entry(
                            method.toGenericString(),
                            value
                        );
                    } catch (Exception e) {
                        LOG.error("Unable to invoke method: " + method.toGenericString(), e);
                        return null;
                    }
                }
            ).filter(Objects::nonNull)
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                )
            );
    }

    public static Map<String, Object> getPublicFields(Object target) {
        if (target == null) {
            return Map.of();
        }

        return Arrays.stream(target.getClass().getFields())
            .filter(
                (field) -> Modifier.isPublic(field.getModifiers())
            ).sorted(
                Comparator.comparing(Field::toGenericString)
            ).map(
                (field) -> {
                    try {
                        return Map.entry(
                            field.toGenericString(),
                            Modifier.isStatic(field.getModifiers()) ? field.get(null) : field.get(target)
                        );
                    } catch (Exception e) {
                        LOG.error("Unable to retrieve field value: " + field.toGenericString(), e);
                        return null;
                    }
                }
            ).filter(Objects::nonNull)
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                )
            );
    }

    public static Set<String> getAllPublicAttributes(Object target) {
        if (target == null) {
            return Set.of();
        }

        var fields = Arrays.stream(target.getClass().getFields())
            .filter(
                (field) -> Modifier.isPublic(field.getModifiers())
            ).filter(
                (field) -> !field.getType().isAssignableFrom(field.getDeclaringClass())
            ).map(
                Field::toGenericString
            )
            .toList();
        var methods = Arrays.stream(target.getClass().getMethods())
            .filter(
                (method) -> Modifier.isPublic(method.getModifiers())
            ).sorted(
                Comparator.comparing(Method::toGenericString)
            ).map(
                Method::toGenericString
            )
            .toList();

        // Combine the two lists into an ordered set.
        return
            Set.copyOf(
                Stream.of(
                    fields,
                    methods
                ).flatMap(
                    List::stream
                ).collect(
                    Collectors.toList()
                )
            );
    }

}
