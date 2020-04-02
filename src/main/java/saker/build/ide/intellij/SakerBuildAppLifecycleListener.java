package saker.build.ide.intellij;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class SakerBuildAppLifecycleListener implements AppLifecycleListener, DumbAware {

    @Override
    public void appStarting(@Nullable Project projectFromCommandLine) {
        SakerBuildPlugin.init();
    }

    @Override
    public void appWillBeClosed(boolean isRestart) {
        SakerBuildPlugin.close();
    }

}
