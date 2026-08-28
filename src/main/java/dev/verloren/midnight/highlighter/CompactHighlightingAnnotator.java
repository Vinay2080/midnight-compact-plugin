package dev.verloren.midnight.highlighter;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Semantic annotator for the Midnight Compact smart contract language.
 *
 * <p>Enriches lexical highlighting with fine-grained semantic distinctions for:
 * <ul>
 *   <li>Declarations (circuits, witnesses, contracts, modules, structs, enums, enum members, fields, type aliases, type parameters, consts, params, locals, ledgers, imports)</li>
 *   <li>Calls and usages (circuit calls, witness calls, builtin functions, struct field accesses, enum member accesses, constant usages, param usages, local usages, ledger usages)</li>
 *   <li>Type references and built-in type primitives</li>
 *   <li>String escape sequences (valid and invalid)</li>
 *   <li>Documentation comments ({@code ///}, {@code /**}) vs standard comments</li>
 *   <li>Pragma directives and version literals</li>
 * </ul>
 * </p>
 */
public class CompactHighlightingAnnotator implements Annotator {

  private static final Set<String> BUILTIN_FUNCTIONS = Set.of(
      "assert", "disclose", "fold", "slice", "pad", "emit", "map",
      "transientHash", "persistentHash", "transientCommit", "persistentCommit",
      "transcribe", "publicKey", "degradeToTransient",
      "default", "some", "none", "left", "right",
      "merkleTreePathRoot", "merkleTreePathRootNoLeafHash", "nativeToken", "tokenType"
  );

  private static final Set<String> BUILTIN_TYPES = Set.of(
      "Field", "Boolean", "Uint", "Bytes", "Vector", "Opaque", "Cell", "Void",
      "JubjubScalar", "Secp256k1Base", "Secp256k1Scalar",
      "Counter", "Set", "Map", "List", "HistoricMerkleTree", "MerkleTree",
      "Kernel", "ContractAddress", "ShieldedCoinInfo", "QualifiedShieldedCoinInfo",
      "ZswapCoinPublicKey", "ShieldedSendResult",
      "Maybe", "Either", "MerkleTreeDigest", "MerkleTreePath", "MerkleTreePathEntry", "LeafPreimage"
  );

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof PsiFile) {
      return;
    }

    IElementType tokenType = element.getNode().getElementType();

    // 1. Comments (Doc comments vs standard comments)
    if (tokenType == CompactTokenTypes.LINE_COMMENT) {
      if (element.getText().startsWith("///")) {
        highlightDocComment(holder, element);
      }
      return;
    }
    if (tokenType == CompactTokenTypes.BLOCK_COMMENT) {
      if (element.getText().startsWith("/**")) {
        highlightDocComment(holder, element);
      }
      return;
    }

    // 2. String literal escape sequences
    if (tokenType == CompactTokenTypes.STRING_LITERAL) {
      highlightStringEscapes(holder, element);
      return;
    }

    // 3. Modifiers (export, pure, sealed, new, implements, external)
    if (tokenType == CompactTokenTypes.EXPORT
        || tokenType == CompactTokenTypes.PURE
        || tokenType == CompactTokenTypes.SEALED
        || tokenType == CompactTokenTypes.NEW
        || tokenType == CompactTokenTypes.IMPLEMENTS
        || tokenType == CompactTokenTypes.EXTERNAL) {
      highlight(holder, element, CompactHighlighterColors.MODIFIER);
      return;
    }

    // 4. Builtin function keywords (assert, disclose, fold, slice, pad, emit)
    if (tokenType == CompactTokenTypes.ASSERT
        || tokenType == CompactTokenTypes.DISCLOSE
        || tokenType == CompactTokenTypes.FOLD
        || tokenType == CompactTokenTypes.SLICE
        || tokenType == CompactTokenTypes.PAD
        || tokenType == CompactTokenTypes.EMIT) {
      highlight(holder, element, CompactHighlighterColors.BUILTIN_FUNCTION);
      return;
    }
    if (tokenType == CompactTokenTypes.MAP) {
      if (element.getParent() instanceof CompactTypeReferenceImpl) {
        highlight(holder, element, CompactHighlighterColors.BUILTIN_TYPE);
      } else {
        highlight(holder, element, CompactHighlighterColors.BUILTIN_FUNCTION);
      }
      return;
    }

    // 5. Version literal in pragmas or dependencies
    if (tokenType == CompactTokenTypes.VERSION_LITERAL) {
      highlight(holder, element, CompactHighlighterColors.VERSION);
      return;
    }

    // 6. Built-in types
    switch (element) {
      case CompactBuiltinTypeImpl builtinType -> {
        PsiElement firstChild = builtinType.getFirstChild();
        if (firstChild != null) {
          highlight(holder, firstChild, CompactHighlighterColors.BUILTIN_TYPE);
        }
        return;
      }


      // 7. Direct call expressions (e.g., fetchEntropy(), compute(...))
      case CompactCallExprImpl callExpr -> {
        ASTNode idNode = callExpr.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
        if (idNode != null) {
          String callName = idNode.getText();
          if (BUILTIN_FUNCTIONS.contains(callName)) {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.BUILTIN_FUNCTION);
            return;
          }
          PsiReference ref = callExpr.getReference();
          PsiElement target = ref != null ? ref.resolve() : null;
          if (target instanceof CompactImportElementImpl importElement) {
            target = CompactResolveUtil.resolveImportElementSource(importElement);
          }
          if (target instanceof CompactWitnessDeclaration) {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.WITNESS_CALL);
          } else {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.CIRCUIT_CALL);
          }
        }
        return;
      }


      // 8. Struct literal expressions (e.g., Point { x: 1, y: 2 })
      case CompactStructLiteralExprImpl structLiteral -> {
        ASTNode idNode = structLiteral.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
        if (idNode != null) {
          PsiReference ref = structLiteral.getReference();
          PsiElement target = ref != null ? ref.resolve() : null;
          if (target instanceof CompactImportElementImpl importElement) {
            target = CompactResolveUtil.resolveImportElementSource(importElement);
          }
          if (target instanceof CompactStructDefinition) {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.STRUCT_DECLARATION);
          } else if (BUILTIN_TYPES.contains(idNode.getText())) {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.BUILTIN_TYPE);
          } else {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.STRUCT_DECLARATION);
          }
        }
        return;
      }
      default -> {
      }
    }

    // 9. Struct arguments & destructuring pattern struct elements
    if (element.getNode() != null && element.getNode().getElementType() == dev.verloren.midnight.parser.CompactElementTypes.STRUCT_ARG) {
      ASTNode idNode = element.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
      if (idNode != null) {
        highlight(holder, idNode.getPsi(), CompactHighlighterColors.FIELD_DECLARATION);
      }
      return;
    }
    if (element.getNode() != null && element.getNode().getElementType() == dev.verloren.midnight.parser.CompactElementTypes.PATTERN_STRUCT_ELEMENT) {
      ASTNode idNode = element.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
      if (idNode != null) {
        highlight(holder, idNode.getPsi(), CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION);
      }
      return;
    }

    // 10. Named declarations
    switch (element) {
      case CompactNamedElement namedElement -> {
        PsiElement nameIdentifier = namedElement.getNameIdentifier();
        if (nameIdentifier != null) {
          TextAttributesKey declKey = getDeclarationKey(namedElement);
          if (declKey != null) {
            highlight(holder, nameIdentifier, declKey);
          }
        }

      }


      // 11. Type references (e.g., x: Point, state: GameState)
      case CompactTypeReferenceImpl typeRef -> annotateTypeReference(holder, typeRef);


      // 12. Member access expressions (e.g., p.x or GameState.PLAYING)
      case CompactMemberExprImpl memberExpr -> annotateMemberExpr(holder, memberExpr);


      // 13. Reference expressions & calls (e.g., foo, MAX, x)
      case CompactReferenceExprImpl refExpr -> annotateReferenceExpr(holder, refExpr);


      // 14. Pragma declarations
      case CompactPragmaFormImpl pragmaForm -> annotatePragma(holder, pragmaForm);
      default -> {
      }
    }

  }

  // =========================================================================
  // Declaration Highlighting
  // =========================================================================

  private static @Nullable TextAttributesKey getDeclarationKey(@NotNull CompactNamedElement declaration) {
    switch (declaration) {
      case CompactCircuitDefinition _ -> {
        return CompactHighlighterColors.CIRCUIT_DECLARATION;
      }
      case CompactWitnessDeclaration _ -> {
        return CompactHighlighterColors.WITNESS_DECLARATION;
      }
      case CompactConstructorDeclaration _ -> {
        return CompactHighlighterColors.CONSTRUCTOR_DECLARATION;
      }
      case CompactExternalContractDeclaration _ -> {
        return CompactHighlighterColors.CONTRACT_DECLARATION;
      }
      case CompactModuleDefinition _ -> {
        return CompactHighlighterColors.MODULE_DECLARATION;
      }
      case CompactStructDefinition _ -> {
        return CompactHighlighterColors.STRUCT_DECLARATION;
      }
      case CompactEnumDefinition _ -> {
        return CompactHighlighterColors.ENUM_DECLARATION;
      }
      case CompactEnumMemberImpl _ -> {
        return CompactHighlighterColors.ENUM_MEMBER_DECLARATION;
      }
      case CompactStructFieldImpl _ -> {
        return CompactHighlighterColors.FIELD_DECLARATION;
      }
      case CompactTypeDefinition _ -> {
        return CompactHighlighterColors.TYPE_ALIAS_DECLARATION;
      }
      case CompactGenericParameterImpl _ -> {
        return CompactHighlighterColors.TYPE_PARAMETER;
      }
      case CompactLedgerDeclaration _ -> {
        return CompactHighlighterColors.LEDGER_DECLARATION;
      }
      case CompactPatternImpl pattern -> {
        if (PsiTreeUtil.getParentOfType(pattern, CompactParameterImpl.class) != null
                || PsiTreeUtil.getParentOfType(pattern, CompactTypedPatternImpl.class) != null
                || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
                || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.SIMPLE_PARAMETER_LIST)
                || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST)) {
          return CompactHighlighterColors.PARAMETER_DECLARATION;
        }
        if (PsiTreeUtil.getParentOfType(pattern, CompactConstBindingImpl.class) != null) {
          if (PsiTreeUtil.getParentOfType(pattern, CompactBlock.class) != null) {
            return CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION;
          }
          return CompactHighlighterColors.CONSTANT_DECLARATION;
        }
      }
      default -> {
      }
    }
    switch (declaration) {
      case CompactParameterImpl _ -> {
        return CompactHighlighterColors.PARAMETER_DECLARATION;
      }
      case CompactConstBindingImpl constBinding -> {
        if (PsiTreeUtil.getParentOfType(constBinding, CompactBlock.class) != null) {
          return CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION;
        }
        return CompactHighlighterColors.CONSTANT_DECLARATION;
      }
      case CompactImportElementImpl _ -> {
        return CompactHighlighterColors.IMPORTED_SYMBOL;
      }
      default -> {
      }
    }
    return null;
  }

  // =========================================================================
  // Type Reference Highlighting
  // =========================================================================

  private static void annotateTypeReference(@NotNull AnnotationHolder holder, @NotNull CompactTypeReferenceImpl typeRef) {
    PsiElement idElement = typeRef.findIdentifierChild();
    if (idElement == null) {
      ASTNode mapNode = typeRef.getNode().findChildByType(CompactTokenTypes.MAP);
      if (mapNode != null) {
        idElement = mapNode.getPsi();
      }
    }
    if (idElement == null) {
      return;
    }

    String name = idElement.getText();
    if (BUILTIN_TYPES.contains(name)) {
      highlight(holder, idElement, CompactHighlighterColors.BUILTIN_TYPE);
      return;
    }

    PsiReference ref = typeRef.getReference();
    PsiElement target = ref != null ? ref.resolve() : null;
    if (target instanceof CompactImportElementImpl importElement) {
      target = CompactResolveUtil.resolveImportElementSource(importElement);
    }

    if (target instanceof CompactStructDefinition) {
      highlight(holder, idElement, CompactHighlighterColors.STRUCT_DECLARATION);
    } else if (target instanceof CompactEnumDefinition) {
      highlight(holder, idElement, CompactHighlighterColors.ENUM_DECLARATION);
    } else if (target instanceof CompactTypeDefinition) {
      highlight(holder, idElement, CompactHighlighterColors.TYPE_ALIAS_DECLARATION);
    } else if (target instanceof CompactGenericParameterImpl) {
      highlight(holder, idElement, CompactHighlighterColors.TYPE_PARAMETER);
    } else {
      highlight(holder, idElement, CompactHighlighterColors.TYPE_REFERENCE);
    }
  }

  // =========================================================================
  // Member Expression Highlighting
  // =========================================================================

  private static void annotateMemberExpr(@NotNull AnnotationHolder holder, @NotNull CompactMemberExprImpl memberExpr) {
    ASTNode dotNode = memberExpr.getNode().findChildByType(CompactTokenTypes.DOT);
    if (dotNode == null) {
      return;
    }

    ASTNode memberNode = null;
    ASTNode curr = dotNode.getTreeNext();
    while (curr != null) {
      if (curr.getElementType() == CompactTokenTypes.IDENTIFIER) {
        memberNode = curr;
        break;
      }
      curr = curr.getTreeNext();
    }
    if (memberNode == null) {
      return;
    }

    PsiElement memberPsi = memberNode.getPsi();
    PsiReference ref = memberExpr.getReference();
    PsiElement target = ref != null ? ref.resolve() : null;

    if (target instanceof CompactEnumMemberImpl) {
      highlight(holder, memberPsi, CompactHighlighterColors.ENUM_MEMBER_ACCESS);
    } else if (target instanceof CompactStructFieldImpl) {
      highlight(holder, memberPsi, CompactHighlighterColors.FIELD_ACCESS);
    } else {
      // Fallback based on reference type
      if (ref instanceof dev.verloren.midnight.reference.CompactEnumMemberReference) {
        highlight(holder, memberPsi, CompactHighlighterColors.ENUM_MEMBER_ACCESS);
      } else {
        highlight(holder, memberPsi, CompactHighlighterColors.FIELD_ACCESS);
      }
    }
  }

  // =========================================================================
  // Reference Expression & Call Highlighting
  // =========================================================================

  private static void annotateReferenceExpr(@NotNull AnnotationHolder holder, @NotNull CompactReferenceExprImpl refExpr) {
    // Avoid double-highlighting if this is part of a member access RHS (handled in annotateMemberExpr)
    PsiElement parent = refExpr.getParent();
    if (parent instanceof CompactMemberExprImpl memberParent && memberParent.getBaseExpression() != refExpr) {
      return;
    }

    String name = refExpr.getText();

    // Check for builtin function call
    if (BUILTIN_FUNCTIONS.contains(name)) {
      highlight(holder, refExpr, CompactHighlighterColors.BUILTIN_FUNCTION);
      return;
    }

    boolean isCall = isCallExpression(refExpr);

    PsiReference ref = refExpr.getReference();
    PsiElement target = ref != null ? ref.resolve() : null;

    if (target instanceof CompactImportElementImpl importElement) {
      CompactNamedElement unwrapped = CompactResolveUtil.resolveImportElementSource(importElement);
      if (unwrapped != null) {
        target = unwrapped;
      }
    }

    if (target instanceof CompactCircuitDefinition) {
      highlight(holder, refExpr, CompactHighlighterColors.CIRCUIT_CALL);
    } else if (target instanceof CompactWitnessDeclaration) {
      highlight(holder, refExpr, CompactHighlighterColors.WITNESS_CALL);
    } else if (target instanceof CompactEnumMemberImpl) {
      highlight(holder, refExpr, CompactHighlighterColors.ENUM_MEMBER_ACCESS);
    } else if (target instanceof CompactEnumDefinition) {
      highlight(holder, refExpr, CompactHighlighterColors.ENUM_DECLARATION);
    } else if (target instanceof CompactStructDefinition) {
      highlight(holder, refExpr, CompactHighlighterColors.STRUCT_DECLARATION);
    } else if (target instanceof CompactTypeDefinition) {
      highlight(holder, refExpr, CompactHighlighterColors.TYPE_ALIAS_DECLARATION);
    } else if (target instanceof CompactLedgerDeclaration) {
      if (isWriteAccess(refExpr)) {
        highlight(holder, refExpr, CompactHighlighterColors.LEDGER_WRITE);
      } else {
        highlight(holder, refExpr, CompactHighlighterColors.LEDGER_USAGE);
      }
    } else if (target instanceof CompactParameterImpl || (target instanceof CompactPatternImpl && (
        PsiTreeUtil.getParentOfType(target, CompactParameterImpl.class) != null
        || PsiTreeUtil.getParentOfType(target, CompactTypedPatternImpl.class) != null
        || hasAncestorOfType(target, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
        || hasAncestorOfType(target, dev.verloren.midnight.parser.CompactElementTypes.SIMPLE_PARAMETER_LIST)
        || hasAncestorOfType(target, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST)
    ))) {
      highlight(holder, refExpr, CompactHighlighterColors.PARAMETER_USAGE);
    } else if (target instanceof CompactConstBindingImpl || (target instanceof CompactPatternImpl && PsiTreeUtil.getParentOfType(target, CompactConstBindingImpl.class) != null)) {
      if (PsiTreeUtil.getParentOfType(target, CompactBlock.class) != null) {
        if (isWriteAccess(refExpr)) {
          highlight(holder, refExpr, CompactHighlighterColors.LOCAL_VARIABLE_WRITE);
        } else {
          highlight(holder, refExpr, CompactHighlighterColors.LOCAL_VARIABLE_USAGE);
        }
      } else {
        highlight(holder, refExpr, CompactHighlighterColors.CONSTANT_USAGE);
      }
    } else if (target instanceof CompactGenericParameterImpl) {
      highlight(holder, refExpr, CompactHighlighterColors.TYPE_PARAMETER);
    } else if (isCall) {
      highlight(holder, refExpr, CompactHighlighterColors.CIRCUIT_CALL);
    } else if (isConstantIdentifier(name)) {
      highlight(holder, refExpr, CompactHighlighterColors.CONSTANT_USAGE);
    }
  }

  private static boolean isWriteAccess(@NotNull CompactReferenceExprImpl refExpr) {
    PsiElement parent = refExpr.getParent();
    if (parent != null && parent.getNode() != null && parent.getNode().getElementType() == dev.verloren.midnight.parser.CompactElementTypes.ASSIGN_EXPR) {
      PsiElement first = parent.getFirstChild();
      return first == refExpr || (PsiTreeUtil.isAncestor(first, refExpr, false));
    }
    return false;
  }

  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType type) {
    return CompactPsiUtil.hasAncestorOfType(element, type);
  }

  private static boolean isCallExpression(@NotNull CompactReferenceExprImpl refExpr) {
    PsiElement parent = refExpr.getParent();
    while (parent instanceof CompactParenExprImpl) {
      parent = parent.getParent();
    }
    return parent instanceof CompactCallExprImpl;
  }

  // =========================================================================
  // Pragma Directive Highlighting
  // =========================================================================

  private static void annotatePragma(@NotNull AnnotationHolder holder, @NotNull CompactPragmaFormImpl pragmaForm) {
    PsiElement pragmaId = pragmaForm.getPragmaIdentifier();
    if (pragmaId != null) {
      highlight(holder, pragmaId, CompactHighlighterColors.PRAGMA);
    }
  }

  // =========================================================================
  // String Escape Highlighting
  // =========================================================================

  private static void highlightStringEscapes(@NotNull AnnotationHolder holder, @NotNull PsiElement element) {
    String text = element.getText();
    int startOffset = element.getTextRange().getStartOffset();
    int len = text.length();
    if (len < 2) {
      return;
    }

    int i = 1;
    while (i < len - 1) {
      if (text.charAt(i) == '\\') {
        if (i + 1 >= len - 1) {
          // Trailing backslash before end quote
          highlightRange(holder, TextRange.create(startOffset + i, startOffset + i + 1), CompactHighlighterColors.INVALID_STRING_ESCAPE);
          break;
        }

        char next = text.charAt(i + 1);
        if (next == 'n' || next == 'r' || next == 't' || next == '\\' || next == '"' || next == '\'' || next == '0') {
          highlightRange(holder, TextRange.create(startOffset + i, startOffset + i + 2), CompactHighlighterColors.VALID_STRING_ESCAPE);
          i += 2;
        } else if (next == 'x') {
          if (i + 3 < len - 1 && isHexDigit(text.charAt(i + 2)) && isHexDigit(text.charAt(i + 3))) {
            highlightRange(holder, TextRange.create(startOffset + i, startOffset + i + 4), CompactHighlighterColors.VALID_STRING_ESCAPE);
            i += 4;
          } else {
            highlightRange(holder, TextRange.create(startOffset + i, Math.min(startOffset + i + 2, startOffset + len - 1)), CompactHighlighterColors.INVALID_STRING_ESCAPE);
            i += 2;
          }
        } else if (next == 'u' && i + 2 < len - 1 && text.charAt(i + 2) == '{') {
          int closeBrace = text.indexOf('}', i + 3);
          if (closeBrace != -1 && closeBrace < len - 1 && isValidHexSequence(text, i + 3, closeBrace)) {
            highlightRange(holder, TextRange.create(startOffset + i, startOffset + closeBrace + 1), CompactHighlighterColors.VALID_STRING_ESCAPE);
            i = closeBrace + 1;
          } else {
            int invalidEnd = closeBrace != -1 && closeBrace < len - 1 ? closeBrace + 1 : Math.min(i + 3, len - 1);
            highlightRange(holder, TextRange.create(startOffset + i, startOffset + invalidEnd), CompactHighlighterColors.INVALID_STRING_ESCAPE);
            i = invalidEnd;
          }
        } else {
          highlightRange(holder, TextRange.create(startOffset + i, startOffset + i + 2), CompactHighlighterColors.INVALID_STRING_ESCAPE);
          i += 2;
        }
      } else {
        i++;
      }
    }
  }

  private static boolean isHexDigit(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static boolean isValidHexSequence(String text, int start, int end) {
    if (start >= end || end - start > 6) {
      return false;
    }
    for (int i = start; i < end; i++) {
      if (!isHexDigit(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static void highlight(@NotNull AnnotationHolder holder, @NotNull PsiElement element, @NotNull TextAttributesKey key) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        .range(element.getTextRange())
        .textAttributes(key)
        .create();
  }

  private static void highlightRange(@NotNull AnnotationHolder holder, @NotNull TextRange range, @NotNull TextAttributesKey key) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        .range(range)
        .textAttributes(key)
        .create();
  }

  private static final java.util.regex.Pattern DOC_TAG_PATTERN =
      java.util.regex.Pattern.compile("@([a-zA-Z_][a-zA-Z0-9_]*)");

  private static void highlightDocComment(@NotNull AnnotationHolder holder, @NotNull PsiElement element) {
    highlight(holder, element, CompactHighlighterColors.DOC_COMMENT);

    String text = element.getText();
    int startOffset = element.getTextRange().getStartOffset();
    java.util.regex.Matcher matcher = DOC_TAG_PATTERN.matcher(text);

    while (matcher.find()) {
      int tagStart = matcher.start();
      int tagEnd = matcher.end();
      highlightRange(holder, TextRange.create(startOffset + tagStart, startOffset + tagEnd), CompactHighlighterColors.DOC_COMMENT_TAG);

      String tagName = matcher.group(1);
      if ("param".equals(tagName) || "type".equals(tagName) || "module".equals(tagName)) {
        int idx = tagEnd;
        while (idx < text.length() && Character.isWhitespace(text.charAt(idx)) && text.charAt(idx) != '\n' && text.charAt(idx) != '\r') {
          idx++;
        }
        if (idx < text.length() && (Character.isLetter(text.charAt(idx)) || text.charAt(idx) == '_' || text.charAt(idx) == '$')) {
          int valueStart = idx;
          while (idx < text.length() && (Character.isLetterOrDigit(text.charAt(idx)) || text.charAt(idx) == '_' || text.charAt(idx) == '$')) {
            idx++;
          }
          int valueEnd = idx;
          highlightRange(holder, TextRange.create(startOffset + valueStart, startOffset + valueEnd), CompactHighlighterColors.DOC_COMMENT_TAG_VALUE);
        }
      }
    }
  }

  private static boolean isConstantIdentifier(@NotNull String name) {
    if (name.length() < 2) {
      return false;
    }
    if (!Character.isUpperCase(name.charAt(0))) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (!Character.isUpperCase(c) && !Character.isDigit(c) && c != '_') {
        return false;
      }
    }
    return !BUILTIN_TYPES.contains(name);
  }
}
