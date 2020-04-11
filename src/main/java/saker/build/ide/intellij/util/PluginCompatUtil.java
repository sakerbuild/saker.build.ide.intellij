package saker.build.ide.intellij.util;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class PluginCompatUtil {

    public static <T> List<T> getExtensionList(ExtensionPointName<T> ep) {
        return Arrays.asList(ep.getExtensions());
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>... args) {
        try {
            return clazz.getMethod(name, args);
        } catch (Exception e) {
            System.err.println("Plugin version compatibility error: " + e);
            return null;
        }
    }

    public static Method getMethod(String classname, String name, Class<?>... args) {
        try {
            return Class.forName(classname, false, PluginCompatUtil.class.getClassLoader()).getMethod(name, args);
        } catch (Exception e) {
            System.err.println("Plugin version compatibility error: " + e);
            return null;
        }
    }
}
