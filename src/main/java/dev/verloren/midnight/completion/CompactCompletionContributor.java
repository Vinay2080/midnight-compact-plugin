package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.completion.providers.CompactKeywordCompletionProvider;
import dev.verloren.midnight.completion.providers.CompactMemberAccessCompletionProvider;
import dev.verloren.midnight.completion.providers.CompactStructLiteralCompletionProvider;
import dev.verloren.midnight.completion.providers.CompactTypeCompletionProvider;
import dev.verloren.midnight.completion.providers.CompactValueCompletionProvider;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Main code completion extension contributor for the Compact language.
 *
 * <p>Serves as the central entry point for IntelliJ Platform's basic completion subsystem
 * ({@link CompletionType#BASIC}). Instead of accumulating monolithic completion logic in a
 * single file, this contributor classifies the grammatical context at the caret via
 * {@link CompactCompletionContext#classify(PsiElement)} and dispatches candidate generation
 * to dedicated, single-responsibility completion providers in {@code dev.verloren.midnight.completion.providers}.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Extension Point:</b> Registered as {@code <completion.contributor>} in {@code plugin.xml}
 *       for {@link CompactLanguage#INSTANCE}.</li>
 *   <li><b>Calling Pipeline:</b> Invoked asynchronously by {@code com.intellij.codeInsight.completion.CodeCompletionHandlerBase}
 *       when the user presses {@code Ctrl+Space} or when typing triggers auto-popup.</li>
 *   <li><b>Threading Model:</b> Runs within an active <b>Read Action</b> ({@code @RequiresReadLock})
 *       on background pooled worker threads (IntelliJ Platform 2024+ asynchronous completion architecture).
 *       UI modifications or direct document writes are strictly forbidden here; mutations are deferred to
 *       {@link com.intellij.codeInsight.completion.InsertHandler} callbacks upon item selection.</li>
 *   <li><b>Cancellation Support:</b> Operates under daemon cancellation policies; if the developer types a new character,
 *       IntelliJ interrupts execution via {@link com.intellij.openapi.progress.ProgressManager#checkCanceled()}.</li>
 * </ul>
 *
 * <h3>Subsystem Delegation Architecture</h3>
 * <pre>{@code
 *   CodeCompletionHandlerBase (Background Thread, Read Action)
 *              │
 *              ▼
 *   CompactCompletionContributor
 *              │
 *              ▼
 *   CompactCompletionContext.classify(position)
 *              │
 *      ┌───────┼───────┬───────────┬──────────────┬──────────────┐
 *      ▼       ▼       ▼           ▼              ▼              ▼
 *   Keyword  Type    Value    MemberAccess   StructLiteral     None
 *   Provider Provider Provider  Provider       Provider     (Suppress)
 * }</pre>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Pattern-Based Early Rejection:</b> Uses {@link PlatformPatterns} to filter out comments and
 *       non-Compact elements before initiating context classification, minimising CPU footprint on typing.</li>
 *   <li><b>Public API Facades:</b> Re-exports common keywords, type lookup elements, and type-compatibility
 *       helpers as static facade methods, preserving backwards compatibility and facilitating automated testing.</li>
 * </ul>
 *
 * @see CompactCompletionContext
 * @see CompactKeywordCompletionProvider
 * @see CompactTypeCompletionProvider
 * @see CompactMemberAccessCompletionProvider
 * @see CompactValueCompletionProvider
 * @see CompactStructLiteralCompletionProvider
 */
public class CompactCompletionContributor extends CompletionContributor {

  /**
   * Top-level Compact declaration keywords (e.g. {@code circuit}, {@code ledger}, {@code contract}).
   */
  public static final String[] DECLARATION_KEYWORDS = CompactKeywordCompletionProvider.DECLARATION_KEYWORDS;

  /**
   * Control-flow and local statement keywords valid inside circuit/function bodies (e.g. {@code const}, {@code if}, {@code return}).
   */
  public static final String[] STATEMENT_KEYWORDS = CompactKeywordCompletionProvider.STATEMENT_KEYWORDS;

  /**
   * Expression operand keywords valid in value positions (e.g. {@code true}, {@code false}, {@code disclose}).
   */
  public static final String[] VALUE_KEYWORDS = CompactKeywordCompletionProvider.VALUE_KEYWORDS;

  /**
   * Compact built-in primitive and composite type identifiers (e.g. {@code Field}, {@code Boolean}, {@code Bytes}).
   */
  public static final String[] BUILTIN_TYPES = CompactTypeCompletionProvider.BUILTIN_TYPES;

  /**
   * Type arguments pair container for decomposed {@code Either<Left, Right>} types.
   *
   * @param left  the left variant type name
   * @param right the right variant type name
   */
  public record TypeArgs(String left, String right) {}

  /**
   * Constructs the contributor and registers the primary {@link CompletionType#BASIC} provider pattern.
   */
  @SuppressWarnings("this-escape")
  public CompactCompletionContributor() {
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement()
            .withLanguage(CompactLanguage.INSTANCE)
            .andNot(PlatformPatterns.psiComment())
            .andNot(PlatformPatterns.psiElement().inside(PlatformPatterns.psiComment())),
        new CompletionProvider<>() {
          @Override
          protected void addCompletions(
              @NotNull CompletionParameters parameters,
              @NotNull ProcessingContext context,
              @NotNull CompletionResultSet result
          ) {
            addCompactCompletions(parameters.getPosition(), result);
          }
        });
  }

  /**
   * Evaluates the AST context at {@code position} and delegates candidate population to the appropriate provider.
   *
   * @param position the leaf PSI element at the caret
   * @param result   the completion result set to accumulate lookup elements into
   */
  private static void addCompactCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    if (CompactCompletionContext.isComment(position)) {
      return;
    }

    switch (CompactCompletionContext.classify(position)) {
      case KEYWORD -> CompactKeywordCompletionProvider.addDeclarationCompletions(result);
      case AFTER_EXPORT -> CompactKeywordCompletionProvider.addAfterExportCompletions(result);
      case AFTER_SEALED -> CompactKeywordCompletionProvider.addAfterSealedCompletions(result);
      case AFTER_PURE -> CompactKeywordCompletionProvider.addAfterPureCompletions(result);
      case AFTER_NEW -> CompactKeywordCompletionProvider.addAfterNewCompletions(result);
      case AFTER_PRAGMA -> CompactTypeCompletionProvider.addAfterPragmaCompletions(result);
      case STATEMENT -> {
        CompactKeywordCompletionProvider.addAll(result, STATEMENT_KEYWORDS);
        CompactValueCompletionProvider.addValueCompletions(position, result);
      }
      case TYPE -> {
        CompactTypeCompletionProvider.addBuiltinTypeCompletions(result);
        CompactMemberAccessCompletionProvider.addNamed(result, CompactResolveUtil.collectTypeDeclarations(position));
        CompactValueCompletionProvider.addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.TYPE));
      }
      case BYTES_SIZE -> CompactTypeCompletionProvider.addBytesSizeCompletions(result);
      case UINT_SIZE -> CompactTypeCompletionProvider.addUintSizeCompletions(result);
      case MEMBER -> CompactMemberAccessCompletionProvider.addMemberCompletions(position, result);
      case VALUE -> CompactValueCompletionProvider.addValueCompletions(position, result);
      case NONE -> {}
    }
  }

  /**
   * Facade method populating built-in Compact primitive and parameterized types into {@code result}.
   *
   * @param result the active completion result set
   */
  public static void addBuiltinTypeCompletions(@NotNull CompletionResultSet result) {
    CompactTypeCompletionProvider.addBuiltinTypeCompletions(result);
  }

  /**
   * Facade creating pre-configured {@link LookupElement} instances for all built-in types.
   *
   * @return list of lookup elements with appropriate type insert handlers
   */
  public static List<LookupElement> createBuiltinTypeLookupElements() {
    return CompactTypeCompletionProvider.createBuiltinTypeLookupElements();
  }

  /**
   * Parses the left and right type arguments from an {@code Either<Left, Right>} type signature string.
   *
   * @param typeName raw type signature string (e.g. {@code "Either<Uint<8>, Bytes<32>>"})
   * @return decomposed {@link TypeArgs} containing left and right subtypes, or {@code null} if unparseable
   */
  public static @Nullable TypeArgs parseEitherTypeArgs(@NotNull String typeName) {
    CompactStructLiteralCompletionProvider.TypeArgs args =
        CompactStructLiteralCompletionProvider.parseEitherTypeArgs(typeName);
    return args != null ? new TypeArgs(args.left(), args.right()) : null;
  }

  /**
   * Determines the expected type constraint at the given caret element based on assignment or call context.
   *
   * @param position the PSI element at the cursor
   * @return the inferred {@link CompactType}, or {@code null} if unconstrained
   */
  public static @Nullable CompactType getExpectedType(@NotNull PsiElement position) {
    return CompactValueCompletionProvider.getExpectedType(position);
  }

  /**
   * Resolves the resolved type of a candidate declaration element.
   *
   * @param element the candidate named declaration
   * @return the inferred {@link CompactType}
   */
  public static @NotNull CompactType getCandidateType(@NotNull CompactNamedElement element) {
    return CompactValueCompletionProvider.getCandidateType(element);
  }

  /**
   * Tests whether a candidate expression type satisfies the expected target type requirements.
   *
   * @param candidateType type of the suggested item
   * @param expectedType  contextual expected type (or {@code null} if unconstrained)
   * @return {@code true} if types are assignable / compatible
   */
  public static boolean isTypeCompatible(@NotNull CompactType candidateType, @Nullable CompactType expectedType) {
    return CompactValueCompletionProvider.isTypeCompatible(candidateType, expectedType);
  }

  /**
   * Constructs the pre-configured lookup element for the {@code assert} keyword with its insert handler.
   *
   * @return the assert lookup element
   */
  public static @NotNull LookupElement createAssertLookupElement() {
    return CompactKeywordCompletionProvider.createAssertLookupElement();
  }
}
