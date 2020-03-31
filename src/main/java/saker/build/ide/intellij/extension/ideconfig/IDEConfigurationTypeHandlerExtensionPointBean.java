package saker.build.ide.intellij.extension.ideconfig;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.Nls;
import saker.build.ide.intellij.SakerBuildPlugin;

public class IDEConfigurationTypeHandlerExtensionPointBean extends AbstractExtensionPointBean {
    public static final ExtensionPointName<IDEConfigurationTypeHandlerExtensionPointBean> EP_NAME = ExtensionPointName
            .create(SakerBuildPlugin.ID + ".ide.configuration.typeHandler");

    @Attribute("id")
    public String id;

    @Attribute("displayName")
    @Nls(capitalization = Nls.Capitalization.Title)
    public String name;

    @Attribute("implementationClass")
    public String implementationClass;

    @Attribute("type")
    public String type;

    @Attribute("typeName")
    public String typeName;

    public String getType() {
        return type;
    }

    public String getTypeName() {
        return typeName;
    }

    public IIDEConfigurationTypeHandler createTypeHandler(Project project) {
        return instantiateClass(implementationClass,
                project == null ? ApplicationManager.getApplication().getPicoContainer() : project.getPicoContainer());
    }
}
