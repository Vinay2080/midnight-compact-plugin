package dev.verloren.midnight.ide.templates;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal registry of declaration types supported by Compact code generation and live templates.
 *
 * <p>Maps each syntactic declaration construct to its canonical lowercase base name
 * (e.g., {@code circuit} &rarr; {@code \"circuit\"}, {@code witness} &rarr; {@code \"witness\"}).
 * The registry is extensible to accommodate newly introduced declaration types.</p>
 */
public enum CompactDeclarationType {
  CIRCUIT("circuit"),
  WITNESS("witness"),
  STRUCT("struct"),
  ENUM("enum"),
  MODULE("module"),
  CONTRACT("contract"),
  TYPE("type"),
  LEDGER("ledger"),
  CONST("const");

  private final String baseName;

  CompactDeclarationType(@NotNull String baseName) {
    this.baseName = baseName;
  }

  public @NotNull String getBaseName() {
    return baseName;
  }

  private static final Map<String, String> CUSTOM_TYPES = new ConcurrentHashMap<>();

  /**
   * Registers a custom declaration type and base name for runtime extensibility.
   *
   * @param typeKey  case-insensitive key identifying the declaration type
   * @param baseName default base name used for numbering (e.g. {@code \"mydecl\"})
   */
  public static void registerCustomType(@NotNull String typeKey, @NotNull String baseName) {
    CUSTOM_TYPES.put(typeKey.toLowerCase(Locale.ROOT).trim(), baseName.toLowerCase(Locale.ROOT).trim());
  }

  /**
   * Resolves the canonical base name for a given declaration type key, keyword, or identifier.
   */
  public static @NotNull String resolveBaseName(@NotNull String typeOrKeyword) {
    String normalized = typeOrKeyword.toLowerCase(Locale.ROOT).trim();
    if (normalized.startsWith("export ")) {
      normalized = normalized.substring("export ".length()).trim();
    }
    CompactDeclarationType known = fromKeyword(normalized);
    if (known != null) {
      return known.getBaseName();
    }
    String custom = CUSTOM_TYPES.get(normalized);
    if (custom != null) {
      return custom;
    }
    return normalized;
  }

  /**
   * Resolves a known {@link CompactDeclarationType} from a keyword or template shortcut name.
   */
  public static @Nullable CompactDeclarationType fromKeyword(@Nullable String keyword) {
    if (keyword == null) {
      return null;
    }
    String normalized = keyword.toLowerCase(Locale.ROOT).trim();
    if (normalized.startsWith("export ")) {
      normalized = normalized.substring("export ".length()).trim();
    }
    return switch (normalized) {
      case "circuit", "cir" -> CIRCUIT;
      case "witness", "wit" -> WITNESS;
      case "struct", "str" -> STRUCT;
      case "enum", "en" -> ENUM;
      case "module", "mod" -> MODULE;
      case "contract", "cct", "ccti" -> CONTRACT;
      case "type" -> TYPE;
      case "ledger", "led", "ledg" -> LEDGER;
      case "const" -> CONST;
      default -> null;
    };
  }

  /**
   * Deduces the declaration type from a concrete PSI element.
   */
  public static @Nullable CompactDeclarationType fromPsi(@Nullable PsiElement element) {
    if (element == null) {
      return null;
    }
    if (element instanceof CompactCircuitDefinition || element instanceof CompactExternalCircuit) {
      return CIRCUIT;
    }
    if (element instanceof CompactWitnessDeclaration) {
      return WITNESS;
    }
    if (element instanceof CompactStructDefinition) {
      return STRUCT;
    }
    if (element instanceof CompactEnumDefinition) {
      return ENUM;
    }
    if (element instanceof CompactModuleDefinition) {
      return MODULE;
    }
    if (element instanceof CompactExternalContractDeclaration) {
      return CONTRACT;
    }
    if (element instanceof CompactTypeDefinition) {
      return TYPE;
    }
    if (element instanceof CompactLedgerDeclaration) {
      return LEDGER;
    }
    if (element instanceof CompactConstBindingImpl || (element.getNode() != null && element.getNode().getElementType() == CompactElementTypes.CONST_STATEMENT)) {
      return CONST;
    }
    return null;
  }

  /**
   * Deduces the declaration type from an AST composite {@link IElementType}.
   */
  public static @Nullable CompactDeclarationType fromElementType(@Nullable IElementType elementType) {
    if (elementType == null) {
      return null;
    }
    if (elementType == CompactElementTypes.CIRCUIT_DEFINITION || elementType == CompactElementTypes.EXTERNAL_CIRCUIT) {
      return CIRCUIT;
    }
    if (elementType == CompactElementTypes.WITNESS_DECLARATION) {
      return WITNESS;
    }
    if (elementType == CompactElementTypes.STRUCT_DECLARATION) {
      return STRUCT;
    }
    if (elementType == CompactElementTypes.ENUM_DECLARATION) {
      return ENUM;
    }
    if (elementType == CompactElementTypes.MODULE_DEFINITION) {
      return MODULE;
    }
    if (elementType == CompactElementTypes.CONTRACT_DECLARATION) {
      return CONTRACT;
    }
    if (elementType == CompactElementTypes.TYPE_ALIAS_DECLARATION) {
      return TYPE;
    }
    if (elementType == CompactElementTypes.LEDGER_DECLARATION) {
      return LEDGER;
    }
    if (elementType == CompactElementTypes.CONST_BINDING || elementType == CompactElementTypes.CONST_STATEMENT) {
      return CONST;
    }
    return null;
  }
}
