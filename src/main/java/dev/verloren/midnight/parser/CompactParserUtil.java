package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CompactParserUtil {
  public static final int PREC_LOGICAL_OR = 0;
  public static final int PREC_LOGICAL_AND = 1;
  public static final int PREC_EQUALITY = 2;
  public static final int PREC_RELATIONAL = 3;
  public static final int PREC_CAST = 4;
  public static final int PREC_ADDITIVE = 5;
  public static final int PREC_MULTIPLICATIVE = 6;

  public static boolean at(@NotNull PsiBuilder builder, @NotNull IElementType tokenType) {
    return builder.getTokenType() == tokenType;
  }

  public static boolean expect(@NotNull PsiBuilder builder, @NotNull IElementType tokenType, @NotNull String message) {
    if (!at(builder, tokenType)) {
      builder.error(message);
      return false;
    }

    builder.advanceLexer();
    return true;
  }

  public static void errorAndAdvance(@NotNull PsiBuilder builder, @NotNull String message) {
    builder.error(message);
    if (!builder.eof()) {
      builder.advanceLexer();
    }
  }

  public static void sync(@NotNull PsiBuilder builder, @NotNull TokenSet recoverySet) {
    while (!builder.eof() && !recoverySet.contains(builder.getTokenType())) {
      builder.advanceLexer();
    }
  }

  public static int binaryPrecedence(@Nullable IElementType tokenType) {
    if (tokenType == CompactTokenTypes.OROR) {
      return PREC_LOGICAL_OR;
    }
    if (tokenType == CompactTokenTypes.ANDAND) {
      return PREC_LOGICAL_AND;
    }
    if (tokenType == CompactTokenTypes.EQEQ || tokenType == CompactTokenTypes.NEQ) {
      return PREC_EQUALITY;
    }
    if (tokenType == CompactTokenTypes.LT
            || tokenType == CompactTokenTypes.LTE
            || tokenType == CompactTokenTypes.GT
            || tokenType == CompactTokenTypes.GTE) {
      return PREC_RELATIONAL;
    }
    if (tokenType == CompactTokenTypes.AS) {
      return PREC_CAST;
    }
    if (tokenType == CompactTokenTypes.PLUS || tokenType == CompactTokenTypes.MINUS) {
      return PREC_ADDITIVE;
    }
    if (tokenType == CompactTokenTypes.STAR || tokenType == CompactTokenTypes.SLASH) {
      return PREC_MULTIPLICATIVE;
    }

    return -1;
  }

  private CompactParserUtil() {
  }
}