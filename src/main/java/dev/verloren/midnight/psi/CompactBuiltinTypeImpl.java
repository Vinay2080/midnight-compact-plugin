package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import dev.verloren.midnight.type.CompactUintType;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of a built-in primitive or parameterized type element wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#BUILTIN_TYPE}.
 * Represents native primitive and cryptographic types supported by the Midnight Compact language:
 * <ul>
 *   <li>{@code Boolean} &rarr; {@link CompactPrimitiveType#BOOLEAN}</li>
 *   <li>{@code Field} &rarr; {@link CompactPrimitiveType#FIELD}</li>
 *   <li>{@code Uint<N>} &rarr; {@link CompactUintType} (e.g. {@code Uint<8>}, {@code Uint<64>})</li>
 *   <li>{@code Bytes<N>}, {@code Vector<N, T>}, {@code Opaque<"tag">}</li>
 *   <li>Cryptographic curve scalar/base types: {@code JubjubScalar}, {@code Secp256k1Base}, {@code Secp256k1Scalar}</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during type checking and type normalization.</li>
 *   <li>Inspection and annotator pipelines validating parameter and return types.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only element; modifications occur via editor commands.</li>
 * </ul>
 *
 * @see CompactTypeElement
 * @see CompactPrimitiveType
 * @see CompactUintType
 */
public class CompactBuiltinTypeImpl extends CompactPsiElement implements CompactTypeElement {
  /**
   * Constructs a built-in type PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the built-in type
   */
  public CompactBuiltinTypeImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the canonical {@link CompactType} corresponding to this built-in type element.
   *
   * @return the resolved {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if unrecognized
   */
  @Override
  public @NotNull CompactType getType() {
    String text = getText();
    if ("Boolean".equals(text)) return CompactPrimitiveType.BOOLEAN;
    if ("Field".equals(text)) return CompactPrimitiveType.FIELD;
    if (text.startsWith("Uint")) {
      CompactUintType uintType = CompactUintType.parse(text);
      if (uintType != null) {
        return uintType;
      }
    }
    if (text.startsWith("Bytes") || text.startsWith("Vector") || text.startsWith("Opaque")
        || text.startsWith("JubjubScalar") || text.startsWith("Secp256k1Base") || text.startsWith("Secp256k1Scalar")) {
      return new CompactPrimitiveType(text);
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
