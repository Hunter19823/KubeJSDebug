package pie.ilikepiefoo.kubejsdebug;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionAccessorTools {

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
}
