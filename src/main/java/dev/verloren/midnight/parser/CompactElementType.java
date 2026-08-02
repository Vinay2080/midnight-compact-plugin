package dev.verloren.midnight.parser;

import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public final class CompactElementType extends IElementType {
  public CompactElementType(@NonNls @NotNull String debugName) {
    super(debugName, CompactLanguage.INSTANCE);
  }
}
