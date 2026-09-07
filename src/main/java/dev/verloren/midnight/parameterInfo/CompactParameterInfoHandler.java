package dev.verloren.midnight.parameterInfo;

import com.intellij.lang.ASTNode;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Parameter Info Handler for the Compact smart contract language ({@code codeInsight.parameterInfo}).
 *
 * <p>Provides parameter and struct-field tooltip hints during method/circuit invocations and struct
 * instantiation when the user presses {@code Ctrl + P} (or when typing parameter delimiters {@code (} or {@code {}):
 * <ul>
 *   <li><b>Circuit, Witness, Constructor & External Circuit Calls:</b> Shows parameter signatures, highlighting
 *       the current active parameter based on caret position and top-level commas.</li>
 *   <li><b>Struct Literals:</b> Shows struct field names and types, highlighting either the explicit named
 *       field under the caret or the next unassigned field.</li>
 *   <li><b>Dynamic Active Parameter Tracking:</b> Recomputes caret offset within argument lists, correctly handling
 *       nested calls and nested struct literals.</li>
 * </ul>
 * </p>
 */
public class CompactParameterInfoHandler implements ParameterInfoHandler<PsiElement, CompactParametersDescription> {

  @Override
  public @Nullable PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
    PsiElement owner = findOwner(context.getFile(), context.getOffset());
    if (owner != null) {
      CompactParametersDescription[] items = createItemsToShow(owner);
      if (items != null && items.length > 0) {
        context.setItemsToShow(items);
        return owner;
      }
    }
    return null;
  }

  @Override
  public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
    context.showHint(element, element.getTextRange().getStartOffset(), this);
  }

  @Override
  public @Nullable PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
    return findOwner(context.getFile(), context.getOffset());
  }

  @Override
  public void updateParameterInfo(@NotNull PsiElement parameterOwner, @NotNull UpdateParameterInfoContext context) {
    if (context.getParameterOwner() == null) {
      context.setParameterOwner(parameterOwner);
    } else if (!context.getParameterOwner().equals(parameterOwner)) {
      context.removeHint();
      return;
    }

    int offset = context.getOffset();
    int currentParam = -1;

    Object[] objects = context.getObjectsToView();
    CompactParametersDescription desc = (objects != null && objects.length > 0 && objects[0] instanceof CompactParametersDescription d)
        ? d
        : null;

    if (parameterOwner instanceof CompactCallExprImpl call) {
      currentParam = calculateCallParameterIndex(call, offset);
    } else if (parameterOwner instanceof CompactStructLiteralExprImpl structLiteral) {
      currentParam = calculateStructParameterIndex(structLiteral, offset, desc);
    }

    context.setCurrentParameter(currentParam);
  }

  @Override
  public void updateUI(CompactParametersDescription p, @NotNull ParameterInfoUIContext context) {
    if (p == null) {
      context.setUIComponentEnabled(false);
      return;
    }
    p.updateUI(context);
  }

  // =========================================================================
  // PSI Ownership & Item Creation
  // =========================================================================

  public static @Nullable PsiElement findOwner(@Nullable PsiFile file, int offset) {
    if (file == null) {
      return null;
    }

    PsiElement element = file.findElementAt(offset);
    if (element == null && offset > 0) {
      element = file.findElementAt(offset - 1);
    }

    while (element != null && !(element instanceof PsiFile)) {
      if (element instanceof CompactCallExprImpl call) {
        if (isInsideDelimiters(call, CompactTokenTypes.LPAREN, CompactTokenTypes.RPAREN, offset)) {
          return call;
        }
      } else if (element instanceof CompactStructLiteralExprImpl structLiteral) {
        if (isInsideDelimiters(structLiteral, CompactTokenTypes.LBRACE, CompactTokenTypes.RBRACE, offset)) {
          return structLiteral;
        }
      }
      element = element.getParent();
    }
    return null;
  }

  private static boolean isInsideDelimiters(
      @NotNull PsiElement owner,
      @NotNull IElementType leftBracketType,
      @NotNull IElementType rightBracketType,
      int offset
  ) {
    ASTNode left = owner.getNode().findChildByType(leftBracketType);
    ASTNode right = owner.getNode().findChildByType(rightBracketType);
    if (left == null) {
      return false;
    }
    int start = left.getStartOffset();
    int end = right != null ? right.getTextRange().getEndOffset() : owner.getTextRange().getEndOffset();
    return offset >= start && offset <= end;
  }

  public static CompactParametersDescription @Nullable [] createItemsToShow(@NotNull PsiElement owner) {
    if (owner instanceof CompactCallExprImpl call) {
      return createCallItemsToShow(call);
    } else if (owner instanceof CompactStructLiteralExprImpl structLiteral) {
      return createStructItemsToShow(structLiteral);
    }
    return null;
  }

  private static CompactParametersDescription @Nullable [] createCallItemsToShow(@NotNull CompactCallExprImpl call) {
    PsiElement resolved = call.resolveCallee();
    if (resolved == null) {
      String name = getCalleeName(call);
      if (name != null) {
        if ("constructor".equals(name)) {
          CompactExternalContractDeclaration contract = PsiTreeUtil.getParentOfType(call, CompactExternalContractDeclaration.class);
          if (contract != null) {
            resolved = PsiTreeUtil.findChildOfType(contract, CompactConstructorDeclaration.class);
          }
          if (resolved == null && call.getContainingFile() != null) {
            resolved = PsiTreeUtil.findChildOfType(call.getContainingFile(), CompactConstructorDeclaration.class);
          }
        }
        if (resolved == null) {
          List<CompactNamedElement> values = CompactResolveUtil.resolveValue(name, call);
          if (!values.isEmpty()) {
            resolved = values.getFirst();
          }
        }
      }
    }

    if (resolved instanceof CompactImportElementImpl importElem) {
      resolved = CompactResolveUtil.resolveImportElementSource(importElem);
    }

    List<CompactNamedElement> parameters = null;
    if (resolved instanceof CompactCircuitDefinition circuit) {
      parameters = circuit.getParameters();
    } else if (resolved instanceof CompactWitnessDeclaration witness) {
      parameters = witness.getParameters();
    } else if (resolved instanceof CompactConstructorDeclaration constructor) {
      parameters = constructor.getParameters();
    } else if (resolved instanceof CompactExternalCircuit externalCircuit) {
      parameters = CompactPsiUtil.getParameters(externalCircuit);
    }

    if (parameters == null) {
      return null;
    }

    List<String> formatted = new ArrayList<>();
    for (CompactNamedElement param : parameters) {
      String paramName = param.getName() != null ? param.getName() : "_";
      String typeText = getParameterTypeText(param);
      formatted.add(paramName + ": " + typeText);
    }

    return new CompactParametersDescription[]{
        new CompactParametersDescription(formatted, false)
    };
  }

  private static CompactParametersDescription @Nullable [] createStructItemsToShow(@NotNull CompactStructLiteralExprImpl structLiteral) {
    PsiReference ref = structLiteral.getReference();
    PsiElement resolved = ref != null ? ref.resolve() : null;

    if (resolved == null) {
      ASTNode idNode = structLiteral.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
      if (idNode != null) {
        List<CompactNamedElement> types = CompactResolveUtil.resolveType(idNode.getText(), structLiteral);
        if (!types.isEmpty()) {
          resolved = types.getFirst();
        }
      }
    }

    if (resolved instanceof CompactImportElementImpl importElem) {
      resolved = CompactResolveUtil.resolveImportElementSource(importElem);
    }

    if (resolved instanceof CompactStructDefinition struct) {
      List<CompactStructFieldImpl> fields = struct.getFields();
      List<String> formatted = new ArrayList<>();
      List<String> fieldNames = new ArrayList<>();
      for (CompactStructFieldImpl field : fields) {
        String fieldName = field.getName() != null ? field.getName() : "_";
        String typeText = getFieldTypeText(field);
        formatted.add(fieldName + ": " + typeText);
        fieldNames.add(fieldName);
      }
      return new CompactParametersDescription[]{
          new CompactParametersDescription(formatted, fieldNames, true)
      };
    }

    return null;
  }

  public static @NotNull String getParameterTypeText(@NotNull CompactNamedElement param) {
    // Check direct child CompactTypeElement
    CompactTypeElement typeElement = PsiTreeUtil.findChildOfType(param, CompactTypeElement.class);
    if (typeElement != null) {
      String text = typeElement.getText().trim();
      if (!text.isEmpty()) {
        return text;
      }
    }

    // Check parent (e.g. CompactOptionallyTypedPattern or CompactTypedPattern)
    PsiElement parent = param.getParent();
    if (parent != null) {
      for (PsiElement child : parent.getChildren()) {
        if (child instanceof CompactTypeElement && child != param) {
          String text = child.getText().trim();
          if (!text.isEmpty()) {
            return text;
          }
        }
      }
    }

    CompactType type = param.getType();
    if (!"Unknown".equalsIgnoreCase(type.name())) {
      return type.name();
    }
    return "Unknown";
  }

  public static @NotNull String getFieldTypeText(@NotNull CompactStructFieldImpl field) {
    CompactParameterImpl param = PsiTreeUtil.findChildOfType(field, CompactParameterImpl.class);
    if (param != null) {
      CompactTypeElement typeElement = PsiTreeUtil.findChildOfType(param, CompactTypeElement.class);
      if (typeElement != null) {
        String text = typeElement.getText().trim();
        if (!text.isEmpty()) {
          return text;
        }
      }
    } else {
      CompactTypeElement typeElement = PsiTreeUtil.findChildOfType(field, CompactTypeElement.class);
      if (typeElement != null) {
        String text = typeElement.getText().trim();
        if (!text.isEmpty()) {
          return text;
        }
      }
    }
    CompactType type = field.getType();
    if (!"Unknown".equalsIgnoreCase(type.name())) {
      return type.name();
    }
    return "Unknown";
  }

  private static @Nullable String getCalleeName(@NotNull CompactCallExprImpl call) {
    CompactReferenceExprImpl refExpr = PsiTreeUtil.getChildOfType(call, CompactReferenceExprImpl.class);
    if (refExpr != null) {
      return refExpr.getText();
    }
    for (PsiElement child : call.getChildren()) {
      if (child instanceof CompactReferenceExprImpl) {
        return child.getText();
      }
    }
    // Check first AST token of call
    ASTNode first = call.getNode().getFirstChildNode();
    if (first != null) {
      return first.getText();
    }
    return null;
  }

  // =========================================================================
  // Active Index Calculation
  // =========================================================================

  public static int calculateCallParameterIndex(@NotNull CompactCallExprImpl call, int offset) {
    ASTNode lparen = call.getNode().findChildByType(CompactTokenTypes.LPAREN);
    if (lparen == null || offset <= lparen.getStartOffset()) {
      return -1;
    }
    int start = lparen.getTextRange().getEndOffset();
    return countTopLevelCommas(call.getNode(), start, offset);
  }

  public static int calculateStructParameterIndex(
      @NotNull CompactStructLiteralExprImpl structLiteral,
      int offset,
      @Nullable CompactParametersDescription desc
  ) {
    ASTNode lbrace = structLiteral.getNode().findChildByType(CompactTokenTypes.LBRACE);
    if (lbrace == null || offset <= lbrace.getStartOffset()) {
      return -1;
    }

    if (desc != null && desc.isStructLiteral() && !desc.getFieldNames().isEmpty()) {
      List<String> declaredFields = desc.getFieldNames();

      // 1. Check if caret is inside a specific struct argument field
      String currentField = findCurrentFieldName(structLiteral, offset);
      if (currentField != null) {
        int idx = declaredFields.indexOf(currentField);
        if (idx >= 0) {
          return idx;
        }
      }

      // 2. Otherwise determine index by count of already assigned fields before offset
      List<String> assignedBeforeCaret = findAssignedFieldsBeforeOffset(structLiteral, offset);
      for (int i = 0; i < declaredFields.size(); i++) {
        if (!assignedBeforeCaret.contains(declaredFields.get(i))) {
          return i;
        }
      }
      return declaredFields.size();
    }

    int start = lbrace.getTextRange().getEndOffset();
    return countTopLevelCommas(structLiteral.getNode(), start, offset);
  }

  private static @Nullable String findCurrentFieldName(@NotNull CompactStructLiteralExprImpl structLiteral, int offset) {
    for (PsiElement child : structLiteral.getChildren()) {
      if (child.getNode() != null && child.getNode().getElementType() == CompactElementTypes.STRUCT_ARG) {
        if (child.getTextRange().containsOffset(offset) || child.getTextRange().getEndOffset() == offset) {
          ASTNode idNode = child.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
          if (idNode != null) {
            return idNode.getText();
          }
        }
      }
    }
    return null;
  }

  private static @NotNull List<String> findAssignedFieldsBeforeOffset(@NotNull CompactStructLiteralExprImpl structLiteral, int offset) {
    List<String> assigned = new ArrayList<>();
    for (PsiElement child : structLiteral.getChildren()) {
      if (child.getNode() != null && child.getNode().getElementType() == CompactElementTypes.STRUCT_ARG) {
        if (child.getTextRange().getEndOffset() < offset) {
          ASTNode idNode = child.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
          if (idNode != null) {
            assigned.add(idNode.getText());
          }
        }
      }
    }
    return assigned;
  }

  private static int countTopLevelCommas(@NotNull ASTNode root, int startOffset, int targetOffset) {
    int[] counter = new int[]{0, 0, 0, 0}; // [commaCount, parenDepth, braceDepth, bracketDepth]
    walkAstLeaves(root, startOffset, targetOffset, counter);
    return counter[0];
  }

  private static boolean walkAstLeaves(@NotNull ASTNode node, int startOffset, int targetOffset, int[] state) {
    if (node.getFirstChildNode() == null) {
      // Leaf node / token
      if (node.getStartOffset() >= startOffset && node.getStartOffset() < targetOffset) {
        IElementType type = node.getElementType();
        if (type == CompactTokenTypes.LPAREN) state[1]++;
        else if (type == CompactTokenTypes.RPAREN) state[1] = Math.max(0, state[1] - 1);
        else if (type == CompactTokenTypes.LBRACE) state[2]++;
        else if (type == CompactTokenTypes.RBRACE) state[2] = Math.max(0, state[2] - 1);
        else if (type == CompactTokenTypes.LBRACKET) state[3]++;
        else if (type == CompactTokenTypes.RBRACKET) state[3] = Math.max(0, state[3] - 1);
        else if (type == CompactTokenTypes.COMMA && state[1] == 0 && state[2] == 0 && state[3] == 0) {
          state[0]++;
        }
      }
      return node.getTextRange().getEndOffset() < targetOffset;
    }

    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (child.getTextRange().getEndOffset() <= startOffset) {
        continue;
      }
      if (child.getStartOffset() >= targetOffset) {
        return false;
      }
      boolean continueWalk = walkAstLeaves(child, startOffset, targetOffset, state);
      if (!continueWalk) {
        return false;
      }
    }
    return true;
  }
}
