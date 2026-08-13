package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public interface CompactTypeElement extends PsiElement {
    @NotNull CompactType getType();
}
