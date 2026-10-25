package dev.verloren.midnight.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Formatting block implementation adapting Compact AST nodes for IntelliJ's formatting engine.
 *
 * <h3>Subsystem Role &amp; Architecture</h3>
 * <p>Extends {@link AbstractBlock}. Represents a single node in the formatting block tree constructed by
 * {@link CompactFormattingModelBuilder}. Each block corresponds to an {@link ASTNode} and governs:
 * <ul>
 *   <li><b>Contextual Spacing ({@link #getSpacing}):</b> Implements precise spacing logic beyond static builder rules:
 *       <ul>
 *         <li>Disambiguates angle brackets ({@code <}, {@code >}) between generic type parameters/arguments
 *             (tight spacing: {@code Bytes<32>}) and binary comparison operators (spaced: {@code a < b}).</li>
 *         <li>Disambiguates colons ({@code :}) between ternary operators (spaced: {@code a ? b : c}) and
 *             type annotations (tight prefix, spaced suffix: {@code x: Field}).</li>
 *         <li>Maintains single-space padding inside struct literal braces ({@code Point { x: 1 }}) and export forms.</li>
 *         <li>Suppresses whitespace before commas, semicolons, brackets, and callable opening parentheses.</li>
 *       </ul>
 *   </li>
 *   <li><b>Contextual Indentation ({@link #computeChildIndent}):</b> Calculates normal 2-space indentation for
 *       statement blocks, struct fields, enum members, contract methods, and module declarations while keeping
 *       enclosing delimiters (braces/brackets) flush.</li>
 *   <li><b>Smart Enter &amp; Incomplete Syntax ({@link #isIncomplete} / {@link #getChildAttributes}):</b>
 *       Informs the editor of missing closing delimiters ({@code }}, {@code )}, {@code ]}, {@code >}) so that
 *       pressing Enter inside an unclosed block automatically indents the cursor to the correct nesting level.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Instantiated initially by {@link CompactFormattingModelBuilder} for the root file node.
 * IntelliJ's formatter recursively calls {@link #buildChildren()} to construct the block hierarchy,
 * followed by multiple passes of {@link #getSpacing} and {@link #getIndent} to compute document diffs.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Evaluated on pooled background worker threads during code formatting,
 *       or on the Event Dispatch Thread (EDT) during interactive typing/Enter handling.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while traversing
 *       the AST and querying element types.</li>
 *   <li><b>Memory Constraints:</b> Blocks are transient objects discarded immediately after reformatting.</li>
 * </ul>
 *
 * <h3>Formatting Mechanics Diagram</h3>
 * <pre>
 *   [ASTNode (CompactBlock)]
 *              │
 *              ├── buildChildren() ──► Skips whitespace, creates child CompactBlocks
 *              │
 *              ├── computeChildIndent()
 *              │     ├── Inside BLOCK { ... } ────────► Normal Indent (+2 spaces)
 *              │     ├── Inside Struct/Enum/Module ───► Normal Indent (+2 spaces)
 *              │     └── Braces / Brackets / Outer ───► None Indent
 *              │
 *              └── getSpacing(child1, child2)
 *                    ├── SpacingBuilder fallback
 *                    ├── Colons &amp; Ternary operators
 *                    ├── Generic angle brackets vs comparison operators
 *                    └── Struct literal &amp; Export braces
 * </pre>
 *
 * @see dev.verloren.midnight.formatter.CompactFormattingModelBuilder
 * @see dev.verloren.midnight.formatter.CompactLanguageCodeStyleSettingsProvider
 * @see com.intellij.psi.formatter.common.AbstractBlock
 */
public class CompactBlock extends AbstractBlock {

  /**
   * The indentation assigned to this block relative to its parent.
   */
  private final Indent myIndent;

  /**
   * Shared spacing builder supplying declarative token spacing rules.
   */
  private final SpacingBuilder mySpacingBuilder;

  /**
   * Constructs a Compact formatting block for the specified AST node.
   *
   * @param node           the underlying AST node
   * @param indent         the computed indent for this block, or null
   * @param wrap           the wrap strategy for this block, or null
   * @param alignment      the alignment group for this block, or null
   * @param spacingBuilder the shared declarative spacing builder
   */
  public CompactBlock(
      @NotNull ASTNode node,
      @Nullable Indent indent,
      @Nullable Wrap wrap,
      @Nullable Alignment alignment,
      @NotNull SpacingBuilder spacingBuilder
  ) {
    super(node, wrap, alignment);
    myIndent = indent;
    mySpacingBuilder = spacingBuilder;
  }

  @Override
  protected List<Block> buildChildren() {
    List<Block> blocks = new ArrayList<>();
    for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (child.getElementType() == TokenType.WHITE_SPACE) {
        continue;
      }
      Indent childIndent = computeChildIndent(myNode, child);
      blocks.add(new CompactBlock(child, childIndent, null, null, mySpacingBuilder));
    }
    return blocks;
  }

  @Override
  public @Nullable Indent getIndent() {
    return myIndent;
  }

  @Override
  public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
    if (!(child1 instanceof CompactBlock) || !(child2 instanceof CompactBlock)) {
      return null;
    }

    ASTNode node1 = ((CompactBlock) child1).getNode();
    ASTNode node2 = ((CompactBlock) child2).getNode();
    IElementType type1 = node1.getElementType();
    IElementType type2 = node2.getElementType();
    IElementType parentType = myNode.getElementType();

    // Guard against error nodes
    if (type1 == TokenType.ERROR_ELEMENT || type2 == TokenType.ERROR_ELEMENT) {
      return null;
    }

    // Top level declarations in file root
    if (parentType instanceof IFileElementType || myNode.getTreeParent() == null) {
      if (CompactTokenSets.COMMENTS.contains(type1) || CompactTokenSets.COMMENTS.contains(type2)) {
        return Spacing.createSpacing(0, 0, 1, true, 1);
      }
      return Spacing.createSpacing(0, 0, 1, true, 1);
    }

    // Never space before comma or semicolon
    if (type2 == CompactTokenTypes.COMMA || type2 == CompactTokenTypes.SEMICOLON) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }

    // Never space before closing bracket, closing parenthesis
    if (type2 == CompactTokenTypes.RBRACKET || type2 == CompactTokenTypes.RPAREN) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }

    // Ternary expression ':' spacing (1 space before and after)
    if (parentType == CompactElementTypes.TERNARY_EXPR) {
      if (type1 == CompactTokenTypes.COLON || type2 == CompactTokenTypes.COLON) {
        return Spacing.createSpacing(1, 1, 0, false, 0);
      }
    }

    // Colons in other contexts (e.g., type annotations, return types): 0 before, 1 after
    if (type2 == CompactTokenTypes.COLON) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }
    if (type1 == CompactTokenTypes.COLON) {
      return Spacing.createSpacing(1, 1, 0, false, 0);
    }

    // Inside BLOCK { ... }
    if (parentType == CompactElementTypes.BLOCK) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 0, 0, 1, false);
      if (braceSpacing != null) {
        return braceSpacing;
      }
      return Spacing.createSpacing(0, 0, 1, false, 1);
    }

    // Inside STRUCT_DECLARATION
    if (parentType == CompactElementTypes.STRUCT_DECLARATION) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 1, 1, 0, true);
      if (braceSpacing != null) {
        return braceSpacing;
      }
      if (type1 == CompactElementTypes.STRUCT_FIELD || type1 == CompactTokenTypes.SEMICOLON || type1 == CompactTokenTypes.COMMA) {
        return Spacing.createSpacing(1, 1, 0, true, 1);
      }
    }

    // Inside ENUM_DECLARATION
    if (parentType == CompactElementTypes.ENUM_DECLARATION) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 1, 1, 0, true);
      if (braceSpacing != null) {
        return braceSpacing;
      }
      if (type1 == CompactElementTypes.ENUM_MEMBER || type1 == CompactTokenTypes.COMMA) {
        return Spacing.createSpacing(1, 1, 0, true, 1);
      }
    }

    // Inside CONTRACT_DECLARATION
    if (parentType == CompactElementTypes.CONTRACT_DECLARATION) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 1, 1, 0, true);
      if (braceSpacing != null) {
        return braceSpacing;
      }
      if (type1 == CompactElementTypes.EXTERNAL_CIRCUIT || type1 == CompactTokenTypes.SEMICOLON || type1 == CompactTokenTypes.COMMA) {
        return Spacing.createSpacing(1, 1, 0, true, 1);
      }
    }

    // Inside MODULE_DEFINITION
    if (parentType == CompactElementTypes.MODULE_DEFINITION) {
      Spacing braceSpacing = getBraceSpacing(type1, type2, 0, 0, 1, false);
      if (braceSpacing != null) {
        return braceSpacing;
      }
      if (isInsideBraces(myNode, node1) || isInsideBraces(myNode, node2)) {
        return Spacing.createSpacing(0, 0, 1, true, 1);
      }
    }

    // Generics parameter and argument lists (and BUILTIN_TYPE, TYPE_REFERENCE, DEFAULT_EXPR, SLICE_EXPR angle brackets)
    if (parentType == CompactElementTypes.GENERIC_PARAMETER_LIST
        || parentType == CompactElementTypes.GENERIC_ARGUMENT_LIST
        || parentType == CompactElementTypes.BUILTIN_TYPE
        || parentType == CompactElementTypes.TYPE_REFERENCE
        || parentType == CompactElementTypes.DEFAULT_EXPR
        || parentType == CompactElementTypes.SLICE_EXPR) {
      if (type1 == CompactTokenTypes.LT || type2 == CompactTokenTypes.LT) {
        return Spacing.createSpacing(0, 0, 0, false, 0);
      }
      if (type1 == CompactTokenTypes.GT || type2 == CompactTokenTypes.GT) {
        return Spacing.createSpacing(0, 0, 0, false, 0);
      }
      if (type1 == CompactTokenTypes.COMMA) {
        return Spacing.createSpacing(1, 1, 0, false, 0);
      }
    }

    // No space before generic parameter/argument lists or '<' in types/calls/declarations
    if (type2 == CompactElementTypes.GENERIC_PARAMETER_LIST
        || type2 == CompactElementTypes.GENERIC_ARGUMENT_LIST
        || type2 == CompactTokenTypes.LT) {
      if (parentType != CompactElementTypes.BINARY_EXPR) {
        return Spacing.createSpacing(0, 0, 0, false, 0);
      }
    }

    // In BINARY_EXPR: spaces around LT (<) and GT (>) when used as comparison operators
    if (parentType == CompactElementTypes.BINARY_EXPR) {
      if (type1 == CompactTokenTypes.LT || type1 == CompactTokenTypes.GT
          || type2 == CompactTokenTypes.LT || type2 == CompactTokenTypes.GT) {
        return Spacing.createSpacing(1, 1, 0, false, 0);
      }
    }

    // In STRUCT_LITERAL_EXPR: Mariusz { 1, 2, 3, }
    if (parentType == CompactElementTypes.STRUCT_LITERAL_EXPR) {
      if (type1 == CompactTokenTypes.LBRACE) {
        return Spacing.createSpacing(1, 1, 0, false, 0);
      }
      if (type2 == CompactTokenTypes.RBRACE) {
        return Spacing.createSpacing(1, 1, 0, false, 0);
      }
    }

    // In EXPORT_FORM: export { Maybe };
    if (parentType == CompactElementTypes.EXPORT_FORM) {
      if (type1 == CompactTokenTypes.LBRACE) {
        return Spacing.createSpacing(1, 1, 0, false, 0);
      }
      if (type2 == CompactTokenTypes.RBRACE) {
        return Spacing.createSpacing(1, 1, 0, false, 0);
      }
    }

    // No space before '(' in callable declarations and expressions
    if (type2 == CompactElementTypes.SIMPLE_PARAMETER_LIST
        || type2 == CompactElementTypes.PATTERN_PARAMETER_LIST
        || (type2 == CompactTokenTypes.LPAREN && isCallableContext(parentType))) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }

    // Space before BLOCK in circuit, constructor, if, for, module
    if (type2 == CompactElementTypes.BLOCK) {
      return Spacing.createSpacing(1, 1, 0, false, 0);
    }

    // No space before '[' in tuple indexing / Bytes array
    if (type2 == CompactTokenTypes.LBRACKET && (parentType == CompactElementTypes.INDEX_EXPR || parentType == CompactElementTypes.BYTES_EXPR)) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }

    return mySpacingBuilder.getSpacing(this, child1, child2);
  }

  private static boolean isCallableContext(IElementType parentType) {
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

  private static Indent computeChildIndent(ASTNode parent, ASTNode child) {
    IElementType parentType = parent.getElementType();
    IElementType childType = child.getElementType();

    if (childType == TokenType.ERROR_ELEMENT) {
      return Indent.getNoneIndent();
    }

    if (childType == CompactTokenTypes.LBRACE || childType == CompactTokenTypes.RBRACE
        || childType == CompactTokenTypes.LPAREN || childType == CompactTokenTypes.RPAREN
        || childType == CompactTokenTypes.LBRACKET || childType == CompactTokenTypes.RBRACKET) {
      return Indent.getNoneIndent();
    }

    // Statements inside BLOCK get normal indent
    if (parentType == CompactElementTypes.BLOCK) {
      return Indent.getNormalIndent();
    }

    // Inside STRUCT_DECLARATION, ENUM_DECLARATION, CONTRACT_DECLARATION, MODULE_DEFINITION
    if (parentType == CompactElementTypes.STRUCT_DECLARATION
        || parentType == CompactElementTypes.ENUM_DECLARATION
        || parentType == CompactElementTypes.CONTRACT_DECLARATION
        || parentType == CompactElementTypes.MODULE_DEFINITION) {
      if (isInsideBraces(parent, child)) {
        return Indent.getNormalIndent();
      }
      return Indent.getNoneIndent();
    }

    // Statement body in IF / FOR without BLOCK
    if (parentType == CompactElementTypes.IF_STATEMENT || parentType == CompactElementTypes.FOR_STATEMENT) {
      if (childType != CompactElementTypes.BLOCK && childType != CompactTokenTypes.IF && childType != CompactTokenTypes.FOR && childType != CompactTokenTypes.ELSE && childType != CompactElementTypes.EXPRESSION_SEQUENCE) {
        return Indent.getNormalIndent();
      }
    }

    return Indent.getNoneIndent();
  }

  private static boolean isInsideBraces(ASTNode parent, ASTNode child) {
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

  @Override
  public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
    IElementType type = myNode.getElementType();
    if (type == CompactElementTypes.BLOCK
        || type == CompactElementTypes.STRUCT_DECLARATION
        || type == CompactElementTypes.ENUM_DECLARATION
        || type == CompactElementTypes.CONTRACT_DECLARATION
        || type == CompactElementTypes.MODULE_DEFINITION) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    return new ChildAttributes(Indent.getNoneIndent(), null);
  }

  @Override
  public boolean isIncomplete() {
    IElementType type = myNode.getElementType();
    if (type == CompactElementTypes.BLOCK
        || type == CompactElementTypes.STRUCT_DECLARATION
        || type == CompactElementTypes.ENUM_DECLARATION
        || type == CompactElementTypes.CONTRACT_DECLARATION
        || type == CompactElementTypes.MODULE_DEFINITION) {
      return myNode.findChildByType(CompactTokenTypes.RBRACE) == null;
    }
    if (type == CompactElementTypes.SIMPLE_PARAMETER_LIST
        || type == CompactElementTypes.PATTERN_PARAMETER_LIST
        || type == CompactElementTypes.ARROW_PARAMETER_LIST
        || type == CompactElementTypes.PAREN_EXPR) {
      return myNode.findChildByType(CompactTokenTypes.RPAREN) == null;
    }
    if (type == CompactElementTypes.TUPLE_EXPR
        || type == CompactElementTypes.TUPLE_TYPE
        || type == CompactElementTypes.INDEX_EXPR) {
      return myNode.findChildByType(CompactTokenTypes.RBRACKET) == null;
    }
    if (type == CompactElementTypes.GENERIC_PARAMETER_LIST
        || type == CompactElementTypes.GENERIC_ARGUMENT_LIST) {
      return myNode.findChildByType(CompactTokenTypes.GT) == null;
    }
    return false;
  }

  @Override
  public boolean isLeaf() {
    return myNode.getFirstChildNode() == null;
  }

  private static @Nullable Spacing getBraceSpacing(IElementType type1, IElementType type2,
                                                   int minSpaces, int maxSpaces, int minLineFeeds,
                                                   boolean keepLineBreaks) {
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
