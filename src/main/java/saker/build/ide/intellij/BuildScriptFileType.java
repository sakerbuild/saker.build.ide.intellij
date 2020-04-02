package saker.build.ide.intellij;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class BuildScriptFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile, DumbAware {
    public static final BuildScriptFileType INSTANCE = new BuildScriptFileType();

    public BuildScriptFileType() {
        super(BuildScriptLanguage.INSTANCE);
    }

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        try {
            VirtualFileSystem fs = file.getFileSystem();
            if (!(fs instanceof LocalFileSystem)) {
                //only support local files
                return false;
            }
            ProjectManager pm = ProjectManager.getInstance();
            for (Project project : pm.getOpenProjects()) {
                if (!SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project)) {
                    continue;
                }
                ISakerBuildPluginImpl pluginimpl = SakerBuildPlugin.getPluginImpl();
                ISakerBuildProjectImpl sakerproject = pluginimpl.getOrCreateProject(project);
                if (sakerproject.isScriptModellingConfigurationAppliesTo(file.getPath())) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return "Saker.build script file";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Build script file for saker.build";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "build";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return PluginIcons.SCRIPT_FILE;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Nullable
    @Override
    public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
        return "UTF-8";
    }
}
