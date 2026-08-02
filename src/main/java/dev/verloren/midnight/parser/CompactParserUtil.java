package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import dev.verloren.midnight.lexer.CompactTokenTypes;

public final class CompactParserUtil extends GeneratedParserUtilBase {

    private CompactParserUtil() {
    }

    public static boolean pragmaIdentifier(PsiBuilder builder, int level) {
        if (builder.getTokenType() != CompactTokenTypes.IDENTIFIER) {
            builder.error("Identifier expected after 'pragma'");
            return false;
        }
        return consumeToken(builder, CompactTokenTypes.IDENTIFIER);
    }
}