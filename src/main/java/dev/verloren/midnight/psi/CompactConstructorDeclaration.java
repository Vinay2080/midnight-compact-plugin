package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CompactConstructorDeclaration extends PsiElement {
  @NotNull List<CompactNamedElement> getParameters();
  @Nullable CompactBlock getBody();
}
