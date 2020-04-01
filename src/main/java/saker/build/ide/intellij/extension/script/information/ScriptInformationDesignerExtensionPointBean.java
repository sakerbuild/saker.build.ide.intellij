package saker.build.ide.intellij.extension.script.information;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.Nls;
import saker.build.ide.intellij.SakerBuildPlugin;

public class ScriptInformationDesignerExtensionPointBean extends AbstractExtensionPointBean {
    public static final ExtensionPointName<ScriptInformationDesignerExtensionPointBean> EP_NAME = ExtensionPointName
            .create(SakerBuildPlugin.ID + ".designer.script.information");

    @Attribute("id")
    public String id;

    @Attribute("implementationClass")
    public String implementationClass;

    @Attribute("displayName")
    @Nls(capitalization = Nls.Capitalization.Title)
    public String displayName;

    @Attribute("schemaId")
    public String schemaId;

    public IScriptInformationDesigner createContributor(Project project) {
        return instantiateClass(implementationClass,
                project == null ? ApplicationManager.getApplication().getPicoContainer() : project.getPicoContainer());
    }

    public String getSchemaId() {
        return schemaId;
    }
}
