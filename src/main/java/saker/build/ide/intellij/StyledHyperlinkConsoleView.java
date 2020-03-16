package saker.build.ide.intellij;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StyledHyperlinkConsoleView extends ConsoleView {
    public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType, @Nullable HyperlinkInfo info);
}
