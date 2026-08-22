package dev.verloren.midnight.formatter;

import com.intellij.formatting.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the code formatting model for Compact files (reformat code action {@code Ctrl+Alt+L}).
 *
 * <p>Constructs a tree of {@link CompactBlock} instances and defines the canonical {@link SpacingBuilder}
 * enforcing spacing rules around operators, keywords, punctuation, and delimiters.</p>
 */
public class CompactFormattingModelBuilder implements FormattingModelBuilder {

  @Override
  public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
    CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
    SpacingBuilder spacingBuilder = createSpacingBuilder(settings);

    CompactBlock rootBlock = new CompactBlock(
        formattingContext.getPsiElement().getNode(),
        Indent.getNoneIndent(),
        null,
        null,
        spacingBuilder
    );

    return FormattingModelProvider.createFormattingModelForPsiFile(
        formattingContext.getContainingFile(),
        rootBlock,
        settings
    );
  }

  public static @NotNull SpacingBuilder createSpacingBuilder(@NotNull CodeStyleSettings settings) {
    return new SpacingBuilder(settings, CompactLanguage.INSTANCE)
        // Assignment operators: space around
        .around(CompactTokenSets.ASSIGNMENT_OPERATORS).spaces(1)

        // Binary operators: space around
        .around(CompactTokenTypes.PLUS).spaces(1)
        .around(CompactTokenTypes.MINUS).spaces(1)
        .around(CompactTokenTypes.STAR).spaces(1)
        .around(CompactTokenTypes.SLASH).spaces(1)
        .around(CompactTokenTypes.PERCENT).spaces(1)
        .around(CompactTokenTypes.EQEQ).spaces(1)
        .around(CompactTokenTypes.NEQ).spaces(1)
        .around(CompactTokenTypes.LTE).spaces(1)
        .around(CompactTokenTypes.GTE).spaces(1)
        .around(CompactTokenTypes.ANDAND).spaces(1)
        .around(CompactTokenTypes.OROR).spaces(1)
        .around(CompactTokenTypes.ARROW).spaces(1)
        .around(CompactTokenTypes.QUESTION).spaces(1)
        .around(CompactTokenTypes.AS).spaces(1)

        // Commas and semicolons
        .before(CompactTokenTypes.COMMA).spaces(0)
        .after(CompactTokenTypes.COMMA).spaces(1)
        .before(CompactTokenTypes.SEMICOLON).spaces(0)

        // Colons
        .before(CompactTokenTypes.COLON).spaces(0)
        .after(CompactTokenTypes.COLON).spaces(1)

        // Dots and Range
        .around(CompactTokenTypes.DOT).spaces(0)
        .around(CompactTokenTypes.RANGE).spaces(0)
        .after(CompactTokenTypes.SPREAD).spaces(0)

        // Parentheses and Brackets
        .after(CompactTokenTypes.LPAREN).spaces(0)
        .before(CompactTokenTypes.RPAREN).spaces(0)
        .after(CompactTokenTypes.LBRACKET).spaces(0)
        .before(CompactTokenTypes.RBRACKET).spaces(0)

        // Control flow keywords before '('
        .after(CompactTokenTypes.IF).spaces(1)
        .after(CompactTokenTypes.FOR).spaces(1)

        // Declaration & contextual keywords
        .after(CompactTokenTypes.PRAGMA).spaces(1)
        .after(CompactTokenTypes.IMPORT).spaces(1)
        .after(CompactTokenTypes.EXPORT).spaces(1)
        .after(CompactTokenTypes.INCLUDE).spaces(1)
        .after(CompactTokenTypes.MODULE).spaces(1)
        .after(CompactTokenTypes.STRUCT).spaces(1)
        .after(CompactTokenTypes.ENUM).spaces(1)
        .after(CompactTokenTypes.CONTRACT).spaces(1)
        .after(CompactTokenTypes.CIRCUIT).spaces(1)
        .after(CompactTokenTypes.WITNESS).spaces(1)
        .after(CompactTokenTypes.LEDGER).spaces(1)
        .after(CompactTokenTypes.CONST).spaces(1)
        .after(CompactTokenTypes.TYPE).spaces(1)
        .after(CompactTokenTypes.NEW).spaces(1)
        .after(CompactTokenTypes.SEALED).spaces(1)
        .after(CompactTokenTypes.PURE).spaces(1)
        .after(CompactTokenTypes.RETURN).spaces(1)
        .after(CompactTokenTypes.FROM).spaces(1)
        .before(CompactTokenTypes.FROM).spaces(1)
        .after(CompactTokenTypes.OF).spaces(1)
        .before(CompactTokenTypes.OF).spaces(1)
        .after(CompactTokenTypes.PREFIX).spaces(1)
        .before(CompactTokenTypes.PREFIX).spaces(1)
        .after(CompactTokenTypes.IMPLEMENTS).spaces(1)
        .before(CompactTokenTypes.IMPLEMENTS).spaces(1)
        .after(CompactTokenTypes.ELSE).spaces(1)
        .before(CompactTokenTypes.ELSE).spaces(1)

        // Braces
        .before(CompactTokenTypes.LBRACE).spaces(1);
  }
}
