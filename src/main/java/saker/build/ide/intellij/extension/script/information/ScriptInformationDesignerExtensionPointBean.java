package saker.build.ide.intellij.extension.script.information;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.Nls;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.intellij.extension.params.AbstractSakerExtensionPointBean;

public class ScriptInformationDesignerExtensionPointBean extends AbstractSakerExtensionPointBean {
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
        return instantiateClassSaker(implementationClass,
                project == null ? ApplicationManager.getApplication().getPicoContainer() : project.getPicoContainer());
    }

    public String getSchemaId() {
        return schemaId;
    }
}
