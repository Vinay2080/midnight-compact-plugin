package dev.verloren.midnight.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

import static dev.verloren.midnight.parser.CompactParserUtil.at;

/**
 * Handwritten recursive-descent parser for the Compact smart contract language.
 *
 * <p>Implements IntelliJ's {@link PsiParser} interface, coordinating parsing across
 * dedicated delegates for declarations, statements, expressions, and type patterns.</p>
 */
public final class CompactParser implements PsiParser {
  private static final TokenSet TOP_LEVEL_RECOVERY = TokenSet.create(
          CompactTokenTypes.SEMICOLON,
          CompactTokenTypes.PRAGMA,
          CompactTokenTypes.IMPORT,
          CompactTokenTypes.EXPORT,
          CompactTokenTypes.INCLUDE,
          CompactTokenTypes.MODULE,
          CompactTokenTypes.CONTRACT,
          CompactTokenTypes.CIRCUIT,
          CompactTokenTypes.STRUCT,
          CompactTokenTypes.ENUM,
          CompactTokenTypes.TYPE,
          CompactTokenTypes.LEDGER,
          CompactTokenTypes.WITNESS,
          CompactTokenTypes.CONSTRUCTOR,
          CompactTokenTypes.SEALED,
          CompactTokenTypes.PURE,
          CompactTokenTypes.NEW
  );

  private final CompactParserContext context;

  public CompactParser() {
    this(new CompactParserContext());
  }

  CompactParser(@NotNull CompactParserContext context) {
    this.context = context;
  }

  @Override
  public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker file = builder.mark();

    while (!builder.eof()) {
      int startOffset = builder.getCurrentOffset();
      if (!context.declaration.parseProgramElement(builder)) {
        builder.error("Expected Compact declaration");
        if (!builder.eof() && !TOP_LEVEL_RECOVERY.contains(builder.getTokenType())) {
          CompactParserUtil.sync(builder, TOP_LEVEL_RECOVERY);
        }
        if (!builder.eof() && at(builder, CompactTokenTypes.SEMICOLON)) {
          builder.advanceLexer();
        } else if (!builder.eof() && !isProgramElementStart(builder)) {
          builder.advanceLexer();
        }
      }
      if (!builder.eof() && builder.getCurrentOffset() == startOffset) {
        builder.advanceLexer();
      }
    }

    file.done(root);
    return builder.getTreeBuilt();
  }

  private static boolean isProgramElementStart(@NotNull PsiBuilder builder) {
    IElementType token = builder.getTokenType();
    return token == CompactTokenTypes.PRAGMA
            || token == CompactTokenTypes.INCLUDE
            || token == CompactTokenTypes.IMPORT
            || token == CompactTokenTypes.EXPORT
            || token == CompactTokenTypes.MODULE
            || token == CompactTokenTypes.CONTRACT
            || token == CompactTokenTypes.CIRCUIT
            || token == CompactTokenTypes.STRUCT
            || token == CompactTokenTypes.ENUM
            || token == CompactTokenTypes.TYPE
            || token == CompactTokenTypes.LEDGER
            || token == CompactTokenTypes.WITNESS
            || token == CompactTokenTypes.CONSTRUCTOR
            || token == CompactTokenTypes.SEALED
            || token == CompactTokenTypes.PURE
            || token == CompactTokenTypes.NEW;
  }
}
