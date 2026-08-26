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
          Map.entry("constructor_decl", CompactHighlighterColors.CONSTRUCTOR_DECLARATION),
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
      Map.entry("doc_tag", CompactHighlighterColors.DOC_COMMENT_TAG),
      Map.entry("doc_tag_value", CompactHighlighterColors.DOC_COMMENT_TAG_VALUE),
      Map.entry("pragma", CompactHighlighterColors.PRAGMA),
          Map.entry("version", CompactHighlighterColors.VERSION),
          Map.entry("bad_char", CompactHighlighterColors.BAD_CHARACTER)
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
            // SPDX-License-Identifier: MIT
            // OpenZeppelin Compact Contracts (ShieldedToken.compact)
            <pragma>pragma</pragma> <pragma>language_version</pragma> <version>>= 0.23.0</version>;

            <doc_comment>/**
             * <doc_tag>@module</doc_tag> <doc_tag_value>ShieldedToken</doc_tag_value>
             * <doc_tag>@description</doc_tag> A privacy-preserving shielded token module.
             * <doc_tag>@notice</doc_tag> Utilizes zero-knowledge witness state and on-chain ledger circuits.
             */</doc_comment>
            <modifier>export</modifier> module <module_decl>ShieldedToken</module_decl> {
                import CompactStandardLibrary;
                import { <import_symbol>Utils</import_symbol> } from '../utils/Utils' prefix Utils_;

                // ─── Enums & Custom Types ──────────────────────────────────────────────
                <modifier>export</modifier> enum <enum_decl>UpdateType</enum_decl> {
                    <enum_member_decl>Grant</enum_member_decl>,
                    <enum_member_decl>Revoke</enum_member_decl>
                }

                <modifier>export</modifier> <modifier>new</modifier> type <type_alias_decl>RoleCommitment</type_alias_decl> = <builtin_type>Bytes</builtin_type><32>;
                <modifier>export</modifier> <modifier>new</modifier> type <type_alias_decl>AccountIdentifier</type_alias_decl> = <builtin_type>Bytes</builtin_type><32>;
        
                <modifier>export</modifier> struct <struct_decl>CoinInfo</struct_decl><<type_param>T</type_param>> {
                    <field_decl>color</field_decl>: <builtin_type>Field</builtin_type>,
                    <field_decl>value</field_decl>: <builtin_type>Uint</builtin_type><64>,
                    <field_decl>tag</field_decl>: <type_param>T</type_param>
                }

                <modifier>export</modifier> const <const_decl>MAX_SUPPLY</const_decl>: <builtin_type>Uint</builtin_type><64> = 1000000;
       
                // ─── Public & Sealed Ledger State ──────────────────────────────────────
                <modifier>export</modifier> <modifier>sealed</modifier> ledger <ledger_decl>_instanceSalt</ledger_decl>: <builtin_type>Bytes</builtin_type><32>;
                <modifier>export</modifier> ledger <ledger_decl>_totalSupply</ledger_decl>: <builtin_type>Uint</builtin_type><64>;
                <modifier>export</modifier> ledger <ledger_decl>_counter</ledger_decl>: <builtin_type>Counter</builtin_type>;
                <modifier>export</modifier> ledger <ledger_decl>_name</ledger_decl>: <builtin_type>Maybe</builtin_type><<builtin_type>Opaque</builtin_type><"string">>;
                <modifier>export</modifier> ledger <ledger_decl>_operatorRoles</ledger_decl>: <builtin_type>MerkleTree</builtin_type><20, <type_ref>RoleCommitment</type_ref>>;
        
                // ─── Witness Declarations (Private Off-Chain Callbacks) ────────────────
                witness <witness_decl>wit_secretKey</witness_decl>(): <builtin_type>Bytes</builtin_type><32>;
                witness <witness_decl>wit_getRolePath</witness_decl>(<param_decl>role</param_decl>: <type_ref>RoleCommitment</type_ref>): <builtin_type>MerkleTreePath</builtin_type><20, <type_ref>RoleCommitment</type_ref>>;
        
                // ─── Constructor ───────────────────────────────────────────────────────
                <constructor_decl>constructor</constructor_decl>(<param_decl>salt</param_decl>: <builtin_type>Bytes</builtin_type><32>, <param_decl>initialSupply</param_decl>: <builtin_type>Uint</builtin_type><64>) {
                    <ledger_write>_instanceSalt</ledger_write> = <builtin_fn>disclose</builtin_fn>(<param_usage>salt</param_usage>);
                    <ledger_write>_totalSupply</ledger_write> = <builtin_fn>disclose</builtin_fn>(<param_usage>initialSupply</param_usage>);
                    <ledger_write>_name</ledger_write> = <builtin_fn>some</builtin_fn><<builtin_type>Opaque</builtin_type><"string">>(<builtin_fn>pad</builtin_fn>(32, "ShieldedToken"));
                    <ledger_write>_counter</ledger_write>.increment(1);
                }
        
                // ─── Pure Circuit (Deterministic Off-Chain Prover) ─────────────────────
                <modifier>export</modifier> <modifier>pure</modifier> circuit <circuit_decl>DEFAULT_ADMIN_ROLE</circuit_decl>(): <type_ref>RoleCommitment</type_ref> {
                    return <builtin_fn>default</builtin_fn><<builtin_type>Bytes</builtin_type><32>> as <type_ref>RoleCommitment</type_ref>;
                }

                /**
                 * <doc_tag>@description</doc_tag> Mints tokens to recipient and generates coin commitment.
                 * <doc_tag>@param</doc_tag> <doc_tag_value>recipient</doc_tag_value> The account identifier
                 * <doc_tag>@param</doc_tag> <doc_tag_value>amount</doc_tag_value> The amount of tokens to mint
                 * <doc_tag>@return</doc_tag> The newly minted CoinInfo struct
                 */
                <modifier>export</modifier> circuit <circuit_decl>mint</circuit_decl>(<param_decl>recipient</param_decl>: <type_ref>AccountIdentifier</type_ref>, <param_decl>amount</param_decl>: <builtin_type>Uint</builtin_type><64>): <struct_decl>CoinInfo</struct_decl><<builtin_type>Field</builtin_type>> {
                    <builtin_fn>assert</builtin_fn>(<param_usage>amount</param_usage> > 0 && <param_usage>amount</param_usage> <= <const_usage>MAX_SUPPLY</const_usage>, "Invalid amount: <escape_valid>\\n</escape_valid><escape_valid>\\t</escape_valid><escape_invalid>\\q</escape_invalid>");

                    const <local_decl>sk</local_decl> = <witness_call>wit_secretKey</witness_call>();
                    const <local_decl>derivedHash</local_decl> = <builtin_fn>persistentHash</builtin_fn><<builtin_type>Vector</builtin_type><2, <builtin_type>Bytes</builtin_type><32>>>([<local_usage>sk</local_usage>, <ledger_usage>_instanceSalt</ledger_usage>]);
                    <ledger_write>_totalSupply</ledger_write> = <builtin_fn>disclose</builtin_fn>(<ledger_usage>_totalSupply</ledger_usage> + <param_usage>amount</param_usage>);

                    const <local_decl>coin</local_decl> = <struct_decl>CoinInfo</struct_decl> {
                        <field_decl>color</field_decl>: 0x1A2F,
                        <field_decl>value</field_decl>: <param_usage>amount</param_usage>,
                        <field_decl>tag</field_decl>: 12345
                    };

                    const <local_decl>coinValue</local_decl> = <local_usage>coin</local_usage>.<field_access>value</field_access>;
                    const <local_decl>status</local_decl> = <enum_decl>UpdateType</enum_decl>.<enum_member_access>Grant</enum_member_access>;
                    const <local_decl>admin</local_decl> = <circuit_call>DEFAULT_ADMIN_ROLE</circuit_call>();

                    let <local_decl>localCounter</local_decl> = 0b1010;
                    <local_write>localCounter</local_write> = <local_usage>localCounter</local_usage> + 1;
        
                    return <local_usage>coin</local_usage>;
                }
            }
        
            <modifier>export</modifier> contract <contract_decl>ShieldedTokenContract</contract_decl> <modifier>implements</modifier> <type_ref>ShieldedToken</type_ref> {
        }
        """;
  }
}
