package dev.verloren.midnight.lexer;

import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.CompactLanguage;

public class CompactTokenType extends IElementType {
  public CompactTokenType(String debugName) {
    super(debugName, CompactLanguage.INSTANCE);
  }
}
