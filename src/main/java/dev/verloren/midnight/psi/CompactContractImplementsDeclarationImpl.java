package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a contract implementation clause wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#IMPLEMENTS_DECLARATION}.
 * Implements {@link CompactContractImplementsDeclaration}:
 * <ul>
 *   <li><b>Type Reference Lookup:</b> Retrieves the nested {@link CompactTypeReferenceImpl} using
 *       {@link PsiTreeUtil#getChildOfType(PsiElement, Class)}.</li>
 *   <li><b>Interface Resolution:</b> Traverses the reference of the type reference node to find
 *       the declaration target, validating that it resolves to a {@link CompactExternalContractDeclaration}.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatches {@link CompactVisitor#visitContractImplementsDeclaration(CompactContractImplementsDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> All query and resolution methods require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only PSI node; AST replacements are initiated by refactoring operations.</li>
 * </ul>
 *
 * @see CompactContractImplementsDeclaration
 * @see CompactExternalContractDeclaration
 * @see CompactTypeReferenceImpl
 */
public class CompactContractImplementsDeclarationImpl extends CompactPsiElement implements CompactContractImplementsDeclaration {

  /**
   * Constructs an implements declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the implements clause
   */
  public CompactContractImplementsDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Retrieves the {@link CompactTypeReferenceImpl} specifying the implemented interface.
   *
   * @return the type reference child, or {@code null} if missing
   */
  @Override
  public @Nullable CompactTypeReferenceImpl getTypeReference() {
    return PsiTreeUtil.getChildOfType(this, CompactTypeReferenceImpl.class);
  }

  /**
   * Returns the textual representation of the implemented interface.
   *
   * @return interface name, or {@code null} if unparseable
   */
  @Override
  @SuppressWarnings("unused")
  public @Nullable String getInterfaceName() {
    CompactTypeReferenceImpl typeRef = getTypeReference();
    return typeRef != null ? typeRef.getText() : null;
  }

  /**
   * Resolves the referenced interface to its {@link CompactExternalContractDeclaration} target.
   *
   * @return resolved external contract declaration, or {@code null} if resolution fails
   */
  @Override
  public @Nullable CompactExternalContractDeclaration resolveInterface() {
    // Step 1: Obtain the type reference child element
    CompactTypeReferenceImpl typeRef = getTypeReference();
    if (typeRef != null && typeRef.getReference() != null) {
      // Step 2: Resolve the type reference target
      PsiElement resolved = typeRef.getReference().resolve();
      // Step 3: Verify the resolved symbol is indeed an external contract interface
      if (resolved instanceof CompactExternalContractDeclaration externalContract) {
        return externalContract;
      }
    }
    return null;
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitContractImplementsDeclaration(CompactContractImplementsDeclaration)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitContractImplementsDeclaration(this);
  }
}
