package saker.build.ide.intellij;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import org.jetbrains.annotations.Nullable;

public class BuildScriptEditorCharFilter extends CharFilter {
    @Nullable
    @Override
    public Result acceptChar(char c, int prefixLength, Lookup lookup) {
        switch (c){
            case '[':
            case ']':
            case '<':
            case '>':
            case '(':
            case ')':
            case '=':
            case ':':
            case '?':
            case ';':
            case '{':
            case '}':
                return Result.HIDE_LOOKUP;
        }
        return Result.ADD_TO_PREFIX;
    }
}
