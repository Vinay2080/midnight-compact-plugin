package dev.verloren.midnight.type;

import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Static evaluator for inferring the semantic types of Compact expressions.
 *
 * <p>Determines resulting types for binary expressions (logical, comparison, arithmetic)
 * and unary expressions (logical NOT, negation).</p>
 */
public class CompactTypeInferenceUtil {
  /**
   * Infers the resulting type of binary expression {@code left op right}.
   *
   * @param left     left operand expression
   * @param operator binary operator token type
   * @param right    right operand expression
   * @return inferred type of the expression (e.g. {@link CompactPrimitiveType#BOOLEAN} for comparisons)
   */
  public static @NotNull CompactType inferBinaryExprType(@NotNull CompactExpression left, @NotNull IElementType operator, @NotNull CompactExpression right) {
    CompactType leftType = left.getType();

    // Logical operators return Boolean
    if (operator == CompactTokenTypes.ANDAND || operator == CompactTokenTypes.OROR) {
      return CompactPrimitiveType.BOOLEAN;
    }

    // Comparison operators return Boolean
    if (operator == CompactTokenTypes.EQEQ || operator == CompactTokenTypes.NEQ ||
            operator == CompactTokenTypes.LT || operator == CompactTokenTypes.LTE ||
            operator == CompactTokenTypes.GT || operator == CompactTokenTypes.GTE) {
      return CompactPrimitiveType.BOOLEAN;
    }

    // Arithmetic operators return the type of the operands (assuming they match for now)
    if (operator == CompactTokenTypes.PLUS || operator == CompactTokenTypes.MINUS ||
            operator == CompactTokenTypes.STAR || operator == CompactTokenTypes.SLASH ||
            operator == CompactTokenTypes.PERCENT) {
      CompactType rightType = right.getType();
      if (leftType instanceof CompactNumericLiteralType && !(rightType instanceof CompactNumericLiteralType) && !CompactPrimitiveType.UNKNOWN.equals(rightType)) {
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

  public static @NotNull String getRawTypeName(@NotNull String typeName) {
    int angleIndex = typeName.indexOf('<');
    return angleIndex != -1 ? typeName.substring(0, angleIndex).trim() : typeName;
  }

  public static @NotNull java.util.List<String> parseGenericArgs(@NotNull String typeStr) {
    int start = typeStr.indexOf('<');
    int end = typeStr.lastIndexOf('>');
    if (start == -1 || end <= start) {
      return java.util.List.of();
    }
    String inner = typeStr.substring(start + 1, end);
    java.util.List<String> args = new java.util.ArrayList<>();
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

  public static @NotNull String substituteGenerics(@NotNull String typeStr, @NotNull java.util.Map<String, String> substitution) {
    if (substitution.isEmpty() || typeStr.isEmpty()) {
      return typeStr;
    }
    StringBuilder result = new StringBuilder();
    StringBuilder currentToken = new StringBuilder();
    for (int i = 0; i < typeStr.length(); i++) {
      char c = typeStr.charAt(i);
      if (Character.isJavaIdentifierPart(c) || c == '#') {
        currentToken.append(c);
      } else {
        if (!currentToken.isEmpty()) {
          String token = currentToken.toString();
          result.append(substitution.getOrDefault(token, token));
          currentToken.setLength(0);
        }
        result.append(c);
      }
    }
    if (!currentToken.isEmpty()) {
      String token = currentToken.toString();
      result.append(substitution.getOrDefault(token, token));
    }
    return result.toString();
  }

  public static @NotNull java.util.Map<String, String> buildGenericSubstitution(
      @NotNull com.intellij.psi.PsiElement declaration,
      @NotNull java.util.List<String> genericArgs
  ) {
    java.util.List<dev.verloren.midnight.psi.CompactGenericParameterImpl> params = new java.util.ArrayList<>(
        com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(declaration, dev.verloren.midnight.psi.CompactGenericParameterImpl.class)
    );
    if (params.isEmpty() || genericArgs.isEmpty()) {
      return java.util.Map.of();
    }
    java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
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

  public static boolean isGenericAssignable(@org.jspecify.annotations.Nullable String from, @org.jspecify.annotations.Nullable String to) {
    if (from == null || to == null) return false;
    if (from.equals(to)) return true;
    String baseFrom = getRawTypeName(from);
    String baseTo = getRawTypeName(to);
    if (!baseFrom.equals(baseTo)) return false;
    java.util.List<String> argsFrom = parseGenericArgs(from);
    java.util.List<String> argsTo = parseGenericArgs(to);
    if (argsFrom.isEmpty() || argsTo.isEmpty()) return true;
    if (argsFrom.size() != argsTo.size()) return false;
    for (int i = 0; i < argsFrom.size(); i++) {
      String a = argsFrom.get(i);
      String b = argsTo.get(i);
      if (a.equals(b)) continue;
      if (isTypeVariable(a) || isTypeVariable(b)) continue;
      if (!new CompactPrimitiveType(a).isAssignableTo(new CompactPrimitiveType(b))) return false;
    }
    return true;
  }

  public static boolean isTypeVariable(@NotNull String name) {
    if (name.isEmpty()) return false;
    String cleanName = name.startsWith("#") ? name.substring(1) : name;
    return cleanName.matches("^[A-Z][0-9]?$") || cleanName.matches("^T[0-9]*$");
  }
}

