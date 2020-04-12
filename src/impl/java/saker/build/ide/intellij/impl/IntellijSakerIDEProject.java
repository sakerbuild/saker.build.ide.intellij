package saker.build.ide.intellij.impl;

import com.intellij.execution.filters.BrowserHyperlinkInfo;
import com.intellij.execution.filters.FileHyperlinkInfo;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import org.apache.commons.io.output.WriterOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.daemon.DaemonEnvironment;
import saker.build.exception.BuildTargetNotFoundException;
import saker.build.exception.ScriptPositionedExceptionView;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.ide.configuration.IDEConfiguration;
import saker.build.ide.intellij.ContributedExtensionConfiguration;
import saker.build.ide.intellij.ExtensionDisablement;
import saker.build.ide.intellij.ISakerBuildProjectImpl;
import saker.build.ide.intellij.SakerBuildActionGroup;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.intellij.console.ColoredHyperlinkInfo;
import saker.build.ide.intellij.console.SakerBuildConsoleToolWindowFactory;
import saker.build.ide.intellij.extension.ideconfig.IDEConfigurationTypeHandlerExtensionPointBean;
import saker.build.ide.intellij.extension.ideconfig.IIDEConfigurationTypeHandler;
import saker.build.ide.intellij.extension.ideconfig.IIDEProjectConfigurationRootEntry;
import saker.build.ide.intellij.extension.params.ExecutionUserParameterContributorProviderExtensionPointBean;
import saker.build.ide.intellij.extension.params.IExecutionUserParameterContributor;
import saker.build.ide.intellij.extension.script.information.IScriptInformationDesigner;
import saker.build.ide.intellij.extension.script.outline.IScriptOutlineDesigner;
import saker.build.ide.intellij.extension.script.proposal.IScriptProposalDesigner;
import saker.build.ide.intellij.impl.dialog.BuildTargetChooserDialog;
import saker.build.ide.intellij.impl.editor.BuildScriptEditorHighlighter;
import saker.build.ide.intellij.impl.editor.BuildScriptEditorStateManager;
import saker.build.ide.intellij.impl.properties.SakerBuildProjectConfigurable;
import saker.build.ide.intellij.impl.ui.IDEConfigurationSelectorDialog;
import saker.build.ide.intellij.impl.ui.ProjectPropertiesValidationDialog;
import saker.build.ide.intellij.util.PluginCompatUtil;
import saker.build.ide.support.ExceptionDisplayer;
import saker.build.ide.support.SakerIDEPlugin;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.configuration.ProjectIDEConfigurationCollection;
import saker.build.ide.support.persist.StructuredObjectInput;
import saker.build.ide.support.persist.StructuredObjectOutput;
import saker.build.ide.support.persist.XMLStructuredReader;
import saker.build.ide.support.persist.XMLStructuredWriter;
import saker.build.ide.support.properties.IDEPluginProperties;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.PropertiesValidationErrorResult;
import saker.build.ide.support.properties.PropertiesValidationException;
import saker.build.ide.support.properties.ProviderMountIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;
import saker.build.runtime.environment.BuildTaskExecutionResult;
import saker.build.runtime.environment.BuildTaskExecutionResultImpl;
import saker.build.runtime.execution.BuildUserPromptHandler;
import saker.build.runtime.execution.ExecutionParametersImpl;
import saker.build.runtime.execution.ExecutionProgressMonitor;
import saker.build.runtime.execution.SakerLog;
import saker.build.runtime.execution.SecretInputReader;
import saker.build.runtime.params.ExecutionPathConfiguration;
import saker.build.scripting.ScriptParsingFailedException;
import saker.build.scripting.model.ScriptModellingEnvironment;
import saker.build.task.TaskProgressMonitor;
import saker.build.task.TaskResultCollection;
import saker.build.task.exception.MultiTaskExecutionFailedException;
import saker.build.task.exception.TaskExecutionFailedException;
import saker.build.task.utils.TaskUtils;
import saker.build.thirdparty.saker.util.DateUtils;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.build.thirdparty.saker.util.io.ByteSink;
import saker.build.thirdparty.saker.util.io.ByteSource;
import saker.build.thirdparty.saker.util.io.IOUtils;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.build.util.exc.ExceptionView;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntellijSakerIDEProject implements ExceptionDisplayer, ISakerBuildProjectImpl {
    public interface ProjectPropertiesChangeListener {
        public default void projectPropertiesChanging() {
        }

        public default void projectPropertiesChanged() {
        }
    }

    private static final String CONFIG_FILE_ROOT_OBJECT_NAME = "saker.build.ide.intellij.project.config";
    private static final String PROPERTIES_FILE_NAME = "." + CONFIG_FILE_ROOT_OBJECT_NAME;

    private static final String LAST_BUILD_SCRIPT_PATH = "last-build-script-path";
    private static final String LAST_BUILD_TARGET_NAME = "last-build-target-name";

    private final IntellijSakerIDEPlugin plugin;
    private final SakerIDEProject sakerProject;
    private final Project project;

    private final Lock executionLock = new ReentrantLock();
    private final Object configurationChangeLock = new Object();

    private final Set<ProjectPropertiesChangeListener> propertiesChangeListeners = Collections
            .newSetFromMap(new WeakHashMap<>());

    private List<ContributedExtensionConfiguration<IExecutionUserParameterContributor>> executionParameterContributors = Collections
            .emptyList();
    private Path projectConfigurationFilePath;

    public IntellijSakerIDEProject(IntellijSakerIDEPlugin plugin, SakerIDEProject sakerProject, Project project) {
        this.plugin = plugin;
        this.sakerProject = sakerProject;
        this.project = project;
    }

    public void initialize() {
        sakerProject.addExceptionDisplayer(this);

        String basepath = project.getBasePath();
        if (basepath == null) {
            Logger.getInstance(IntellijSakerIDEProject.class)
                    .warn("Failed to initialize saker.build project. No project base path found.");
            return;
        }

        Path projectpath = Paths.get(basepath);
        this.projectConfigurationFilePath = projectpath.resolve(PROPERTIES_FILE_NAME);

        Set<ExtensionDisablement> extensiondisablements = new HashSet<>();

        try (InputStream in = Files.newInputStream(projectConfigurationFilePath)) {
            XMLStructuredReader reader = new XMLStructuredReader(in);
            try (StructuredObjectInput configurationobj = reader.readObject(CONFIG_FILE_ROOT_OBJECT_NAME)) {
                IntellijSakerIDEPlugin.readExtensionDisablements(configurationobj, extensiondisablements);
            }
        } catch (NoSuchFileException e) {
        } catch (IOException e) {
            displayException(e);
        }

        executionParameterContributors = new ArrayList<>();
        for (ExecutionUserParameterContributorProviderExtensionPointBean extbean : PluginCompatUtil
                .getExtensionList(ExecutionUserParameterContributorProviderExtensionPointBean.EP_NAME)) {
            if (extbean.getId() == null) {
                Logger.getInstance(IntellijSakerIDEProject.class)
                        .warn("No id attribute specified for extension: " + extbean);
                continue;
            }

            boolean enabled = !ExtensionDisablement
                    .isDisabled(extensiondisablements, extbean.getPluginId(), extbean.getId());

            try {
                IExecutionUserParameterContributor contributor = extbean.createContributor(project);
                executionParameterContributors
                        .add(new ContributedExtensionConfiguration<>(contributor, extbean, enabled));
            } catch (Exception e) {
                Logger.getInstance(IntellijSakerIDEProject.class)
                        .error("Failed to instantiate extension: " + extbean, e);
            }
        }
        executionParameterContributors = ImmutableUtils.unmodifiableList(executionParameterContributors);

        sakerProject.initialize(projectpath);
    }

    public List<ContributedExtensionConfiguration<IExecutionUserParameterContributor>> getExecutionParameterContributors() {
        return executionParameterContributors;
    }

    public void addProjectPropertiesChangeListener(ProjectPropertiesChangeListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (propertiesChangeListeners) {
            propertiesChangeListeners.add(listener);
        }
    }

    public void removeProjectPropertiesChangeListener(ProjectPropertiesChangeListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (propertiesChangeListeners) {
            propertiesChangeListeners.remove(listener);
        }
    }

    @Override
    public void displayException(Throwable exc) {
        plugin.displayException(exc);
        //TODO make project specific
    }

    public IDEProjectProperties getIDEProjectProperties() {
        return sakerProject.getIDEProjectProperties();
    }

    public final ProjectIDEConfigurationCollection getProjectIDEConfigurationCollection() {
        return sakerProject.getProjectIDEConfigurationCollection();
    }

    public Path getProjectPath() {
        return sakerProject.getProjectPath();
    }

    public final ScriptModellingEnvironment getScriptingEnvironment() {
        return sakerProject.getScriptingEnvironment();
    }

    public final NavigableSet<SakerPath> getTrackedScriptPaths() {
        return sakerProject.getTrackedScriptPaths();
    }

    public final Set<String> getScriptTargets(SakerPath scriptpath) throws ScriptParsingFailedException, IOException {
        return sakerProject.getScriptTargets(scriptpath);
    }

    public SakerPath getWorkingDirectoryExecutionPath() {
        return projectPathToExecutionPath(SakerPath.EMPTY);
    }

    @Override
    public String projectPathToExecutionPath(String path) {
        return Objects.toString(projectPathToExecutionPath(SakerIDESupportUtils.tryParsePath(path)), null);
    }

    public SakerPath projectPathToExecutionPath(SakerPath path) {
        return SakerIDESupportUtils
                .projectPathToExecutionPath(getIDEProjectProperties(), SakerPath.valueOf(getProjectPath()), path);
    }

    public void build(ProgressIndicator monitor) {
        withLatestOrChosenBuildTarget((scriptpath, target) -> {
            build(scriptpath, target, monitor);
        });
    }

    @Override
    public void buildAsync() {
        withLatestOrChosenBuildTarget(this::buildAsync);
    }

    public void buildAsync(SakerPath scriptfile, String targetname) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Saker.build execution", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                build(scriptfile, targetname, indicator);
            }
        });
    }

    @Override
    public String executionPathToProjectRelativePath(String executionpath) {
        return Objects
                .toString(executionPathToProjectRelativePath(SakerIDESupportUtils.tryParsePath(executionpath)), null);
    }

    public SakerPath executionPathToProjectRelativePath(SakerPath executionsakerpath) {
        return SakerIDESupportUtils
                .executionPathToProjectRelativePath(getIDEProjectProperties(), SakerPath.valueOf(getProjectPath()),
                        executionsakerpath);
    }

    private static ProviderMountIDEProperty getMountPropertyForPath(SakerPath path, IDEProjectProperties properties) {
        return SakerIDESupportUtils.getMountPropertyForPath(path, properties);
    }

    public VirtualFile getVirtualFileAtExecutionPath(SakerPath path) {
        SakerPath relpath = executionPathToProjectRelativePath(path);
        if (relpath == null) {
            return null;
        }
        return LocalFileSystem.getInstance()
                .findFileByPath(SakerPath.valueOf(project.getBasePath()).resolve(relpath).toString());
    }

    public IScriptInformationDesigner getScriptInformationDesignerForSchemaIdentifier(String schemaid) {
        return plugin.getScriptInformationDesignerForSchemaIdentifier(schemaid, project);
    }

    public IScriptProposalDesigner getScriptProposalDesignerForSchemaIdentifiers(Set<String> schemaidentifiers) {
        return plugin.getScriptProposalDesignerForSchemaIdentifiers(schemaidentifiers, project);
    }

    public IScriptOutlineDesigner getScriptOutlineDesignerForSchemaIdentifier(String schemaid) {
        return plugin.getScriptOutlineDesignerForSchemaIdentifier(schemaid, project);
    }

    @Override
    public boolean isScriptModellingConfigurationAppliesTo(String localfilepath) {
        SakerPath projectsakerpath = SakerPath.valueOf(getProjectPath());
        SakerPath scriptsakerpath = SakerPath.valueOf(localfilepath);
        if (!scriptsakerpath.startsWith(projectsakerpath)) {
            return false;
        }
        SakerPath projectrelativepath = projectsakerpath.relativize(scriptsakerpath);
        SakerPath execpath = projectPathToExecutionPath(projectrelativepath);

        return isScriptModellingConfigurationAppliesTo(execpath);
    }

    private boolean isScriptModellingConfigurationAppliesTo(SakerPath execpath) {
        return SakerIDESupportUtils.isScriptModellingConfigurationAppliesTo(execpath, getIDEProjectProperties());
    }

    @Override
    public EditorHighlighter getEditorHighlighter(VirtualFile file, EditorColorsScheme colors) {
        SakerPath execpath = projectPathToExecutionPath(SakerPath.valueOf(file.getPath()));
        if (execpath == null) {
            return null;
        }
        return new BuildScriptEditorHighlighter(this, execpath, colors);
    }

    private final NavigableMap<SakerPath, BuildScriptEditorStateManager> scriptEditorStateManagers = new TreeMap<>();
    private final NavigableMap<SakerPath, Object> scriptEditorStateManagerLocks = new ConcurrentSkipListMap<>();

    public BuildScriptEditorStateManager subscribeScriptEditorStateManager(SakerPath path, Document document) {
        if (path == null || document == null) {
            return null;
        }
        synchronized (scriptEditorStateManagerLocks.computeIfAbsent(path, Functionals.objectComputer())) {
            BuildScriptEditorStateManager result = scriptEditorStateManagers.computeIfAbsent(path, p -> {
                return new BuildScriptEditorStateManager(document, this, p);
            });
            result.subscribe();
            return result;
        }
    }

    public void unsubscribeScriptEditorStateManager(BuildScriptEditorStateManager statemanager) {
        if (statemanager == null) {
            return;
        }
        SakerPath scriptpath = statemanager.getScriptExecutionPath();
        synchronized (scriptEditorStateManagerLocks.computeIfAbsent(scriptpath, Functionals.objectComputer())) {
            statemanager.unsubscribe();
            if (!statemanager.isAlive()) {
                scriptEditorStateManagers.remove(scriptpath, statemanager);
            }
        }
    }

    private static final String CONSOLE_MARKER_STR_PATTERN = "[ \t]*(\\[(?:.*?)\\])?[ \t]*(((.*?)(:(-?[0-9]+)(:([0-9]*)(-([0-9]+))?)?)?):)?[ ]*([wW]arning|[eE]rror|[iI]nfo|[sS]uccess|[fF]atal [eE]rror):[ ]*(.*)";
    private static final int CONSOLE_MARKER_GROUP_DISPLAY_ID = 1;
    private static final int CONSOLE_MARKER_GROUP_PATHANDLOCATION = 3;
    private static final int CONSOLE_MARKER_GROUP_FILEPATH = 4;
    private static final int CONSOLE_MARKER_GROUP_LINE = 6;
    private static final int CONSOLE_MARKER_GROUP_LINESTART = 8;
    private static final int CONSOLE_MARKER_GROUP_LINEEND = 10;
    private static final int CONSOLE_MARKER_GROUP_SEVERITY = 11;
    private static final int CONSOLE_MARKER_GROUP_MESSAGE = 12;
    private static final Pattern CONSOLE_MARKER_PATTERN = Pattern.compile(CONSOLE_MARKER_STR_PATTERN);

    private static final String AT_BUILD_FILE_ERROR_STR_PATTERN = "[ \\t]+at[ \\t]+(.*):([0-9]+):([0-9]+)-([0-9]+)";
    private static final int AT_BUILD_FILE_ERROR_GROUP_PATH = 1;
    private static final int AT_BUILD_FILE_ERROR_GROUP_LINE_NUMBER = 2;
    private static final int AT_BUILD_FILE_ERROR_GROUP_LINE_START = 3;
    private static final int AT_BUILD_FILE_ERROR_GROUP_LINE_END = 4;
    private static final Pattern AT_BUILD_FILE_ERROR_PATTERN = Pattern.compile(AT_BUILD_FILE_ERROR_STR_PATTERN);

    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final Map<String, ConsoleViewContentType> SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP = new TreeMap<>(
            String::compareToIgnoreCase);

    static {
        TextAttributes errattr = new TextAttributes();
        TextAttributes warnattr = new TextAttributes();
        TextAttributes successattr = new TextAttributes();
        TextAttributes infoattr = new TextAttributes();
        errattr.setBackgroundColor(new JBColor(new Color(254, 231, 224), new Color(81, 38, 39)));
        warnattr.setBackgroundColor(new JBColor(new Color(254, 243, 218), new Color(80, 77, 22)));
        successattr.setBackgroundColor(new JBColor(new Color(217, 242, 221), new Color(28, 74, 23)));
        infoattr.setBackgroundColor(new JBColor(new Color(227, 235, 253), new Color(37, 50, 82)));
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP.put("error", new ConsoleViewContentType("SAKER_BUILD_ERROR", errattr));
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP
                .put("fatal error", new ConsoleViewContentType("SAKER_BUILD_ERROR", errattr));
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP.put("info", new ConsoleViewContentType("SAKER_BUILD_INFO", infoattr));
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP
                .put("warning", new ConsoleViewContentType("SAKER_BUILD_WARNING", warnattr));
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP
                .put("success", new ConsoleViewContentType("SAKER_BUILD_SUCCESS", successattr));
    }

    private class ConsoleViewOutputStream extends OutputStream {
        protected ConsoleView console;
        protected WriterOutputStream writerOutput;

        protected ConsoleViewContentType defaultContentType = ConsoleViewContentType.NORMAL_OUTPUT;

        public ConsoleViewOutputStream(ConsoleView console) {
            this.console = console;
            this.writerOutput = new WriterOutputStream(new Writer() {
                private StringBuilder sb = new StringBuilder();

                private char lfcrChar = 0;

                @Override
                public void write(int c) throws IOException {
                    if (c == '\n' || c == '\r') {
                        if (c == lfcrChar) {
                            appendLine("");
                            return;
                        }
                        if (lfcrChar == 0) {
                            lfcrChar = (char) c;
                            appendLine(sb.toString());
                            sb.setLength(0);
                        }
                    } else {
                        lfcrChar = 0;
                        sb.append((char) c);
                    }
                }

                @Override
                public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
                    for (int i = 0; i < len; i++) {
                        char c = cbuf[off + i];
                        write(c);
                    }
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                    if (sb.length() > 0) {
                        appendLine(sb.toString());
                        sb.setLength(0);
                    }
                }

            }, StandardCharsets.UTF_8, 4096, true);
        }

        protected void appendLine(String line) {
            console.print(line + LINE_SEPARATOR, defaultContentType);
        }

        @Override
        public void write(int b) throws IOException {
            writerOutput.write(b);
        }

        @Override
        public void write(@NotNull byte[] b) throws IOException {
            writerOutput.write(b);
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            writerOutput.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            writerOutput.flush();
        }

        @Override
        public void close() throws IOException {
            writerOutput.close();
        }
    }

    private static class SelectionOpenFileHyperlinkInfo implements FileHyperlinkInfo, ColoredHyperlinkInfo {
        private Project project;
        private VirtualFile file;
        private int line;
        private int column;
        private int length;

        private Color backgroundColor;

        public SelectionOpenFileHyperlinkInfo(Project project, VirtualFile file, int line, int column, int length) {
            this.project = project;
            this.file = file;
            this.line = line;
            this.column = column;
            this.length = length;
        }

        public void setBackgroundColor(Color backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        @Override
        public Color getBackgroundColor() {
            return backgroundColor;
        }

        @Override
        public void navigate(Project project) {
            if (length > 0) {
                Editor editor = FileEditorManager.getInstance(project)
                        .openTextEditor(new OpenFileDescriptor(project, file), true);
                if (editor != null) {
                    SelectionModel selectionmodel = editor.getSelectionModel();
                    Document document = editor.getDocument();
                    int lc = document.getLineCount();
                    int line = this.line;
                    int length = this.length;
                    if (line >= lc) {
                        //this can happen is the file is edited meanwhile.
                        line = lc - 1;
                        length = 0;
                    }
                    int coloffset = document.getLineStartOffset(line) + column;
                    int lineendoffset = document.getLineEndOffset(line);
                    if (coloffset < 0 || coloffset > lineendoffset) {
                        coloffset = lineendoffset;
                    }

                    //only higlight the first line, as the selection for multiple lines doesn't work,
                    //    as IntelliJ doesn't handle line endings properly
                    // (\r\n line endings occupy only a single element, therefore multi-line highlight will drift off)
                    int selectionendoffset = coloffset + Math.min(length, lineendoffset - coloffset);
                    selectionmodel.setSelection(coloffset, selectionendoffset);
                    editor.getCaretModel().moveToOffset(selectionendoffset);
                    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                    return;
                }
                //fallback based on FileHyperlinkInfoBase
                new BrowserHyperlinkInfo(file.getUrl()).navigate(project);
                return;
            }
            OpenFileDescriptor descriptor = getDescriptor();
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
            if (editor != null) {
                return;
            }
            //fallback based on FileHyperlinkInfoBase
            new BrowserHyperlinkInfo(file.getUrl()).navigate(project);
        }

        @Nullable
        @Override
        public OpenFileDescriptor getDescriptor() {
            return new OpenFileDescriptor(project, file, line, column);
        }
    }

    protected void build(SakerPath scriptfile, String targetname, ProgressIndicator monitor) {
        if (monitor == null) {
            monitor = new EmptyProgressIndicator();
        }
        ProgressMonitorWrapper monitorwrapper = new ProgressMonitorWrapper(monitor);

        boolean wasinterrupted = false;
        final Object buildidentity = new Object();

        Thread buildThread = Thread.currentThread();
        try {
            BuildTaskExecutionResult result = null;
            SakerPath executionworkingdir = null;

            OutputStream out = StreamUtils.nullOutputStream();
            OutputStream err = StreamUtils.nullOutputStream();
            SakerBuildConsoleToolWindowFactory.CancelBuildAnAction[] cancelbuildaction = { null };
            ToolWindow[] tw = { null };
            ConsoleView[] console = { null };
            try {

                executionLock.lockInterruptibly();
                WriteAction.runAndWait(() -> {
                    try {
                        tw[0] = ToolWindowManager.getInstance(project)
                                .getToolWindow(SakerBuildConsoleToolWindowFactory.ID);
                        Content toolwindowcontent = tw[0].getContentManager().getContent(0);
                        console[0] = SakerBuildConsoleToolWindowFactory.getConsoleViewFromContent(toolwindowcontent);
                        console[0].clear();
                        cancelbuildaction[0] = SakerBuildConsoleToolWindowFactory
                                .getConsoleViewCancelBuildAction(toolwindowcontent);
                        if (cancelbuildaction[0] != null) {
                            cancelbuildaction[0].setCancellationActions(buildidentity, monitorwrapper::setCancelled,
                                    buildThread::interrupt);
                        }
                        tw[0].activate(null, false);
                        FileDocumentManager.getInstance().saveAllDocuments();
                    } catch (Exception e) {
                        displayException(e);
                    }
                });
                if (monitorwrapper.isCancelled()) {
                    out.write(("Build cancelled." + LINE_SEPARATOR).getBytes());
                    return;
                }
                out = new StandardConsoleViewOutputStream(console[0]);
                err = new ErrorConsoleViewOutputStream(console[0]);
                ExecutionParametersImpl params;
                IDEProjectProperties projectproperties;
                try {
                    projectproperties = getIDEProjectPropertiesWithExecutionParameterContributions(
                            getIDEProjectProperties(), monitor);
                } catch (ProcessCanceledException e) {
                    out.write("Build cancelled.\n".getBytes());
                    return;
                }
                try {
                    params = sakerProject.createExecutionParameters(projectproperties);
                    //there were no validation errors
                } catch (PropertiesValidationException e) {
                    err.write(("Invalid build configuration:" + LINE_SEPARATOR).getBytes());
                    for (PropertiesValidationErrorResult error : e.getErrors()) {
                        String errormsg = SakerIDESupportUtils.createValidationErrorMessage(error);

                        console[0].printHyperlink(errormsg + LINE_SEPARATOR, new HyperlinkInfo() {
                            @Override
                            public void navigate(Project project) {
                                ProjectPropertiesValidationDialog.showSettingsForValidationError(project, error);
                            }

                            @Override
                            public boolean includeInOccurenceNavigation() {
                                return false;
                            }
                        });
                    }

                    ApplicationManager.getApplication().invokeLater(() -> {
                        ProjectPropertiesValidationDialog dialog = new ProjectPropertiesValidationDialog(this,
                                e.getErrors());
                        dialog.setVisible(true);
                    });
                    return;
                }
                DaemonEnvironment daemonenv = sakerProject.getExecutionDaemonEnvironment(projectproperties);
                ExecutionPathConfiguration pathconfiguration = params.getPathConfiguration();
                executionworkingdir = pathconfiguration.getWorkingDirectory();
                params.setRequiresIDEConfiguration(projectproperties.isRequireTaskIDEConfiguration());

                try {
                    PropertiesComponent propertiescomponent = PropertiesComponent.getInstance(project);
                    propertiescomponent.setValue(LAST_BUILD_SCRIPT_PATH, scriptfile.toString());
                    propertiescomponent.setValue(LAST_BUILD_TARGET_NAME, targetname);
                } catch (Exception e) {
                    displayException(e);
                }

                SakerPath thisworking = pathconfiguration.getWorkingDirectory();
                SakerPath relativescriptpath = scriptfile;
                if (thisworking != null && scriptfile.startsWith(thisworking)) {
                    relativescriptpath = thisworking.relativize(scriptfile);
                }
                String jobname = targetname + "@" + relativescriptpath;

                InputStream consolein = StreamUtils.nullInputStream();

                params.setProgressMonitor(monitorwrapper);
                params.setStandardOutput(ByteSink.valueOf(out));
                params.setErrorOutput(ByteSink.valueOf(err));
                params.setStandardInput(ByteSource.valueOf(consolein));
                BuildUserPromptHandler userprompthandler = new BuildUserPromptHandler() {
                    @Override
                    public int prompt(String title, String message, List<String> options) {
                        Objects.requireNonNull(options, "options");
                        //TODO handle user prompt
                        if (options.isEmpty()) {
                            return -1;
                        }
                        return -1;
                    }
                };
                params.setUserPrompHandler(userprompthandler);
                SecretInputReader secretinputreader = new SecretInputReader() {
                    @Override
                    public String readSecret(String titleinfo, String message, String prompt, String secretidentifier) {
                        //TODO handle user secret
                        return null;
                    }
                };
                params.setSecretInputReader(secretinputreader);
                try {
                    out.write(("Build started. (" + jobname + ")" + LINE_SEPARATOR).getBytes());
                } catch (IOException e) {
                    //shouldnt happen, we don't display this exception to the user
                    e.printStackTrace();
                }
                long starttime = System.nanoTime();
                result = sakerProject.build(scriptfile, targetname, daemonenv, params);
                long finishtime = System.nanoTime();

                //clear the interrupt flag if we were interrupted so the finalizing succeeds.
                wasinterrupted = Thread.interrupted();

                try {
                    if (result.getResultKind() == BuildTaskExecutionResult.ResultKind.INITIALIZATION_ERROR) {
                        out.write(("Failed to initialize execution." + LINE_SEPARATOR).getBytes());
                    } else {
                        out.write(("Build finished. " + new Date(System.currentTimeMillis()) + " (" + DateUtils
                                .durationToString((finishtime - starttime) / 1_000_000) + ")" + LINE_SEPARATOR)
                                .getBytes());
                    }
                } catch (IOException e) {
                    //shouldnt happen, we don't display this exception to the user
                    e.printStackTrace();
                }
                SakerPath builddir = params.getBuildDirectory();
                Path projectpath = getProjectPath();
                Path builddirlocalpath = null;
                if (builddir != null) {
                    try {
                        if (builddir.isRelative()) {
                            builddir = pathconfiguration.getWorkingDirectory().resolve(builddir);
                        }
                        builddirlocalpath = pathconfiguration.toLocalPath(builddir);
                        if (builddirlocalpath != null && builddirlocalpath.startsWith(projectpath)) {
                            //TODO mark build dir as derived
                            VirtualFile builddirfile = LocalFileSystem.getInstance()
                                    .findFileByPath(builddirlocalpath.toString());
                            if (builddirfile != null) {
                                builddirfile.refresh(true, true);
                            }
                        }
                    } catch (Exception e) {
                        //CoreException, or if we fail some path parsing, converting, or others
                        displayException(e);
                    }
                }
                if (ObjectUtils.isNullOrEmpty(projectproperties.getExecutionDaemonConnectionName())) {
                    //set derived to the mirror directory only if the build is running in-process
                    try {
                        SakerPath mirrordir = params.getMirrorDirectory();
                        if (mirrordir != null) {
                            Path localmirrorpath = LocalFileProvider.toRealPath(mirrordir);
                            if (localmirrorpath.startsWith(projectpath)) {
                                //TODO mark mirror folder as derived
                                if (builddirlocalpath == null || !localmirrorpath.startsWith(builddirlocalpath)) {
                                    //only refresh the mirror path if it is not under the build directory
                                    VirtualFile mirrordirfile = LocalFileSystem.getInstance()
                                            .findFileByPath(localmirrorpath.toString());
                                    if (mirrordirfile != null) {
                                        mirrordirfile.refresh(true, true);
                                    }
                                }
                            }
                        }
                    } catch (InvalidPathException e) {
                        //the mirror path is not a valid path on the local file system
                        //ignoreable
                    } catch (Exception e) {
                        //CoreException, or if we fail some path parsing, converting, or others
                        displayException(e);
                    }
                }
                TaskResultCollection resultcollection = result.getTaskResultCollection();
                if (resultcollection != null) {
                    Collection<? extends IDEConfiguration> ideconfigs = resultcollection.getIDEConfigurations();
                    addIDEConfigurations(ideconfigs);
                }
                return;
            } catch (Throwable e) {
                if (result == null) {
                    result = BuildTaskExecutionResultImpl.createInitializationFailed(e);
                    try {
                        out.write(("Failed to initialize execution." + LINE_SEPARATOR).getBytes());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                return;
            } finally {
                if (cancelbuildaction[0] != null) {
                    SwingUtilities.invokeLater(() -> cancelbuildaction[0].clear(buildidentity));
                }
                if (result != null) {
                    ScriptPositionedExceptionView posexcview = result.getPositionedExceptionView();
                    if (posexcview != null) {
                        if (err != null) {
                            SakerLog.ExceptionFormat exceptionformat = SakerLog.CommonExceptionFormat.DEFAULT_FORMAT;
                            IDEPluginProperties pluginprops = plugin.getIDEPluginProperties();
                            if (pluginprops != null) {
                                String propexcformat = pluginprops.getExceptionFormat();
                                if (propexcformat != null) {
                                    //convert to upper case to attempt to handle possible case differences
                                    try {
                                        exceptionformat = SakerLog.CommonExceptionFormat
                                                .valueOf(propexcformat.toUpperCase(Locale.ENGLISH));
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                            }

                            try (PrintStream errps = new PrintStream(err)) {
                                TaskUtils.printTaskExceptionsOmitTransitive(posexcview, errps, executionworkingdir,
                                        exceptionformat);
                            }
                            if (console[0] != null) {
                                if (isBuildTargetNotFoundExceptionResult(result)) {
                                    console[0].printHyperlink("Choose a different build target\n", new HyperlinkInfo() {
                                        @Override
                                        public void navigate(Project project) {
                                            BuildTargetChooserDialog.BuildTargetItem item = askBuildTarget();
                                            if (item != null) {
                                                buildAsync(item.getScriptPath(), item.getTarget());
                                            }
                                        }
                                    });
                                }
                                console[0].printHyperlink("Show complete stacktrace\n", new HyperlinkInfo() {
                                    private boolean printed = false;

                                    @Override
                                    public synchronized void navigate(Project project) {
                                        if (!printed) {
                                            printed = true;
                                            ConsoleViewOutputStream cvout = new ErrorConsoleViewOutputStream(
                                                    console[0]);
                                            try (PrintStream ps = new PrintStream(cvout)) {
                                                ps.println();
                                                ps.println("Complete build exception stacktrace:");
                                                SakerLog.printFormatException(posexcview, ps,
                                                        SakerLog.CommonExceptionFormat.FULL);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
                IOException streamscloseexc = IOUtils.closeExc(out, err);
                if (streamscloseexc != null) {
                    displayException(streamscloseexc);
                }
                executionLock.unlock();
            }
        } finally {
            if (wasinterrupted) {
                buildThread.interrupt();
            }
        }
    }

    private static boolean isBuildTargetNotFoundExceptionResult(BuildTaskExecutionResult result) {
        if (result.getResultKind() != BuildTaskExecutionResult.ResultKind.FAILURE) {
            return false;
        }
        ExceptionView ev = result.getExceptionView();
        if (!MultiTaskExecutionFailedException.class.getName().equals(ev.getExceptionClassName())) {
            return false;
        }
        ExceptionView[] suppressed = ev.getSuppressed();
        if (ObjectUtils.isNullOrEmpty(suppressed)) {
            return false;
        }
        for (ExceptionView supr : suppressed) {
            if (!TaskExecutionFailedException.class.getName().equals(supr.getExceptionClassName())) {
                return false;
            }
            ExceptionView cause = supr.getCause();
            if (cause == null) {
                return false;
            }
            if (!BuildTargetNotFoundException.class.getName().equals(cause.getExceptionClassName())) {
                return false;
            }
        }
        return true;
    }

    private void addIDEConfigurations(Collection<? extends IDEConfiguration> ideconfigs) {
        if (ObjectUtils.isNullOrEmpty(ideconfigs)) {
            return;
        }
        Set<Map.Entry<String, String>> typeidentifierkinds = new HashSet<>();
        for (IDEConfiguration ideconfig : ideconfigs) {
            typeidentifierkinds
                    .add(ImmutableUtils.makeImmutableMapEntry(ideconfig.getType(), ideconfig.getIdentifier()));
        }
        ProjectIDEConfigurationCollection ideconfigcoll = getProjectIDEConfigurationCollection();
        List<IDEConfiguration> nconfigs = new ArrayList<>(ideconfigcoll.getConfigurations());

        //remove all configurations from the previous collection which have a type-identifier kind
        //    that is being overwritten
        for (Iterator<IDEConfiguration> it = nconfigs.iterator(); it.hasNext(); ) {
            IDEConfiguration ideconfig = it.next();
            Map.Entry<String, String> entry = ImmutableUtils
                    .makeImmutableMapEntry(ideconfig.getType(), ideconfig.getIdentifier());
            if (typeidentifierkinds.contains(entry)) {
                it.remove();
            }
        }
        nconfigs.addAll(ideconfigs);
        ProjectIDEConfigurationCollection nideconfiguration = new ProjectIDEConfigurationCollection(nconfigs);
        //equality is checked by SakerIDEProject
        sakerProject.setProjectIDEConfigurationCollection(nideconfiguration);
    }

    private void withLatestOrChosenBuildTarget(BiConsumer<SakerPath, String> consumer) {
        PropertiesComponent propertiescomponent = PropertiesComponent.getInstance(project);
        try {
            String lastscriptpath = propertiescomponent.getValue(LAST_BUILD_SCRIPT_PATH);
            if (lastscriptpath != null) {
                String lasttargetname = propertiescomponent.getValue(LAST_BUILD_TARGET_NAME);
                SakerPath lastbuildpath = SakerPath.valueOf(lastscriptpath);
                consumer.accept(lastbuildpath, lasttargetname);
                return;
            }
            BuildTargetChooserDialog.BuildTargetItem item = askBuildTarget();
            if (item != null) {
                consumer.accept(item.getScriptPath(), item.getTarget());
            }
        } catch (Exception e) {
            displayException(e);
        }
    }

    private BuildTargetChooserDialog.BuildTargetItem askBuildTarget() {
        List<BuildTargetChooserDialog.BuildTargetItem> items = new ArrayList<>();

        Set<? extends SakerPath> buildfiles = getTrackedScriptPaths();
        Iterator<? extends SakerPath> it = buildfiles.iterator();
        SakerPath workingdirpath = getWorkingDirectoryExecutionPath();

        while (it.hasNext()) {
            SakerPath displaybuildfile;
            SakerPath buildfile = it.next();
            if (workingdirpath != null && buildfile.startsWith(workingdirpath)) {
                displaybuildfile = workingdirpath.relativize(buildfile);
            } else {
                displaybuildfile = buildfile;
            }

            try {
                Set<String> targets = getScriptTargets(buildfile);
                if (!ObjectUtils.isNullOrEmpty(targets)) {
                    for (String target : targets) {
                        items.add(new BuildTargetChooserDialog.BuildTargetItem(buildfile, target, displaybuildfile));
                    }
                }
            } catch (ScriptParsingFailedException | IOException e) {
                displayException(e);
                //XXX open dialog with errors?
            }
        }

        items.sort((l, r) -> {
            int cmp = l.getDisplayPath().compareTo(r.getDisplayPath());
            if (cmp != 0) {
                return cmp;
            }
            cmp = l.getTarget().compareTo(r.getTarget());
            if (cmp != 0) {
                return cmp;
            }
            return 0;
        });

        BuildTargetChooserDialog.BuildTargetItem item;
        if (items.size() == 1) {
            item = items.get(0);
        } else {
            BuildTargetChooserDialog dialog = new BuildTargetChooserDialog(project, items);
            dialog.setVisible(true);
            item = dialog.getSelectedItem();
        }
        return item;
    }

    public final void addProjectResourceListener(SakerIDEProject.ProjectResourceListener listener) {
        sakerProject.addProjectResourceListener(listener);
    }

    public final void removeProjectResourceListener(SakerIDEProject.ProjectResourceListener listener) {
        sakerProject.removeProjectResourceListener(listener);
    }

    @Override
    public IntellijSakerIDEPlugin getPlugin() {
        return plugin;
    }

    protected void close() throws IOException {
        IOException exc = null;
        List<ContributedExtensionConfiguration<IExecutionUserParameterContributor>> envparamcontributors = executionParameterContributors;
        if (!ObjectUtils.isNullOrEmpty(envparamcontributors)) {
            this.executionParameterContributors = Collections.emptyList();
            for (ContributedExtensionConfiguration<IExecutionUserParameterContributor> contributor : envparamcontributors) {
                try {
                    IExecutionUserParameterContributor paramcontributor = contributor.getContributor();
                    if (paramcontributor != null) {
                        paramcontributor.dispose();
                    }
                } catch (Exception e) {
                    //catch just in case
                    exc = IOUtils.addExc(exc, e);
                }
            }
        }
        IOUtils.throwExc(exc);
    }

    @Override
    public Project getProject() {
        return project;
    }

    public SakerIDEProject getSakerProject() {
        return sakerProject;
    }

    @Override
    public Configurable getProjectPropertiesConfigurable() {
        return new SakerBuildProjectConfigurable(this);
    }

    public void setIDEProjectProperties(IDEProjectProperties properties,
            Set<ExtensionDisablement> extensionDisablements) {
        synchronized (configurationChangeLock) {
            try {
                projectPropertiesChanging();
            } catch (Exception e) {
                displayException(e);
            }
            try {
                Set<ExtensionDisablement> prevdisablements = getExtensionDisablements();
                sakerProject.setIDEProjectProperties(properties);
                this.executionParameterContributors = SakerBuildPlugin
                        .applyExtensionDisablements(this.executionParameterContributors, extensionDisablements);
                if (!prevdisablements.equals(extensionDisablements)) {
                    try {
                        writeProjectConfigurationFile(extensionDisablements);
                    } catch (IOException e) {
                        displayException(e);
                    }
                }
                sakerProject.updateForProjectProperties(
                        getIDEProjectPropertiesWithExecutionParameterContributions(properties, null));
            } finally {
                try {
                    projectPropertiesChanged();
                } catch (Exception e) {
                    //don't propagate exceptions
                    displayException(e);
                }
            }
        }
    }

    @Deprecated
    public void setIDEProjectProperties(IDEProjectProperties properties,
            List<ContributedExtensionConfiguration<IExecutionUserParameterContributor>> executionParameterContributors) {
        synchronized (configurationChangeLock) {
            try {
                projectPropertiesChanging();
            } catch (Exception e) {
                displayException(e);
            }
            try {
                Set<ExtensionDisablement> prevdisablements = getExtensionDisablements();
                sakerProject.setIDEProjectProperties(properties);
                this.executionParameterContributors = executionParameterContributors;
                Set<ExtensionDisablement> currentdisablements = getExtensionDisablements();
                if (!prevdisablements.equals(currentdisablements)) {
                    try {
                        writeProjectConfigurationFile(currentdisablements);
                    } catch (IOException e) {
                        displayException(e);
                    }
                }
                sakerProject.updateForProjectProperties(
                        getIDEProjectPropertiesWithExecutionParameterContributions(properties, null));
            } finally {
                try {
                    projectPropertiesChanged();
                } catch (Exception e) {
                    //don't propagate exceptions
                    displayException(e);
                }
            }
        }
    }

    @NotNull
    public Set<ExtensionDisablement> getExtensionDisablements() {
        return SakerBuildPlugin.getExtensionDisablements(this.executionParameterContributors);
    }

    public void setIDEProjectProperties(IDEProjectProperties properties) {
        synchronized (configurationChangeLock) {
            try {
                projectPropertiesChanging();
            } catch (Exception e) {
                displayException(e);
            }
            try {
                sakerProject.setIDEProjectProperties(properties);
                ProgressManager.getInstance()
                        .run(new Task.Backgroundable(null, "Updating saker.build project properties", false) {
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                sakerProject.updateForProjectProperties(properties);
                            }
                        });

            } finally {
                try {
                    projectPropertiesChanged();
                } catch (Exception e) {
                    //don't propagate exceptions
                    displayException(e);
                }
            }
        }
    }

    private IDEProjectProperties getIDEProjectPropertiesWithExecutionParameterContributions(
            IDEProjectProperties properties, ProgressIndicator monitor) {
        if (executionParameterContributors.isEmpty()) {
            return properties;
        }
        SimpleIDEProjectProperties.Builder builder = SimpleIDEProjectProperties.builder(properties);
        Map<String, String> userparameters = SakerIDEPlugin.entrySetToMap(properties.getUserParameters());
        List<ContributedExtensionConfiguration<IExecutionUserParameterContributor>> contributors = executionParameterContributors;

        NavigableMap<String, String> userparamworkmap = getUserParametersWithContributors(userparameters, contributors,
                monitor);
        builder.setUserParameters(userparamworkmap.entrySet());
        return builder.build();
    }

    public NavigableMap<String, String> getUserParametersWithContributors(Map<String, String> userparameters,
            List<? extends ContributedExtensionConfiguration<? extends IExecutionUserParameterContributor>> contributors,
            ProgressIndicator monitor) {
        return IntellijSakerIDEPlugin
                .getUserParametersWithContributors(userparameters, contributors, this, monitor, (ext, userparams) -> {
                    return ext.contribute(this, userparams, monitor);
                });
    }

    private void writeProjectConfigurationFile(Iterable<? extends ExtensionDisablement> disablements) throws
            IOException {
        try (OutputStream os = Files.newOutputStream(projectConfigurationFilePath)) {
            try (XMLStructuredWriter writer = new XMLStructuredWriter(os)) {
                try (StructuredObjectOutput configurationobj = writer.writeObject(CONFIG_FILE_ROOT_OBJECT_NAME)) {
                    IntellijSakerIDEPlugin.writeExtensionDisablements(configurationobj, disablements);
                }
            }
        }
    }

    private void projectPropertiesChanging() {
        List<ProjectPropertiesChangeListener> listenercopy;
        synchronized (propertiesChangeListeners) {
            listenercopy = ImmutableUtils.makeImmutableList(propertiesChangeListeners);
        }
        for (ProjectPropertiesChangeListener l : listenercopy) {
            l.projectPropertiesChanging();
        }
    }

    private void projectPropertiesChanged() {
        List<ProjectPropertiesChangeListener> listenercopy;
        synchronized (propertiesChangeListeners) {
            listenercopy = ImmutableUtils.makeImmutableList(propertiesChangeListeners);
        }
        for (ProjectPropertiesChangeListener l : listenercopy) {
            l.projectPropertiesChanged();
        }
    }

    public void clean(ProgressIndicator monitor) {
        try {
            PropertiesComponent propertiescomponent = PropertiesComponent.getInstance(project);
            propertiescomponent.unsetValue(LAST_BUILD_SCRIPT_PATH);
            propertiescomponent.unsetValue(LAST_BUILD_TARGET_NAME);

            sakerProject.clean();
            String projrelpath = executionPathToProjectRelativePath(getIDEProjectProperties().getBuildDirectory());
            if (projrelpath != null) {
                VirtualFile builddirfile = LocalFileSystem.getInstance()
                        .findFileByPath(sakerProject.getProjectPath().resolve(projrelpath).toString());
                if (builddirfile != null) {
                    builddirfile.refresh(true, true);
                }
            }
        } catch (Exception e) {
            displayException(e);
        }
    }

    @Override
    public void addSakerBuildTargetsMenuActions(List<AnAction> result) {
        addBuildFilesToTargetsMenu(result);
        result.add(new Separator());
        addIDEConfigurationsToTargetsMenu(result);
        result.add(new Separator());
        result.add(new AnAction("Clean project") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                cleanAsync();
            }
        });
    }

    private void cleanAsync() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Clean saker.build project", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                clean(indicator);
            }
        });
    }

    private void addIDEConfigurationsToTargetsMenu(List<AnAction> result) {
        result.add(new ActionGroup("IDE Configuration", true) {
            @NotNull
            @Override
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                Collection<? extends IDEConfiguration> configurations = getProjectIDEConfigurationCollection()
                        .getConfigurations();
                if (configurations.isEmpty()) {
                    return new AnAction[] { new AnAction("Run a build to generate an IDE configuration") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            buildAsync();
                        }

                        @Override
                        public boolean isDumbAware() {
                            return true;
                        }
                    } };
                }
                Map<String, Set<String>> idetypetypenames = new TreeMap<>();

                for (IDEConfigurationTypeHandlerExtensionPointBean extbean : PluginCompatUtil
                        .getExtensionList(IDEConfigurationTypeHandlerExtensionPointBean.EP_NAME)) {
                    String typename = extbean.getTypeName();
                    if (ObjectUtils.isNullOrEmpty(typename)) {
                        continue;
                    }
                    String type = extbean.getType();
                    if (ObjectUtils.isNullOrEmpty(type)) {
                        continue;
                    }
                    idetypetypenames.computeIfAbsent(type, Functionals.treeSetComputer()).add(typename);
                }

                Map<String, ApplyIDEConfigurationActionGroup> idetypemenumanagers = new TreeMap<>();
                for (IDEConfiguration ideconfig : configurations) {
                    String configtype = ideconfig.getType();
                    if (ObjectUtils.isNullOrEmpty(configtype)) {
                        continue;
                    }
                    String id = ideconfig.getIdentifier();
                    if (ObjectUtils.isNullOrEmpty(id)) {
                        continue;
                    }
                    Set<String> configtypenames = idetypetypenames.get(configtype);
                    if (configtypenames == null) {
                        configtypenames = Collections.singleton("<" + configtype + ">");
                    }
                    for (String typename : configtypenames) {
                        ApplyIDEConfigurationActionGroup ideconfigmenumanager = idetypemenumanagers.get(typename);
                        if (ideconfigmenumanager == null) {
                            ideconfigmenumanager = new ApplyIDEConfigurationActionGroup(typename);
                            idetypemenumanagers.put(typename, ideconfigmenumanager);
                        }
                        ideconfigmenumanager.ideConfigurations.add(ideconfig);
                    }
                }

                return idetypemenumanagers.values().toArray(SakerBuildActionGroup.EMPTY_ANACTION_ARRAY);
            }
        });
    }

    private final NotificationGroup IDE_CONFIGURATION_NOTIFICATION_GROUP = new NotificationGroup(
            "Saker.build IDE configuration errors", NotificationDisplayType.BALLOON, false);

    private void applyIDEConfiguration(IDEConfiguration ideconfig) {
        if (ideconfig == null) {
            //shouldn't really happen
            IDE_CONFIGURATION_NOTIFICATION_GROUP
                    .createNotification("Missing IDE configuration", NotificationType.WARNING);
            return;
        }
        String ideconfigtype = ideconfig.getType();
        if (ideconfigtype == null) {
            IDE_CONFIGURATION_NOTIFICATION_GROUP
                    .createNotification("Unrecognized IDE configuration type", NotificationType.WARNING)
                    .notify(project);
            return;
        }
        NavigableMap<String, Object> configfieldmap = new TreeMap<>();
        for (String fn : ideconfig.getFieldNames()) {
            if (fn == null) {
                continue;
            }
            Object fval = ideconfig.getField(fn);
            if (fval == null) {
                continue;
            }
            configfieldmap.put(fn, fval);
        }
        boolean recognizedtype = false;
        List<IIDEConfigurationTypeHandler> parsers = new ArrayList<>();
        for (IDEConfigurationTypeHandlerExtensionPointBean extbean : PluginCompatUtil
                .getExtensionList(IDEConfigurationTypeHandlerExtensionPointBean.EP_NAME)) {
            String type = extbean.getType();
            if (!ideconfigtype.equals(type)) {
                continue;
            }
            recognizedtype = true;
            try {
                IIDEConfigurationTypeHandler typehandler = extbean.createTypeHandler(project);
                parsers.add(typehandler);
            } catch (Exception e) {
                displayException(e);
                IDE_CONFIGURATION_NOTIFICATION_GROUP.createNotification("Extension error", extbean.implementationClass,
                        "Failed to load IDE configuration handler extension. (" + e + ")", NotificationType.ERROR)
                        .notify(project);
            }
        }
        if (parsers.isEmpty()) {
            if (!recognizedtype) {
                IDE_CONFIGURATION_NOTIFICATION_GROUP
                        .createNotification("Unrecognized IDE configuration type", NotificationType.WARNING)
                        .notify(project);
            }
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Apply IDE Configuration", true) {
            @Override
            public void run(@NotNull ProgressIndicator monitor) {
                List<IIDEProjectConfigurationRootEntry> rootentries = new ArrayList<>();
                boolean hadsuccess = false;
                NavigableMap<String, Object> immutableconfigfieldmap = ImmutableUtils
                        .unmodifiableNavigableMap(configfieldmap);
                for (IIDEConfigurationTypeHandler parser : parsers) {
                    IIDEProjectConfigurationRootEntry[] entries;
                    try {
                        entries = parser
                                .parseConfiguration(IntellijSakerIDEProject.this, immutableconfigfieldmap, monitor);
                    } catch (Exception e) {
                        displayException(e);
                        IDE_CONFIGURATION_NOTIFICATION_GROUP
                                .createNotification("Extension error", parser.getClass().getName(),
                                        "Failed to analyze IDE configuration. (" + e + ")", NotificationType.ERROR)
                                .notify(project);
                        continue;
                    }
                    if (ObjectUtils.isNullOrEmpty(entries)) {
                        continue;
                    }
                    for (IIDEProjectConfigurationRootEntry roote : entries) {
                        if (roote == null) {
                            continue;
                        }
                        rootentries.add(roote);
                    }
                    hadsuccess = true;
                }
                if (!hadsuccess) {
                    return;
                }
                IDEConfigurationSelectorDialog dialog = new IDEConfigurationSelectorDialog(IntellijSakerIDEProject.this,
                        rootentries, ideconfig.getIdentifier());
                dialog.setVisible(true);
                if (dialog.isOk()) {
                    for (IIDEProjectConfigurationRootEntry rootentry : rootentries) {
                        if (!rootentry.isSelected()) {
                            continue;
                        }
                        try {
                            rootentry.apply(monitor);
                        } catch (Exception e) {
                            displayException(e);
                            IDE_CONFIGURATION_NOTIFICATION_GROUP
                                    .createNotification("Extension error", rootentry.getClass().getName(),
                                            "Failed to apply IDE configuration. (" + e + ")", NotificationType.ERROR)
                                    .notify(project);
                            return;
                        }
                    }
                }
            }
        });
    }

    private void addBuildFilesToTargetsMenu(List<AnAction> result) {
        NavigableSet<SakerPath> filepaths = getTrackedScriptPaths();
        if (filepaths.isEmpty()) {
            result.add(new AnAction("Add new build file") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    VirtualFile projectdir = LocalFileSystem.getInstance().findFileByPath(getProjectPath().toString());
                    if (!projectdir.isDirectory()) {
                        return;
                    }
                    VirtualFile child = projectdir.findChild("saker.build");
                    if (child != null) {
                        FileEditorManager.getInstance(project).openEditor(new OpenFileDescriptor(project, child), true);
                        return;
                    }
                    try {
                        VirtualFile createdfile = projectdir.createChildData(e, "saker.build");
                        FileEditorManager.getInstance(project)
                                .openEditor(new OpenFileDescriptor(project, createdfile), true);
                        return;
                    } catch (IOException ex) {
                        displayException(ex);
                    }
                }

                @Override
                public boolean isDumbAware() {
                    return true;
                }
            });
        } else {
            SakerPath workingdirpath = getWorkingDirectoryExecutionPath();
            for (SakerPath buildfilepath : filepaths) {
                SakerPath relativepath = buildfilepath;
                if (workingdirpath != null) {
                    if (buildfilepath.startsWith(workingdirpath)) {
                        relativepath = workingdirpath.relativize(relativepath);
                    }
                }
                result.add(new ActionGroup(relativepath.toString(), true) {
                    @NotNull
                    @Override
                    public AnAction[] getChildren(@Nullable AnActionEvent e) {
                        List<AnAction> targetresult = new ArrayList<>();
                        appendTargetsToBuildFileMenu(targetresult);
                        targetresult.add(new Separator());
                        targetresult.add(new AnAction("Open in editor") {
                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e) {
                                openEditor();
                            }

                            @Override
                            public boolean isDumbAware() {
                                return true;
                            }
                        });
                        return targetresult.toArray(SakerBuildActionGroup.EMPTY_ANACTION_ARRAY);
                    }

                    private void openEditor() {
                        VirtualFile vfile = getVirtualFileAtExecutionPath(buildfilepath);
                        if (vfile != null) {
                            FileEditorManager.getInstance(project)
                                    .openEditor(new OpenFileDescriptor(project, vfile), true);
                        }
                    }

                    private void appendTargetsToBuildFileMenu(List<AnAction> targetresult) {
                        Set<String> scripttargets;
                        try {
                            scripttargets = getScriptTargets(buildfilepath);
                        } catch (ScriptParsingFailedException ex) {
                            ex.printStackTrace();
                            targetresult.add(new AnAction("Failed to parse script file") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    openEditor();
                                }

                                @Override
                                public boolean isDumbAware() {
                                    return true;
                                }
                            });
                            return;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            targetresult.add(new AnAction("Failed to open script file") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    openEditor();
                                }

                                @Override
                                public boolean isDumbAware() {
                                    return true;
                                }
                            });
                            return;
                        }
                        if (scripttargets == null) {
                            targetresult.add(new AnAction("Script is not part of the configuration") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    openEditor();
                                }

                                @Override
                                public boolean isDumbAware() {
                                    return true;
                                }
                            });
                            return;
                        }
                        if (scripttargets.isEmpty()) {
                            targetresult.add(new AnAction("No targets found") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    openEditor();
                                }

                                @Override
                                public boolean isDumbAware() {
                                    return true;
                                }
                            });
                            return;
                        }
                        for (String target : scripttargets) {
                            targetresult.add(new AnAction(target) {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    buildAsync(buildfilepath, target);
                                }

                                @Override
                                public boolean isDumbAware() {
                                    return true;
                                }
                            });
                        }
                    }
                });

            }
        }
    }

    private static class ProgressMonitorWrapper implements ExecutionProgressMonitor, TaskProgressMonitor {
        protected final ProgressIndicator progressMonitor;
        private volatile boolean cancelled;

        public ProgressMonitorWrapper(ProgressIndicator progressMonitor) {
            this.progressMonitor = progressMonitor;
        }

        @Override
        public boolean isCancelled() {
            if (cancelled) {
                return true;
            }
            if (progressMonitor != null && progressMonitor.isCanceled()) {
                cancelled = true;
                return true;
            }
            return false;
        }

        public void setCancelled() {
            this.cancelled = true;
        }

        @Override
        public TaskProgressMonitor startTaskProgress() {
            return this;
        }

    }

    private class ErrorConsoleViewOutputStream extends ConsoleViewOutputStream {
        private Matcher matcher;

        public ErrorConsoleViewOutputStream(ConsoleView console) {
            super(console);
            matcher = AT_BUILD_FILE_ERROR_PATTERN.matcher("");
            defaultContentType = ConsoleViewContentType.ERROR_OUTPUT;
        }

        @Override
        protected void appendLine(String line) {
            matcher.reset(line);
            if (matcher.matches()) {
                String path = matcher.group(AT_BUILD_FILE_ERROR_GROUP_PATH);
                if (!ObjectUtils.isNullOrEmpty(path)) {
                    path = path.trim();
                    VirtualFile vfile = getVirtualFileAtExecutionPath(SakerPath.valueOf(path));
                    if (vfile != null) {
                        String linenumstr = matcher.group(AT_BUILD_FILE_ERROR_GROUP_LINE_NUMBER);
                        String linestart = matcher.group(AT_BUILD_FILE_ERROR_GROUP_LINE_START);
                        String lineend = matcher.group(AT_BUILD_FILE_ERROR_GROUP_LINE_END);
                        int linenumber = linenumstr == null ? 0 : (Integer.parseInt(linenumstr) - 1);
                        if (linenumber <= 0) {
                            linenumber = 0;
                        }
                        int linestartnumber = linestart == null ? 0 : (Integer.parseInt(linestart) - 1);
                        if (linestartnumber < 0) {
                            linestartnumber = 0;
                        }
                        int len = 0;
                        if (lineend != null) {
                            int lineendnumber = Integer.parseInt(lineend) - 1;
                            if (lineendnumber < 0) {
                                lineendnumber = 0;
                            }
                            len = lineendnumber - linestartnumber + 1;
                        }
                        int pathstartidx = matcher.start(AT_BUILD_FILE_ERROR_GROUP_PATH);
                        console.print(line.substring(0, pathstartidx), defaultContentType);
                        console.printHyperlink(line.substring(pathstartidx),
                                new SelectionOpenFileHyperlinkInfo(project, vfile, linenumber, linestartnumber, len));
                        console.print(LINE_SEPARATOR, defaultContentType);
                        return;
                    }
                }
            }
            super.appendLine(line);
        }
    }

    private class StandardConsoleViewOutputStream extends ConsoleViewOutputStream {
        private Matcher matcher;

        public StandardConsoleViewOutputStream(ConsoleView console) {
            super(console);
            matcher = CONSOLE_MARKER_PATTERN.matcher("");
        }

        @Override
        protected void appendLine(String line) {
            matcher.reset(line);
            ConsoleViewContentType contenttype = defaultContentType;
            if (matcher.matches()) {
                String severity = matcher.group(CONSOLE_MARKER_GROUP_SEVERITY);
                contenttype = SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP.getOrDefault(severity, contenttype);

                String file = matcher.group(CONSOLE_MARKER_GROUP_FILEPATH);
                if (!ObjectUtils.isNullOrEmpty(file)) {
                    file = file.trim();
                    VirtualFile vfile = getVirtualFileAtExecutionPath(SakerPath.valueOf(file));
                    if (vfile != null) {
                        String linenumstr = matcher.group(CONSOLE_MARKER_GROUP_LINE);
                        String linestart = matcher.group(CONSOLE_MARKER_GROUP_LINESTART);
                        String lineend = matcher.group(CONSOLE_MARKER_GROUP_LINEEND);
                        int linenumber = linenumstr == null ? 0 : (Integer.parseInt(linenumstr) - 1);
                        if (linenumber <= 0) {
                            linenumber = 0;
                        }
                        int linestartnumber = linestart == null ? 0 : (Integer.parseInt(linestart) - 1);
                        if (linestartnumber < 0) {
                            linestartnumber = 0;
                        }

                        int len = 0;
                        if (lineend != null) {
                            int lineendnumber = Integer.parseInt(lineend) - 1;
                            if (lineendnumber < 0) {
                                lineendnumber = 0;
                            }
                            len = lineendnumber - linestartnumber + 1;
                        }
                        int pathandlocstart = matcher.start(CONSOLE_MARKER_GROUP_PATHANDLOCATION);
                        int pathandlocend = matcher.end(CONSOLE_MARKER_GROUP_PATHANDLOCATION);
                        SelectionOpenFileHyperlinkInfo hyperlinkinfo = new SelectionOpenFileHyperlinkInfo(project,
                                vfile, linenumber, linestartnumber, len);
                        hyperlinkinfo.setBackgroundColor(contenttype.getAttributes().getBackgroundColor());

                        console.print(line.substring(0, pathandlocstart), contenttype);
                        console.printHyperlink(line.substring(pathandlocstart, pathandlocend), hyperlinkinfo);
                        console.print(line.substring(pathandlocend) + LINE_SEPARATOR, contenttype);
                        return;
                    }
                }
            }
            console.print(line + LINE_SEPARATOR, contenttype);
        }
    }

    private class ApplyIDEConfigurationAnAction extends AnAction {
        private final IDEConfiguration ideconfig;

        public ApplyIDEConfigurationAnAction(IDEConfiguration ideconfig) {
            super(ideconfig.getIdentifier());
            this.ideconfig = ideconfig;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            applyIDEConfiguration(ideconfig);
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }
    }

    private class ApplyIDEConfigurationActionGroup extends ActionGroup {
        protected List<IDEConfiguration> ideConfigurations;

        public ApplyIDEConfigurationActionGroup(String typename) {
            super(typename, true);
            ideConfigurations = new ArrayList<>();
        }

        @NotNull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            List<AnAction> ideactionresults = new ArrayList<>();
            for (IDEConfiguration ideconfig : ideConfigurations) {
                ideactionresults.add(new ApplyIDEConfigurationAnAction(ideconfig));
            }

            return ideactionresults.toArray(SakerBuildActionGroup.EMPTY_ANACTION_ARRAY);
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }
    }
}
