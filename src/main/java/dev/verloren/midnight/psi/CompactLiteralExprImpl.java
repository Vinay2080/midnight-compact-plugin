package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactLiteralExprImpl extends CompactPsiElement implements CompactExpression {
    public CompactLiteralExprImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @NotNull CompactType getType() {
        ASTNode first = getNode().getFirstChildNode();
        if (first == null) return CompactPrimitiveType.UNKNOWN;

        IElementType type = first.getElementType();
        if (type == CompactTokenTypes.TRUE || type == CompactTokenTypes.FALSE) {
            return CompactPrimitiveType.BOOLEAN;
        }
        if (type == CompactTokenTypes.DECIMAL_LITERAL || 
            type == CompactTokenTypes.HEX_LITERAL || 
            type == CompactTokenTypes.BINARY_LITERAL || 
            type == CompactTokenTypes.OCTAL_LITERAL) {
            return CompactPrimitiveType.FIELD;
        }
        // Other literal types (Uint, Bytes, Field) would be handled here
        return CompactPrimitiveType.UNKNOWN;
    }
}
