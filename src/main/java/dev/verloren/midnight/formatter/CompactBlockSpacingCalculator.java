package dev.verloren.midnight.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spacing calculator governing contextual formatting rules between adjacent Compact AST blocks.
 */
public final class CompactBlockSpacingCalculator {

  private CompactBlockSpacingCalculator() {}

  /**
   * Computes spacing between two child blocks within a parent block.
   */
  public static @Nullable Spacing getSpacing(
      @NotNull Block parentBlock,
      @NotNull ASTNode myNode,
      @NotNull SpacingBuilder spacingBuilder,
      @Nullable Block child1,
      @NotNull Block child2
  ) {
    if (!(child1 instanceof CompactBlock) || !(child2 instanceof CompactBlock)) {
      return null;
    }

    ASTNode node1 = ((CompactBlock) child1).getNode();
    ASTNode node2 = ((CompactBlock) child2).getNode();
    IElementType type1 = node1.getElementType();
    IElementType type2 = node2.getElementType();
    IElementType parentType = myNode.getElementType();

    Spacing punctSpacing = getPunctuationSpacing(type1, type2, parentType, myNode);
    if (punctSpacing != null) {
      return punctSpacing;
    }

    Spacing containerSpacing = getContainerSpacing(type1, type2, parentType, myNode, node1, node2);
    if (containerSpacing != null) {
      return containerSpacing;
    }

    Spacing opSpacing = getOperatorAndCallSpacing(type1, type2, parentType);
    if (opSpacing != null) {
      return opSpacing;
    }

    return spacingBuilder.getSpacing(parentBlock, child1, child2);
  }

