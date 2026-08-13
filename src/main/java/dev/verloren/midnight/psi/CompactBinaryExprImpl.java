package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import dev.verloren.midnight.type.CompactTypeInferenceUtil;
import org.jetbrains.annotations.NotNull;

public class CompactBinaryExprImpl extends CompactPsiElement implements CompactExpression {
    public CompactBinaryExprImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @NotNull CompactType getType() {
        CompactExpression[] children = PsiTreeUtil.getChildrenOfType(this, CompactExpression.class);
        if (children == null || children.length < 2) {
            return CompactPrimitiveType.UNKNOWN;
        }
        
        ASTNode operatorNode = getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenSets.OPERATORS);
        if (operatorNode == null) {
            return CompactPrimitiveType.UNKNOWN;
        }
        
        return CompactTypeInferenceUtil.inferBinaryExprType(children[0], operatorNode.getElementType(), children[1]);
    }
}
