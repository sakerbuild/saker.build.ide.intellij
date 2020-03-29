package saker.build.ide.intellij.extension.params;

import com.intellij.openapi.progress.ProgressIndicator;
import saker.build.ide.intellij.api.ISakerPlugin;

import java.util.Map;
import java.util.Set;

public interface IEnvironmentUserParameterContributor {
    public Set<UserParameterModification> contribute(ISakerPlugin plugin, Map<String, String> parameters,
            ProgressIndicator monitor);

    public default void dispose() {
    }
}
