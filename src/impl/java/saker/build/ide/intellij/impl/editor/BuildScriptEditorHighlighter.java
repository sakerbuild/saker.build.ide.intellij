package saker.build.ide.intellij.impl.editor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.JBColor;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.jetbrains.annotations.NotNull;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.BuildScriptLanguage;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.SakerIDEPlugin;
import saker.build.ide.support.SakerIDEProject;
import saker.build.runtime.environment.SakerEnvironmentImpl;
import saker.build.scripting.model.ScriptModellingEnvironment;
import saker.build.scripting.model.ScriptSyntaxModel;
import saker.build.scripting.model.ScriptToken;
import saker.build.scripting.model.TextRegionChange;
import saker.build.scripting.model.TokenStyle;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.ByteSource;
import saker.build.thirdparty.saker.util.io.function.IOSupplier;

import java.awt.Color;
import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class BuildScriptEditorHighlighter implements EditorHighlighter {
    private static final AtomicReferenceFieldUpdater<BuildScriptEditorHighlighter, ModelReference> ARFU_model = AtomicReferenceFieldUpdater
            .newUpdater(BuildScriptEditorHighlighter.class, ModelReference.class, "model");

    private static final IElementType TOKEN_ELEMENT_TYPE = new IElementType("SAKER_SCRIPT_TOKEN",
            BuildScriptLanguage.INSTANCE);

    private final IntellijSakerIDEProject project;
    private final SakerPath scriptFileExecutionPath;

    private final Object inputAccessLock = new Object();
    private StringBuilder input;
    private List<TextRegionChange> regionChanges = new ArrayList<>();

    private EditorColorsScheme colors;
    private HighlighterClient editor;

    private volatile ModelReference model;

    private volatile boolean disposed = false;

    private boolean bulkChange;

    private ScriptingResourcesChangeListener changeListener;

    private ConcurrentHashMap<TokenStyle, TextAttributes> tokenStyleAttributes = new ConcurrentHashMap<>();

    public BuildScriptEditorHighlighter(IntellijSakerIDEProject project, SakerPath scriptpath,
            EditorColorsScheme colors) {
        this.project = project;
        this.scriptFileExecutionPath = scriptpath;
        this.colors = colors;
    }

    public ScriptSyntaxModel getModel() {
        ModelReference modelref = this.model;
        if (modelref == null) {
            return null;
        }
        return modelref.model;
    }

    @NotNull
    @Override
    public HighlighterIterator createIterator(int startOffset) {
        ModelReference modelref = this.model;
        if (modelref != null) {
            try {
                ScriptSyntaxModel model = modelref.model;
                synchronized (modelref) {
                    synchronized (inputAccessLock) {
                        modelref.text = input.toString();
                        if (!regionChanges.isEmpty()) {
                            modelref.regionChanges.addAll(regionChanges);
                            regionChanges.clear();
                            modelref.updateCalled = false;
                        }
                    }
                    if (!modelref.updateCalled) {
                        modelref.updateCalled = true;
                        IOSupplier<ByteSource> inputsupplier = () -> ByteSource
                                .valueOf(new CharSequenceInputStream(modelref.text, StandardCharsets.UTF_8));
                        if (modelref.regionChanges.isEmpty()) {
                            //create the model directly for the first time
                            model.createModel(inputsupplier);
                        } else {
                            model.updateModel(modelref.regionChanges, inputsupplier);
                        }
                        modelref.regionChanges.clear();
                    }
                }
                Map<String, Set<? extends TokenStyle>> styles = model.getTokenStyles();

                Iterable<? extends ScriptToken> tokens = model.getTokens(startOffset, Integer.MAX_VALUE);
                ArrayList<? extends ScriptToken> tokenslist = ObjectUtils.newArrayList(tokens);
                if (tokenslist.isEmpty()) {
                    //no tokens
                    return emptyIterator;
                }
                int startidx = 0;
                while (startidx < tokenslist.size()) {
                    ScriptToken token = tokenslist.get(startidx);
                    if (token.getEndOffset() >= startOffset) {
                        break;
                    }
                    startidx++;
                }

                List<? extends ScriptToken> uselist =
                        startidx == 0 ? tokenslist : tokenslist.subList(startidx, tokenslist.size());

                if (uselist.isEmpty()) {
                    return emptyIterator;
                }

                return new HighlighterIterator() {
                    private ListIterator<? extends ScriptToken> iter = uselist.listIterator();
                    private ScriptToken token = iter.next();

                    @Override
                    public TextAttributes getTextAttributes() {
                        String type = token.getType();
                        Set<? extends TokenStyle> tokenstyles = styles.get(type);
                        if (tokenstyles == null) {
                            return DEFAULT_TOKEN_STYLE;
                        }
                        //TODO take theme into account
                        int currenttheme = TokenStyle.THEME_LIGHT;
                        TokenStyle style = findAppropriateStyleForTheme(tokenstyles, currenttheme);
                        return getTokenStyleAttributes(style);
                    }

                    @Override
                    public int getStart() {
                        return Math.max(startOffset, token.getOffset());
                    }

                    @Override
                    public int getEnd() {
                        return token.getEndOffset();
                    }

                    @Override
                    public void advance() {
                        if (!iter.hasNext()) {
                            token = null;
                        } else {
                            token = iter.next();
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
                        return editor.getDocument();
                    }

                    @Override
                    public IElementType getTokenType() {
                        return TOKEN_ELEMENT_TYPE;
                    }

                };
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        TextAttributes textattrs = new TextAttributes();
        int myTextLength = input.length();
        return new EmptyFullTextHighlighterIterator(startOffset, textattrs, myTextLength);
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

    private static TokenStyle findAppropriateStyleForTheme(Set<? extends TokenStyle> styles, int theme) {
        if (styles == null) {
            return null;
        }
        Iterator<? extends TokenStyle> it = styles.iterator();
        if (!it.hasNext()) {
            return null;
        }

        TokenStyle first = it.next();
        TokenStyle notheme = null;
        if (((first.getStyle() & theme) == theme)) {
            return first;
        }
        if (((first.getStyle() & TokenStyle.THEME_MASK) == 0)) {
            notheme = first;
        }
        while (it.hasNext()) {
            TokenStyle ts = it.next();
            if (((ts.getStyle() & theme) == theme)) {
                return ts;
            }
            if (((ts.getStyle() & TokenStyle.THEME_MASK) == 0)) {
                notheme = ts;
            }
        }
        return notheme == null ? first : notheme;
    }

    @Override
    public void setText(@NotNull CharSequence text) {
        synchronized (inputAccessLock) {
            if (this.input != null) {
                //already set. should be updated via document listener
                return;
            }
            this.input = new StringBuilder(text);
        }
        initModel();
    }

    @Override
    public void setEditor(@NotNull HighlighterClient editor) {
        this.editor = editor;

        EditorFactory editorfactory = EditorFactory.getInstance();
        editorfactory.addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorReleased(@NotNull EditorFactoryEvent event) {
                if (event.getEditor() == editor) {
                    //TODO dispose the model
                    disposed = true;
                    disposeModel();
                    System.out.println("BuildScriptEditorHighlighter.editorReleased DISPOSE HIGHLIGHTER");
                    project.disposeHighlighter(scriptFileExecutionPath, BuildScriptEditorHighlighter.this);
                    editorfactory.removeEditorFactoryListener(this);
                }
            }
        }, editor.getProject());
    }

    @Override
    public void setColorScheme(@NotNull EditorColorsScheme scheme) {
        System.out.println("BuildScriptEditorHighlighter.setColorScheme " + scheme);
        this.colors = scheme;
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent e) {
        synchronized (inputAccessLock) {
            TextRegionChange regionchange = toTextRegionChange(e);
            regionChanges.add(regionchange);

            applyDocumentEvent(input, e);
        }
    }

    @Override
    public void bulkUpdateStarting(@NotNull Document document) {
        bulkChange = true;
    }

    @Override
    public void bulkUpdateFinished(@NotNull Document document) {
        bulkChange = false;
    }

    private static TextRegionChange toTextRegionChange(DocumentEvent event) {
        return new TextRegionChange(event.getOffset(), event.getOldLength(), event.getNewFragment().toString());
    }

    private static void applyDocumentEvent(StringBuilder sb, DocumentEvent event) {
        int offset = event.getOffset();
        sb.replace(offset, offset + event.getOldLength(), event.getNewFragment().toString());
    }

    private static void applyDocumentEvent(StringBuilder sb, TextRegionChange change) {
        int offset = change.getOffset();
        sb.replace(offset, offset + change.getLength(), ObjectUtils.nullDefault(change.getText(), ""));
    }

    private void initModel() {
        if (disposed) {
            return;
        }
        if (model != null) {
            //model already inited
            return;
        }
        synchronized (this) {
            ScriptModellingEnvironment scriptingenv = project.getScriptingEnvironment();
            uninstallListenerLocked();

            installListenerLocked();

            if (scriptingenv != null) {
                disposeModelLocked();
                model = new ModelReference(scriptingenv.getModel(scriptFileExecutionPath));
            }
        }
    }

    private void disposeModel() {
        synchronized (this) {
            uninstallListenerLocked();
            disposeModelLocked();
        }
    }

    private void disposeModelLocked() {
        ModelReference modelref = ARFU_model.getAndSet(this, null);
        if (modelref != null) {
            modelref.invalidateModel();
        }
    }

    private void installListenerLocked() {
        changeListener = new ScriptingResourcesChangeListener();
        project.addProjectResourceListener(changeListener);
        project.getPlugin().addPluginResourceListener(changeListener);
    }

    private void uninstallListenerLocked() {
        if (changeListener != null) {
            project.removeProjectResourceListener(changeListener);
            project.getPlugin().removePluginResourceListener(changeListener);
            changeListener = null;
        }
    }

    private class ScriptingResourcesChangeListener implements SakerIDEProject.ProjectResourceListener, SakerIDEPlugin.PluginResourceListener {
        @Override
        public void environmentClosing(SakerEnvironmentImpl environment) {
            disposeModel();
        }

        @Override
        public void environmentCreated(SakerEnvironmentImpl environment) {
            initModel();
        }

        @Override
        public void scriptModellingEnvironmentClosing(ScriptModellingEnvironment env) {
            disposeModel();
        }

        @Override
        public void scriptModellingEnvironmentCreated(ScriptModellingEnvironment env) {
            initModel();
        }
    }

    private final EmptyNoTokensHighlighterIterator emptyIterator = new EmptyNoTokensHighlighterIterator();
    private static final TextAttributes DEFAULT_TOKEN_STYLE = new TextAttributes();

    private class EmptyNoTokensHighlighterIterator implements HighlighterIterator {

        @Override
        public TextAttributes getTextAttributes() {
            return DEFAULT_TOKEN_STYLE;
        }

        @Override
        public int getStart() {
            return 0;
        }

        @Override
        public int getEnd() {
            return 0;
        }

        @Override
        public void advance() {
        }

        @Override
        public void retreat() {
        }

        @Override
        public boolean atEnd() {
            return true;
        }

        @Override
        public IElementType getTokenType() {
            return TOKEN_ELEMENT_TYPE;
        }

        @Override
        public Document getDocument() {
            return editor.getDocument();
        }
    }

    private class EmptyFullTextHighlighterIterator implements HighlighterIterator {
        private final int startOffset;
        private final TextAttributes textattrs;
        private final int myTextLength;
        private int index;

        public EmptyFullTextHighlighterIterator(int startOffset, TextAttributes textattrs, int myTextLength) {
            this.startOffset = startOffset;
            this.textattrs = textattrs;
            this.myTextLength = myTextLength;
            this.index = startOffset;
        }

        @Override
        public TextAttributes getTextAttributes() {
            return textattrs;
        }

        @Override
        public int getStart() {
            return startOffset;
        }

        @Override
        public int getEnd() {
            return myTextLength;
        }

        @Override
        public void advance() {
            index = myTextLength;
        }

        @Override
        public void retreat() {
            index = startOffset;
        }

        @Override
        public boolean atEnd() {
            return index >= myTextLength;
        }

        @Override
        public IElementType getTokenType() {
            return TOKEN_ELEMENT_TYPE;
        }

        @Override
        public Document getDocument() {
            return editor.getDocument();
        }
    }

    private static class ModelState {
        private String text;
        private List<TextRegionChange> events;

        public ModelState(String text, List<TextRegionChange> events) {
            this.text = text;
            this.events = events;
        }
    }

    private static class ModelReference {
        protected final ScriptSyntaxModel model;
        protected String text;
        protected List<TextRegionChange> regionChanges = new ArrayList<>();

        protected boolean updateCalled = false;

        public ModelReference(ScriptSyntaxModel model) {
            this.model = model;
        }

        public synchronized void invalidateModel() {
            model.invalidateModel();
            regionChanges.clear();
            text = null;
        }
    }
}
