package dev.verloren.midnight.parameterInfo;

import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Presentation descriptor representing the parameter or field list of a callable or struct literal
 * displayed in the Compact parameter info tooltip popup.
 */
public record CompactParametersDescription(
    @NotNull List<String> parameters,
    @NotNull List<String> fieldNames,
    @NotNull String presentableText,
    boolean isStructLiteral
) {

  public CompactParametersDescription(@NotNull List<String> parameters, boolean isStructLiteral) {
    this(parameters, Collections.emptyList(), isStructLiteral);
  }

  public CompactParametersDescription(
      @NotNull List<String> parameters,
      @NotNull List<String> fieldNames,
      boolean isStructLiteral
  ) {
    this(
        List.copyOf(parameters),
        List.copyOf(fieldNames),
        parameters.isEmpty() ? (isStructLiteral ? "<no fields>" : "<no parameters>") : String.join(", ", parameters),
        isStructLiteral
    );
  }

  /**
   * Compatibility accessor returning the parameter signatures as an array.
   */
  public String[] getParameters() {
    return parameters.toArray(new String[0]);
  }

  /**
   * Compatibility accessor returning the declared struct field names.
   */
  public List<String> getFieldNames() {
    return fieldNames;
  }

  /**
   * Compatibility accessor returning the formatted presentation text.
   */
  public String getPresentableText() {
    return presentableText;
  }

  public @NotNull TextRange getRange(int index) {
    if (index < 0 || index >= parameters.size()) {
      return TextRange.EMPTY_RANGE;
    }
    int start = 0;
    for (int i = 0; i < index; i++) {
      start += parameters.get(i).length() + 2; // account for comma and space: ","
    }
    return new TextRange(start, start + parameters.get(index).length());
  }

  public void updateUI(@NotNull ParameterInfoUIContext context) {
    int currentIndex = context.getCurrentParameterIndex();
    TextRange range = getRange(currentIndex);
    boolean isTooManyArguments = currentIndex >= parameters.size() && !parameters.isEmpty();
    boolean isDisabled = !context.isUIComponentEnabled() || isTooManyArguments;

    context.setupUIComponentPresentation(
        presentableText,
        range.getStartOffset(),
        range.getEndOffset(),
        isDisabled,
        false,
        false,
        context.getDefaultParameterColor()
    );
  }
}
