package saker.build.ide.intellij.extension.params;

import com.intellij.openapi.progress.ProgressIndicator;
import saker.build.ide.intellij.api.ISakerProject;

import java.util.Map;
import java.util.Set;

public interface IExecutionUserParameterContributor {
    public Set<UserParameterModification> contribute(ISakerProject project, Map<String, String> parameters,
            ProgressIndicator monitor);

    public default void dispose() {
    }
}
