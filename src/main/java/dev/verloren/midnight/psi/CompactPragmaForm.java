package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface CompactPragmaForm extends PsiElement {
  @Nullable
  PsiElement getPragmaIdentifier();
}