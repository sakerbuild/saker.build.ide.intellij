package saker.build.ide.intellij.console;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import saker.build.ide.intellij.SakerBuildPlugin;

public class SakerBuildConsoleToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        ConsoleView consoleview = builder.getConsole();
//        ConsoleView consoleview = new StyledHyperlinkConsoleViewImpl(project);
        Content content = contentManager.getFactory().createContent(consoleview.getComponent(), "Build Output", true);
        contentManager.addContent(content);
    }

    @Override
    public void init(ToolWindow window) {
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project);
    }

//    private static class StyledHyperlinkConsoleViewImpl extends ConsoleViewImpl implements StyledHyperlinkConsoleView {
//        //from EditorHyperlinkSupport
//        private static final Key<EditorHyperlinkSupport> EDITOR_HYPERLINK_SUPPORT_KEY = Key
//                .create("EDITOR_HYPERLINK_SUPPORT_KEY");
//
//        public StyledHyperlinkConsoleViewImpl(Project project) {
//            super(project, false);
//        }
//
//        @Override
//        public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType,
//                @Nullable HyperlinkInfo info) {
//            super.print(text, contentType, info);
//        }
//
//        @NotNull
//        @Override
//        protected EditorEx doCreateConsoleEditor() {
//            EditorEx result = super.doCreateConsoleEditor();
//            EditorHyperlinkSupport hyperlinksupport = new EditorHyperlinkSupport(result, getProject()) {
//                @NotNull
//                @Override
//                public RangeHighlighter createHyperlink(int highlightStartOffset, int highlightEndOffset,
//                        @Nullable TextAttributes highlightAttributes, @NotNull HyperlinkInfo hyperlinkInfo) {
//                    if (highlightAttributes == null) {
//                        System.out.println(
//                                "StyledHyperlinkConsoleViewImpl.createHyperlink NO HIGHLIGHT " + hyperlinkInfo);
//                    }
//                    return super.createHyperlink(highlightStartOffset, highlightEndOffset, highlightAttributes,
//                            hyperlinkInfo);
//                }
//            };
//            result.putUserData(EDITOR_HYPERLINK_SUPPORT_KEY, hyperlinksupport);
////            EditorHyperlinkSupport.get()
//            return result;
//        }
//    }
}
