package saker.build.ide.intellij.console;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ActivityTracker;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.PluginIcons;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.intellij.StyledHyperlinkConsoleView;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class SakerBuildConsoleToolWindowFactory implements ToolWindowFactory, DumbAware {
    private static final Key<ConsoleView> KEY_CONTENT_CONSOLE_VIEW = Key.create("consoleView");
    private static final Key<CancelBuildAnAction> KEY_CONTENT_CANCEL_BUILD_ACTION = Key.create("cancelBuildAction");

    public static ConsoleView getConsoleViewFromContent(Content content) {
        return content.getUserData(KEY_CONTENT_CONSOLE_VIEW);
    }

    public static CancelBuildAnAction getConsoleViewCancelBuildAction(Content content) {
        return content.getUserData(KEY_CONTENT_CANCEL_BUILD_ACTION);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DefaultActionGroup actiongroup = new DefaultActionGroup();

        CancelBuildAnAction cancelbuildaction = new CancelBuildAnAction();
        actiongroup.add(cancelbuildaction);

        ContentManager contentManager = toolWindow.getContentManager();
        JComponent cvcomponent;
        ConsoleView consoleview;
        try {
            consoleview = new StyledHyperlinkConsoleViewImpl(project);
            //need to call getComponent before createConsoleActions so the Editor is instantiated
            cvcomponent = consoleview.getComponent();
            actiongroup.add(new Separator());
            actiongroup.addAll(consoleview.createConsoleActions());
            Disposer.register(project, consoleview);
        } catch (LinkageError e) {
            //in case of ConsoleViewImpl errors?
            //anyway, fall back to default console view
            Logger.getInstance(SakerBuildConsoleToolWindowFactory.class).error(e);

            TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
            consoleview = builder.getConsole();
            cvcomponent = consoleview.getComponent();
        }

        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true);
        panel.setContent(cvcomponent);
        ActionToolbar actiontoolbar = ActionManager.getInstance()
                .createActionToolbar("SAKER_BUILD_CONSOLE_ACTIONS", actiongroup, false);
        actiontoolbar.setTargetComponent(cvcomponent);
        panel.setToolbar(actiontoolbar.getComponent());

        Content content = contentManager.getFactory().createContent(panel, "Build Output", true);
        content.putUserData(KEY_CONTENT_CONSOLE_VIEW, consoleview);
        content.putUserData(KEY_CONTENT_CANCEL_BUILD_ACTION, cancelbuildaction);
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

                            //TODO the hyperlink is still out of range sometimes.
                            //java.lang.IllegalArgumentException: Wrong end: 363; document length=253; start=313
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

        @NotNull
        @Override
        public AnAction[] createConsoleActions() {
            Editor myEditor = getEditor();
            final AnAction switchSoftWrapsAction = new ToggleUseSoftWrapsToolbarAction(
                    SoftWrapAppliancePlaces.CONSOLE) {
                @Override
                protected Editor getEditor(@NotNull AnActionEvent e) {
                    return myEditor;
                }
            };
            final AnAction autoScrollToTheEndAction = new ScrollToTheEndToolbarAction(myEditor);

            return new AnAction[] { switchSoftWrapsAction, autoScrollToTheEndAction };
        }
    }

    public static TextAttributes getHyperlinkAttributes() {
        return EditorColorsManager.getInstance().getGlobalScheme()
                .getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);
    }

    public static class CancelBuildAnAction extends AnAction {
        private Object buildIdentity;
        private Runnable cancelAction;
        private Runnable interruptAction;

        {
            getTemplatePresentation().setEnabled(false);
        }

        public CancelBuildAnAction() {
            super("Cancel Build", "Cancels the build execution", AllIcons.Actions.Suspend);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            System.out.println("SakerBuildConsoleToolWindowFactory.actionPerformed ");
            if (cancelAction != null) {
                cancelAction.run();
                cancelAction = null;
            } else if (interruptAction != null) {
                interruptAction.run();
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            boolean enabled = cancelAction != null || interruptAction != null;
            Presentation pres = e.getPresentation();
            pres.setEnabled(enabled);
            if (!enabled || cancelAction != null) {
                pres.setIcon(AllIcons.Actions.Suspend);
                pres.setText("Cancel Build");
                pres.setDescription("Cancels the build execution");
            } else if (interruptAction != null) {
                pres.setIcon(PluginIcons.ICON_STOP_INTERRUPT);
                pres.setText("Interrupt Build");
                pres.setDescription("Interrupts the build execution");
            }
        }

        public synchronized void setCancellationActions(Object buildIdentity, Runnable cancel, Runnable interrupt) {
            this.buildIdentity = buildIdentity;
            this.cancelAction = cancel;
            this.interruptAction = interrupt;
            ActivityTracker.getInstance().inc();
        }

        public synchronized void clear(Object buildIdentity) {
            if (this.buildIdentity == buildIdentity) {
                this.cancelAction = null;
                this.interruptAction = null;
                this.buildIdentity = null;
                ActivityTracker.getInstance().inc();
            }
        }
    }
}
