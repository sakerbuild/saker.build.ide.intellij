package saker.build.ide.intellij.extension.params;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import saker.build.ide.intellij.SakerBuildPlugin;

public class EnvironmentUserParameterContributorProviderExtensionPointBean extends AbstractUserParameterExtensionPointBean {
    public static final ExtensionPointName<EnvironmentUserParameterContributorProviderExtensionPointBean> EP_NAME = ExtensionPointName
            .create(SakerBuildPlugin.ID + ".params.environment.contributor");

    public IEnvironmentUserParameterContributor createContributor() {
        return instantiateClass(implementationClass, ApplicationManager.getApplication().getPicoContainer());
    }

}
