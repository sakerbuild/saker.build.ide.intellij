package saker.build.ide.intellij.extension.params;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import saker.build.ide.intellij.SakerBuildPlugin;

public class ExecutionUserParameterContributorProviderExtensionPointBean extends AbstractUserParameterExtensionPointBean {
    public static final ExtensionPointName<ExecutionUserParameterContributorProviderExtensionPointBean> EP_NAME = ExtensionPointName
            .create(SakerBuildPlugin.ID + ".params.execution.contributor");

    public IExecutionUserParameterContributor createContributor(Project project) {
        return instantiateClass(implementationClass, project.getPicoContainer());
    }
}
