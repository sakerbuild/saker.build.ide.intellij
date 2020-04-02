package saker.build.ide.intellij;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureView;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class BuildScriptStructureViewFactory implements PsiStructureViewFactory, DumbAware {
    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
        if (!(psiFile instanceof BuildScriptParserDefinition.BuildScriptPsiFile)) {
            return null;
        }
        return new StructureViewBuilder() {
            @NotNull
            @Override
            public StructureView createStructureView(FileEditor fileEditor, @NotNull Project project) {
                Editor editor;
                if (fileEditor instanceof TextEditor) {
                    editor = ((TextEditor) fileEditor).getEditor();
                    if (editor instanceof EditorEx) {
                        EditorHighlighter highlighter = ((EditorEx) editor).getHighlighter();
                        if (highlighter instanceof IBuildScriptEditorHighlighter) {
                            return ((IBuildScriptEditorHighlighter) highlighter)
                                    .createStructureView(psiFile, fileEditor);
                        }
                    }
                } else {
                    editor = null;
                }
                return new EmptyStructureView(fileEditor, editor, psiFile);
            }
        };
    }

    private static class EmptyStructureViewModel extends TextEditorBasedStructureViewModel {
        private static final PresentationData EMPTY_PRESENTATION_DATA = new PresentationData("Empty", null, null, null);

        private final StructureViewTreeElement rootElement = new StructureViewTreeElement() {
            @Override
            public Object getValue() {
                return getPsiFile();
            }

            @NotNull
            @Override
            public ItemPresentation getPresentation() {
                return EMPTY_PRESENTATION_DATA;
            }

            @NotNull
            @Override
            public TreeElement[] getChildren() {
                return new TreeElement[0];
            }

            @Override
            public void navigate(boolean requestFocus) {
            }

            @Override
            public boolean canNavigate() {
                return false;
            }

            @Override
            public boolean canNavigateToSource() {
                return false;
            }
        };

        public EmptyStructureViewModel(@NotNull PsiFile psiFile) {
            super(psiFile);
        }

        public EmptyStructureViewModel(Editor editor) {
            super(editor);
        }

        public EmptyStructureViewModel(Editor editor, PsiFile file) {
            super(editor, file);
        }

        @NotNull
        @Override
        public StructureViewTreeElement getRoot() {
            return rootElement;
        }
    }

    private static class EmptyStructureView implements StructureView {
        private final FileEditor fileEditor;
        private final Editor editor;
        private final PsiFile psiFile;

        public EmptyStructureView(FileEditor fileEditor, Editor editor, PsiFile psiFile) {
            this.fileEditor = fileEditor;
            this.editor = editor;
            this.psiFile = psiFile;
        }

        @Override
        public FileEditor getFileEditor() {
            return fileEditor;
        }

        @Override
        public boolean navigateToSelectedElement(boolean requestFocus) {
            return false;
        }

        @Override
        public JComponent getComponent() {
            JPanel panel = new JPanel(new GridLayoutManager(1, 1));
            panel.add(new JLabel("No structure view available."), new GridConstraints());
            return panel;
        }

        @Override
        public void centerSelectedRow() {
        }

        @Override
        public void restoreState() {
        }

        @Override
        public void storeState() {
        }

        @NotNull
        @Override
        public StructureViewModel getTreeModel() {
            if (editor == null) {
                return new EmptyStructureViewModel(psiFile);
            }
            return new EmptyStructureViewModel(editor, psiFile);
        }

        @Override
        public void dispose() {
        }
    }
}
