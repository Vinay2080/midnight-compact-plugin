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
 * Code completion provider for Compact smart contracts.
 *
 * <p>Extends {@link CompletionContributor} and dispatches caret context via
 * {@link CompactCompletionContext#classify(PsiElement)} to dedicated completion providers
 * in {@code dev.verloren.midnight.completion.providers}.</p>
 */
public class CompactCompletionContributor extends CompletionContributor {

  public static final String[] DECLARATION_KEYWORDS = CompactKeywordCompletionProvider.DECLARATION_KEYWORDS;
  public static final String[] STATEMENT_KEYWORDS = CompactKeywordCompletionProvider.STATEMENT_KEYWORDS;
  public static final String[] VALUE_KEYWORDS = CompactKeywordCompletionProvider.VALUE_KEYWORDS;
  public static final String[] BUILTIN_TYPES = CompactTypeCompletionProvider.BUILTIN_TYPES;

  public record TypeArgs(String left, String right) {}

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

  public static void addBuiltinTypeCompletions(@NotNull CompletionResultSet result) {
    CompactTypeCompletionProvider.addBuiltinTypeCompletions(result);
  }

  public static List<LookupElement> createBuiltinTypeLookupElements() {
    return CompactTypeCompletionProvider.createBuiltinTypeLookupElements();
  }

  public static @Nullable TypeArgs parseEitherTypeArgs(@NotNull String typeName) {
    CompactStructLiteralCompletionProvider.TypeArgs args =
        CompactStructLiteralCompletionProvider.parseEitherTypeArgs(typeName);
    return args != null ? new TypeArgs(args.left(), args.right()) : null;
  }

  public static @Nullable CompactType getExpectedType(@NotNull PsiElement position) {
    return CompactValueCompletionProvider.getExpectedType(position);
  }

  public static @NotNull CompactType getCandidateType(@NotNull CompactNamedElement element) {
    return CompactValueCompletionProvider.getCandidateType(element);
  }

  public static boolean isTypeCompatible(@NotNull CompactType candidateType, @Nullable CompactType expectedType) {
    return CompactValueCompletionProvider.isTypeCompatible(candidateType, expectedType);
  }

  public static @NotNull LookupElement createAssertLookupElement() {
    return CompactKeywordCompletionProvider.createAssertLookupElement();
  }
}
