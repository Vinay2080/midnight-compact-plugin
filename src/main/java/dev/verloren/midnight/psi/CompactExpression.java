package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public interface CompactExpression extends PsiElement {
  @NotNull CompactType getType();
}
