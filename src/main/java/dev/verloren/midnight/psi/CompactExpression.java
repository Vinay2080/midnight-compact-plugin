package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * Common interface implemented by all expression nodes in the Compact PSI hierarchy that produce values.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Serves as the root semantic type provider for expressions within the Compact language plugin.
 * Any syntactic construct that evaluates to a runtime or compile-time value implements this interface,
 * enabling uniform semantic type inference, type checking, and expression-level refactorings:
 * <ul>
 *   <li><b>Type Inference:</b> Dispatches {@link #getType()} to retrieve the evaluated {@link CompactType}
 *       (e.g., primitives like {@code Boolean}, {@code Field}, or user-defined struct/enum types).</li>
 *   <li><b>Inspection &amp; Annotator Pipelines:</b> Used by type-checking inspections to verify assignment
 *       compatibility, binary operator operand types, and argument lists against parameter signatures.</li>
 *   <li><b>Code Completion:</b> Guides smart-type completion contributors to filter lookup elements
 *       matching the expected contextual expression type.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during expression and statement type evaluation.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} during code analysis.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactIdentifierCompletionProvider} during code completion.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} traverses the AST and resolves symbol definitions;
 *       it strictly requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only semantic interface; never executes write operations.</li>
 * </ul>
 *
 * <h3>Compact Expression Hierarchy (ASCII)</h3>
 * <pre>
 *                     CompactExpression
 *                             │
 *        ┌────────────────────┼────────────────────┐
 *        ▼                    ▼                    ▼
 * Primary &amp; Literals     Operator Exprs       Member &amp; Call Exprs
 * ├─ LiteralExprImpl     ├─ BinaryExprImpl    ├─ CallExprImpl
 * ├─ ReferenceExprImpl   ├─ UnaryExprImpl     ├─ MemberExprImpl
 * ├─ TupleExprImpl       ├─ TernaryExprImpl   ├─ IndexExprImpl
 * └─ DefaultExprImpl     └─ CastExprImpl      └─ StructLiteralExprImpl
 * </pre>
 *
 * @see CompactType
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil
 * @see CompactTypeElement
 */
public interface CompactExpression extends PsiElement {

  /**
   * Evaluates and returns the semantic {@link CompactType} produced by this expression.
   *
   * <p>Implementations compute this type via AST inspection, operand evaluation, or reference resolution.
   * If the type cannot be determined (e.g. incomplete syntax or unresolved reference), implementations
   * return {@link dev.verloren.midnight.type.CompactPrimitiveType#UNKNOWN}.</p>
   *
   * @return the evaluated semantic type of this expression; never {@code null}
   */
  @NotNull CompactType getType();
}
