package dev.verloren.midnight.ide.templates;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal generator and scope analyzer for default declaration names in Compact.
 *
 * <p>Generates auto-numbered names adhering to the language naming rules:
 * <ul>
 *   <li>Determines the base name from the declaration type (e.g., {@code circuit}, {@code witness}, {@code ledger}).</li>
 *   <li>Preserves explicitly provided names exactly as requested without alteration.</li>
 *   <li>Collects existing declared symbol names within the relevant scope (block, module, contract, or file).</li>
 *   <li>Finds the lowest positive integer starting at {@code 1}, filling any numbering gaps (e.g., if {@code circuit1}
 *       and {@code circuit3} exist, generates {@code circuit2}).</li>
 *   <li>Avoids collisions across declaration types and across sibling scopes.</li>
 *   <li>Supports an optional {@code ignoredOffset} parameter to ignore in-flight template variables or tokens
 *       at the current caret position, preventing self-collision and template re-evaluation oscillations.</li>
 * </ul>
 * </p>
 */
@SuppressWarnings("unused")
public final class CompactDeclarationNameGenerator {

  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");

  private CompactDeclarationNameGenerator() {
  }

  /**
   * Generates a default auto-numbered name for the specified declaration type within context.
   */
  public static @NotNull String generateName(@NotNull CompactDeclarationType type, @Nullable PsiElement context) {
    return generateName(type.getBaseName(), null, context, -1);
  }

  /**
   * Generates a declaration name for the specified declaration type, honoring any explicitly specified name.
   */
  public static @NotNull String generateName(
      @NotNull CompactDeclarationType type,
      @Nullable String explicitName,
      @Nullable PsiElement context
  ) {
    return generateName(type.getBaseName(), explicitName, context, -1);
  }

  /**
   * Generates a default auto-numbered name for the given base name within context.
   */
  public static @NotNull String generateName(@NotNull String baseName, @Nullable PsiElement context) {
    return generateName(baseName, null, context, -1);
  }

  /**
   * Generates a declaration name honoring explicit name, with fallback to lowest available integer.
   */
  public static @NotNull String generateName(
      @NotNull String baseName,
      @Nullable String explicitName,
      @Nullable PsiElement context
  ) {
    return generateName(baseName, explicitName, context, -1);
  }

  /**
   * Core generation routine: returns the explicit name if provided, otherwise calculates the lowest
   * available numbered name based on the base name and enclosing scope, ignoring any token at {@code ignoredOffset}.
   *
   * @param baseName      the canonical base name (e.g. {@code "circuit"}, {@code "witness"})
   * @param explicitName  optional user-specified name; if non-null and non-blank, returned verbatim
   * @param context       the PSI anchor or invocation point
   * @param ignoredOffset document offset of the current variable/token to ignore (or -1 if none)
   * @return the generated name (e.g. {@code "circuit1"}, {@code "circuit2"}) or explicit name
   */
  public static @NotNull String generateName(
      @NotNull String baseName,
      @Nullable String explicitName,
      @Nullable PsiElement context,
      int ignoredOffset
  ) {
    // 1. Preserve explicitly provided name
    if (explicitName != null && !explicitName.trim().isEmpty()) {
      return explicitName.trim();
    }

    String canonicalBase = CompactDeclarationType.resolveBaseName(baseName);
    Set<String> existingNames = collectExistingNamesInScope(context, ignoredOffset);

    // 2. Increment sequentially from 1 to find the lowest free numbered identifier
    int index = 1;
    while (true) {
      String candidate = canonicalBase + index;
      if (!existingNames.contains(candidate)) {
        return candidate;
      }
      index++;
    }
  }

  /**
   * Discovers the relevant enclosing scope (block, module, contract, or top-level file)
   * and collects all declared identifier names visible within that scope.
   */
  public static @NotNull Set<String> collectExistingNamesInScope(@Nullable PsiElement context) {
    return collectExistingNamesInScope(context, -1);
  }

