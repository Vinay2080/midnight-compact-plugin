package dev.verloren.midnight.type;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactExpression;
import dev.verloren.midnight.psi.CompactGenericParameterImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static evaluator for inferring, parsing, and substituting semantic types of Compact expressions.
 */
public class CompactTypeInferenceUtil {

  /**
   * Infers the resulting type of binary expression {@code left op right}.
   */
  public static @NotNull CompactType inferBinaryExprType(
          @NotNull CompactExpression left,
          @NotNull IElementType operator,
          @NotNull CompactExpression right
  ) {
    CompactType leftType = left.getType();

    if (operator == CompactTokenTypes.ANDAND || operator == CompactTokenTypes.OROR) {
      return CompactPrimitiveType.BOOLEAN;
    }

    if (operator == CompactTokenTypes.EQEQ || operator == CompactTokenTypes.NEQ
            || operator == CompactTokenTypes.LT || operator == CompactTokenTypes.LTE
            || operator == CompactTokenTypes.GT || operator == CompactTokenTypes.GTE) {
      return CompactPrimitiveType.BOOLEAN;
    }

    if (operator == CompactTokenTypes.PLUS || operator == CompactTokenTypes.MINUS
            || operator == CompactTokenTypes.STAR || operator == CompactTokenTypes.SLASH
            || operator == CompactTokenTypes.PERCENT) {
      CompactType rightType = right.getType();
      if (leftType instanceof CompactNumericLiteralType && !(rightType instanceof CompactNumericLiteralType)
              && !CompactPrimitiveType.UNKNOWN.equals(rightType)) {
        return rightType;
      }
      return leftType;
    }

    return CompactPrimitiveType.UNKNOWN;
  }

