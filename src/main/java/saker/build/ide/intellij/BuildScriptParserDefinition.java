package saker.build.ide.intellij;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.DummyLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

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
        return new SimpleFile(viewProvider);
    }

    private static class SimpleFile extends PsiFileBase {
        public SimpleFile(@NotNull FileViewProvider viewProvider) {
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
}
