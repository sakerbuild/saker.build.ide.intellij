package saker.build.ide.intellij;

import java.nio.file.Path;

public class ImplementationStartArguments {
    public final Path sakerJarPath;
    public final Path pluginDirectory;

    public ImplementationStartArguments(Path sakerJarPath, Path pluginDirectory) {
        this.sakerJarPath = sakerJarPath;
        this.pluginDirectory = pluginDirectory;
    }
}
