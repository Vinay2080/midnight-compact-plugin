package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.reference.CompactValueReference;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility helper methods for Compact PSI tree navigation, parameter collection, and file resolution.
 */
public final class CompactPsiUtil {
  private CompactPsiUtil() {
  }

  /**
   * Collects all declared parameters (including pattern parameters) in a circuit or witness declaration.
   */
  public static @NotNull List<CompactNamedElement> getParameters(@NotNull PsiElement owner) {
    List<CompactNamedElement> params = new ArrayList<>();
    for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(owner, CompactNamedElement.class)) {
      if (named instanceof CompactParameterImpl || isPatternParameter(named)) {
        params.add(named);
      }
    }
    return params;
  }

  /**
   * Checks whether a named pattern is part of a parameter list.
   */
  public static boolean isPatternParameter(@NotNull CompactNamedElement declaration) {
    if (!(declaration instanceof CompactPatternImpl)) {
      return false;
    }
    return hasAncestorOfType(declaration, CompactElementTypes.PATTERN_PARAMETER_LIST)
        || hasAncestorOfType(declaration, CompactElementTypes.ARROW_PARAMETER_LIST);
  }

  /**
   * Checks whether an element has an ancestor with the given element type.
   */
  public static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull IElementType type) {
    for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
      if (parent.getNode() != null && parent.getNode().getElementType() == type) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether an element has an ancestor with the given token type within a scope limit.
   */
  public static boolean hasAncestorWithToken(
      @NotNull PsiElement element,
      @NotNull IElementType tokenType,
      @NotNull PsiElement limit
  ) {
    for (PsiElement p = element; p != null && p != limit; p = p.getParent()) {
      if (p.getNode() != null && p.getNode().findChildByType(tokenType) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a {@link CompactValueReference} for the identifier child of the given element.
   */
  public static @Nullable PsiReference createIdentifierValueReference(@NotNull PsiElement element) {
    ASTNode identifier = element.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    if (identifier == null) {
      return null;
    }
    PsiElement psi = identifier.getPsi();
    int start = psi.getTextRange().getStartOffset() - element.getTextRange().getStartOffset();
    return new CompactValueReference(element, TextRange.from(start, psi.getTextLength()));
  }

  /**
   * Extracts the return type of circuit, witness, or constructor declaration.
   */
  public static @NotNull CompactType getCallableReturnType(@Nullable PsiElement element) {
    if (element instanceof CompactCircuitDefinition circuit) {
      CompactTypeElement rt = circuit.getReturnTypeElement();
      return rt != null ? rt.getType() : new CompactPrimitiveType("Void");
    }
    if (element instanceof CompactWitnessDeclaration witness) {
      CompactTypeElement rt = witness.getReturnTypeElement();
      return rt != null ? rt.getType() : new CompactPrimitiveType("Void");
    }
    if (element instanceof CompactConstructorDeclaration) {
      return new CompactPrimitiveType("Void");
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Extracts the unquoted string content from a string literal child of the given element.
   */
  public static @Nullable String extractStringLiteralValue(@NotNull PsiElement element) {
    ASTNode stringNode = element.getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
    if (stringNode == null) {
      return null;
    }
    String text = stringNode.getText();
    if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
      if (text.length() >= 2) {
        return text.substring(1, text.length() - 1);
      }
    }
    return text;
  }

  /**
   * Generates candidate file paths with and without {@code .compact} extension and {@code ./} prefix.
   */
  public static @NotNull List<String> getCandidateFilePaths(@NotNull String path) {
    List<String> candidates = new ArrayList<>();
    String normalized = path.replace('\\', '/');
    candidates.add(normalized);
    if (!normalized.endsWith(".compact")) {
      candidates.add(normalized + ".compact");
    }
    if (normalized.startsWith("./")) {
      String stripped = normalized.substring(2);
      candidates.add(stripped);
      if (!stripped.endsWith(".compact")) {
        candidates.add(stripped + ".compact");
      }
    }
    return candidates;
  }

  /**
   * Searches for candidate files within a directory and returns the resolved {@link CompactFile}.
   */
  public static @Nullable CompactFile findCompactFileInDir(
      @NotNull Project project,
      @Nullable VirtualFile dir,
      @NotNull List<String> candidates
  ) {
    if (dir == null) {
      return null;
    }
    for (String candidate : candidates) {
      VirtualFile targetVirtualFile = dir.findFileByRelativePath(candidate);
      if (targetVirtualFile != null && targetVirtualFile.isValid()) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(targetVirtualFile);
        if (psiFile instanceof CompactFile compactFile) {
          return compactFile;
        }
      }
    }
    return null;
  }

  /**
   * Resolves a relative Compact file path from containing file directory, parent directory, and project content roots.
   */
  public static @Nullable CompactFile resolveRelativeCompactFile(
      @NotNull PsiElement context,
      @NotNull String path
  ) {
    PsiFile containingFile = context.getContainingFile();
    if (containingFile == null) {
      return null;
    }

    Project project = context.getProject();
    List<String> candidates = getCandidateFilePaths(path);

    // 1. Directory of containing a file
    PsiDirectory dir = containingFile.getContainingDirectory();
    if (dir != null) {
      for (String candidate : candidates) {
        PsiFile direct = dir.findFile(candidate);
        if (direct instanceof CompactFile compactFile) {
          return compactFile;
        }
      }
      CompactFile psiFile = findCompactFileInDir(project, dir.getVirtualFile(), candidates);
      if (psiFile != null) return psiFile;
    }

    // 2. VirtualFile parent relative path
    VirtualFile virtualFile = containingFile.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      virtualFile = containingFile.getViewProvider().getVirtualFile();
    }
    {
      CompactFile psiFile = findCompactFileInDir(project, virtualFile.getParent(), candidates);
      if (psiFile != null) return psiFile;
    }

    // 3. Project content root relative path
    VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
    for (VirtualFile root : contentRoots) {
      for (String candidate : candidates) {
        VirtualFile targetVirtualFile = root.findFileByRelativePath(candidate);
        if (targetVirtualFile != null && targetVirtualFile.isValid()) {
          PsiFile psiFile = PsiManager.getInstance(project).findFile(targetVirtualFile);
          if (psiFile instanceof CompactFile compactFile && psiFile != containingFile) {
            return compactFile;
          }
        }
      }
    }

    return null;
  }
}
