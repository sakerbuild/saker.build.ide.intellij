package saker.build.ide.intellij;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.DummyLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiElementBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuildScriptParserDefinition implements ParserDefinition {
    private static final IFileElementType FILE_NODE_TYPE = new IFileElementType(BuildScriptLanguage.INSTANCE);
    private static final SimpleTokenType TOKEN_TYPE = new SimpleTokenType();

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new DummyLexer(TOKEN_TYPE);
    }

    @Override
    public PsiParser createParser(Project project) {
        return new PsiParser() {
            @NotNull
            @Override
            public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
                PsiBuilder.Marker mark = builder.mark();
                while (!builder.eof()) {
                    builder.advanceLexer();
                }
                mark.done(root);
                return builder.getTreeBuilt();
            }
        };
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE_NODE_TYPE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new BuildScriptPsiFile(viewProvider);
    }

    public static class BuildScriptPsiFile extends PsiFileBase {
        public BuildScriptPsiFile(@NotNull FileViewProvider viewProvider) {
            super(viewProvider, BuildScriptLanguage.INSTANCE);
        }

        @NotNull
        @Override
        public FileType getFileType() {
            return BuildScriptFileType.INSTANCE;
        }

        @Override
        public String toString() {
            return "Saker.build script file";
        }

        @Override
        public PsiElement findElementAt(int elemoffset) {
            PsiElement superfound = super.findElementAt(elemoffset);
            if (superfound == null) {
                return null;
            }
            return new OffsetHoldingASTDelegatePsiElement(elemoffset, superfound);
        }

    }

    private static class SimpleTokenType extends IElementType {
        public SimpleTokenType() {
            super("SAKER_BUILD_TOKEN_TYPE", BuildScriptLanguage.INSTANCE);
        }

        @Override
        public String toString() {
            return "SimpleTokenType." + super.toString();
        }
    }

    private static class OffsetHoldingASTDelegatePsiElement extends PsiElementBase implements InvocationOffsetHolder {
        private final int elemoffset;
        private final PsiElement superfound;

        public OffsetHoldingASTDelegatePsiElement(int elemoffset, PsiElement superfound) {
            this.elemoffset = elemoffset;
            this.superfound = superfound;
        }

        @NotNull
        @Override
        public ASTNode getNode() {
            return superfound.getNode();
        }

        @NotNull
        @Override
        public Language getLanguage() {
            return BuildScriptLanguage.INSTANCE;
        }

        @NotNull
        @Override
        public PsiElement[] getChildren() {
            return superfound.getChildren();
        }

        @Override
        public PsiElement getParent() {
            return superfound.getParent();
        }

        @Override
        public TextRange getTextRange() {
            return superfound.getTextRange();
        }

        @Override
        public int getStartOffsetInParent() {
            return superfound.getStartOffsetInParent();
        }

        @Override
        public int getTextLength() {
            return superfound.getTextLength();
        }

        @Nullable
        @Override
        public PsiElement findElementAt(int offset) {
            return superfound.findElementAt(offset);
        }

        @Override
        public int getTextOffset() {
            return superfound.getTextOffset();
        }

        @Override
        public String getText() {
            return superfound.getText();
        }

        @NotNull
        @Override
        public char[] textToCharArray() {
            return superfound.textToCharArray();
        }

        @Override
        public int getInvocationOffset() {
            return elemoffset;
        }

        @Override
        public PsiManager getManager() {
            return superfound.getManager();
        }

        @NotNull
        @Override
        public Project getProject() {
            return superfound.getProject();
        }

        @Override
        public boolean isEquivalentTo(PsiElement another) {
            return super.isEquivalentTo(another);
        }

        @Override
        public PsiElement getPrevSibling() {
            return null;
        }

        @Override
        public PsiElement getNextSibling() {
            return null;
        }
    }
}
