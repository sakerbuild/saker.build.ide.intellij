package saker.build.ide.intellij;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuildScriptLanguage extends Language {
    public static final BuildScriptLanguage INSTANCE = new BuildScriptLanguage();

    protected BuildScriptLanguage() {
        super("SAKER_BUILD_SCRIPT_LANGUAGE");
    }

    @Nullable
    @Override
    public LanguageFileType getAssociatedFileType() {
        return BuildScriptFileType.INSTANCE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Saker.build script language";
    }

}