  public static @NotNull CompactType inferUnaryExprType(@NotNull IElementType operator, @NotNull CompactExpression operand) {
    if (operator == CompactTokenTypes.NOT) {
      return CompactPrimitiveType.BOOLEAN;
    }
    if (operator == CompactTokenTypes.MINUS) {
      return operand.getType();
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Parses a textual type signature into its first-class {@link CompactType} representation.
   */
  public static @NotNull CompactType parseType(@NotNull String typeStr) {
    String trimmed = typeStr.trim();
    if (trimmed.isEmpty()) {
      return CompactPrimitiveType.UNKNOWN;
    }
    CompactPrimitiveType primitive = CompactPrimitiveType.fromName(trimmed);
    if (primitive != null) {
      return primitive;
    }
    if (trimmed.matches("^\\d+$")) {
      return new CompactNumericLiteralType(trimmed);
    }
    CompactUintType uintType = CompactUintType.parse(trimmed);
    if (uintType != null) {
      return uintType;
    }
    int angleIndex = trimmed.indexOf('<');
    if (angleIndex != -1 && trimmed.endsWith(">")) {
      String rawName = trimmed.substring(0, angleIndex).trim();
      List<String> rawArgs = parseGenericArgs(trimmed);
      List<CompactType> argTypes = rawArgs.stream()
              .map(CompactTypeInferenceUtil::parseType)
              .toList();
      return new CompactParameterizedType(rawName, argTypes);
    }
    if (isTypeVariable(trimmed)) {
      return new CompactTypeVariable(trimmed);
    }
    return new CompactPrimitiveType(trimmed);
  }

  public static @NotNull String getRawTypeName(@NotNull String typeName) {
    int angleIndex = typeName.indexOf('<');
    return angleIndex != -1 ? typeName.substring(0, angleIndex).trim() : typeName;
  }

  public static @NotNull List<String> parseGenericArgs(@NotNull String typeStr) {
    int start = typeStr.indexOf('<');
    int end = typeStr.lastIndexOf('>');
    if (start == -1 || end <= start) {
      return List.of();
    }
    String inner = typeStr.substring(start + 1, end);
    List<String> args = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int depth = 0;
    for (int i = 0; i < inner.length(); i++) {
      char c = inner.charAt(i);
      if (c == '<') {
        depth++;
        current.append(c);
      } else if (c == '>') {
        depth--;
        current.append(c);
      } else if (c == ',' && depth == 0) {
        String arg = current.toString().trim();
        if (!arg.isEmpty()) {
          args.add(arg);
        }
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    String lastArg = current.toString().trim();
    if (!lastArg.isEmpty()) {
      args.add(lastArg);
    }
    return args;
  }

  /**
   * Constructs a {@link CompactTypeSubstitutor} mapping declared generic parameters to concrete argument types.
   */
  public static @NotNull CompactTypeSubstitutor buildSubstitutor(
          @NotNull PsiElement declaration,
          @NotNull List<CompactType> typeArguments
  ) {
    List<CompactGenericParameterImpl> params = new ArrayList<>(
            PsiTreeUtil.findChildrenOfType(declaration, CompactGenericParameterImpl.class)
    );
    if (params.isEmpty() || typeArguments.isEmpty()) {
      return CompactTypeSubstitutor.empty();
    }
    CompactTypeSubstitutor.Builder builder = CompactTypeSubstitutor.builder();
    for (int i = 0; i < Math.min(params.size(), typeArguments.size()); i++) {
      String paramName = params.get(i).getName();
      if (paramName != null && !paramName.isEmpty()) {
        builder.put(paramName, typeArguments.get(i));
      }
    }
    return builder.build();
  }

  public static @NotNull String substituteGenerics(@NotNull String typeStr, @NotNull Map<String, String> substitution) {
    if (substitution.isEmpty() || typeStr.isEmpty()) {
      return typeStr;
    }
    CompactTypeSubstitutor.Builder builder = CompactTypeSubstitutor.builder();
    for (Map.Entry<String, String> entry : substitution.entrySet()) {
      builder.put(entry.getKey(), parseType(entry.getValue()));
    }
    CompactType parsed = parseType(typeStr);
    return builder.build().substitute(parsed).name();
  }

  public static @NotNull Map<String, String> buildGenericSubstitution(
          @NotNull PsiElement declaration,
          @NotNull List<String> genericArgs
  ) {
    List<CompactGenericParameterImpl> params = new ArrayList<>(
            PsiTreeUtil.findChildrenOfType(declaration, CompactGenericParameterImpl.class)
    );
    if (params.isEmpty() || genericArgs.isEmpty()) {
      return Map.of();
    }
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < Math.min(params.size(), genericArgs.size()); i++) {
      String paramName = params.get(i).getName();
      if (paramName != null && !paramName.isEmpty()) {
        map.put(paramName, genericArgs.get(i));
        if (paramName.startsWith("#")) {
          map.put(paramName.substring(1), genericArgs.get(i));
        }
      }
    }
    return map;
  }

  public static boolean isGenericAssignable(@Nullable String from, @Nullable String to) {
    if (from == null || to == null) {
      return false;
    }
    if (from.equals(to)) {
      return true;
    }
    String baseFrom = getRawTypeName(from);
    String baseTo = getRawTypeName(to);
    if (!baseFrom.equals(baseTo)) {
      return false;
    }
    List<String> argsFrom = parseGenericArgs(from);
    List<String> argsTo = parseGenericArgs(to);
    if (argsFrom.isEmpty() || argsTo.isEmpty()) {
      return true;
    }
    if (argsFrom.size() != argsTo.size()) {
      return false;
    }
    for (int i = 0; i < argsFrom.size(); i++) {
      String a = argsFrom.get(i);
      String b = argsTo.get(i);
      if (a.equals(b)) {
        continue;
      }
      if (isTypeVariable(a) || isTypeVariable(b)) {
        continue;
      }
      CompactType typeA = parseType(a);
      CompactType typeB = parseType(b);
      if (!typeA.isAssignableTo(typeB)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isTypeVariable(@NotNull String name) {
    if (name.isEmpty()) {
      return false;
    }
    if (CompactPrimitiveType.fromName(name) != null) {
      return false;
    }
    String cleanName = name.startsWith("#") ? name.substring(1) : name;
    if (name.startsWith("#")) {
      return !cleanName.isEmpty();
    }
    return cleanName.matches("^[A-Z][0-9]?$") || cleanName.matches("^T[0-9]+$")
            || cleanName.matches("^T[A-Z][a-zA-Z0-9]*$");
  }
}
