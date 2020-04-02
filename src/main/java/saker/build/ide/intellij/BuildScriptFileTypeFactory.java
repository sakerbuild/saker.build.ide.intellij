package saker.build.ide.intellij;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class BuildScriptFileTypeFactory extends FileTypeFactory implements DumbAware {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        consumer.consume(BuildScriptFileType.INSTANCE);
    }
}
