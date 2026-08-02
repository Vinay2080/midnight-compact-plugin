package dev.verloren.midnight.lexer;

import com.intellij.lexer.FlexAdapter;

public class CompactLexerAdapter extends FlexAdapter {
    public CompactLexerAdapter() {
        super(new CompactLexer((java.io.Reader) null));
    }
}
