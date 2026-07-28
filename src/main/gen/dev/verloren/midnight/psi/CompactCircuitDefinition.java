// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface CompactCircuitDefinition extends PsiElement {

  @NotNull
  CompactBlock getBlock();

  @Nullable
  CompactGparams getGparams();

  @NotNull
  CompactPatternParameterList getPatternParameterList();

  @NotNull
  CompactTypeExpression getTypeExpression();

}
