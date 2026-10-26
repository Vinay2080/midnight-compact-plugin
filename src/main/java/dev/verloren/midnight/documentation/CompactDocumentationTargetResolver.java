package dev.verloren.midnight.documentation;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactContractImplementsDeclaration;
import dev.verloren.midnight.psi.CompactEnumMemberImpl;
import dev.verloren.midnight.psi.CompactExportDeclaration;
import dev.verloren.midnight.psi.CompactImportDeclarationImpl;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactParameterImpl;
import dev.verloren.midnight.psi.CompactPatternImpl;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.psi.CompactStructFieldImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Target resolution helper for documentation popups, resolving targets from context elements and comments.
 */
public final class CompactDocumentationTargetResolver {

  private CompactDocumentationTargetResolver() {}

  /**
   * Resolves the target declaration for a context element within a file.
   *
   * @param contextElement the leaf or inner context element
   * @param file           the containing PSI file
   * @return the resolved declaration target, or null
   */
  public static @Nullable PsiElement findResolvedTarget(@NotNull PsiElement contextElement, @NotNull PsiFile file) {
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      if (p.getReference() != null) {
        PsiElement target = p.getReference().resolve();
        if (target != null) return target;
      }
    }
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      if (p instanceof PsiComment comment) {
        PsiElement target = findTargetDeclarationForComment(comment);
        return target != null ? target : comment;
      }
    }
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      switch (p) {
        case CompactStructFieldImpl _, CompactEnumMemberImpl _ -> { return p; }
        case CompactParameterImpl param when param.getParent() instanceof CompactStructFieldImpl -> {
          return param.getParent();
        }
        case CompactImportDeclarationImpl importDecl -> { return importDecl; }
        case CompactPatternImpl _, CompactPragmaForm _, CompactNamedElement _ -> { return p; }
        default -> {}
      }
    }
    return null;
  }

  /**
   * Identifies the declaration target associated with the specified documentation comment.
   *
   * @param comment the doc or block comment
   * @return the target declaration, or null
   */
  public static @Nullable PsiElement findTargetDeclarationForComment(@NotNull PsiComment comment) {
    if (comment instanceof CompactDocComment docComment) {
      comment = docComment.getDelegate();
    }
    PsiElement next = comment.getNextSibling();
    while (next instanceof PsiWhiteSpace || next instanceof PsiComment) {
      next = next.getNextSibling();
    }
    return switch (next) {
      case CompactNamedElement _,
           CompactConstructorDeclaration _,
           CompactContractImplementsDeclaration _,
           CompactPragmaForm _ -> next;
      case CompactExportDeclaration exportDecl -> {
        for (PsiElement child : exportDecl.getChildren()) {
          if (child instanceof CompactNamedElement) {
            yield child;
          }
        }
        yield exportDecl;
      }
      case null -> null;
      default -> PsiTreeUtil.findChildOfType(next, CompactNamedElement.class);
    };
  }
}
