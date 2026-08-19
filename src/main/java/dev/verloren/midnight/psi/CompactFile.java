package dev.verloren.midnight.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Root PSI element representing a complete Compact source file ({@code .compact}).
 *
 * <p>Extends {@link PsiFileBase} and serves as the top-level container for all
 * top-level declarations, imports, pragmas, and include statements in a Compact file.</p>
 */
public class CompactFile extends PsiFileBase {

  public CompactFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, CompactLanguage.INSTANCE);
  }

  @Override
  public @NotNull FileType getFileType() {
    return CompactFileType.INSTANCE;
  }

  public @NotNull PsiElement[] getProgramElements() {
    return getChildren();
  }

  public @NotNull Collection<CompactNamedElement> getDeclarations() {
    return PsiTreeUtil.findChildrenOfType(this, CompactNamedElement.class);
  }

  @Override
  public String toString() {
    return "Compact File";
  }
}