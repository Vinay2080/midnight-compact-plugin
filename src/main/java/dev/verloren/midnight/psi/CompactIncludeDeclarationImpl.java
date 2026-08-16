package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactIncludeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

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
    String path = getIncludePath();
    if (path == null || path.isBlank()) {
      return null;
    }

    PsiFile containingFile = getContainingFile();
    if (containingFile == null) {
      return null;
    }

    // 1. Directory of containing file
    PsiDirectory dir = containingFile.getContainingDirectory();
    if (dir != null) {
      PsiFile direct = dir.findFile(path);
      if (direct instanceof CompactFile) {
        return (CompactFile) direct;
      }
    }

    // 2. VirtualFile parent relative path
    VirtualFile virtualFile = containingFile.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      virtualFile = containingFile.getViewProvider().getVirtualFile();
    }
    if (virtualFile != null) {
      VirtualFile parentDir = virtualFile.getParent();
      if (parentDir != null) {
        VirtualFile targetVirtualFile = parentDir.findFileByRelativePath(path);
        if (targetVirtualFile != null && targetVirtualFile.isValid()) {
          PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(targetVirtualFile);
          if (psiFile instanceof CompactFile) {
            return (CompactFile) psiFile;
          }
        }
      }
    }

    // 3. Fallback: Search by file name in project scope
    String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
    Collection<VirtualFile> virtualFiles = FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.allScope(getProject()));
    for (VirtualFile vf : virtualFiles) {
      if (vf.isValid()) {
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(vf);
        if (psiFile instanceof CompactFile && psiFile != containingFile) {
          return (CompactFile) psiFile;
        }
      }
    }

    return null;
  }
}