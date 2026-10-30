package dev.verloren.midnight.parameterInfo;

import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Presentation descriptor representing the parameter or field list of a callable or struct literal
 * displayed in the Compact parameter info tooltip popup.
 *
 * <h3>Subsystem Role &amp; Architecture</h3>
 * <p>Immutable record passed to IntelliJ's {@link ParameterInfoUIContext} by {@link CompactParameterInfoHandler}.
 * It encapsulates both the raw signature elements and the synthesized presentation text:
 * <ul>
 *   <li><b>Signature Elements:</b> Formatted strings describing each parameter (e.g. {@code amount: Uint<64>}).</li>
 *   <li><b>Struct Field Tracking:</b> Retains raw field names to support non-positional struct literal parameter matching.</li>
 *   <li><b>Presentation Text:</b> Precomputed comma-separated string (e.g. {@code "x: Field, y: Field"}) or fallback
 *       ({@code "<no parameters>"} / {@code "<no fields>"}).</li>
 *   <li><b>Text Range Highlighting:</b> Calculates precise substring character ranges ({@link #getRange})
 *       enabling IntelliJ to render the currently active parameter in bold font.</li>
 *   <li><b>Overflow Handling:</b> Disables or marks UI presentation when the caller passes more arguments
 *       than declared by the signature ({@link #updateUI}).</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Query and UI update methods are invoked on the Event Dispatch Thread (EDT)
 *       by the platform's tooltip popup renderer.</li>
 *   <li><b>Immutability:</b> All lists are defensively copied via {@link List#copyOf}, guaranteeing total immutability
 *       and thread safety.</li>
 * </ul>
 *
 * @param parameters      defensively copied list of formatted parameter strings (e.g. {@code name: Type})
 * @param fieldNames      defensively copied list of raw struct field names (empty for callable signatures)
 * @param presentableText synthesized comma-separated text displayed in the parameter popup
 * @param isStructLiteral true if describing a struct literal; false if describing a callable (circuit/witness/constructor)
 *
 * @see dev.verloren.midnight.parameterInfo.CompactParameterInfoHandler
 * @see com.intellij.lang.parameterInfo.ParameterInfoUIContext
 */
public record CompactParametersDescription(
    @NotNull List<String> parameters,
    @NotNull List<String> fieldNames,
    @NotNull String presentableText,
    boolean isStructLiteral
) {

  /**
   * Convenience constructor for callable parameter descriptions without explicit field name tracking.
   *
   * @param parameters      list of parameter signature strings
   * @param isStructLiteral true if describing a struct literal; false otherwise
   */
  public CompactParametersDescription(@NotNull List<String> parameters, boolean isStructLiteral) {
    this(parameters, Collections.emptyList(), isStructLiteral);
  }

  /**
   * Canonical normalizing constructor initializing defensive immutable copies and computing presentation text.
   *
   * @param parameters      list of formatted parameter strings
   * @param fieldNames      list of declared field names
   * @param isStructLiteral true if describing a struct literal; false otherwise
   */
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
   *
   * @return array of formatted parameter signature strings
   */
  public String[] getParameters() {
    return parameters.toArray(new String[0]);
  }

  /**
   * Compatibility accessor returning the declared struct field names.
   *
   * @return immutable list of field names
   */
  public List<String> getFieldNames() {
    return fieldNames;
  }

  /**
   * Compatibility accessor returning the formatted presentation text.
   *
   * @return presentation string displayed in the tooltip
   */
  public String getPresentableText() {
    return presentableText;
  }

  /**
   * Computes the precise character text range of the parameter at {@code index} within {@link #presentableText}.
   *
   * <p>Accounts for the length of preceding parameter strings and the separating two-character delimiters ({@code ", "}).</p>
   *
   * @param index zero-based parameter index
   * @return {@link TextRange} of the parameter substring, or {@link TextRange#EMPTY_RANGE} if the index is out of bounds
   */
  public @NotNull TextRange getRange(int index) {
    // Step 1: Guard against out-of-bounds indices
    if (index < 0 || index >= parameters.size()) {
      return TextRange.EMPTY_RANGE;
    }
    // Step 2: Sum lengths of all preceding parameters plus comma-space delimiters (2 chars)
    int start = 0;
    for (int i = 0; i < index; i++) {
      start += parameters.get(i).length() + 2; // account for comma and space: ","
    }
    // Step 3: Return range encompassing the active parameter substring
    return new TextRange(start, start + parameters.get(index).length());
  }

  /**
   * Configures the IntelliJ tooltip UI presentation component with highlighting and styling.
   *
   * @param context the platform UI context
   */
  public void updateUI(@NotNull ParameterInfoUIContext context) {
    // Step 1: Query active parameter index from context
    int currentIndex = context.getCurrentParameterIndex();
    TextRange range = getRange(currentIndex);

    // Step 2: Detect argument count overflow (caller passed more arguments than declared)
    boolean isTooManyArguments = currentIndex >= parameters.size() && !parameters.isEmpty();
    boolean isDisabled = !context.isUIComponentEnabled() || isTooManyArguments;

    // Step 3: Configure platform UI presentation with range bounds and colour
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
