package dev.verloren.midnight.highlighter;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.colors.AttributesDescriptor;


/**
 * Central dictionary of {@link TextAttributesKey}s and {@link AttributesDescriptor}s for Compact syntax
 * and semantic highlighting.
 *
 * <p>Maps every meaningful Compact language construct to standard IntelliJ semantic highlight
 * attributes so that the user's active color scheme controls the visual presentation.</p>
 */
public final class CompactHighlighterColors {
  private CompactHighlighterColors() {}

  // =========================================================================
  // 1. Keywords
  // =========================================================================
  public static final TextAttributesKey KEYWORD =
      TextAttributesKey.createTextAttributesKey("COMPACT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey MODIFIER =
      TextAttributesKey.createTextAttributesKey("COMPACT_MODIFIER", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey RESERVED_KEYWORD =
      TextAttributesKey.createTextAttributesKey("COMPACT_RESERVED_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

  // =========================================================================
  // 2. Types & Type Parameters
  // =========================================================================
  public static final TextAttributesKey BUILTIN_TYPE =
      TextAttributesKey.createTextAttributesKey("COMPACT_BUILTIN_TYPE", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey STRUCT_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_STRUCT_DECLARATION", DefaultLanguageHighlighterColors.CLASS_NAME);
  public static final TextAttributesKey ENUM_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_ENUM_DECLARATION", DefaultLanguageHighlighterColors.CLASS_NAME);
  public static final TextAttributesKey TYPE_ALIAS_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_TYPE_ALIAS_DECLARATION", DefaultLanguageHighlighterColors.INTERFACE_NAME);
  public static final TextAttributesKey TYPE_PARAMETER =
      TextAttributesKey.createTextAttributesKey("COMPACT_TYPE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER);
  public static final TextAttributesKey TYPE_REFERENCE =
      TextAttributesKey.createTextAttributesKey("COMPACT_TYPE_REFERENCE", DefaultLanguageHighlighterColors.CLASS_REFERENCE);

  // =========================================================================
  // 3. Declarations
  // =========================================================================
  public static final TextAttributesKey CONTRACT_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_CONTRACT_DECLARATION", DefaultLanguageHighlighterColors.CLASS_NAME);
  public static final TextAttributesKey MODULE_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_MODULE_DECLARATION", DefaultLanguageHighlighterColors.IDENTIFIER);
  public static final TextAttributesKey CIRCUIT_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_CIRCUIT_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
  public static final TextAttributesKey WITNESS_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_WITNESS_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
  public static final TextAttributesKey CONSTRUCTOR_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_CONSTRUCTOR_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
  public static final TextAttributesKey FIELD_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_FIELD_DECLARATION", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
  public static final TextAttributesKey ENUM_MEMBER_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_ENUM_MEMBER_DECLARATION", DefaultLanguageHighlighterColors.STATIC_FIELD);
  public static final TextAttributesKey CONSTANT_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_CONSTANT_DECLARATION", DefaultLanguageHighlighterColors.CONSTANT);
  public static final TextAttributesKey LEDGER_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_LEDGER_DECLARATION", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);
  public static final TextAttributesKey PARAMETER_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_PARAMETER_DECLARATION", DefaultLanguageHighlighterColors.PARAMETER);
  public static final TextAttributesKey LOCAL_VARIABLE_DECLARATION =
      TextAttributesKey.createTextAttributesKey("COMPACT_LOCAL_VARIABLE_DECLARATION", DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
  public static final TextAttributesKey IMPORTED_SYMBOL =
      TextAttributesKey.createTextAttributesKey("COMPACT_IMPORTED_SYMBOL", DefaultLanguageHighlighterColors.IDENTIFIER);

  // =========================================================================
  // 4. Calls & Usages
  // =========================================================================
  public static final TextAttributesKey CIRCUIT_CALL =
      TextAttributesKey.createTextAttributesKey("COMPACT_CIRCUIT_CALL", DefaultLanguageHighlighterColors.FUNCTION_CALL);
  public static final TextAttributesKey WITNESS_CALL =
      TextAttributesKey.createTextAttributesKey("COMPACT_WITNESS_CALL", DefaultLanguageHighlighterColors.STATIC_METHOD);
  public static final TextAttributesKey BUILTIN_FUNCTION =
      TextAttributesKey.createTextAttributesKey("COMPACT_BUILTIN_FUNCTION", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
  public static final TextAttributesKey FIELD_ACCESS =
      TextAttributesKey.createTextAttributesKey("COMPACT_FIELD_ACCESS", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
  public static final TextAttributesKey ENUM_MEMBER_ACCESS =
      TextAttributesKey.createTextAttributesKey("COMPACT_ENUM_MEMBER_ACCESS", DefaultLanguageHighlighterColors.STATIC_FIELD);
  public static final TextAttributesKey CONSTANT_USAGE =
      TextAttributesKey.createTextAttributesKey("COMPACT_CONSTANT_USAGE", DefaultLanguageHighlighterColors.CONSTANT);
  public static final TextAttributesKey PARAMETER_USAGE =
      TextAttributesKey.createTextAttributesKey("COMPACT_PARAMETER_USAGE", DefaultLanguageHighlighterColors.PARAMETER);
  public static final TextAttributesKey LOCAL_VARIABLE_USAGE =
      TextAttributesKey.createTextAttributesKey("COMPACT_LOCAL_VARIABLE_USAGE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
  public static final TextAttributesKey LOCAL_VARIABLE_WRITE =
      TextAttributesKey.createTextAttributesKey("COMPACT_LOCAL_VARIABLE_WRITE", DefaultLanguageHighlighterColors.REASSIGNED_LOCAL_VARIABLE);
  public static final TextAttributesKey LEDGER_USAGE =
      TextAttributesKey.createTextAttributesKey("COMPACT_LEDGER_USAGE", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);
  public static final TextAttributesKey LEDGER_WRITE =
      TextAttributesKey.createTextAttributesKey("COMPACT_LEDGER_WRITE", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);

  // =========================================================================
  // 5. Literals
  // =========================================================================
  public static final TextAttributesKey NUMBER =
      TextAttributesKey.createTextAttributesKey("COMPACT_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
  public static final TextAttributesKey STRING =
      TextAttributesKey.createTextAttributesKey("COMPACT_STRING", DefaultLanguageHighlighterColors.STRING);
  public static final TextAttributesKey BOOLEAN =
      TextAttributesKey.createTextAttributesKey("COMPACT_BOOLEAN", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey VALID_STRING_ESCAPE =
      TextAttributesKey.createTextAttributesKey("COMPACT_VALID_STRING_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
  public static final TextAttributesKey INVALID_STRING_ESCAPE =
      TextAttributesKey.createTextAttributesKey("COMPACT_INVALID_STRING_ESCAPE", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
  public static final TextAttributesKey VERSION =
      TextAttributesKey.createTextAttributesKey("COMPACT_VERSION", DefaultLanguageHighlighterColors.NUMBER);

  // =========================================================================
  // 6. Comments
  // =========================================================================
  public static final TextAttributesKey LINE_COMMENT =
      TextAttributesKey.createTextAttributesKey("COMPACT_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
  public static final TextAttributesKey BLOCK_COMMENT =
      TextAttributesKey.createTextAttributesKey("COMPACT_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
  public static final TextAttributesKey DOC_COMMENT =
      TextAttributesKey.createTextAttributesKey("COMPACT_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);

  // =========================================================================
  // 7. Operators & Punctuation
  // =========================================================================
  public static final TextAttributesKey OPERATOR =
      TextAttributesKey.createTextAttributesKey("COMPACT_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
  public static final TextAttributesKey PARENTHESES =
      TextAttributesKey.createTextAttributesKey("COMPACT_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
  public static final TextAttributesKey BRACES =
      TextAttributesKey.createTextAttributesKey("COMPACT_BRACES", DefaultLanguageHighlighterColors.BRACES);
  public static final TextAttributesKey BRACKETS =
      TextAttributesKey.createTextAttributesKey("COMPACT_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
  public static final TextAttributesKey COMMA =
      TextAttributesKey.createTextAttributesKey("COMPACT_COMMA", DefaultLanguageHighlighterColors.COMMA);
  public static final TextAttributesKey SEMICOLON =
      TextAttributesKey.createTextAttributesKey("COMPACT_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
  public static final TextAttributesKey DOT =
      TextAttributesKey.createTextAttributesKey("COMPACT_DOT", DefaultLanguageHighlighterColors.DOT);
  public static final TextAttributesKey COLON =
      TextAttributesKey.createTextAttributesKey("COMPACT_COLON", DefaultLanguageHighlighterColors.DOT);

  // =========================================================================
  // 8. Pragmas & Bad Character
  // =========================================================================
  public static final TextAttributesKey PRAGMA =
      TextAttributesKey.createTextAttributesKey("COMPACT_PRAGMA", DefaultLanguageHighlighterColors.METADATA);
  public static final TextAttributesKey BAD_CHARACTER =
      TextAttributesKey.createTextAttributesKey("COMPACT_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

  // =========================================================================
  // Descriptors for Color Settings Page
  // =========================================================================
  public static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
      new AttributesDescriptor("Keywords//Keyword", KEYWORD),
      new AttributesDescriptor("Keywords//Modifier", MODIFIER),
      new AttributesDescriptor("Keywords//Reserved keyword", RESERVED_KEYWORD),

      new AttributesDescriptor("Types//Built-in type", BUILTIN_TYPE),
      new AttributesDescriptor("Types//Struct", STRUCT_DECLARATION),
      new AttributesDescriptor("Types//Enum", ENUM_DECLARATION),
      new AttributesDescriptor("Types//Type alias", TYPE_ALIAS_DECLARATION),
      new AttributesDescriptor("Types//Type parameter", TYPE_PARAMETER),
      new AttributesDescriptor("Types//Type reference", TYPE_REFERENCE),

      new AttributesDescriptor("Declarations//Contract", CONTRACT_DECLARATION),
      new AttributesDescriptor("Declarations//Module", MODULE_DECLARATION),
      new AttributesDescriptor("Declarations//Circuit", CIRCUIT_DECLARATION),
      new AttributesDescriptor("Declarations//Witness", WITNESS_DECLARATION),
      new AttributesDescriptor("Declarations//Constructor", CONSTRUCTOR_DECLARATION),
      new AttributesDescriptor("Declarations//Struct field", FIELD_DECLARATION),
      new AttributesDescriptor("Declarations//Enum member", ENUM_MEMBER_DECLARATION),
      new AttributesDescriptor("Declarations//Constant", CONSTANT_DECLARATION),
      new AttributesDescriptor("Declarations//Ledger state", LEDGER_DECLARATION),
      new AttributesDescriptor("Declarations//Parameter", PARAMETER_DECLARATION),
      new AttributesDescriptor("Declarations//Local variable", LOCAL_VARIABLE_DECLARATION),
      new AttributesDescriptor("Declarations//Imported symbol", IMPORTED_SYMBOL),

      new AttributesDescriptor("Calls & Usages//Circuit call", CIRCUIT_CALL),
      new AttributesDescriptor("Calls & Usages//Witness call", WITNESS_CALL),
      new AttributesDescriptor("Calls & Usages//Built-in function", BUILTIN_FUNCTION),
      new AttributesDescriptor("Calls & Usages//Struct field access", FIELD_ACCESS),
      new AttributesDescriptor("Calls & Usages//Enum member access", ENUM_MEMBER_ACCESS),
      new AttributesDescriptor("Calls & Usages//Constant usage", CONSTANT_USAGE),
      new AttributesDescriptor("Calls & Usages//Parameter usage", PARAMETER_USAGE),
      new AttributesDescriptor("Calls & Usages//Local variable usage", LOCAL_VARIABLE_USAGE),
      new AttributesDescriptor("Calls & Usages//Local variable write", LOCAL_VARIABLE_WRITE),
      new AttributesDescriptor("Calls & Usages//Ledger state usage", LEDGER_USAGE),
      new AttributesDescriptor("Calls & Usages//Ledger state write", LEDGER_WRITE),

      new AttributesDescriptor("Literals//Number", NUMBER),
      new AttributesDescriptor("Literals//String", STRING),
      new AttributesDescriptor("Literals//Boolean", BOOLEAN),
      new AttributesDescriptor("Literals//Escape sequence//Valid", VALID_STRING_ESCAPE),
      new AttributesDescriptor("Literals//Escape sequence//Invalid", INVALID_STRING_ESCAPE),
      new AttributesDescriptor("Literals//Version", VERSION),

      new AttributesDescriptor("Comments//Line comment", LINE_COMMENT),
      new AttributesDescriptor("Comments//Block comment", BLOCK_COMMENT),
      new AttributesDescriptor("Comments//Doc comment", DOC_COMMENT),

      new AttributesDescriptor("Operators & Punctuation//Operator", OPERATOR),
      new AttributesDescriptor("Operators & Punctuation//Parentheses", PARENTHESES),
      new AttributesDescriptor("Operators & Punctuation//Braces", BRACES),
      new AttributesDescriptor("Operators & Punctuation//Brackets", BRACKETS),
      new AttributesDescriptor("Operators & Punctuation//Comma", COMMA),
      new AttributesDescriptor("Operators & Punctuation//Semicolon", SEMICOLON),
      new AttributesDescriptor("Operators & Punctuation//Dot", DOT),
      new AttributesDescriptor("Operators & Punctuation//Colon", COLON),

      new AttributesDescriptor("Pragmas//Pragma directive", PRAGMA),
      new AttributesDescriptor("Bad character", BAD_CHARACTER)
  };
}
