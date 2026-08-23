package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactIncludeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.List;

/**
 * PSI representation of a Compact {@code include "filename.compact";} declaration.
 *
 * <p>Resolves the target Compact file relative to the containing directory or project
 * content roots. Results are cached via {@link CachedValuesManager} tied to
 * {@link PsiModificationTracker#MODIFICATION_COUNT}.</p>
 */
public class CompactIncludeDeclarationImpl extends CompactPsiElement implements CompactIncludeDeclaration {
  public CompactIncludeDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitIncludeDeclaration(this);
  }

  @Override
  public @Nullable PsiReference getReference() {
    ASTNode stringNode = getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
    if (stringNode == null) {
      return null;
    }
    int start = stringNode.getStartOffset() - getTextRange().getStartOffset();
    return new CompactIncludeReference(this, TextRange.from(start, stringNode.getTextLength()));
  }

  public @Nullable String getIncludePath() {
    ASTNode stringNode = getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
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

  public @Nullable CompactFile resolveIncludedFile() {
    return CachedValuesManager.getCachedValue(this, () ->
        CachedValueProvider.Result.create(doResolveIncludedFile(), PsiModificationTracker.MODIFICATION_COUNT)
    );
  }

  private @Nullable CompactFile doResolveIncludedFile() {
    String path = getIncludePath();
    if (path == null || path.isBlank()) {
      return null;
    }

    PsiFile containingFile = getContainingFile();
    if (containingFile == null) {
      return null;
    }

    List<String> candidates = getCandidateFilePaths(path);

    // 1. Directory of containing file
    PsiDirectory dir = containingFile.getContainingDirectory();
    if (dir != null) {
      for (String candidate : candidates) {
        PsiFile direct = dir.findFile(candidate);
        if (direct instanceof CompactFile) {
          return (CompactFile) direct;
        }
      }
      VirtualFile dirVirtualFile = dir.getVirtualFile();
      if (dirVirtualFile != null) {
        for (String candidate : candidates) {
          VirtualFile targetVirtualFile = dirVirtualFile.findFileByRelativePath(candidate);
          if (targetVirtualFile != null && targetVirtualFile.isValid()) {
            PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(targetVirtualFile);
            if (psiFile instanceof CompactFile) {
              return (CompactFile) psiFile;
            }
          }
        }
      }
    }

    // 2. VirtualFile parent relative path
    VirtualFile virtualFile = containingFile.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      virtualFile = containingFile.getViewProvider().getVirtualFile();
    }
    VirtualFile parentDir = virtualFile != null ? virtualFile.getParent() : null;
    if (parentDir != null) {
      for (String candidate : candidates) {
        VirtualFile targetVirtualFile = parentDir.findFileByRelativePath(candidate);
        if (targetVirtualFile != null && targetVirtualFile.isValid()) {
          PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(targetVirtualFile);
          if (psiFile instanceof CompactFile) {
            return (CompactFile) psiFile;
          }
        }
      }
    }

    // 3. Project content root relative path
    VirtualFile[] contentRoots = ProjectRootManager.getInstance(getProject()).getContentRoots();
    for (VirtualFile root : contentRoots) {
      for (String candidate : candidates) {
        VirtualFile targetVirtualFile = root.findFileByRelativePath(candidate);
        if (targetVirtualFile != null && targetVirtualFile.isValid()) {
          PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(targetVirtualFile);
          if (psiFile instanceof CompactFile && psiFile != containingFile) {
            return (CompactFile) psiFile;
          }
        }
      }
    }

    return null;
  }

  private static @NotNull java.util.List<String> getCandidateFilePaths(@NotNull String path) {
    java.util.List<String> candidates = new java.util.ArrayList<>();
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
}