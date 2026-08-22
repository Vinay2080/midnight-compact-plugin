package dev.verloren.midnight.resolve.npm;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.icons.MidnightIcons;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.symbol.CompactSymbolNamespace;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Synthetic PSI element representing an exported symbol from an external npm package declaration or source file.
 */
public class CompactNpmSymbolElement extends FakePsiElement implements CompactNamedElement {
  private final PsiFile containingFile;
  private final String name;
  private final int textOffset;
  private final CompactNpmSymbolKind kind;
  private final String packageName;

  public CompactNpmSymbolElement(
      @NotNull PsiFile containingFile,
      @NotNull String name,
      int textOffset,
      @NotNull CompactNpmSymbolKind kind,
      @NotNull String packageName
  ) {
    this.containingFile = containingFile;
    this.name = name;
    this.textOffset = Math.max(0, textOffset);
    this.kind = kind;
    this.packageName = packageName;
  }

  @Override
  public @Nullable String getName() {
    return name;
  }

  @Override
  public PsiElement setName(@NotNull String newName) throws IncorrectOperationException {
    throw new IncorrectOperationException("Cannot rename external npm package symbol");
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    return this;
  }

  @Override
  public int getTextOffset() {
    return textOffset;
  }

  @Override
  public @NotNull TextRange getTextRange() {
    return TextRange.from(textOffset, name.length());
  }

  @Override
  public String getText() {
    return name;
  }

  @Override
  public @NotNull PsiFile getContainingFile() {
    return containingFile;
  }

  @Override
  public @NotNull Project getProject() {
    return containingFile.getProject();
  }

  @Override
  public PsiElement getParent() {
    return containingFile;
  }

  @Override
  public boolean isValid() {
    return containingFile.isValid();
  }

  @Override
  public @NotNull CompactType getType() {
    return CompactPrimitiveType.UNKNOWN;
  }

  @Override
  public @NotNull SearchScope getUseScope() {
    return GlobalSearchScope.projectScope(getProject());
  }

  public @NotNull CompactNpmSymbolKind getKind() {
    return kind;
  }

  public @NotNull String getPackageName() {
    return packageName;
  }

  public boolean isInNamespace(@NotNull CompactResolveUtil.Namespace namespace) {
    return switch (namespace) {
      case VALUE -> kind.isValue();
      case TYPE -> kind.isType();
    };
  }

  public @NotNull CompactSymbolNamespace getSymbolNamespace() {
    if (kind.isValue() && !kind.isType()) {
      return CompactSymbolNamespace.VALUE;
    }
    if (kind.isType() && !kind.isValue()) {
      return CompactSymbolNamespace.TYPE;
    }
    return CompactSymbolNamespace.VALUE;
  }

  @Override
  public void navigate(boolean requestFocus) {
    VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile != null) {
      new OpenFileDescriptor(getProject(), virtualFile, textOffset).navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return containingFile.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return true;
  }

  @Override
  public @Nullable Icon getIcon(boolean open) {
    return MidnightIcons.FILE;
  }

  @Override
  public @Nullable ItemPresentation getPresentation() {
    return new ItemPresentation() {
      @Override
      public @NotNull String getPresentableText() {
        return name;
      }

      @Override
      public @NotNull String getLocationString() {
        return packageName + " (" + containingFile.getName() + ")";
      }

      @Override
      public @Nullable Icon getIcon(boolean unused) {
        return MidnightIcons.FILE;
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactNpmSymbolElement that = (CompactNpmSymbolElement) o;
    return textOffset == that.textOffset
        && Objects.equals(containingFile, that.containingFile)
        && Objects.equals(name, that.name)
        && kind == that.kind
        && Objects.equals(packageName, that.packageName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(containingFile, name, textOffset, kind, packageName);
  }

  @Override
  public String toString() {
    return "CompactNpmSymbolElement(" + name + " in " + packageName + ", kind=" + kind + ")";
  }
}
