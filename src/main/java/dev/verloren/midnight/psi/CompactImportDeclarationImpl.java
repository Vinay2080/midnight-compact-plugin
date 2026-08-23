package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactImportReference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CompactImportDeclarationImpl extends CompactPsiElement implements CompactImportDeclaration {
  public CompactImportDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitImportDeclaration(this);
  }

  @Override
  public @Nullable PsiReference getReference() {
    ASTNode stringNode = getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
    if (stringNode != null) {
      int start = stringNode.getStartOffset() - getTextRange().getStartOffset();
      return new CompactImportReference(this, TextRange.from(start, stringNode.getTextLength()), CompactImportReference.Kind.FILE);
    }
    PsiElement moduleIdentifier = getModuleIdentifier();
    if (moduleIdentifier != null) {
      int start = moduleIdentifier.getTextRange().getStartOffset() - getTextRange().getStartOffset();
      return new CompactImportReference(this, TextRange.from(start, moduleIdentifier.getTextLength()), CompactImportReference.Kind.MODULE);
    }
    return null;
  }

  public @Nullable String getImportPath() {
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

  public @Nullable PsiElement getModuleIdentifier() {
    for (PsiElement child : getChildren()) {
      if (child.getNode() != null && child.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
        return child;
      }
    }
    ASTNode[] identifiers = getNode().getChildren(TokenSet.create(CompactTokenTypes.IDENTIFIER));
    if (identifiers.length > 0) {
      ASTNode last = identifiers[identifiers.length - 1];
      if (last.getTreeParent() == getNode()) {
        return last.getPsi();
      }
    }
    return null;
  }

  public @Nullable String getModuleName() {
    PsiElement moduleIdentifier = getModuleIdentifier();
    return moduleIdentifier == null ? null : moduleIdentifier.getText();
  }

  public @Nullable String getPrefix() {
    ASTNode prefixNode = getNode().findChildByType(dev.verloren.midnight.parser.CompactElementTypes.IMPORT_PREFIX);
    if (prefixNode == null) {
      return null;
    }
    ASTNode identifier = prefixNode.findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getText();
  }

  public @NotNull Collection<CompactImportElementImpl> getImportElements() {
    return PsiTreeUtil.findChildrenOfType(this, CompactImportElementImpl.class);
  }

  public @Nullable CompactFile resolveImportedFile() {
    return CachedValuesManager.getCachedValue(this, () ->
        CachedValueProvider.Result.create(doResolveImportedFile(), PsiModificationTracker.MODIFICATION_COUNT)
    );
  }

  private @Nullable CompactFile doResolveImportedFile() {
    String path = getImportPath();
    if (path == null || path.isBlank()) {
      String moduleName = getModuleName();
      if (moduleName != null && !moduleName.isBlank()) {
        path = moduleName;
      } else {
        return null;
      }
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

  private static @NotNull List<String> getCandidateFilePaths(@NotNull String path) {
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
}