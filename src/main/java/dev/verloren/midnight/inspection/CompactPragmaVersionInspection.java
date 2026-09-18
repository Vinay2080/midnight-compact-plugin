package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.annotator.CompactSwitchCompilerQuickFix;
import dev.verloren.midnight.annotator.CompactUpdatePragmaQuickFix;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Inspection validating that the project's configured Compact compiler satisfies
 * the contract's {@code pragma language_version} (or {@code pragma compiler_version}) directive.
 */
public class CompactPragmaVersionInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Validates that the active Compact compiler satisfies the contract's pragma language_version requirement.";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof CompactPragmaForm pragma) {
          checkPragma(pragma, holder);
        }
      }
    };
  }

  private static void checkPragma(@NotNull CompactPragmaForm pragma, @NotNull ProblemsHolder holder) {
    String constraint = pragma.getConstraintText();
    if (constraint == null || constraint.trim().isEmpty()) {
      constraint = pragma.getRequiredVersion();
    }
    if (constraint == null || constraint.trim().isEmpty()) {
      return;
    }

    PsiElement id = pragma.getPragmaIdentifier();
    String idText = id != null ? id.getText() : "language_version";
    boolean isCompilerPragma = "compiler_version".equals(idText);

    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(holder.getProject());
    String reqVer = pragma.getRequiredVersion();

    if (activeVer == null || activeVer.trim().isEmpty()) {
      List<LocalQuickFix> fixes = buildCompilerQuickFixes(constraint, reqVer, isCompilerPragma, null);
      holder.registerProblem(
          pragma,
          "No Compact compiler configured or installed satisfying pragma '" + constraint + "'",
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
          fixes.toArray(LocalQuickFix.EMPTY_ARRAY)
      );
      return;
    }

    String effectiveActiveVer = isCompilerPragma ? activeVer : CompactVersionManager.getLanguageVersionForToolchain(activeVer);
    boolean satisfies = CompactSemVerUtil.satisfiesConstraint(effectiveActiveVer, constraint);

    if (!satisfies) {
      List<LocalQuickFix> fixes = buildCompilerQuickFixes(constraint, reqVer, isCompilerPragma, activeVer);
      fixes.add(new CompactUpdatePragmaQuickFix(activeVer));

      String desc = isCompilerPragma
          ? "Compact compiler (v" + activeVer + ") does not satisfy pragma constraint '" + constraint + "'"
          : "Compact compiler (v" + activeVer + ", Language v" + effectiveActiveVer + ") does not satisfy pragma constraint '" + constraint + "'";

      holder.registerProblem(
          pragma,
          desc,
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
          fixes.toArray(LocalQuickFix.EMPTY_ARRAY)
      );
    }
  }

  private static @NotNull List<LocalQuickFix> buildCompilerQuickFixes(
      @NotNull String constraint,
      @Nullable String reqVer,
      boolean isCompilerPragma,
      @Nullable String activeVer
  ) {
    List<LocalQuickFix> fixes = new ArrayList<>();
    Set<String> addedToolchains = new HashSet<>();
    Set<String> addedLanguageVersions = new HashSet<>();

    // 1. Prioritize already downloaded/installed versions that satisfy the constraint, sorted newest first
    SequencedMap<String, String> installed = CompactVersionManager.getInstalledVersions();
    List<String> sortedToolchains = new ArrayList<>(installed.keySet());
    sortedToolchains.sort(CompactSemVerUtil.DESCENDING_COMPARATOR);

    for (String installedToolchain : sortedToolchains) {
      String cleanToolchain = CompactVersionManager.cleanVersion(installedToolchain);
      if (activeVer != null && cleanToolchain.equals(CompactVersionManager.cleanVersion(activeVer))) {
        continue; // Active compiler already failed the constraint
      }

      String targetCheckVer = isCompilerPragma
          ? cleanToolchain
          : CompactVersionManager.getLanguageVersionForToolchain(cleanToolchain);

      // Avoid duplicate suggestions for the same language version if multiple patch toolchains are installed
      if (!isCompilerPragma && !addedLanguageVersions.add(targetCheckVer)) {
        continue;
      }

      if (CompactSemVerUtil.satisfiesConstraint(targetCheckVer, constraint)) {
        fixes.add(new CompactSwitchCompilerQuickFix(targetCheckVer, isCompilerPragma, cleanToolchain));
        addedToolchains.add(cleanToolchain);
      }
    }

    // 2. Offer to download and switch to the exact required version if not already installed and offered
    if (reqVer != null && !reqVer.isBlank()) {
      String reqToolchain = isCompilerPragma
          ? CompactVersionManager.cleanVersion(reqVer)
          : CompactVersionManager.resolveToolchainVersionForLanguage(reqVer);
      if (!addedToolchains.contains(reqToolchain)) {
        fixes.add(new CompactSwitchCompilerQuickFix(reqVer, isCompilerPragma));
      }
    }

    return fixes;
  }
}
