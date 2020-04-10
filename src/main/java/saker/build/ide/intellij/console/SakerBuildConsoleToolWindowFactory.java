package saker.build.ide.intellij.console;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.impl.EditorHyperlinkSupport;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ui.JBUI;
import com.sun.scenario.effect.impl.sw.java.JSWBlend_COLOR_BURNPeer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.intellij.StyledHyperlinkConsoleView;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class SakerBuildConsoleToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        ConsoleView consoleview;
        try {
            consoleview = new StyledHyperlinkConsoleViewImpl(project);
        } catch (LinkageError e) {
            //in case of ConsoleViewImpl errors?
            //anyway, fall back to default console view
            TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
            consoleview = builder.getConsole();
            Logger.getInstance(SakerBuildConsoleToolWindowFactory.class).error(e);
        }
        JComponent cvcomponent = consoleview.getComponent();
        Content content = contentManager.getFactory().createContent(cvcomponent, "Build Output", true);
        contentManager.addContent(content);
    }

    @Override
    public void init(ToolWindow window) {
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project);
    }

    private static class StyledHyperlinkConsoleViewImpl extends ConsoleViewImpl implements StyledHyperlinkConsoleView {
        private final Object lock = new Object();
        private Object clearState = new Object();

        public StyledHyperlinkConsoleViewImpl(Project project) {
            super(project, false);
        }

        @Override
        public void clear() {
            synchronized (lock) {
                super.clear();
                clearState = new Object();
            }
        }

        @Override
        public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType,
                @Nullable HyperlinkInfo info) {
            synchronized (lock) {
                super.print(text, contentType, info);
            }
        }

        @Override
        public void printHyperlink(@NotNull String hyperlinkText, @Nullable HyperlinkInfo info) {
            synchronized (lock) {
                Object clstate = this.clearState;
                if (info instanceof ColoredHyperlinkInfo) {
                    TextAttributes hyperlinkattrs = getHyperlinkAttributes();
                    hyperlinkattrs.setForegroundColor(JBUI.CurrentTheme.Link.linkColor());
                    hyperlinkattrs.setEffectColor(JBUI.CurrentTheme.Link.linkColor());
                    Color bgcol = ((ColoredHyperlinkInfo) info).getBackgroundColor();
                    if (bgcol != null) {
                        hyperlinkattrs = hyperlinkattrs.clone();
                        hyperlinkattrs.setBackgroundColor(bgcol);
                    }
                    TextAttributes fhyperlinkattrs = hyperlinkattrs;
                    int csize = getContentSize();
                    print(hyperlinkText, new ConsoleViewContentType("SAKER_BUILD_CONSOLE_HYPERLINK", hyperlinkattrs));
                    SwingUtilities.invokeLater(() -> {
                        synchronized (lock) {
                            if (this.clearState != clstate) {
                                return;
                            }
                            flushDeferredText();

                            getHyperlinks()
                                    .createHyperlink(csize, csize + hyperlinkText.length(), fhyperlinkattrs, info);
                        }
                    });
                } else {
                    super.printHyperlink(hyperlinkText, info);
                }
            }
        }

        @Override
        public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
            synchronized (lock) {
                super.print(text, contentType);
            }
        }
    }

    public static TextAttributes getHyperlinkAttributes() {
        return EditorColorsManager.getInstance().getGlobalScheme()
                .getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);
    }
}
