package dev.verloren.midnight.documentation.docstring;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactImportDeclarationImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Standard library documentation provider for Compact builtin contracts and libraries.
 */
public final class CompactStandardLibraryDoc {

  private CompactStandardLibraryDoc() {}

  public static @Nullable String getStandardLibraryDescription(@NotNull PsiElement element) {
    if (element instanceof CompactImportDeclarationImpl importDecl && "CompactStandardLibrary".equals(importDecl.getModuleName())) {
      return "<p>The official standard zero-knowledge utility library for the Midnight Compact language.</p>"
          + "<p>Provides core algebraic data types (<code>Maybe&lt;T&gt;</code>, <code>Either&lt;A, B&gt;</code>), Merkle tree path verification, cryptographic commitments and hashing, and Midnight kernel shielded token operations.</p>";
    }
    if (element instanceof CompactFile file) {
      if ("standard-library.compact".equals(file.getName())) {
        return "<p><b>Compact Standard Library</b> (<code>standard-library.compact</code>)</p>"
            + "<p>The official standard zero-knowledge utility library for the Midnight Compact language.</p>"
            + "<p>Provides core algebraic data types (<code>Maybe&lt;T&gt;</code>, <code>Either&lt;A, B&gt;</code>), Merkle tree path verification, cryptographic commitments and hashing, and Midnight kernel shielded token operations.</p>";
      }
      if ("zkir-v3-library.compact".equals(file.getName())) {
        return "<p><b>ZKIR v3 Library</b> (<code>zkir-v3-library.compact</code>)</p>"
            + "<p>Low-level zero-knowledge intermediate representation library containing Secp256k1 elliptic curve verification and scalar primitives.</p>";
      }
    }
    return null;
  }
}