  private static @Nullable Spacing getPunctuationSpacing(
      IElementType type1,
      IElementType type2,
      IElementType parentType,
      ASTNode myNode
  ) {
    if (type1 == TokenType.ERROR_ELEMENT || type2 == TokenType.ERROR_ELEMENT) {
      return null;
    }
    if (parentType instanceof IFileElementType || myNode.getTreeParent() == null) {
      return Spacing.createSpacing(0, 0, 1, true, 1);
    }
    if (type2 == CompactTokenTypes.COMMA || type2 == CompactTokenTypes.SEMICOLON) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }
    if (type2 == CompactTokenTypes.RBRACKET || type2 == CompactTokenTypes.RPAREN) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }
    if (parentType == CompactElementTypes.TERNARY_EXPR) {
      if (type1 == CompactTokenTypes.COLON || type2 == CompactTokenTypes.COLON) {
        return Spacing.createSpacing(1, 1, 0, false, 0);
      }
    }
    if (type2 == CompactTokenTypes.COLON) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }
    if (type1 == CompactTokenTypes.COLON) {
      return Spacing.createSpacing(1, 1, 0, false, 0);
    }
    return null;
  }

  private static @Nullable Spacing getContainerSpacing(
      IElementType type1,
      IElementType type2,
      IElementType parentType,
      ASTNode myNode,
      ASTNode node1,
      ASTNode node2
  ) {
    if (parentType == CompactElementTypes.BLOCK) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 0, 0, 1, false);
      return braceSpacing != null ? braceSpacing : Spacing.createSpacing(0, 0, 1, false, 1);
    }
    if (parentType == CompactElementTypes.STRUCT_DECLARATION) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 1, 1, 0, true);
      if (braceSpacing != null) return braceSpacing;
      if (type1 == CompactElementTypes.STRUCT_FIELD || type1 == CompactTokenTypes.SEMICOLON || type1 == CompactTokenTypes.COMMA) {
        return Spacing.createSpacing(1, 1, 0, true, 1);
      }
    }
    if (parentType == CompactElementTypes.ENUM_DECLARATION) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 1, 1, 0, true);
      if (braceSpacing != null) return braceSpacing;
      if (type1 == CompactElementTypes.ENUM_MEMBER || type1 == CompactTokenTypes.COMMA) {
        return Spacing.createSpacing(1, 1, 0, true, 1);
      }
    }
    if (parentType == CompactElementTypes.CONTRACT_DECLARATION) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 1, 1, 0, true);
      if (braceSpacing != null) return braceSpacing;
      if (type1 == CompactElementTypes.EXTERNAL_CIRCUIT || type1 == CompactTokenTypes.SEMICOLON || type1 == CompactTokenTypes.COMMA) {
        return Spacing.createSpacing(1, 1, 0, true, 1);
      }
    }
    if (parentType == CompactElementTypes.MODULE_DEFINITION) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 0, 0, 1, false);
      if (braceSpacing != null) return braceSpacing;
      if (isInsideBraces(myNode, node1) || isInsideBraces(myNode, node2)) {
        return Spacing.createSpacing(0, 0, 1, true, 1);
      }
    }
    return null;
  }

  private static @Nullable Spacing getOperatorAndCallSpacing(
      IElementType type1,
      IElementType type2,
      IElementType parentType
  ) {
    if (isGenericsContext(parentType)) {
      if (type1 == CompactTokenTypes.LT || type2 == CompactTokenTypes.LT) return Spacing.createSpacing(0, 0, 0, false, 0);
      if (type1 == CompactTokenTypes.GT || type2 == CompactTokenTypes.GT) return Spacing.createSpacing(0, 0, 0, false, 0);
      if (type1 == CompactTokenTypes.COMMA) return Spacing.createSpacing(1, 1, 0, false, 0);
    }
    if ((type2 == CompactElementTypes.GENERIC_PARAMETER_LIST || type2 == CompactElementTypes.GENERIC_ARGUMENT_LIST || type2 == CompactTokenTypes.LT)
        && parentType != CompactElementTypes.BINARY_EXPR) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }
    if (parentType == CompactElementTypes.BINARY_EXPR && (type1 == CompactTokenTypes.LT || type1 == CompactTokenTypes.GT || type2 == CompactTokenTypes.LT || type2 == CompactTokenTypes.GT)) {
      return Spacing.createSpacing(1, 1, 0, false, 0);
    }
    if (parentType == CompactElementTypes.STRUCT_LITERAL_EXPR || parentType == CompactElementTypes.EXPORT_FORM) {
      if (type1 == CompactTokenTypes.LBRACE || type2 == CompactTokenTypes.RBRACE) return Spacing.createSpacing(1, 1, 0, false, 0);
    }
    if (type2 == CompactElementTypes.SIMPLE_PARAMETER_LIST || type2 == CompactElementTypes.PATTERN_PARAMETER_LIST || (type2 == CompactTokenTypes.LPAREN && isCallableContext(parentType))) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }
    if (type2 == CompactElementTypes.BLOCK) {
      return Spacing.createSpacing(1, 1, 0, false, 0);
    }
    if (type2 == CompactTokenTypes.LBRACKET && (parentType == CompactElementTypes.INDEX_EXPR || parentType == CompactElementTypes.BYTES_EXPR)) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }
    return null;
  }

  private static boolean isGenericsContext(IElementType parentType) {
    return parentType == CompactElementTypes.GENERIC_PARAMETER_LIST
        || parentType == CompactElementTypes.GENERIC_ARGUMENT_LIST
        || parentType == CompactElementTypes.BUILTIN_TYPE
        || parentType == CompactElementTypes.TYPE_REFERENCE
        || parentType == CompactElementTypes.DEFAULT_EXPR
        || parentType == CompactElementTypes.SLICE_EXPR;
  }

  public static boolean isCallableContext(IElementType parentType) {
    return parentType == CompactElementTypes.CIRCUIT_DEFINITION
        || parentType == CompactElementTypes.CONSTRUCTOR_DEFINITION
        || parentType == CompactElementTypes.WITNESS_DECLARATION
        || parentType == CompactElementTypes.CALL_EXPR
        || parentType == CompactElementTypes.EXTERNAL_CIRCUIT
        || parentType == CompactElementTypes.ASSERT_EXPR
        || parentType == CompactElementTypes.EMIT_EXPR
        || parentType == CompactElementTypes.DISCLOSE_EXPR
        || parentType == CompactElementTypes.PAD_EXPR
        || parentType == CompactElementTypes.MAP_EXPR
        || parentType == CompactElementTypes.FOLD_EXPR
        || parentType == CompactElementTypes.SLICE_EXPR;
  }

  public static boolean isInsideBraces(@NotNull ASTNode parent, @NotNull ASTNode child) {
    boolean seenLBrace = false;
    for (ASTNode curr = parent.getFirstChildNode(); curr != null; curr = curr.getTreeNext()) {
      if (curr.getElementType() == CompactTokenTypes.LBRACE) {
        seenLBrace = true;
        continue;
      }
      if (curr.getElementType() == CompactTokenTypes.RBRACE) {
        break;
      }
      if (seenLBrace && curr == child) {
        return true;
      }
    }
    return false;
  }

  public static @Nullable Spacing getBraceSpacing(
      IElementType type1,
      IElementType type2,
      int minSpaces,
      int maxSpaces,
      int minLineFeeds,
      boolean keepLineBreaks
  ) {
    if (type1 == CompactTokenTypes.LBRACE && type2 == CompactTokenTypes.RBRACE) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }
    if (type1 == CompactTokenTypes.LBRACE) {
      return Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, 1);
    }
    if (type2 == CompactTokenTypes.RBRACE) {
      return Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, 0);
    }
    return null;
  }
}
