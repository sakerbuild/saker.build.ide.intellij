package saker.build.ide.intellij.impl.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ColorUtil;
import org.jetbrains.annotations.NotNull;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.ui.ScriptEditorModel;
import saker.build.scripting.model.ScriptSyntaxModel;
import saker.build.scripting.model.ScriptTokenInformation;
import saker.build.scripting.model.TextRegionChange;
import saker.build.scripting.model.TokenStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class BuildScriptEditorStateManager implements Disposable, DocumentListener {
    private static final AtomicIntegerFieldUpdater<BuildScriptEditorStateManager> AIFU_subCount = AtomicIntegerFieldUpdater
            .newUpdater(BuildScriptEditorStateManager.class, "subCount");

    private final IntellijSakerIDEProject project;
    private final IntellijScriptEditorModel editorModel;

    private ScriptingResourcesChangeListener changeListener;

    private List<TextRegionChange> bulkRegionChanges = null;

    private volatile int subCount = 0;

    public BuildScriptEditorStateManager(Document document, IntellijSakerIDEProject project,
            SakerPath scriptExecutionPath) {
        this.project = project;
        editorModel = new IntellijScriptEditorModel(scriptExecutionPath);
        editorModel.setEnvironment(project.getSakerProject());
        editorModel.resetInput(document.getCharsSequence());

        changeListener = new ScriptingResourcesChangeListener();
        project.addProjectPropertiesChangeListener(changeListener);

        document.addDocumentListener(this, this);
    }

    public boolean isAlive() {
        return subCount >= 0;
    }

    public void unsubscribe() {
        int sc = AIFU_subCount.updateAndGet(this, c -> {
            if (c <= 0) {
                throw new IllegalStateException();
            }
            if (c == 1) {
                return -1;
            }
            return c - 1;
        });
        if (sc == -1) {
            Disposer.dispose(this);
        }
    }

    public void subscribe() {
        AIFU_subCount.updateAndGet(this, c -> {
            if (c < 0) {
                throw new IllegalStateException();
            }
            return c + 1;
        });
    }

    public SakerPath getScriptExecutionPath() {
        ScriptEditorModel emodel = this.editorModel;
        if (emodel != null) {
            return editorModel.getScriptExecutionPath();
        }
        return null;
    }

    public void setCurrentColorScheme(EditorColorsScheme colorscheme) {
        ScriptEditorModel emodel = this.editorModel;
        if (emodel != null) {
            emodel.setTokenTheme(colorSchemeToTokenTheme(colorscheme));
        }
    }

    public ScriptSyntaxModel getModelMaybeOutOfDate() {
        try {
            ScriptEditorModel emodel = this.editorModel;
            if (emodel != null) {
                return emodel.getModelMaybeOutOfDate();
            }
        } catch (Exception e) {
            project.displayException(e);
        }
        return null;
    }

    public ScriptSyntaxModel getModel() {
        try {
            ScriptEditorModel emodel = this.editorModel;
            if (emodel != null) {
                return emodel.getUpToDateModel();
            }
        } catch (Exception e) {
            project.displayException(e);
        }
        return null;
    }

    public ScriptTokenInformation getTokenInformationAtPosition(int start, int length) {
        try {
            ScriptEditorModel emodel = this.editorModel;
            if (emodel != null) {
                return emodel.getTokenInformationAtPosition(start, length);
            }
        } catch (Exception e) {
            project.displayException(e);
        }
        return null;
    }

    public List<ScriptEditorModel.TokenState> getCurrentTokenState() {
        try {
            ScriptEditorModel emodel = this.editorModel;
            if (emodel != null) {
                return emodel.getCurrentTokenState();
            }
        } catch (Exception e) {
            project.displayException(e);
        }
        return null;
    }

    @Override
    public void dispose() {
        project.removeProjectPropertiesChangeListener(changeListener);
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent e) {
        TextRegionChange regionchange = toTextRegionChange(e);
        if (bulkRegionChanges != null) {
            bulkRegionChanges.add(regionchange);
        } else {
            ScriptEditorModel emodel = this.editorModel;
            if (emodel != null) {
                emodel.textChange(regionchange);
            }
        }
    }

    @Override
    public void bulkUpdateStarting(@NotNull Document document) {
        bulkRegionChanges = new ArrayList<>();
    }

    @Override
    public void bulkUpdateFinished(@NotNull Document document) {
        ScriptEditorModel emodel = this.editorModel;
        if (emodel != null) {
            emodel.textChange(bulkRegionChanges);
        }
        bulkRegionChanges = null;
    }

    public void addModelListener(ScriptEditorModel.ModelUpdateListener listener) {
        ScriptEditorModel emodel = this.editorModel;
        if (emodel != null) {
            emodel.addModelListener(listener);
        }
    }

    public void removeModelListener(ScriptEditorModel.ModelUpdateListener listener) {
        ScriptEditorModel emodel = this.editorModel;
        if (emodel != null) {
            emodel.removeModelListener(listener);
        }
    }

    private static TextRegionChange toTextRegionChange(DocumentEvent event) {
        return new TextRegionChange(event.getOffset(), event.getOldLength(), event.getNewFragment().toString());
    }

    private class ScriptingResourcesChangeListener implements IntellijSakerIDEProject.ProjectPropertiesChangeListener {

        @Override
        public void projectPropertiesChanging() {
            ScriptEditorModel emodel = BuildScriptEditorStateManager.this.editorModel;
            if (emodel != null) {
                emodel.clearModel();
            }
        }

        @Override
        public void projectPropertiesChanged() {
            ScriptEditorModel emodel = BuildScriptEditorStateManager.this.editorModel;
            if (emodel != null) {
                emodel.initModel();
            }
        }
    }

    private static int colorSchemeToTokenTheme(@NotNull EditorColorsScheme scheme) {
        return ColorUtil.isDark(scheme.getDefaultBackground()) ? TokenStyle.THEME_DARK : TokenStyle.THEME_LIGHT;
    }
}
