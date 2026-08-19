package dev.verloren.midnight.highlighter;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import dev.verloren.midnight.icons.MidnightIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * Color Settings Page for Midnight Compact in IDE Settings (Settings -> Editor -> Color Scheme -> Compact).
 *
 * <p>Exposes all lexical and semantic syntax highlighting categories and provides a rich demo code snippet.</p>
 */
public class CompactColorSettingsPage implements ColorSettingsPage {

  private static final Map<String, TextAttributesKey> ADDITIONAL_TAGS = Map.ofEntries(
      Map.entry("modifier", CompactHighlighterColors.MODIFIER),
      Map.entry("circuit_decl", CompactHighlighterColors.CIRCUIT_DECLARATION),
      Map.entry("witness_decl", CompactHighlighterColors.WITNESS_DECLARATION),
      Map.entry("contract_decl", CompactHighlighterColors.CONTRACT_DECLARATION),
      Map.entry("module_decl", CompactHighlighterColors.MODULE_DECLARATION),
      Map.entry("struct_decl", CompactHighlighterColors.STRUCT_DECLARATION),
      Map.entry("enum_decl", CompactHighlighterColors.ENUM_DECLARATION),
      Map.entry("enum_member_decl", CompactHighlighterColors.ENUM_MEMBER_DECLARATION),
      Map.entry("field_decl", CompactHighlighterColors.FIELD_DECLARATION),
      Map.entry("type_alias_decl", CompactHighlighterColors.TYPE_ALIAS_DECLARATION),
      Map.entry("type_param", CompactHighlighterColors.TYPE_PARAMETER),
      Map.entry("ledger_decl", CompactHighlighterColors.LEDGER_DECLARATION),
      Map.entry("const_decl", CompactHighlighterColors.CONSTANT_DECLARATION),
      Map.entry("param_decl", CompactHighlighterColors.PARAMETER_DECLARATION),
      Map.entry("local_decl", CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION),
      Map.entry("import_symbol", CompactHighlighterColors.IMPORTED_SYMBOL),
      Map.entry("circuit_call", CompactHighlighterColors.CIRCUIT_CALL),
      Map.entry("witness_call", CompactHighlighterColors.WITNESS_CALL),
      Map.entry("builtin_fn", CompactHighlighterColors.BUILTIN_FUNCTION),
      Map.entry("field_access", CompactHighlighterColors.FIELD_ACCESS),
      Map.entry("enum_member_access", CompactHighlighterColors.ENUM_MEMBER_ACCESS),
      Map.entry("const_usage", CompactHighlighterColors.CONSTANT_USAGE),
      Map.entry("param_usage", CompactHighlighterColors.PARAMETER_USAGE),
      Map.entry("local_usage", CompactHighlighterColors.LOCAL_VARIABLE_USAGE),
      Map.entry("local_write", CompactHighlighterColors.LOCAL_VARIABLE_WRITE),
      Map.entry("ledger_usage", CompactHighlighterColors.LEDGER_USAGE),
      Map.entry("ledger_write", CompactHighlighterColors.LEDGER_WRITE),
      Map.entry("type_ref", CompactHighlighterColors.TYPE_REFERENCE),
      Map.entry("builtin_type", CompactHighlighterColors.BUILTIN_TYPE),
      Map.entry("escape_valid", CompactHighlighterColors.VALID_STRING_ESCAPE),
      Map.entry("escape_invalid", CompactHighlighterColors.INVALID_STRING_ESCAPE),
      Map.entry("doc_comment", CompactHighlighterColors.DOC_COMMENT),
      Map.entry("pragma", CompactHighlighterColors.PRAGMA),
      Map.entry("version", CompactHighlighterColors.VERSION)
  );

  @Override
  public @NotNull String getDisplayName() {
    return "Compact";
  }

  @Override
  public @Nullable Icon getIcon() {
    return MidnightIcons.FILE;
  }

  @Override
  public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
    return CompactHighlighterColors.DESCRIPTORS;
  }

  @Override
  public ColorDescriptor @NotNull [] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @Override
  public @NotNull SyntaxHighlighter getHighlighter() {
    return new CompactSyntaxHighlighter();
  }

  @Override
  public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return ADDITIONAL_TAGS;
  }

  @Override
  public @NotNull String getDemoText() {
    return """
        <pragma>pragma</pragma> <pragma>language_version</pragma> <version>^0.20.0</version>;

        <doc_comment>/// Smart contract for managing decentralized game sessions.</doc_comment>
        import { <import_symbol>Utils</import_symbol> } from './helpers';

        <modifier>export</modifier> enum <enum_decl>GameState</enum_decl> {
            <enum_member_decl>WAITING</enum_member_decl>,
            <enum_member_decl>PLAYING</enum_member_decl>,
            <enum_member_decl>FINISHED</enum_member_decl>
        }

        <modifier>export</modifier> struct <struct_decl>Player</struct_decl><<type_param>T</type_param>> {
            <field_decl>id</field_decl>: <builtin_type>Field</builtin_type>,
            <field_decl>score</field_decl>: <builtin_type>Uint</builtin_type><8>,
            <field_decl>state</field_decl>: <type_ref>GameState</type_ref>,
            <field_decl>tag</field_decl>: <type_param>T</type_param>
        }

        <modifier>export</modifier> type <type_alias_decl>PlayerId</type_alias_decl> = <builtin_type>Field</builtin_type>;

        <modifier>export</modifier> const <const_decl>MAX_PLAYERS</const_decl>: <builtin_type>Uint</builtin_type><8> = 4;

        <modifier>export</modifier> ledger <ledger_decl>totalPlayers</ledger_decl>: <builtin_type>Uint</builtin_type><8>;

        witness <witness_decl>getSecretSalt</witness_decl>(): <builtin_type>Bytes</builtin_type><32>;

        <modifier>export</modifier> circuit <circuit_decl>initGame</circuit_decl>(<param_decl>player</param_decl>: <struct_decl>Player</struct_decl><<builtin_type>Field</builtin_type>>): [] {
            const <local_decl>limit</local_decl> = <const_usage>MAX_PLAYERS</const_usage>;
            const <local_decl>salt</local_decl> = <witness_call>getSecretSalt</witness_call>();
            const <local_decl>currentShot</local_decl> = <builtin_fn>disclose</builtin_fn>(<param_usage>player</param_usage>.<field_access>id</field_access>);
            <ledger_write>totalPlayers</ledger_write> = <param_usage>player</param_usage>.<field_access>score</field_access>;

            <builtin_fn>assert</builtin_fn>(
                <param_usage>player</param_usage>.<field_access>state</field_access> == <enum_decl>GameState</enum_decl>.<enum_member_access>PLAYING</enum_member_access>,
                "Valid string escape: <escape_valid>\\n</escape_valid><escape_valid>\\t</escape_valid> Invalid: <escape_invalid>\\q</escape_invalid>"
            );
        }
        """;
  }
}
