package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface CompactContractImplementsDeclaration extends PsiElement {

  /**
   * Returns the type reference child specifying the implemented interface.
   */
  @Nullable CompactTypeReferenceImpl getTypeReference();

  /**
   * Returns the textual identifier of the implemented contract interface.
   */
  @SuppressWarnings("unused")
  @Nullable String getInterfaceName();

  /**
   * Resolves this implements declaration to the corresponding {@link CompactExternalContractDeclaration}.
   */
  @Nullable CompactExternalContractDeclaration resolveInterface();
}
