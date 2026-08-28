package dev.verloren.midnight.editor;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Provides code folding for Compact language constructs in IntelliJ IDEA.
 *
 * <p>Supports collapsing of:
 * <ul>
 *   <li>Curly brace code blocks ({@code { ... }}) in contracts, modules, circuits, ledgers, structs, and enums.</li>
 *   <li>Multi-line block comments ({@code /* ... *&#47;}) and contiguous doc-comment blocks ({@code /// ...}).</li>
 *   <li>Contiguous {@code include} and {@code import} declarations at the top of files.</li>
 * </ul>
 * </p>
 */
public class CompactFoldingBuilder extends CustomFoldingBuilder implements DumbAware {

  @Override
  protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                          @NotNull PsiElement root,
                                          @NotNull Document document,
                                          boolean quick) {
    if (!(root instanceof CompactFile)) {
      return;
    }

    foldImportsAndIncludes(descriptors, root, document);
    foldComments(descriptors, root, document);
    foldBraceBlocks(descriptors, root, document);
  }

  private void foldImportsAndIncludes(@NotNull List<FoldingDescriptor> descriptors,
                                      @NotNull PsiElement root,
                                      @NotNull Document document) {
    PsiElement child = root.getFirstChild();
    PsiElement groupStart = null;
    PsiElement groupEnd = null;
    int groupCount = 0;

    while (child != null) {
      if (child instanceof CompactIncludeDeclaration || child instanceof CompactImportDeclaration) {
        if (groupStart == null) {
          groupStart = child;
        }
        groupEnd = child;
        groupCount++;
      } else if (!(child instanceof PsiWhiteSpace || child instanceof PsiComment)) {
        flushImportGroup(descriptors, groupStart, groupEnd, groupCount, document);
        groupStart = null;
        groupEnd = null;
        groupCount = 0;
      }
      child = child.getNextSibling();
    }

    flushImportGroup(descriptors, groupStart, groupEnd, groupCount, document);
  }

  private void flushImportGroup(
      @NotNull List<FoldingDescriptor> descriptors,
      @Nullable PsiElement groupStart,
      @Nullable PsiElement groupEnd,
      int groupCount,
      @NotNull Document document
  ) {
    if (groupCount >= 2 && groupStart != null && groupEnd != null) {
      TextRange range = new TextRange(groupStart.getTextRange().getStartOffset(), groupEnd.getTextRange().getEndOffset());
      if (isMultiLine(range, document)) {
        descriptors.add(new FoldingDescriptor(groupStart.getNode(), range));
      }
    }
  }

  private void foldComments(@NotNull List<FoldingDescriptor> descriptors,
                            @NotNull PsiElement root,
                            @NotNull Document document) {
    PsiTreeUtil.processElements(root, element -> {
      if (element instanceof PsiComment) {
        IElementType type = element.getNode().getElementType();
        if (type == CompactTokenTypes.BLOCK_COMMENT) {
          TextRange range = element.getTextRange();
          if (isMultiLine(range, document)) {
            descriptors.add(new FoldingDescriptor(element.getNode(), range));
          }
        } else if (type == CompactTokenTypes.LINE_COMMENT && element.getText().startsWith("///")) {
          // Check if multi-line doc comment sequence or single doc comment
          TextRange range = element.getTextRange();
          descriptors.add(new FoldingDescriptor(element.getNode(), range));
        }
      }
      return true;
    });
  }

  private void foldBraceBlocks(@NotNull List<FoldingDescriptor> descriptors,
                               @NotNull PsiElement root,
                               @NotNull Document document) {
    PsiTreeUtil.processElements(root, element -> {
      if (isFoldableBlockElement(element)) {
        TextRange range = getBraceRange(element);
        if (range != null && isMultiLine(range, document)) {
          descriptors.add(new FoldingDescriptor(element.getNode(), range));
        }
      }
      return true;
    });
  }

  private boolean isMultiLine(@NotNull TextRange range, @NotNull Document document) {
    return document.getLineNumber(range.getStartOffset()) < document.getLineNumber(range.getEndOffset());
  }

  private boolean isFoldableBlockElement(@NotNull PsiElement element) {
    return element instanceof CompactExternalContractDeclaration ||
            element instanceof CompactContractImplementsDeclaration ||
            element instanceof CompactModuleDefinition ||
            element instanceof CompactCircuitDefinition ||
            element instanceof CompactWitnessDeclaration ||
            element instanceof CompactConstructorDeclaration ||
            element instanceof CompactLedgerDeclaration ||
            element instanceof CompactStructDefinition ||
            element instanceof CompactEnumDefinition ||
            element instanceof CompactBlock;
  }

  private @Nullable TextRange getBraceRange(@NotNull PsiElement element) {
    ASTNode lbrace = null;
    ASTNode rbrace = null;

    ASTNode node = element.getNode();
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (child.getElementType() == CompactTokenTypes.LBRACE && lbrace == null) {
        lbrace = child;
      }
      if (child.getElementType() == CompactTokenTypes.RBRACE) {
        rbrace = child;
      }
    }

    if (lbrace != null && rbrace != null && lbrace.getStartOffset() < rbrace.getStartOffset()) {
      return new TextRange(lbrace.getStartOffset(), rbrace.getTextRange().getEndOffset());
    }

    if (element instanceof CompactBlock) {
      TextRange range = element.getTextRange();
      if (element.getText().startsWith("{") && element.getText().endsWith("}")) {
        return range;
      }
    }

    return null;
  }

  @Override
  protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
    IElementType type = node.getElementType();
    if (type == CompactTokenTypes.BLOCK_COMMENT) {
      return "/*...*/";
    }
    if (type == CompactTokenTypes.LINE_COMMENT && node.getText().startsWith("///")) {
      return "///...";
    }
    PsiElement psi = node.getPsi();
    if (psi instanceof CompactIncludeDeclaration) {
      return "include ...";
    }
    if (psi instanceof CompactImportDeclaration) {
      return "import ...";
    }
    return "{...}";
  }

  @Override
  protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }
}
