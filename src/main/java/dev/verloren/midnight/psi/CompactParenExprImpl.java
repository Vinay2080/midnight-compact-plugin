package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactParenExprImpl extends CompactPsiElement implements CompactExpression {
    public CompactParenExprImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @NotNull CompactType getType() {
        CompactExpression expr = PsiTreeUtil.findChildOfType(this, CompactExpression.class);
        if (expr != null) {
            return expr.getType();
        }
        return CompactPrimitiveType.UNKNOWN;
    }
}
