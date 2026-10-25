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
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Serves as the central type evaluation utility:
 * <ul>
 *   <li><b>Binary & Unary Expression Typing:</b> Evaluates operator semantics in
 *       {@link #inferBinaryExprType} and {@link #inferUnaryExprType} for boolean logic, comparisons,
 *       and arithmetic operations.</li>
 *   <li><b>Type Signature Parsing:</b> Converts raw type strings from PSI nodes or comments into
 *       structured {@link CompactType} instances via {@link #parseType(String)}.</li>
 *   <li><b>Generic Parsing & Balancing:</b> Safely parses nested generic arguments
 *       ({@link #parseGenericArgs(String)}) using balanced bracket counting ({@code <...>}) to prevent
 *       splitting inner commas in types like {@code Vector<32, Either<Field, Boolean>>}.</li>
 *   <li><b>Generic Substitution:</b> Generates {@link CompactTypeSubstitutor} instances from PSI generic
 *       declarations and applies substitutions to type signatures.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Pure Computation:</b> Parsing and arithmetic evaluation methods are pure, stateless functions
 *       and do not require locks.</li>
 *   <li><b>PSI Inspection:</b> Methods accepting {@link PsiElement} arguments (such as {@link #buildSubstitutor}
 *       and {@link #buildGenericSubstitution}) require an active IntelliJ Read Action ({@code @RequiresReadLock}).</li>
 * </ul>
 *
 * <h3>Binary Type Inference Resolution Matrix</h3>
 * <pre>
 *   Operator Category      Operators              Inferred Result Type
 *   ────────────────────────────────────────────────────────────────────────
 *   Logical                &amp;&amp;, ||                 CompactPrimitiveType.BOOLEAN
 *   Comparison             ==, !=, &lt;, &lt;=, &gt;, &gt;=   CompactPrimitiveType.BOOLEAN
 *   Arithmetic             +, -, *, /, %          Left operand type (or Right
 *                                                 if Left is literal integer)
 * </pre>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactTypeSubstitutor
 * @see dev.verloren.midnight.psi.CompactExpression#getType()
 */
public class CompactTypeInferenceUtil {

  /**
   * Infers the resulting type of binary expression {@code left op right}.
   *
   * @param left     left-hand side expression
   * @param operator binary operator token
   * @param right    right-hand side expression
   * @return resulting inferred {@link CompactType}
   */
  public static @NotNull CompactType inferBinaryExprType(
          @NotNull CompactExpression left,
          @NotNull IElementType operator,
          @NotNull CompactExpression right
  ) {
    CompactType leftType = left.getType();

    // 1. Logical operators yield Boolean
    if (operator == CompactTokenTypes.ANDAND || operator == CompactTokenTypes.OROR) {
      return CompactPrimitiveType.BOOLEAN;
    }

    // 2. Comparison and equality operators yield Boolean
    if (operator == CompactTokenTypes.EQEQ || operator == CompactTokenTypes.NEQ
            || operator == CompactTokenTypes.LT || operator == CompactTokenTypes.LTE
            || operator == CompactTokenTypes.GT || operator == CompactTokenTypes.GTE) {
      return CompactPrimitiveType.BOOLEAN;
    }

    // 3. Arithmetic operators preserve operand type, coercing literals if applicable
    if (operator == CompactTokenTypes.PLUS || operator == CompactTokenTypes.MINUS
            || operator == CompactTokenTypes.STAR || operator == CompactTokenTypes.SLASH
            || operator == CompactTokenTypes.PERCENT) {
      CompactType rightType = right.getType();
      // If left operand is an untyped literal constant, adapt to the concrete right operand type
      if (leftType instanceof CompactNumericLiteralType && !(rightType instanceof CompactNumericLiteralType)
              && !CompactPrimitiveType.UNKNOWN.equals(rightType)) {
        return rightType;
      }
      return leftType;
    }

    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Infers the resulting type of a prefix unary expression (e.g. {@code !expr} or {@code -expr}).
   *
   * @param operator prefix unary operator token
   * @param operand  target operand expression
   * @return resulting inferred {@link CompactType}
   */
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
   *
   * <p>Parses primitives, numeric literals, unsigned integers, generic parameterized types,
   * type variables, and custom struct/enum nominal types.</p>
   *
   * @param typeStr string to parse
   * @return structured {@link CompactType}
   */
  public static @NotNull CompactType parseType(@NotNull String typeStr) {
    String trimmed = typeStr.trim();
    if (trimmed.isEmpty()) {
      return CompactPrimitiveType.UNKNOWN;
    }
    // Step 1: Check standard primitive singletons
    CompactPrimitiveType primitive = CompactPrimitiveType.fromName(trimmed);
    if (primitive != null) {
      return primitive;
    }
    // Step 2: Pure integer literals
    if (trimmed.matches("^\\d+$")) {
      return new CompactNumericLiteralType(trimmed);
    }
    // Step 3: Sized or ranged Uint types
    CompactUintType uintType = CompactUintType.parse(trimmed);
    if (uintType != null) {
      return uintType;
    }
    // Step 4: Generic/parameterized types (e.g., Vector<32, Field>)
    int angleIndex = trimmed.indexOf('<');
    if (angleIndex != -1 && trimmed.endsWith(">")) {
      String rawName = trimmed.substring(0, angleIndex).trim();
      List<String> rawArgs = parseGenericArgs(trimmed);
      List<CompactType> argTypes = rawArgs.stream()
              .map(CompactTypeInferenceUtil::parseType)
              .toList();
      return new CompactParameterizedType(rawName, argTypes);
    }
    // Step 5: Type variable detection (e.g., #T, TVal, A)
    if (isTypeVariable(trimmed)) {
      return new CompactTypeVariable(trimmed);
    }
    // Step 6: Nominal fallback to primitive/custom type
    return new CompactPrimitiveType(trimmed);
  }

  /**
   * Strips generic type parameters from a type string, returning the raw base name.
   *
   * @param typeName full type signature (e.g. {@code "Vector<32, Field>"})
   * @return raw base name (e.g. {@code "Vector"})
   */
  public static @NotNull String getRawTypeName(@NotNull String typeName) {
    int angleIndex = typeName.indexOf('<');
    return angleIndex != -1 ? typeName.substring(0, angleIndex).trim() : typeName;
  }

  /**
   * Parses comma-separated top-level generic argument strings from a parameterized type string.
   *
   * <p>Uses bracket counting to avoid splitting nested commas inside nested generic brackets.</p>
   *
   * @param typeStr parameterized type string (e.g. {@code "Map<Field, Vector<32, Uint<8>>>"})
   * @return list of top-level argument strings (e.g. {@code ["Field", "Vector<32, Uint<8>>"]})
   */
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
    // Step-by-step bracket balance tracking
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
   *
   * @param declaration   the PSI element declaring generic parameters (e.g., struct, circuit)
   * @param typeArguments list of concrete type arguments to bind
   * @return configured {@link CompactTypeSubstitutor}
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

  /**
   * Applies a string-based generic substitution map to a type signature.
   *
   * @param typeStr      original type string
   * @param substitution mapping of parameter names to replacement type strings
   * @return substituted type name string
   */
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

  /**
   * Builds a string substitution map from a declaration's generic parameters and argument strings.
   *
   * @param declaration declaration element containing generic parameters
   * @param genericArgs list of argument strings
   * @return map from parameter names (both with and without '#') to argument strings
   */
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

  /**
   * Checks whether two generic type strings are assignable to each other.
   *
   * @param from source type string
   * @param to   target type string
   * @return {@code true} if {@code from} is assignable to {@code to}
   */
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

  /**
   * Tests whether an identifier string conforms to the syntax of a Compact generic type variable.
   *
   * <p>Recognizes:
   * <ul>
   *   <li>Identifiers prefixed with {@code #} (e.g. {@code #T}, {@code #A})</li>
   *   <li>Single uppercase letters or single letter with digit (e.g. {@code T}, {@code K}, {@code V}, {@code T1})</li>
   *   <li>Type variable patterns (e.g. {@code TVal}, {@code TErr})</li>
   * </ul>
   * Built-in primitive types are excluded.</p>
   *
   * @param name identifier text to test
   * @return {@code true} if text represents a type variable
   */
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
