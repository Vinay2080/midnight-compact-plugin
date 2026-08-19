package dev.verloren.midnight.psi;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiNameIdentifierOwner;

/**
 * Common interface for all named declarations in the Compact PSI.
 *
 * <p>Extends IntelliJ's {@link PsiNameIdentifierOwner} and {@link NavigationItem},
 * making the element capable of being found by Find Usages, renamed via Refactoring,
 * navigated to from references, and queried for its inferred semantic type via {@link CompactTypeElement}.</p>
 */
public interface CompactNamedElement extends PsiNameIdentifierOwner, NavigationItem, CompactTypeElement {
}