  /**
   * Discovers the relevant enclosing scope and collects all declared identifier names,
   * ignoring any token that covers {@code ignoredOffset}.
   */
  public static @NotNull Set<String> collectExistingNamesInScope(@Nullable PsiElement context, int ignoredOffset) {
    Set<String> names = new HashSet<>();
    if (context == null) {
      return names;
    }

    PsiElement scopeRoot = findScopeRoot(context);
    if (scopeRoot == null) {
      return names;
    }

    // 1. Inspect parsed PSI named elements directly belonging to this scope container
    switch (scopeRoot) {
      case CompactFile file -> {
        for (CompactNamedElement named : file.getTopLevelDeclarations()) {
          addNameIfPresent(names, named, ignoredOffset);
        }
        for (CompactModuleDefinition mod : PsiTreeUtil.findChildrenOfType(file, CompactModuleDefinition.class)) {
          addNameIfPresent(names, mod, ignoredOffset);
        }
        for (CompactExternalContractDeclaration cct : PsiTreeUtil.findChildrenOfType(file, CompactExternalContractDeclaration.class)) {
          addNameIfPresent(names, cct, ignoredOffset);
        }
      }
      case CompactModuleDefinition module -> {
        for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(module, CompactNamedElement.class)) {
          if (CompactResolveUtil.isDirectModuleDeclaration(named, module)) {
            addNameIfPresent(names, named, ignoredOffset);
          }
        }
      }
      case CompactExternalContractDeclaration contract -> {
        for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(contract, CompactNamedElement.class)) {
          if (PsiTreeUtil.getParentOfType(named, CompactExternalContractDeclaration.class) == contract) {
            addNameIfPresent(names, named, ignoredOffset);
          }
        }
      }
      case CompactBlock block -> {
        for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(block, CompactNamedElement.class)) {
          if (PsiTreeUtil.getParentOfType(named, CompactBlock.class) == block) {
            addNameIfPresent(names, named, ignoredOffset);
          }
        }
      }
      default -> {
        for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(scopeRoot, CompactNamedElement.class)) {
          addNameIfPresent(names, named, ignoredOffset);
        }
      }
    }

    // 2. Scan scope text for any declarations or tokens that might not yet be fully parsed into PSI
    String scopeText = scopeRoot.getText();
    if (scopeText != null && !scopeText.isEmpty()) {
      int scopeStartOffset = scopeRoot.getTextRange() != null ? scopeRoot.getTextRange().getStartOffset() : 0;
      Matcher matcher = IDENTIFIER_PATTERN.matcher(scopeText);
      while (matcher.find()) {
        int tokenStart = scopeStartOffset + matcher.start();
        int tokenEnd = scopeStartOffset + matcher.end();
        if (ignoredOffset >= 0 && ignoredOffset >= tokenStart && ignoredOffset <= tokenEnd) {
          // Token at the current template variable offset is the declaration being generated/edited
          continue;
        }
        names.add(matcher.group(1));
      }
    }

    return names;
  }

  private static void addNameIfPresent(@NotNull Set<String> names, @Nullable CompactNamedElement element, int ignoredOffset) {
    if (element != null) {
      TextRange range = element.getTextRange();
      if (ignoredOffset >= 0 && range != null && ignoredOffset >= range.getStartOffset() && ignoredOffset <= range.getEndOffset()) {
        return;
      }
      String name = element.getName();
      if (name != null && !name.trim().isEmpty()) {
        names.add(name.trim());
      }
    }
  }

  /**
   * Identifies the innermost scope container (block, module, contract, or top-level file).
   */
  public static @Nullable PsiElement findScopeRoot(@NotNull PsiElement context) {
    for (PsiElement element = context; element != null; element = element.getParent()) {
      if (element instanceof CompactBlock || element instanceof CompactModuleDefinition
          || element instanceof CompactExternalContractDeclaration || element instanceof CompactFile) {
        return element;
      }
    }
    PsiFile containingFile = context.getContainingFile();
    return containingFile instanceof CompactFile ? containingFile : null;
  }
}
