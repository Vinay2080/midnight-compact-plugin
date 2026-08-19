package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import dev.verloren.midnight.type.CompactUintType;
import org.jetbrains.annotations.NotNull;

public class CompactBuiltinTypeImpl extends CompactPsiElement implements CompactTypeElement {
  public CompactBuiltinTypeImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    String text = getText();
    if ("Boolean".equals(text)) return CompactPrimitiveType.BOOLEAN;
    if ("Field".equals(text)) return CompactPrimitiveType.FIELD;
    if (text.startsWith("Uint")) {
      CompactUintType uintType = CompactUintType.parse(text);
      if (uintType != null) {
        return uintType;
      }
    }
    if (text.startsWith("Bytes") || text.startsWith("Vector") || text.startsWith("Opaque")
        || text.startsWith("JubjubScalar") || text.startsWith("Secp256k1Base") || text.startsWith("Secp256k1Scalar")) {
      return new CompactPrimitiveType(text);
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
