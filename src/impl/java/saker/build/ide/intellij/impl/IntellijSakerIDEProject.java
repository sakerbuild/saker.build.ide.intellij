package saker.build.ide.intellij.impl;

import com.intellij.execution.filters.BrowserHyperlinkInfo;
import com.intellij.execution.filters.FileHyperlinkInfo;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import javafx.application.Application;
import org.apache.commons.io.output.WriterOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.daemon.DaemonEnvironment;
import saker.build.exception.ScriptPositionedExceptionView;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.ide.configuration.IDEConfiguration;
import saker.build.ide.intellij.ISakerBuildProjectImpl;
import saker.build.ide.intellij.SakerBuildActionGroup;
import saker.build.ide.intellij.impl.editor.BuildScriptEditorHighlighter;
import saker.build.ide.intellij.impl.properties.SakerBuildProjectConfigurable;
import saker.build.ide.intellij.impl.ui.ProjectPropertiesValidationDialog;
import saker.build.ide.support.ExceptionDisplayer;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.configuration.ProjectIDEConfigurationCollection;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.PropertiesValidationErrorResult;
import saker.build.ide.support.properties.PropertiesValidationException;
import saker.build.ide.support.properties.ProviderMountIDEProperty;
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
import saker.build.task.utils.TaskUtils;
import saker.build.thirdparty.saker.util.DateUtils;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.ByteSink;
import saker.build.thirdparty.saker.util.io.ByteSource;
import saker.build.thirdparty.saker.util.io.IOUtils;
import saker.build.thirdparty.saker.util.io.StreamUtils;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final String LAST_BUILD_SCRIPT_PATH = "last-build-script-path";
    private static final String LAST_BUILD_TARGET_NAME = "last-build-target-name";

    private final IntellijSakerIDEPlugin plugin;
    private final SakerIDEProject sakerProject;
    private final Project project;

    private final Lock executionLock = new ReentrantLock();
    private final Object configurationChangeLock = new Object();

    private final Set<ProjectPropertiesChangeListener> propertiesChangeListeners = Collections
            .newSetFromMap(new WeakHashMap<>());

    public IntellijSakerIDEProject(IntellijSakerIDEPlugin plugin, SakerIDEProject sakerProject, Project project) {
        this.plugin = plugin;
        this.sakerProject = sakerProject;
        this.project = project;
    }

    public void initialize() {
        sakerProject.addExceptionDisplayer(this);

        String basepath = project.getBasePath();
        if (basepath != null) {
            Path projectpath = Paths.get(basepath);
            sakerProject.initialize(projectpath);
        }
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
        ProgressManager progmanager = ProgressManager.getInstance();
        progmanager.run(new Task.Backgroundable(project, "Saker.build execution", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                build(scriptfile, targetname, indicator);
            }
        });
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

    private ConcurrentHashMap<SakerPath, BuildScriptEditorHighlighter> highlighters = new ConcurrentHashMap<>();

    @Override
    public EditorHighlighter getEditorHighlighter(VirtualFile file, EditorColorsScheme colors) {
        SakerPath execpath = projectPathToExecutionPath(SakerPath.valueOf(file.getPath()));
        return highlighters.computeIfAbsent(execpath, ep -> new BuildScriptEditorHighlighter(this, ep, colors));
//        return new BuildScriptEditorHighlighter(this, execpath, colors);
    }

    public void disposeHighlighter(SakerPath scriptPath, BuildScriptEditorHighlighter highlighter) {
        highlighters.remove(scriptPath, highlighter);
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
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP.put("error", ConsoleViewContentType.LOG_ERROR_OUTPUT);
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP.put("fatal error", ConsoleViewContentType.ERROR_OUTPUT);
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP.put("info", ConsoleViewContentType.LOG_INFO_OUTPUT);
        SEVERITY_CONSOLE_VIEW_CONTENT_TYPE_MAP.put("warning", ConsoleViewContentType.LOG_WARNING_OUTPUT);
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

    private static class SelectionOpenFileHyperlinkInfo implements FileHyperlinkInfo {
        private Project project;
        private VirtualFile file;
        private int line;
        private int column;
        private int length;

        public SelectionOpenFileHyperlinkInfo(Project project, VirtualFile file, int line, int column, int length) {
            this.project = project;
            this.file = file;
            this.line = line;
            this.column = column;
            this.length = length;
        }

        @Override
        public void navigate(Project project) {
            if (length > 0) {
                Editor editor = FileEditorManager.getInstance(project)
                        .openTextEditor(new OpenFileDescriptor(project, file), true);
                if (editor != null) {
                    SelectionModel selectionmodel = editor.getSelectionModel();
                    Document document = editor.getDocument();
                    int coloffset = document.getLineStartOffset(line) + column;
                    int lineendoffset = document.getLineEndOffset(line);
                    if (coloffset > lineendoffset) {
                        coloffset = lineendoffset;
                    }

                    //only higlight the first line, as the selection for multiple lines doesn't work,
                    //    as IntelliJ doesn't handle line endings properly
                    // (\r\n line endings occupy only a single element, therefore multi-line highlight will drift off)
                    selectionmodel.setSelection(coloffset, coloffset + Math.min(length, lineendoffset - coloffset));
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
        ProgressMonitorWrapper monitorwrapper = new ProgressMonitorWrapper(monitor);

        boolean wasinterrupted = false;

        try {
            BuildTaskExecutionResult result = null;
            SakerPath executionworkingdir = null;

            OutputStream out = null;
            OutputStream err = null;
            try {
                ToolWindow[] tw = { null };
                ConsoleView[] console = { null };
                SwingUtilities.invokeAndWait(() -> {
                    tw[0] = ToolWindowManager.getInstance(project).getToolWindow("saker.build");
                    console[0] = (ConsoleView) tw[0].getContentManager().getContent(0).getComponent();
                });

                out = new ConsoleViewOutputStream(console[0]) {
                    private Matcher matcher = CONSOLE_MARKER_PATTERN.matcher("");

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
                                    console.print(line.substring(0, pathandlocstart), contenttype);
                                    console.printHyperlink(line.substring(pathandlocstart, pathandlocend),
                                            new SelectionOpenFileHyperlinkInfo(project, vfile, linenumber,
                                                    linestartnumber, len));
                                    console.print(line.substring(pathandlocend) + LINE_SEPARATOR, contenttype);
                                    return;
                                }
                            }
                        }
                        console.print(line + LINE_SEPARATOR, contenttype);
                    }
                };
                err = new ConsoleViewOutputStream(console[0]) {
                    private Matcher matcher = AT_BUILD_FILE_ERROR_PATTERN.matcher("");

                    {
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
                                    console.printHyperlink(line.substring(pathstartidx) + LINE_SEPARATOR,
                                            new SelectionOpenFileHyperlinkInfo(project, vfile, linenumber,
                                                    linestartnumber, len));
                                    return;
                                }
                            }
                        }
                        super.appendLine(line);
                    }
                };

                executionLock.lockInterruptibly();
                console[0].clear();
                SwingUtilities.invokeLater(() -> tw[0].activate(null));
                if (monitorwrapper.isCancelled()) {
                    out.write("Build cancelled.\n".getBytes());
                    return;
                }
                ExecutionParametersImpl params;
                IDEProjectProperties projectproperties = getIDEProjectProperties();
                try {
                    params = sakerProject.createExecutionParameters(projectproperties);
                    //there were no validation errors
                } catch (PropertiesValidationException e) {
                    //TODO remove exception display
                    displayException(e);
                    //TODO show some dialog and fixes
                    err.write("Invalid build configuration:\n".getBytes());
                    for (PropertiesValidationErrorResult error : e.getErrors()) {
                        //TODO write as hyperlinkgs
                        err.write(SakerIDESupportUtils.createValidationErrorMessage(error).getBytes());
                        err.write('\n');
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
                    out.write(("Build started. (" + jobname + ")\n").getBytes());
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
                        out.write("Failed to initialize execution.\n".getBytes());
                    } else {
                        out.write(("Build finished. " + new Date(System.currentTimeMillis()) + " (" + DateUtils
                                .durationToString((finishtime - starttime) / 1_000_000) + ")\n").getBytes());
                    }
                } catch (IOException e) {
                    //shouldnt happen, we don't display this exception to the user
                    e.printStackTrace();
                }
                SakerPath builddir = params.getBuildDirectory();
                Path projectpath = getProjectPath();
                if (builddir != null) {
                    try {
                        if (builddir.isRelative()) {
                            builddir = pathconfiguration.getWorkingDirectory().resolve(builddir);
                        }
                        Path builddirlocalpath = pathconfiguration.toLocalPath(builddir);
                        if (builddirlocalpath != null && builddirlocalpath.startsWith(projectpath)) {
                            //TODO refresh build folder
//                            IFolder buildfolder = ideProject
//                                    .getFolder(projectpath.relativize(builddirlocalpath).toString());
//                            if (buildfolder != null) {
//                                buildfolder.refreshLocal(IFolder.DEPTH_INFINITE, monitor);
//                                if (buildfolder.exists()) {
//                                    buildfolder.setDerived(true, monitor);
//                                }
//                            }
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
//                                IFolder mirrorfolder = ideProject
//                                        .getFolder(projectpath.relativize(localmirrorpath).toString());
//                                if (mirrorfolder != null) {
//                                    //intentionally don't refresh. There's no particular need for it
//                                    mirrorfolder.setDerived(true, monitor);
//                                }
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
                        out.write("Failed to initialize execution.\n".getBytes());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                return;
            } finally {
                if (result != null) {
                    ScriptPositionedExceptionView posexcview = result.getPositionedExceptionView();
                    if (posexcview != null) {
                        //TODO make exception format configureable
                        if (err != null) {
                            TaskUtils.printTaskExceptionsOmitTransitive(posexcview, new PrintStream(err),
                                    executionworkingdir, SakerLog.CommonExceptionFormat.DEFAULT_FORMAT);
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
                Thread.interrupted();
            }
        }
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
        } catch (Exception e) {
            displayException(e);
        }
        //TODO ask which one to call and run
    }

    public final void addProjectResourceListener(SakerIDEProject.ProjectResourceListener listener) {
        sakerProject.addProjectResourceListener(listener);
    }

    public final void removeProjectResourceListener(SakerIDEProject.ProjectResourceListener listener) {
        sakerProject.removeProjectResourceListener(listener);
    }

    public IntellijSakerIDEPlugin getPlugin() {
        return plugin;
    }

    protected void close() throws IOException {

    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Configurable getProjectPropertiesConfigurable() {
        return new SakerBuildProjectConfigurable(this);
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
                ProgressManager progmanager = ProgressManager.getInstance();
                progmanager.run(new Task.Backgroundable(null, "Updating saker.build project properties", true) {
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

    @Override
    public void addSakerBuildTargetsMenuActions(List<AnAction> result) {
        NavigableSet<SakerPath> filepaths = getTrackedScriptPaths();
        if (filepaths.isEmpty()) {
            result.add(new AnAction("Add new build file") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    //TODO add new build file
                    System.out.println("TargetsActionGroup.actionPerformed");
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
                        return targetresult.toArray(SakerBuildActionGroup.EMPTY_ANACTION_ARRAY);
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
                                    //TODO dummy
                                }
                            });
                            return;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            targetresult.add(new AnAction("Failed to open script file") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    //TODO dummy
                                }
                            });
                            return;
                        }
                        if (scripttargets == null) {
                            targetresult.add(new AnAction("Script is not part of the configuration") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    //TODO dummy
                                }
                            });
                            return;
                        }
                        if (scripttargets.isEmpty()) {
                            targetresult.add(new AnAction("No targets found") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    //TODO dummy
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

        @Override
        public TaskProgressMonitor startTaskProgress() {
            return this;
        }

    }
}
