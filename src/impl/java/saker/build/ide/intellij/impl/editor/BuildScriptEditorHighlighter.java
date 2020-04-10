package saker.build.ide.intellij.impl.editor;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.FileEditorPositionListener;
import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.StructureView;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.BuildScriptLanguage;
import saker.build.ide.intellij.DocumentationHolder;
import saker.build.ide.intellij.IBuildScriptEditorHighlighter;
import saker.build.ide.intellij.extension.script.information.IScriptInformationDesigner;
import saker.build.ide.intellij.extension.script.outline.IScriptOutlineDesigner;
import saker.build.ide.intellij.extension.script.proposal.IScriptProposalDesigner;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.ui.BaseScriptInformationRoot;
import saker.build.ide.support.ui.ScriptEditorModel;
import saker.build.scripting.model.CompletionProposalEdit;
import saker.build.scripting.model.CompletionProposalEditKind;
import saker.build.scripting.model.FormattedTextContent;
import saker.build.scripting.model.InsertCompletionProposalEdit;
import saker.build.scripting.model.ScriptCompletionProposal;
import saker.build.scripting.model.ScriptStructureOutline;
import saker.build.scripting.model.ScriptSyntaxModel;
import saker.build.scripting.model.ScriptTokenInformation;
import saker.build.scripting.model.StructureOutlineEntry;
import saker.build.scripting.model.TextRegionChange;
import saker.build.scripting.model.TokenStyle;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class BuildScriptEditorHighlighter implements EditorHighlighter, IBuildScriptEditorHighlighter, DumbAware {
    private static final IElementType TOKEN_ELEMENT_TYPE = new IElementType("SAKER_SCRIPT_TOKEN",
            BuildScriptLanguage.INSTANCE);

    private static final AtomicReferenceFieldUpdater<BuildScriptEditorHighlighter, HighlighterClient> ARFU_editor = AtomicReferenceFieldUpdater
            .newUpdater(BuildScriptEditorHighlighter.class, HighlighterClient.class, "editor");

    private BuildScriptEditorStateManager stateManager;
    private final IntellijSakerIDEProject project;
    private SakerPath scriptExecutionPath;
    private EditorColorsScheme currentColorScheme;

    private volatile HighlighterClient editor;

    private final ConcurrentHashMap<TokenStyle, TextAttributes> tokenStyleAttributes = new ConcurrentHashMap<>();

    private final StructureViewBuilder structureViewBuilder = new TreeBasedStructureViewBuilder() {
        @NotNull
        @Override
        public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
            return new BuildScriptStructureViewModel(editor);
        }

        @Override
        public boolean isRootNodeShown() {
            return false;
        }
    };

    //IntelliJ warns that this could be a local variable.
    //  As listeners are weakly referenced by the model, keep this as a field.
    private final ScriptEditorModel.ModelUpdateListener modelUpdateListener = model -> {
        HighlighterClient editor = BuildScriptEditorHighlighter.this.editor;
        if (editor == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            HighlighterClient currenteditor = BuildScriptEditorHighlighter.this.editor;
            if (editor != currenteditor) {
                return;
            }
            currenteditor.repaint(0, Integer.MAX_VALUE);
        });
    };

    public BuildScriptEditorHighlighter(IntellijSakerIDEProject project, SakerPath scriptpath,
            EditorColorsScheme colors) {
        this.project = project;
        this.scriptExecutionPath = scriptpath;
        this.currentColorScheme = colors;
    }

    public ScriptSyntaxModel getModelMaybeOutOfDate() {
        BuildScriptEditorStateManager sm = this.stateManager;
        if (sm != null) {
            return sm.getModelMaybeOutOfDate();
        }
        return null;
    }

    public ScriptSyntaxModel getModel() {
        BuildScriptEditorStateManager sm = this.stateManager;
        if (sm != null) {
            return sm.getModel();
        }
        return null;
    }

    @Override
    public StructureView createStructureView(PsiFile psiFile, FileEditor fileEditor) {
        return structureViewBuilder.createStructureView(fileEditor, project.getProject());
    }

    @Override
    public void performCompletion(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        ScriptSyntaxModel model = getModel();
        if (model == null) {
            return;
        }
        //result = result.withPrefixMatcher(new PlainPrefixMatcher("", true));

        result = result.withPrefixMatcher(PrefixMatcher.ALWAYS_TRUE);

        //result.restartCompletionWhenNothingMatches();
        result = result.withRelevanceSorter(CompletionSorter.emptySorter());
        List<? extends ScriptCompletionProposal> proposals = model.getCompletionProposals(parameters.getOffset());
        IntellijScriptProposalRoot proposalroot = IntellijScriptProposalRoot.create(proposals);
        IScriptProposalDesigner designer = project
                .getScriptProposalDesignerForSchemaIdentifiers(proposalroot.getSchemaIdentifiers());
        if (designer != null) {
            designer.process(proposalroot);
        }
        for (IntellijScriptProposalEntry proposal : proposalroot.getProposals()) {
            addIntellijProposal(parameters, result, proposal);
        }
    }

    private void addIntellijProposal(CompletionParameters parameters, @NotNull CompletionResultSet result,
            IntellijScriptProposalEntry intellijproposal) {
        ScriptCompletionProposal proposal = intellijproposal.getProposal();
        List<? extends CompletionProposalEdit> changes = proposal.getTextChanges();
        if (ObjectUtils.isNullOrEmpty(changes)) {
            return;
        }
        //TODO support complex edits
        for (CompletionProposalEdit c : changes) {
            String ckind = c.getKind();
            if (!CompletionProposalEditKind.INSERT.equalsIgnoreCase(ckind)) {
                //non insert proposals not yet supported
                return;
            }
        }
        int count = changes.size();
        if (count != 1) {
            return;
        }
        for (int i = 0; i < count; i++) {
            CompletionProposalEdit c = changes.get(i);
            for (int j = i + 1; j < count; j++) {
                CompletionProposalEdit c2 = changes.get(j);
                if (CompletionProposalEdit.overlaps(c, c2)) {
                    System.err.println("Overlaps: " + c + " and " + c2);
                    //XXX display info?
                    //invalid proposal
                    return;
                }
            }
        }
        InsertCompletionProposalEdit edit = (InsertCompletionProposalEdit) changes.get(0);
        final int editoffset = edit.getOffset();
        if (edit.getLength() > 0) {
            int prefixendoffset = Math.min(editoffset + edit.getLength(), parameters.getOffset());
            if (prefixendoffset > editoffset) {
                String prefix = parameters.getEditor().getDocument()
                        .getText(new TextRange(editoffset, prefixendoffset));
                result = result.withPrefixMatcher(new PlainPrefixMatcher(prefix));
            }
        }
        LookupElementBuilder builder = LookupElementBuilder.create(new DocumentationHolder() {
            @Override
            public String getDocumentation() {
                return generateDocumentation(intellijproposal.getInformationEntries());
            }
        }, edit.getText());
        builder = builder.withPresentableText(proposal.getDisplayString());
        if (!ObjectUtils.isNullOrEmpty(proposal.getDisplayRelation())) {
            builder = builder.withTailText(" : " + proposal.getDisplayRelation());
        }
        if (!ObjectUtils.isNullOrEmpty(proposal.getDisplayType())) {
            builder = builder.withTypeText(proposal.getDisplayType());
        }
        Icon proposalicon = intellijproposal.getProposalIcon();
        if (proposalicon != null) {
            builder = builder.withIcon(proposalicon);
        }
        builder = builder.withInsertHandler(new InsertHandler<LookupElement>() {
            @Override
            public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
                context.getDocument().replaceString(editoffset, context.getSelectionEndOffset(),
                        ObjectUtils.nullDefault(edit.getText(), ""));
                context.getEditor().getCaretModel().moveToOffset(proposal.getSelectionOffset());
                context.commitDocument();
            }
        });

        LookupElement lookupelem = builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE);
        result.addElement(lookupelem);
    }

    @Override
    public String getDocumentationAtOffset(int offset) {
        BuildScriptEditorStateManager sm = this.stateManager;
        if (sm == null) {
            return null;
        }
        ScriptTokenInformation tokeninfo = sm.getTokenInformationAtPosition(offset, 0);
        if (tokeninfo == null) {
            return null;
        }
        IntellijScriptInformationRoot inforoot = IntellijScriptInformationRoot.create(tokeninfo);
        IScriptInformationDesigner designer = project
                .getScriptInformationDesignerForSchemaIdentifier(inforoot.getSchemaIdentifier());
        if (designer != null) {
            designer.process(inforoot);
        }
        return generateDocumentation(inforoot.getEntries());
    }

    private static void appendHtmlHeader(StringBuilder contentsb, Color bgcol) {
        contentsb.append("<!DOCTYPE html><html><head><style>\r\n");
        contentsb.append("html { font-family: 'Segoe UI',sans-serif; font-style: normal; font-weight: normal; }\r\n");
        contentsb.append("body, h1, h2, h3, h4, h5, h6, p, table, td, caption, th, ul, ol, dl, li, dd, dt {  }\r\n");
        contentsb.append(".ptitle { font-weight: bold; }\r\n");
        contentsb.append(".ptitle>img { margin-right: 0.2em; }\r\n");
        contentsb.append(".psubtitle { font-weight: normal; font-style: italic; margin-left: 6px; }\r\n");
        contentsb.append(".pcontent { margin-top: 5px; margin-left: 6px; }\r\n");
        contentsb.append("hr { opacity: 0.5; }\r\n");
        contentsb.append("pre { font-family: monospace; }</style></head><body");
        if (bgcol != null) {
            contentsb.append(" style=\"background-color: #");
            appendColorRGB(contentsb, bgcol);
            contentsb.append(";\"");
        }
        contentsb.append(">");
    }

    private static void appendColorRGB(StringBuilder contentsb, Color bgcol) {
        contentsb.append(String.format("%02x%02x%02x", bgcol.getRed(), bgcol.getGreen(), bgcol.getBlue()));
    }

    private static void appendHtmlFooter(StringBuilder contentsb) {
        contentsb.append("</body></html>");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void appendFormatted(StringBuilder sectionsb, FormattedTextContent formattedinput) {
        if (formattedinput == null) {
            return;
        }
        //white-space: pre-line doesn't work in intellij, so we replace \n with <br> in case of plaintext formats
        Set<String> formats = formattedinput.getAvailableFormats();
        if (formats.contains(FormattedTextContent.FORMAT_HTML)) {
            String formattedtext = formattedinput.getFormattedText(FormattedTextContent.FORMAT_HTML);
            if (!ObjectUtils.isNullOrEmpty(formattedtext)) {
                //should not be null, but just in case of client error
                sectionsb.append("<div class=\"pcontent\">");
                sectionsb.append(formattedtext);
                sectionsb.append("</div>");
                return;
            }
        }
        if (formats.contains(FormattedTextContent.FORMAT_PLAINTEXT)) {
            String formattedtext = formattedinput.getFormattedText(FormattedTextContent.FORMAT_PLAINTEXT);
            if (formattedtext != null) {
                //should not be null, but just in case of client error
                sectionsb.append("<div class=\"pcontent\">");
                sectionsb.append(escapeHtml(formattedtext).replace("\n", "<br>"));
                sectionsb.append("</div>");
                return;
            }
        }
        for (String f : formats) {
            String formattedtext = formattedinput.getFormattedText(f);
            if (ObjectUtils.isNullOrEmpty(formattedtext)) {
                continue;
            }
            //should not be null, but just in case of client error
            sectionsb.append("<div class=\"pcontent\">");
            sectionsb.append(escapeHtml(formattedtext).replace("\n", "<br>"));
            sectionsb.append("</div>");
            return;
        }
    }

    private static void appendSectionHeader(StringBuilder sectionsb, String title, String subtitle, String iconimgsrc) {
        if (ObjectUtils.isNullOrEmpty(title)) {
            return;
        }
        sectionsb.append("<div class=\"ptitle\">");
        //TODO reimplement script information icons
//        if (!ObjectUtils.isNullOrEmpty(iconimgsrc)) {
//            sectionsb.append("<img src=\"");
//            sectionsb.append(iconimgsrc);
//            sectionsb.append("\" width=\"14\" align=\"middle\">");
//        }
        sectionsb.append(escapeHtml(title));
        sectionsb.append("</div>");
        if (!ObjectUtils.isNullOrEmpty(subtitle)) {
            sectionsb.append("<div class=\"psubtitle\">");
            sectionsb.append(escapeHtml(subtitle));
            sectionsb.append("</div>");
        }
    }

    public static String generateDocumentation(List<? extends IntellijScriptInformationEntry> entries) {
        if (entries == null) {
            return null;
        }
        Set<String> sections = new LinkedHashSet<>();

        StringBuilder sectionsb = new StringBuilder();

        List<IntellijScriptInformationEntry> filtered = BaseScriptInformationRoot
                .getFilteredBuildScriptScriptInformationEntries(entries);

        for (IntellijScriptInformationEntry entry : filtered) {
            sectionsb.setLength(0);
            String title = entry.getTitle();
            String subtitle = entry.getSubTitle();
            appendSectionHeader(sectionsb, SakerIDESupportUtils.resolveInformationTitle(title, subtitle),
                    SakerIDESupportUtils.resolveInformationSubTitle(title, subtitle), entry.getIconSource());
            FormattedTextContent formattedinput = entry.getContent();
            appendFormatted(sectionsb, formattedinput);
            if (sectionsb.length() > 0) {
                sections.add(sectionsb.toString());
            }
        }
        if (sections.isEmpty()) {
            return null;
        }
        StringBuilder contentsb = new StringBuilder();
        appendHtmlHeader(contentsb, null);
        for (Iterator<String> it = sections.iterator(); it.hasNext(); ) {
            String section = it.next();
            contentsb.append(section);
            if (it.hasNext()) {
                contentsb.append("<hr>");
            }
        }
        appendHtmlFooter(contentsb);
        return contentsb.toString();
    }

    @NotNull
    @Override
    public HighlighterIterator createIterator(int startOffset) {
        BuildScriptEditorStateManager sm = this.stateManager;
        if (sm == null) {
            return new EmptyNoTokensHighlighterIterator(startOffset);
        }
        List<ScriptEditorModel.TokenState> tokenstatelist = sm.getCurrentTokenState();
        if (tokenstatelist == null || tokenstatelist.isEmpty()) {
            return new EmptyNoTokensHighlighterIterator(startOffset);
        }
        ListIterator<ScriptEditorModel.TokenState> lit = tokenstatelist.listIterator();
        while (true) {
            if (!lit.hasNext()) {
                //no tokens after the start offset
                return new EmptyNoTokensHighlighterIterator(startOffset);
            }
            ScriptEditorModel.TokenState ts = lit.next();
            if (ts.getEndOffset() >= startOffset) {
                tokenstatelist = tokenstatelist.subList(lit.previousIndex(), tokenstatelist.size());
                break;
            }
            //else continue
        }
        if (tokenstatelist.isEmpty()) {
            return new EmptyNoTokensHighlighterIterator(startOffset);
        }
        ListIterator<? extends ScriptEditorModel.TokenState> iter = tokenstatelist.listIterator();
        return new HighlighterIterator() {
            private ScriptEditorModel.TokenState token = iter.hasNext() ? iter.next() : null;
            private int offset = token == null ? startOffset : token.getOffset();

            @Override
            public TextAttributes getTextAttributes() {
                if (token == null) {
                    return DEFAULT_TOKEN_STYLE;
                }
                TokenStyle style = token.getStyle();
                if (style == null) {
                    return DEFAULT_TOKEN_STYLE;
                }
                return getTokenStyleAttributes(style);
            }

            @Override
            public int getStart() {
                if (token == null) {
                    return offset;
                }
                return token.getOffset();
            }

            @Override
            public int getEnd() {
                if (token == null) {
                    return offset;
                }
                return token.getEndOffset();
            }

            @Override
            public void advance() {
                if (!iter.hasNext()) {
                    token = null;
                } else {
                    token = iter.next();
                    offset = token.getOffset();
                }
            }

            @Override
            public void retreat() {
                token = iter.previous();
            }

            @Override
            public boolean atEnd() {
                return token == null && !iter.hasNext();
            }

            @Override
            public Document getDocument() {
                HighlighterClient editor = BuildScriptEditorHighlighter.this.editor;
                if (editor == null) {
                    return null;
                }
                return editor.getDocument();
            }

            @Override
            public IElementType getTokenType() {
                return TOKEN_ELEMENT_TYPE;
            }
        };
    }

    private TextAttributes getTokenStyleAttributes(TokenStyle style) {
        return tokenStyleAttributes.computeIfAbsent(style, k -> {
            TextAttributes attrs = new TextAttributes();
            attrs.setEffectType(null);
            int fgc = style.getForegroundColor();
            int bgc = style.getBackgroundColor();
            if (fgc != TokenStyle.COLOR_UNSPECIFIED) {
                attrs.setForegroundColor(new Color(fgc));
            }
            if (bgc != TokenStyle.COLOR_UNSPECIFIED) {
                attrs.setBackgroundColor(new Color(bgc));
            }
            int s = k.getStyle();
            if (s != 0) {
                //there's at least one effect
                int fonttype = 0;
                Map<EffectType, Color> effectsmap = null;
                if (((s & TokenStyle.STYLE_ITALIC) == TokenStyle.STYLE_ITALIC)) {
                    fonttype |= Font.ITALIC;
                }
                if (((s & TokenStyle.STYLE_BOLD) == TokenStyle.STYLE_BOLD)) {
                    fonttype |= Font.BOLD;
                }
                attrs.setFontType(fonttype);
                if (((s & TokenStyle.STYLE_UNDERLINE) == TokenStyle.STYLE_UNDERLINE)) {
                    if (effectsmap == null) {
                        effectsmap = new EnumMap<>(EffectType.class);
                    }
                    effectsmap.put(EffectType.LINE_UNDERSCORE, JBColor.foreground());
                }
                if (((s & TokenStyle.STYLE_STRIKETHROUGH) == TokenStyle.STYLE_STRIKETHROUGH)) {
                    if (effectsmap == null) {
                        effectsmap = new EnumMap<>(EffectType.class);
                    }
                    effectsmap.put(EffectType.STRIKEOUT, JBColor.foreground());
                }
                if (effectsmap != null) {
                    try {
                        attrs.withAdditionalEffects(effectsmap);
                    } catch (LinkageError ignored) {
                        //calling unstable api
                    }
                }
            }
            return attrs;
        });
    }

    @Override
    public void setText(@NotNull CharSequence text) {
    }

    @Override
    public void setEditor(@NotNull HighlighterClient editor) {
        if (this.stateManager != null) {
            this.stateManager.removeModelListener(modelUpdateListener);
            project.unsubscribeScriptEditorStateManager(stateManager);
            //null out prematurely so in case other exceptions happen it stays null
            this.stateManager = null;
        }

        this.editor = editor;
        System.out.println(
                "BuildScriptEditorHighlighter.setEditor " + System.identityHashCode(this) + " to editor " + System
                        .identityHashCode(editor) + " with " + editor.getClass().getName());

        stateManager = project.subscribeScriptEditorStateManager(this.scriptExecutionPath, editor.getDocument());
        if (stateManager != null) {
            stateManager.addModelListener(modelUpdateListener);
            stateManager.setCurrentColorScheme(this.currentColorScheme);

            EditorFactory editorfactory = EditorFactory.getInstance();
            editorfactory.addEditorFactoryListener(new EditorFactoryListener() {
                @Override
                public void editorReleased(@NotNull EditorFactoryEvent event) {
                    Editor eventeditor = event.getEditor();
                    if (!(eventeditor instanceof HighlighterClient)) {
                        return;
                    }
                    if (ARFU_editor
                            .compareAndSet(BuildScriptEditorHighlighter.this, (HighlighterClient) eventeditor, null)) {
                        project.unsubscribeScriptEditorStateManager(stateManager);
                        editorfactory.removeEditorFactoryListener(this);
                    }
                }
            }, editor.getProject());
        }
    }

    @Override
    public void setColorScheme(@NotNull EditorColorsScheme scheme) {
        this.currentColorScheme = scheme;
        if (this.stateManager != null) {
            this.stateManager.setCurrentColorScheme(scheme);
        }
    }

    private static int colorSchemeToTokenTheme(@NotNull EditorColorsScheme scheme) {
        return ColorUtil.isDark(scheme.getDefaultBackground()) ? TokenStyle.THEME_DARK : TokenStyle.THEME_LIGHT;
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent e) {
    }

    @Override
    public void bulkUpdateStarting(@NotNull Document document) {
    }

    @Override
    public void bulkUpdateFinished(@NotNull Document document) {
    }

    private static TextRegionChange toTextRegionChange(DocumentEvent event) {
        return new TextRegionChange(event.getOffset(), event.getOldLength(), event.getNewFragment().toString());
    }

    private static final TextAttributes DEFAULT_TOKEN_STYLE = new TextAttributes();

    private class EmptyNoTokensHighlighterIterator implements HighlighterIterator {

        private boolean atEnd;
        private int offset;

        public EmptyNoTokensHighlighterIterator(int offset) {
            this.offset = offset;
        }

        @Override
        public TextAttributes getTextAttributes() {
            return DEFAULT_TOKEN_STYLE;
        }

        @Override
        public int getStart() {
            return offset;
        }

        @Override
        public int getEnd() {
            return offset;
        }

        @Override
        public void advance() {
            atEnd = true;
        }

        @Override
        public void retreat() {
            atEnd = false;
        }

        @Override
        public boolean atEnd() {
            return atEnd;
        }

        @Override
        public IElementType getTokenType() {
            return TOKEN_ELEMENT_TYPE;
        }

        @Override
        public Document getDocument() {
            HighlighterClient editor = BuildScriptEditorHighlighter.this.editor;
            if (editor == null) {
                return null;
            }
            return editor.getDocument();
        }
    }

    private static final Grouper[] EMPTY_GROUPER_ARRAY = new Grouper[0];
    private static final Sorter[] EMPTY_SORTER_ARRAY = new Sorter[0];
    private static final Filter[] EMPTY_FILTER_ARRAY = new Filter[0];
    private static final OutlineEntryElement[] EMPTY_OUTLINE_ENTRY_ELEMENT_ARRAY = new OutlineEntryElement[0];

    private class BuildScriptStructureViewModel implements StructureViewModel, StructureViewModel.ElementInfoProvider, ScriptEditorModel.ModelUpdateListener {
        private final Editor editor;
        private final List<FileEditorPositionListener> fileEditorPositionListeners = ContainerUtil
                .createLockFreeCopyOnWriteList();
        private final List<ModelListener> modelListeners = ContainerUtil.createLockFreeCopyOnWriteList();
        private final Disposable editorCaretListenerDisposable = Disposer.newDisposable();
        private final CaretListener editorCaretListener = new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent e) {
                if (e.getEditor().equals(editor)) {
                    for (FileEditorPositionListener listener : fileEditorPositionListeners) {
                        listener.onCurrentElementChanged();
                    }
                }
            }
        };

        private final OutlineRootElement rootNode;
        private final BuildScriptEditorStateManager editorState = BuildScriptEditorHighlighter.this.stateManager;

        public BuildScriptStructureViewModel(Editor editor) {
            this.editor = editor;
            this.rootNode = new OutlineRootElement(editor);

            this.editor.getCaretModel().addCaretListener(editorCaretListener, editorCaretListenerDisposable);
            if (editorState != null) {
                editorState.addModelListener(this);
            }

            ScriptSyntaxModel model = getModelMaybeOutOfDate();
            rootNode.setModel(model);
        }

        @Nullable
        @Override
        public Object getCurrentEditorElement() {
            int offset = editor.getCaretModel().getOffset();
            return rootNode.findNodeForOffset(offset);
        }

        @Override
        public void addEditorPositionListener(@NotNull FileEditorPositionListener listener) {
            fileEditorPositionListeners.add(listener);
        }

        @Override
        public void removeEditorPositionListener(@NotNull FileEditorPositionListener listener) {
            fileEditorPositionListeners.remove(listener);
        }

        @Override
        public void addModelListener(@NotNull ModelListener modelListener) {
            modelListeners.add(modelListener);
        }

        @Override
        public void removeModelListener(@NotNull ModelListener modelListener) {
            modelListeners.remove(modelListener);
        }

        @NotNull
        @Override
        public OutlineRootElement getRoot() {
            return rootNode;
        }

        @NotNull
        @Override
        public Grouper[] getGroupers() {
            return EMPTY_GROUPER_ARRAY;
        }

        @NotNull
        @Override
        public Sorter[] getSorters() {
            return EMPTY_SORTER_ARRAY;
        }

        @NotNull
        @Override
        public Filter[] getFilters() {
            return EMPTY_FILTER_ARRAY;
        }

        @Override
        public void dispose() {
            Disposer.dispose(editorCaretListenerDisposable);
            fileEditorPositionListeners.clear();
            modelListeners.clear();
            if (editorState != null) {
                editorState.removeModelListener(this);
            }
        }

        public void fireModelUpdate() {
            for (ModelListener listener : modelListeners) {
                listener.onModelChanged();
            }
        }

        @Override
        public boolean shouldEnterElement(Object element) {
            return false;
        }

        @Override
        public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
            return false;
        }

        @Override
        public boolean isAlwaysLeaf(StructureViewTreeElement element) {
            return false;
        }

        @Override
        public void modelUpdated(ScriptSyntaxModel model) {
            rootNode.modelUpdated(model);
            fireModelUpdate();
        }
    }

    private static OutlineEntryElement[] createOutlineEntries(List<? extends IntellijScriptOutlineEntry> outlineentries,
            Editor editor) {
        if (outlineentries == null) {
            return EMPTY_OUTLINE_ENTRY_ELEMENT_ARRAY;
        }
        List<OutlineEntryElement> entries = new ArrayList<>();
        for (IntellijScriptOutlineEntry entry : outlineentries) {
            if (entry == null) {
                continue;
            }
            entries.add(new OutlineEntryElement(entry, editor));
        }

        return entries.toArray(EMPTY_OUTLINE_ENTRY_ELEMENT_ARRAY);
    }

    private static class OutlineEntryElement implements StructureViewTreeElement {
        private final Editor editor;
        private final IntellijScriptOutlineEntry entry;
        private final OutlineEntryElement[] children;

        public OutlineEntryElement(IntellijScriptOutlineEntry entry, Editor editor) {
            this.entry = entry;
            this.editor = editor;
            this.children = createOutlineEntries(entry.getChildren(), editor);
        }

        @Override
        public Object getValue() {
            return entry;
        }

        @NotNull
        @Override
        public ItemPresentation getPresentation() {
            ItemPresentation entrypresentation = entry.getEntryPresentation();
            if (entrypresentation != null) {
                return entrypresentation;
            }
            return new PresentationData(entry.getLabel(), entry.getType(), null, null);
        }

        @NotNull
        @Override
        public TreeElement[] getChildren() {
            return children;
        }

        @Override
        public void navigate(boolean requestFocus) {
            StructureOutlineEntry entry = this.entry.getEntry();
            int offset = entry.getSelectionOffset();
            int selectionlen = entry.getSelectionLength();
            editor.getSelectionModel().setSelection(offset, offset + selectionlen);
            editor.getCaretModel().moveToOffset(offset + selectionlen);
            editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

            if (requestFocus) {
                editor.getContentComponent().requestFocus();
            }
        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return true;
        }

        public boolean isInside(int offset) {
            StructureOutlineEntry entry = this.entry.getEntry();
            int entryoffset = entry.getOffset();
            return offset >= entryoffset && offset < entryoffset + entry.getLength();
        }

        public Object findNodeForOffset(int offset) {
            if (!isInside(offset)) {
                return null;
            }
            for (OutlineEntryElement node : children) {
                Object child = node.findNodeForOffset(offset);
                if (child != null) {
                    return child;
                }
            }
            return this;
        }
    }

    private class OutlineRootElement implements StructureViewTreeElement {
        private OutlineEntryElement[] children = EMPTY_OUTLINE_ENTRY_ELEMENT_ARRAY;
        private Editor editor;

        public OutlineRootElement(Editor editor) {
            this.editor = editor;
        }

        public void setModel(ScriptSyntaxModel model) {
            setOutline(model == null ? null : model.getStructureOutline());
        }

        public void setOutline(ScriptStructureOutline outline) {
            if (outline == null) {
                this.children = EMPTY_OUTLINE_ENTRY_ELEMENT_ARRAY;
            } else {
                IntellijScriptOutlineRoot outlineroot = IntellijScriptOutlineRoot.create(outline);
                IScriptOutlineDesigner designer = project
                        .getScriptOutlineDesignerForSchemaIdentifier(outlineroot.getSchemaIdentifier());
                if (designer != null) {
                    designer.process(outlineroot);
                }
                this.children = createOutlineEntries(outlineroot.getRootEntries(), editor);
            }
        }

        @Override
        public Object getValue() {
            return "root";
        }

        @NotNull
        @Override
        public ItemPresentation getPresentation() {
            //is not shown
            return new PresentationData("root", null, null, null);
        }

        @NotNull
        @Override
        public TreeElement[] getChildren() {
            return children;
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

        public void modelUpdated(ScriptSyntaxModel model) {
            setModel(model);
        }

        public Object findNodeForOffset(int offset) {
            for (OutlineEntryElement node : children) {
                Object child = node.findNodeForOffset(offset);
                if (child != null) {
                    return child;
                }
            }
            return null;
        }
    }

}
