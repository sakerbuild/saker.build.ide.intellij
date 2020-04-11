package saker.build.ide.intellij.extension.params;

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import saker.build.ide.intellij.util.PluginCompatUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AbstractSakerExtensionPointBean extends AbstractExtensionPointBean {

    private static final Method INSTANTIATE_CLASS_METHOD = PluginCompatUtil
            .getMethod(AbstractExtensionPointBean.class, "instantiateClass", String.class, PicoContainer.class);

    //no @Override, as this method was introduced in a later version
    public PluginId getPluginId() {
        PluginDescriptor pluginDescriptor = getPluginDescriptor();
        return pluginDescriptor == null ? null : pluginDescriptor.getPluginId();
    }

    public final <T> T instantiateClassSaker(@NotNull String className, @NotNull PicoContainer container) {
        if (INSTANTIATE_CLASS_METHOD != null) {
            try {
                return (T) INSTANTIATE_CLASS_METHOD.invoke(this, className, container);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new PluginException(cause, getPluginId());
            } catch (IllegalAccessException e) {
                throw new PluginException(e, getPluginId());
            }
        }
        return instantiate(findExtensionClassSaker(className), container);
    }

    @NotNull
    public final <T> Class<T> findExtensionClassSaker(@NotNull String className) {
        try {
            return findClassSaker(className, myPluginDescriptor);
        } catch (ClassNotFoundException e) {
            throw new PluginException(e, getPluginId());
        }
    }

    public static <T> Class<T> findClassSaker(@NotNull String className,
            @Nullable PluginDescriptor pluginDescriptor) throws ClassNotFoundException {
        ClassLoader classLoader = pluginDescriptor == null ? AbstractExtensionPointBean.class.getClassLoader() :
                pluginDescriptor.getPluginClassLoader();
        //noinspection unchecked
        return (Class<T>) Class.forName(className, true, classLoader);
    }
}
