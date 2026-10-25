package dev.verloren.midnight.formatter;

import com.intellij.formatting.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the code formatting model for Compact smart contract files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.lang.formatter} extension point
 * for {@link dev.verloren.midnight.CompactLanguage}. Powers the IDE's automated code reformatting
 * (Reformat Code action {@code Ctrl+Alt+L} / {@code Cmd+Option+L}), rearrange code, and format-on-save:
 * <ul>
 *   <li><b>AST Block Hierarchy:</b> Constructs a hierarchy of {@link CompactBlock} elements mirroring
 *       the Compact AST, computing indentation and line wrapping.</li>
 *   <li><b>Spacing Builder:</b> Assembles declarative {@link SpacingBuilder} rules specifying whitespace
 *       around operators, delimiters, keywords, parentheses, and braces via {@link #createSpacingBuilder}.</li>
 *   <li><b>2-Space Canonical Indent:</b> Integrates with {@link CompactLanguageCodeStyleSettingsProvider}
 *       to apply the standard 2-space indentation style canonical to Compact contracts.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by {@code com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl} when reformatting is triggered.
 * IntelliJ passes a {@link FormattingContext} containing the target PSI element, document range, and active
 * {@link CodeStyleSettings}, receiving back a {@link FormattingModel} that calculates document text edits.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Called on pooled background worker threads managed by the formatting subsystem,
 *       or inside an EDT write command action when applying document modifications.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while constructing the
 *       block tree and inspecting AST nodes.</li>
 * </ul>
 *
 * <h3>Formatting Model Pipeline</h3>
 * <pre>
 *   [User presses Ctrl+Alt+L / "Reformat Code"]
 *                         │
 *                         ▼
 *        [CompactFormattingModelBuilder]
 *                         │
 *        createModel(FormattingContext)
 *                         │
 *          ┌──────────────┴──────────────┐
 *          ▼                             ▼
 *   [createSpacingBuilder]        [Root CompactBlock]
 *   Spacing rules for tokens      Tree of AST formatting blocks
 *          │                             │
 *          └──────────────┬──────────────┘
 *                         ▼
 *            [FormattingModelProvider]
 *                         │
 *              Reformatted Document
 * </pre>
 *
 * @see dev.verloren.midnight.formatter.CompactBlock
 * @see dev.verloren.midnight.formatter.CompactLanguageCodeStyleSettingsProvider
 * @see com.intellij.formatting.FormattingModelBuilder
 * @see com.intellij.formatting.SpacingBuilder
 */
public class CompactFormattingModelBuilder implements FormattingModelBuilder {

  /**
   * Constructs the formatting model for the specified formatting context.
   *
   * @param formattingContext context containing the target PSI element, file, and code style settings
   * @return a fully configured {@link FormattingModel} for the Compact file
   */
  @Override
  public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
    // Step 1: Retrieve project code style settings and construct declarative spacing builder
    CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
    SpacingBuilder spacingBuilder = createSpacingBuilder(settings);

    // Step 2: Instantiate root formatting block starting at file root with none-indent
    CompactBlock rootBlock = new CompactBlock(
        formattingContext.getPsiElement().getNode(),
        Indent.getNoneIndent(),
        null,
        null,
        spacingBuilder
    );

    // Step 3: Wrap root block in platform formatting model bound to the target PSI file
    return FormattingModelProvider.createFormattingModelForPsiFile(
        formattingContext.getContainingFile(),
        rootBlock,
        settings
    );
  }

  /**
   * Creates the canonical spacing builder defining whitespace rules across Compact tokens.
   *
   * @param settings the active code style settings
   * @return configured {@link SpacingBuilder} with rules for operators, delimiters, keywords, and braces
   */
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
