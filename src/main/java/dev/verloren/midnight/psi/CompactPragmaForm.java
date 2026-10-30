package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * PSI element interface representing a compiler pragma directive in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a top-level pragma statement (e.g. {@code pragma language_version >= 0.20;}).
 * Used by compiler version checking, syntax compatibility, and deprecation annotators:
 * <ul>
 *   <li><b>Pragma Name:</b> Identifies the pragma directive key (e.g. {@code "language_version"}) via {@link #getPragmaName()}.</li>
 *   <li><b>Constraint Extraction:</b> Retrieves the raw constraint expression text (e.g. {@code ">= 0.20"}) via {@link #getConstraintText()}.</li>
 *   <li><b>SemVer Parsing:</b> Evaluates the parsed minimum compiler version string via {@link #getRequiredVersion()}.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.version.CompactSemVerUtil} during compiler version compatibility verification.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactPragmaVersionInspection} and version compatibility inspections.</li>
 *   <li>{@link CompactVisitor#visitPragmaForm(CompactPragmaForm)} during file-level inspections.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> All query methods require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only interface; replaced via {@link com.intellij.openapi.command.WriteCommandAction} during quick-fixes.</li>
 * </ul>
 *
 * @see CompactPragmaFormImpl
 * @see dev.verloren.midnight.version.CompactSemVerUtil
 */
public interface CompactPragmaForm extends PsiElement {

  /**
   * Retrieves the leaf PSI element representing the pragma's identifier keyword.
   *
   * @return the identifier leaf element, or {@code null} if unparseable
   */
  @Nullable
  PsiElement getPragmaIdentifier();

  /**
   * Returns the textual name of this pragma directive (e.g. {@code "language_version"}).
   *
   * @return pragma name string, or {@code null} if missing
   */
  @Nullable
  String getPragmaName();

  /**
   * Returns the unparsed constraint text following the pragma name up to the terminating semicolon.
   *
   * @return raw constraint text, or {@code null} if empty
   */
  @Nullable
  String getConstraintText();

  /**
   * Parses and returns the semantic version required by this pragma constraint.
   *
   * @return semantic version string, or {@code null} if invalid
   */
  @Nullable
  String getRequiredVersion();
}
