// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static dev.verloren.midnight.psi.CompactTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class CompactParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return root(b, l + 1);
  }

  /* ********************************************************** */
  // LPAREN (optionally_typed_pattern (COMMA optionally_typed_pattern)* COMMA?)? RPAREN
  public static boolean arrow_parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrow_parameter_list")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && arrow_parameter_list_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, ARROW_PARAMETER_LIST, r);
    return r;
  }

  // (optionally_typed_pattern (COMMA optionally_typed_pattern)* COMMA?)?
  private static boolean arrow_parameter_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrow_parameter_list_1")) return false;
    arrow_parameter_list_1_0(b, l + 1);
    return true;
  }

  // optionally_typed_pattern (COMMA optionally_typed_pattern)* COMMA?
  private static boolean arrow_parameter_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrow_parameter_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = optionally_typed_pattern(b, l + 1);
    r = r && arrow_parameter_list_1_0_1(b, l + 1);
    r = r && arrow_parameter_list_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA optionally_typed_pattern)*
  private static boolean arrow_parameter_list_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrow_parameter_list_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!arrow_parameter_list_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "arrow_parameter_list_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA optionally_typed_pattern
  private static boolean arrow_parameter_list_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrow_parameter_list_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && optionally_typed_pattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean arrow_parameter_list_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrow_parameter_list_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // LBRACE stmt* RBRACE
  public static boolean block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && block_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, BLOCK, r);
    return r;
  }

  // stmt*
  private static boolean block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!stmt(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // optionally_typed_pattern ASSIGN expr
  public static boolean cbinding(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cbinding")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CBINDING, "<cbinding>");
    r = optionally_typed_pattern(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && expr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // EXPORT? PURE? CIRCUIT IDENTIFIER gparams? pattern_parameter_list COLON type_expression block
  public static boolean circuit_definition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "circuit_definition")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CIRCUIT_DEFINITION, "<circuit definition>");
    r = circuit_definition_0(b, l + 1);
    r = r && circuit_definition_1(b, l + 1);
    r = r && consumeTokens(b, 0, CIRCUIT, IDENTIFIER);
    r = r && circuit_definition_4(b, l + 1);
    r = r && pattern_parameter_list(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && type_expression(b, l + 1);
    r = r && block(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPORT?
  private static boolean circuit_definition_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "circuit_definition_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // PURE?
  private static boolean circuit_definition_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "circuit_definition_1")) return false;
    consumeToken(b, PURE);
    return true;
  }

  // gparams?
  private static boolean circuit_definition_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "circuit_definition_4")) return false;
    gparams(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // CONSTRUCTOR pattern_parameter_list block
  public static boolean constructor_definition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constructor_definition")) return false;
    if (!nextTokenIs(b, CONSTRUCTOR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CONSTRUCTOR);
    r = r && pattern_parameter_list(b, l + 1);
    r = r && block(b, l + 1);
    exit_section_(b, m, CONSTRUCTOR_DEFINITION, r);
    return r;
  }

  /* ********************************************************** */
  // EXPORT? CONTRACT IDENTIFIER LBRACE (external_contract_circuit (SEMICOLON external_contract_circuit)* SEMICOLON?)? RBRACE SEMICOLON?   // contract-declaration/semicolons
  //   | EXPORT? CONTRACT IDENTIFIER LBRACE (external_contract_circuit (COMMA    external_contract_circuit)* COMMA?   )? RBRACE SEMICOLON?
  public static boolean contract_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration")) return false;
    if (!nextTokenIs(b, "<contract declaration>", CONTRACT, EXPORT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONTRACT_DECLARATION, "<contract declaration>");
    r = contract_declaration_0(b, l + 1);
    if (!r) r = contract_declaration_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPORT? CONTRACT IDENTIFIER LBRACE (external_contract_circuit (SEMICOLON external_contract_circuit)* SEMICOLON?)? RBRACE SEMICOLON?
  private static boolean contract_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = contract_declaration_0_0(b, l + 1);
    r = r && consumeTokens(b, 0, CONTRACT, IDENTIFIER, LBRACE);
    r = r && contract_declaration_0_4(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    r = r && contract_declaration_0_6(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // EXPORT?
  private static boolean contract_declaration_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_0_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // (external_contract_circuit (SEMICOLON external_contract_circuit)* SEMICOLON?)?
  private static boolean contract_declaration_0_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_0_4")) return false;
    contract_declaration_0_4_0(b, l + 1);
    return true;
  }

  // external_contract_circuit (SEMICOLON external_contract_circuit)* SEMICOLON?
  private static boolean contract_declaration_0_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_0_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = external_contract_circuit(b, l + 1);
    r = r && contract_declaration_0_4_0_1(b, l + 1);
    r = r && contract_declaration_0_4_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (SEMICOLON external_contract_circuit)*
  private static boolean contract_declaration_0_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_0_4_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!contract_declaration_0_4_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "contract_declaration_0_4_0_1", c)) break;
    }
    return true;
  }

  // SEMICOLON external_contract_circuit
  private static boolean contract_declaration_0_4_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_0_4_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SEMICOLON);
    r = r && external_contract_circuit(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // SEMICOLON?
  private static boolean contract_declaration_0_4_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_0_4_0_2")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  // SEMICOLON?
  private static boolean contract_declaration_0_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_0_6")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  // EXPORT? CONTRACT IDENTIFIER LBRACE (external_contract_circuit (COMMA    external_contract_circuit)* COMMA?   )? RBRACE SEMICOLON?
  private static boolean contract_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = contract_declaration_1_0(b, l + 1);
    r = r && consumeTokens(b, 0, CONTRACT, IDENTIFIER, LBRACE);
    r = r && contract_declaration_1_4(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    r = r && contract_declaration_1_6(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // EXPORT?
  private static boolean contract_declaration_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_1_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // (external_contract_circuit (COMMA    external_contract_circuit)* COMMA?   )?
  private static boolean contract_declaration_1_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_1_4")) return false;
    contract_declaration_1_4_0(b, l + 1);
    return true;
  }

  // external_contract_circuit (COMMA    external_contract_circuit)* COMMA?
  private static boolean contract_declaration_1_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_1_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = external_contract_circuit(b, l + 1);
    r = r && contract_declaration_1_4_0_1(b, l + 1);
    r = r && contract_declaration_1_4_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA    external_contract_circuit)*
  private static boolean contract_declaration_1_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_1_4_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!contract_declaration_1_4_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "contract_declaration_1_4_0_1", c)) break;
    }
    return true;
  }

  // COMMA    external_contract_circuit
  private static boolean contract_declaration_1_4_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_1_4_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && external_contract_circuit(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean contract_declaration_1_4_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_1_4_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // SEMICOLON?
  private static boolean contract_declaration_1_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "contract_declaration_1_6")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // EXPORT? ENUM IDENTIFIER LBRACE IDENTIFIER (COMMA IDENTIFIER)* COMMA? RBRACE SEMICOLON?
  public static boolean enum_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_declaration")) return false;
    if (!nextTokenIs(b, "<enum declaration>", ENUM, EXPORT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENUM_DECLARATION, "<enum declaration>");
    r = enum_declaration_0(b, l + 1);
    r = r && consumeTokens(b, 0, ENUM, IDENTIFIER, LBRACE, IDENTIFIER);
    r = r && enum_declaration_5(b, l + 1);
    r = r && enum_declaration_6(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    r = r && enum_declaration_8(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPORT?
  private static boolean enum_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_declaration_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // (COMMA IDENTIFIER)*
  private static boolean enum_declaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_declaration_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!enum_declaration_5_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "enum_declaration_5", c)) break;
    }
    return true;
  }

  // COMMA IDENTIFIER
  private static boolean enum_declaration_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_declaration_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, COMMA, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean enum_declaration_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_declaration_6")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // SEMICOLON?
  private static boolean enum_declaration_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_declaration_8")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // EXPORT LBRACE (IDENTIFIER (COMMA IDENTIFIER)* COMMA?)? RBRACE SEMICOLON?
  public static boolean export_form(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "export_form")) return false;
    if (!nextTokenIs(b, EXPORT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, EXPORT, LBRACE);
    r = r && export_form_2(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    r = r && export_form_4(b, l + 1);
    exit_section_(b, m, EXPORT_FORM, r);
    return r;
  }

  // (IDENTIFIER (COMMA IDENTIFIER)* COMMA?)?
  private static boolean export_form_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "export_form_2")) return false;
    export_form_2_0(b, l + 1);
    return true;
  }

  // IDENTIFIER (COMMA IDENTIFIER)* COMMA?
  private static boolean export_form_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "export_form_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    r = r && export_form_2_0_1(b, l + 1);
    r = r && export_form_2_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA IDENTIFIER)*
  private static boolean export_form_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "export_form_2_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!export_form_2_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "export_form_2_0_1", c)) break;
    }
    return true;
  }

  // COMMA IDENTIFIER
  private static boolean export_form_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "export_form_2_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, COMMA, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean export_form_2_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "export_form_2_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // SEMICOLON?
  private static boolean export_form_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "export_form_4")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // expr0 QUESTION expr COLON expr   // conditional expression  (ternary)
  //   | expr0 ASSIGN      expr           // assignment expression   (=)
  //   | expr0 PLUS_ASSIGN expr           // increment expression    (+=)
  //   | expr0 MINUS_ASSIGN expr          // decrement expression    (-=)
  //   | expr0
  public static boolean expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR, "<expr>");
    r = expr_0(b, l + 1);
    if (!r) r = expr_1(b, l + 1);
    if (!r) r = expr_2(b, l + 1);
    if (!r) r = expr_3(b, l + 1);
    if (!r) r = expr0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // expr0 QUESTION expr COLON expr
  private static boolean expr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr0(b, l + 1);
    r = r && consumeToken(b, QUESTION);
    r = r && expr(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr0 ASSIGN      expr
  private static boolean expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr0(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr0 PLUS_ASSIGN expr
  private static boolean expr_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr0(b, l + 1);
    r = r && consumeToken(b, PLUS_ASSIGN);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr0 MINUS_ASSIGN expr
  private static boolean expr_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr0(b, l + 1);
    r = r && consumeToken(b, MINUS_ASSIGN);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expr1 (OROR expr1)*
  static boolean expr0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr1(b, l + 1);
    r = r && expr0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (OROR expr1)*
  private static boolean expr0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr0_1", c)) break;
    }
    return true;
  }

  // OROR expr1
  private static boolean expr0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OROR);
    r = r && expr1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expr2 (ANDAND expr2)*
  static boolean expr1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr2(b, l + 1);
    r = r && expr1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ANDAND expr2)*
  private static boolean expr1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr1_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr1_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr1_1", c)) break;
    }
    return true;
  }

  // ANDAND expr2
  private static boolean expr1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr1_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ANDAND);
    r = r && expr2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // MAP LPAREN fun COMMA expr (COMMA expr)* COMMA? RPAREN                        // term-map
  //          | FOLD LPAREN fun COMMA expr COMMA expr (COMMA expr)* COMMA? RPAREN            // term-fold
  //          | SLICE LT tsize GT LPAREN expr COMMA expr RPAREN                              // term-slice
  //          | LBRACKET (tuple_arg (COMMA tuple_arg)* COMMA?)? RBRACKET                     // term-tuple
  //          | BYTES_TYPE LBRACKET (tuple_arg (COMMA tuple_arg)* COMMA?)? RBRACKET          // term-bytes
  //          | tref LBRACE (struct_arg (COMMA struct_arg)* COMMA?)? RBRACE                  // term-struct
  //          | ASSERT LPAREN expr COMMA STRING_LITERAL RPAREN                               // term-assert
  //          | EMIT LPAREN expr RPAREN                                                      // term-emit
  //          | DISCLOSE LPAREN expr RPAREN                                                  // term-disclose
  //          | term
  public static boolean expr10(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR_10, "<expr 10>");
    r = expr10_0(b, l + 1);
    if (!r) r = expr10_1(b, l + 1);
    if (!r) r = expr10_2(b, l + 1);
    if (!r) r = expr10_3(b, l + 1);
    if (!r) r = expr10_4(b, l + 1);
    if (!r) r = expr10_5(b, l + 1);
    if (!r) r = expr10_6(b, l + 1);
    if (!r) r = expr10_7(b, l + 1);
    if (!r) r = expr10_8(b, l + 1);
    if (!r) r = term(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // MAP LPAREN fun COMMA expr (COMMA expr)* COMMA? RPAREN
  private static boolean expr10_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, MAP, LPAREN);
    r = r && fun(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    r = r && expr10_0_5(b, l + 1);
    r = r && expr10_0_6(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA expr)*
  private static boolean expr10_0_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_0_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr10_0_5_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr10_0_5", c)) break;
    }
    return true;
  }

  // COMMA expr
  private static boolean expr10_0_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_0_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean expr10_0_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_0_6")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // FOLD LPAREN fun COMMA expr COMMA expr (COMMA expr)* COMMA? RPAREN
  private static boolean expr10_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, FOLD, LPAREN);
    r = r && fun(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    r = r && expr10_1_7(b, l + 1);
    r = r && expr10_1_8(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA expr)*
  private static boolean expr10_1_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_1_7")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr10_1_7_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr10_1_7", c)) break;
    }
    return true;
  }

  // COMMA expr
  private static boolean expr10_1_7_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_1_7_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean expr10_1_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_1_8")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // SLICE LT tsize GT LPAREN expr COMMA expr RPAREN
  private static boolean expr10_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, SLICE, LT);
    r = r && tsize(b, l + 1);
    r = r && consumeTokens(b, 0, GT, LPAREN);
    r = r && expr(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // LBRACKET (tuple_arg (COMMA tuple_arg)* COMMA?)? RBRACKET
  private static boolean expr10_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACKET);
    r = r && expr10_3_1(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // (tuple_arg (COMMA tuple_arg)* COMMA?)?
  private static boolean expr10_3_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_3_1")) return false;
    expr10_3_1_0(b, l + 1);
    return true;
  }

  // tuple_arg (COMMA tuple_arg)* COMMA?
  private static boolean expr10_3_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_3_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = tuple_arg(b, l + 1);
    r = r && expr10_3_1_0_1(b, l + 1);
    r = r && expr10_3_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA tuple_arg)*
  private static boolean expr10_3_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_3_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr10_3_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr10_3_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA tuple_arg
  private static boolean expr10_3_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_3_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && tuple_arg(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean expr10_3_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_3_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // BYTES_TYPE LBRACKET (tuple_arg (COMMA tuple_arg)* COMMA?)? RBRACKET
  private static boolean expr10_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, BYTES_TYPE, LBRACKET);
    r = r && expr10_4_2(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // (tuple_arg (COMMA tuple_arg)* COMMA?)?
  private static boolean expr10_4_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_4_2")) return false;
    expr10_4_2_0(b, l + 1);
    return true;
  }

  // tuple_arg (COMMA tuple_arg)* COMMA?
  private static boolean expr10_4_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_4_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = tuple_arg(b, l + 1);
    r = r && expr10_4_2_0_1(b, l + 1);
    r = r && expr10_4_2_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA tuple_arg)*
  private static boolean expr10_4_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_4_2_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr10_4_2_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr10_4_2_0_1", c)) break;
    }
    return true;
  }

  // COMMA tuple_arg
  private static boolean expr10_4_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_4_2_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && tuple_arg(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean expr10_4_2_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_4_2_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // tref LBRACE (struct_arg (COMMA struct_arg)* COMMA?)? RBRACE
  private static boolean expr10_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_5")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = tref(b, l + 1);
    r = r && consumeToken(b, LBRACE);
    r = r && expr10_5_2(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // (struct_arg (COMMA struct_arg)* COMMA?)?
  private static boolean expr10_5_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_5_2")) return false;
    expr10_5_2_0(b, l + 1);
    return true;
  }

  // struct_arg (COMMA struct_arg)* COMMA?
  private static boolean expr10_5_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_5_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = struct_arg(b, l + 1);
    r = r && expr10_5_2_0_1(b, l + 1);
    r = r && expr10_5_2_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA struct_arg)*
  private static boolean expr10_5_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_5_2_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr10_5_2_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr10_5_2_0_1", c)) break;
    }
    return true;
  }

  // COMMA struct_arg
  private static boolean expr10_5_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_5_2_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && struct_arg(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean expr10_5_2_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_5_2_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // ASSERT LPAREN expr COMMA STRING_LITERAL RPAREN
  private static boolean expr10_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, ASSERT, LPAREN);
    r = r && expr(b, l + 1);
    r = r && consumeTokens(b, 0, COMMA, STRING_LITERAL, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // EMIT LPAREN expr RPAREN
  private static boolean expr10_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_7")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, EMIT, LPAREN);
    r = r && expr(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // DISCLOSE LPAREN expr RPAREN
  private static boolean expr10_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr10_8")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, DISCLOSE, LPAREN);
    r = r && expr(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expr3 ((EQEQ | NOTEQ) expr3)*
  static boolean expr2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr3(b, l + 1);
    r = r && expr2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ((EQEQ | NOTEQ) expr3)*
  private static boolean expr2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr2_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr2_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr2_1", c)) break;
    }
    return true;
  }

  // (EQEQ | NOTEQ) expr3
  private static boolean expr2_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr2_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr2_1_0_0(b, l + 1);
    r = r && expr3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // EQEQ | NOTEQ
  private static boolean expr2_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr2_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, EQEQ);
    if (!r) r = consumeToken(b, NOTEQ);
    return r;
  }

  /* ********************************************************** */
  // expr4 LT  expr4   // <  expression  (non-associative)
  //         | expr4 LTE expr4   // <= expression  (non-associative)
  //         | expr4 GTE expr4   // >= expression  (non-associative)
  //         | expr4 GT  expr4   // >  expression  (non-associative)
  //         | expr4
  public static boolean expr3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr3")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR_3, "<expr 3>");
    r = expr3_0(b, l + 1);
    if (!r) r = expr3_1(b, l + 1);
    if (!r) r = expr3_2(b, l + 1);
    if (!r) r = expr3_3(b, l + 1);
    if (!r) r = expr4(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // expr4 LT  expr4
  private static boolean expr3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr4(b, l + 1);
    r = r && consumeToken(b, LT);
    r = r && expr4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr4 LTE expr4
  private static boolean expr3_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr3_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr4(b, l + 1);
    r = r && consumeToken(b, LTE);
    r = r && expr4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr4 GTE expr4
  private static boolean expr3_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr3_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr4(b, l + 1);
    r = r && consumeToken(b, GTE);
    r = r && expr4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr4 GT  expr4
  private static boolean expr3_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr3_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr4(b, l + 1);
    r = r && consumeToken(b, GT);
    r = r && expr4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expr5 (AS type_expression)*
  static boolean expr4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr5(b, l + 1);
    r = r && expr4_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (AS type_expression)*
  private static boolean expr4_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr4_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr4_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr4_1", c)) break;
    }
    return true;
  }

  // AS type_expression
  private static boolean expr4_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr4_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AS);
    r = r && type_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expr6 ((PLUS | MINUS) expr6)*
  static boolean expr5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr5")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr6(b, l + 1);
    r = r && expr5_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ((PLUS | MINUS) expr6)*
  private static boolean expr5_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr5_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr5_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr5_1", c)) break;
    }
    return true;
  }

  // (PLUS | MINUS) expr6
  private static boolean expr5_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr5_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr5_1_0_0(b, l + 1);
    r = r && expr6(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // PLUS | MINUS
  private static boolean expr5_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr5_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    return r;
  }

  /* ********************************************************** */
  // expr7 (STAR expr7)*
  static boolean expr6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr7(b, l + 1);
    r = r && expr6_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (STAR expr7)*
  private static boolean expr6_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr6_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr6_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr6_1", c)) break;
    }
    return true;
  }

  // STAR expr7
  private static boolean expr6_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr6_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, STAR);
    r = r && expr7(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // NOT expr7           // not expression (!)
  //         | expr8
  public static boolean expr7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr7")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR_7, "<expr 7>");
    r = expr7_0(b, l + 1);
    if (!r) r = expr8(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // NOT expr7
  private static boolean expr7_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr7_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NOT);
    r = r && expr7(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expr8 LBRACKET expr RBRACKET                                // tuple/vector reference  (expr[expr])
  //         | expr8 DOT IDENTIFIER                                        // element reference       (expr.id)
  //         | expr8 DOT IDENTIFIER LPAREN (expr (COMMA expr)* COMMA?)? RPAREN   // element call            (expr.id(expr, ...))
  //         | expr9
  public static boolean expr8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR_8, "<expr 8>");
    r = expr8_0(b, l + 1);
    if (!r) r = expr8_1(b, l + 1);
    if (!r) r = expr8_2(b, l + 1);
    if (!r) r = expr9(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // expr8 LBRACKET expr RBRACKET
  private static boolean expr8_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr8(b, l + 1);
    r = r && consumeToken(b, LBRACKET);
    r = r && expr(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr8 DOT IDENTIFIER
  private static boolean expr8_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr8(b, l + 1);
    r = r && consumeTokens(b, 0, DOT, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr8 DOT IDENTIFIER LPAREN (expr (COMMA expr)* COMMA?)? RPAREN
  private static boolean expr8_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr8(b, l + 1);
    r = r && consumeTokens(b, 0, DOT, IDENTIFIER, LPAREN);
    r = r && expr8_2_4(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (expr (COMMA expr)* COMMA?)?
  private static boolean expr8_2_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8_2_4")) return false;
    expr8_2_4_0(b, l + 1);
    return true;
  }

  // expr (COMMA expr)* COMMA?
  private static boolean expr8_2_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8_2_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr(b, l + 1);
    r = r && expr8_2_4_0_1(b, l + 1);
    r = r && expr8_2_4_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA expr)*
  private static boolean expr8_2_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8_2_4_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr8_2_4_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr8_2_4_0_1", c)) break;
    }
    return true;
  }

  // COMMA expr
  private static boolean expr8_2_4_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8_2_4_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean expr8_2_4_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr8_2_4_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // fun LPAREN (expr (COMMA expr)* COMMA?)? RPAREN              // term-call               (fun(expr, ...))
  //         | expr10
  public static boolean expr9(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr9")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR_9, "<expr 9>");
    r = expr9_0(b, l + 1);
    if (!r) r = expr10(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // fun LPAREN (expr (COMMA expr)* COMMA?)? RPAREN
  private static boolean expr9_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr9_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = fun(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && expr9_0_2(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (expr (COMMA expr)* COMMA?)?
  private static boolean expr9_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr9_0_2")) return false;
    expr9_0_2_0(b, l + 1);
    return true;
  }

  // expr (COMMA expr)* COMMA?
  private static boolean expr9_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr9_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr(b, l + 1);
    r = r && expr9_0_2_0_1(b, l + 1);
    r = r && expr9_0_2_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA expr)*
  private static boolean expr9_0_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr9_0_2_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr9_0_2_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr9_0_2_0_1", c)) break;
    }
    return true;
  }

  // COMMA expr
  private static boolean expr9_0_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr9_0_2_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean expr9_0_2_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr9_0_2_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // expr (COMMA expr)+   // expr-seq-many  (≥ 2 expressions)
  //            | expr
  public static boolean expr_seq(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_seq")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR_SEQ, "<expr seq>");
    r = expr_seq_0(b, l + 1);
    if (!r) r = expr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // expr (COMMA expr)+
  private static boolean expr_seq_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_seq_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr(b, l + 1);
    r = r && expr_seq_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA expr)+
  private static boolean expr_seq_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_seq_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr_seq_0_1_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!expr_seq_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expr_seq_0_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA expr
  private static boolean expr_seq_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_seq_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PURE? CIRCUIT IDENTIFIER simple_parameter_list COLON type_expression
  public static boolean external_contract_circuit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "external_contract_circuit")) return false;
    if (!nextTokenIs(b, "<external contract circuit>", CIRCUIT, PURE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXTERNAL_CONTRACT_CIRCUIT, "<external contract circuit>");
    r = external_contract_circuit_0(b, l + 1);
    r = r && consumeTokens(b, 0, CIRCUIT, IDENTIFIER);
    r = r && simple_parameter_list(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && type_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // PURE?
  private static boolean external_contract_circuit_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "external_contract_circuit_0")) return false;
    consumeToken(b, PURE);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER gargs?                                                       // function-ref
  //       | arrow_parameter_list return_type? ARROW block                           // function-arrow-block
  //       | arrow_parameter_list return_type? ARROW expr                            // function-arrow-expr
  //       | LPAREN fun RPAREN
  public static boolean fun(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fun")) return false;
    if (!nextTokenIs(b, "<fun>", IDENTIFIER, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUN, "<fun>");
    r = fun_0(b, l + 1);
    if (!r) r = fun_1(b, l + 1);
    if (!r) r = fun_2(b, l + 1);
    if (!r) r = fun_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IDENTIFIER gargs?
  private static boolean fun_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fun_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    r = r && fun_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // gargs?
  private static boolean fun_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fun_0_1")) return false;
    gargs(b, l + 1);
    return true;
  }

  // arrow_parameter_list return_type? ARROW block
  private static boolean fun_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fun_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = arrow_parameter_list(b, l + 1);
    r = r && fun_1_1(b, l + 1);
    r = r && consumeToken(b, ARROW);
    r = r && block(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // return_type?
  private static boolean fun_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fun_1_1")) return false;
    return_type(b, l + 1);
    return true;
  }

  // arrow_parameter_list return_type? ARROW expr
  private static boolean fun_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fun_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = arrow_parameter_list(b, l + 1);
    r = r && fun_2_1(b, l + 1);
    r = r && consumeToken(b, ARROW);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // return_type?
  private static boolean fun_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fun_2_1")) return false;
    return_type(b, l + 1);
    return true;
  }

  // LPAREN fun RPAREN
  private static boolean fun_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fun_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && fun(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // nat_literal   // generic-argument-size
  //        | type_expression
  public static boolean garg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "garg")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, GARG, "<garg>");
    r = nat_literal(b, l + 1);
    if (!r) r = type_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LT (garg (COMMA garg)* COMMA?)? GT
  public static boolean gargs(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gargs")) return false;
    if (!nextTokenIs(b, LT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LT);
    r = r && gargs_1(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, GARGS, r);
    return r;
  }

  // (garg (COMMA garg)* COMMA?)?
  private static boolean gargs_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gargs_1")) return false;
    gargs_1_0(b, l + 1);
    return true;
  }

  // garg (COMMA garg)* COMMA?
  private static boolean gargs_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gargs_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = garg(b, l + 1);
    r = r && gargs_1_0_1(b, l + 1);
    r = r && gargs_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA garg)*
  private static boolean gargs_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gargs_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!gargs_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "gargs_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA garg
  private static boolean gargs_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gargs_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && garg(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean gargs_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gargs_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // HASH IDENTIFIER   // generic-param-nat  (#-prefixed, nat-valued type variable)
  //                | IDENTIFIER
  public static boolean generic_param(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_param")) return false;
    if (!nextTokenIs(b, "<generic param>", HASH, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, GENERIC_PARAM, "<generic param>");
    r = parseTokens(b, 0, HASH, IDENTIFIER);
    if (!r) r = consumeToken(b, IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LT (generic_param (COMMA generic_param)* COMMA?)? GT
  public static boolean gparams(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gparams")) return false;
    if (!nextTokenIs(b, LT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LT);
    r = r && gparams_1(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, GPARAMS, r);
    return r;
  }

  // (generic_param (COMMA generic_param)* COMMA?)?
  private static boolean gparams_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gparams_1")) return false;
    gparams_1_0(b, l + 1);
    return true;
  }

  // generic_param (COMMA generic_param)* COMMA?
  private static boolean gparams_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gparams_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = generic_param(b, l + 1);
    r = r && gparams_1_0_1(b, l + 1);
    r = r && gparams_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA generic_param)*
  private static boolean gparams_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gparams_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!gparams_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "gparams_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA generic_param
  private static boolean gparams_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gparams_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && generic_param(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean gparams_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "gparams_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // CONTRACT RESERVED_KEYWORD type_expression SEMICOLON
  public static boolean implements_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "implements_declaration")) return false;
    if (!nextTokenIs(b, CONTRACT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, CONTRACT, RESERVED_KEYWORD);
    r = r && type_expression(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, IMPLEMENTS_DECLARATION, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER AS IDENTIFIER   // import-element-rename
  //                  | IDENTIFIER
  public static boolean import_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_element")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parseTokens(b, 0, IDENTIFIER, AS, IDENTIFIER);
    if (!r) r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, IMPORT_ELEMENT, r);
    return r;
  }

  /* ********************************************************** */
  // IMPORT import_selection? import_name gargs? import_prefix? SEMICOLON
  public static boolean import_form(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_form")) return false;
    if (!nextTokenIs(b, IMPORT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IMPORT);
    r = r && import_form_1(b, l + 1);
    r = r && import_name(b, l + 1);
    r = r && import_form_3(b, l + 1);
    r = r && import_form_4(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, IMPORT_FORM, r);
    return r;
  }

  // import_selection?
  private static boolean import_form_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_form_1")) return false;
    import_selection(b, l + 1);
    return true;
  }

  // gargs?
  private static boolean import_form_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_form_3")) return false;
    gargs(b, l + 1);
    return true;
  }

  // import_prefix?
  private static boolean import_form_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_form_4")) return false;
    import_prefix(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // STRING_LITERAL   // import-name-file  (quoted path)
  //               | IDENTIFIER
  public static boolean import_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_name")) return false;
    if (!nextTokenIs(b, "<import name>", IDENTIFIER, STRING_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IMPORT_NAME, "<import name>");
    r = consumeToken(b, STRING_LITERAL);
    if (!r) r = consumeToken(b, IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // PREFIX IDENTIFIER
  public static boolean import_prefix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_prefix")) return false;
    if (!nextTokenIs(b, PREFIX)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, PREFIX, IDENTIFIER);
    exit_section_(b, m, IMPORT_PREFIX, r);
    return r;
  }

  /* ********************************************************** */
  // LBRACE (import_element (COMMA import_element)* COMMA?)? RBRACE FROM
  public static boolean import_selection(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_selection")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && import_selection_1(b, l + 1);
    r = r && consumeTokens(b, 0, RBRACE, FROM);
    exit_section_(b, m, IMPORT_SELECTION, r);
    return r;
  }

  // (import_element (COMMA import_element)* COMMA?)?
  private static boolean import_selection_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_selection_1")) return false;
    import_selection_1_0(b, l + 1);
    return true;
  }

  // import_element (COMMA import_element)* COMMA?
  private static boolean import_selection_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_selection_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = import_element(b, l + 1);
    r = r && import_selection_1_0_1(b, l + 1);
    r = r && import_selection_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA import_element)*
  private static boolean import_selection_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_selection_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!import_selection_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "import_selection_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA import_element
  private static boolean import_selection_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_selection_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && import_element(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean import_selection_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_selection_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // INCLUDE STRING_LITERAL SEMICOLON
  public static boolean include_form(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "include_form")) return false;
    if (!nextTokenIs(b, INCLUDE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, INCLUDE, STRING_LITERAL, SEMICOLON);
    exit_section_(b, m, INCLUDE_FORM, r);
    return r;
  }

  /* ********************************************************** */
  // EXPORT? SEALED? LEDGER IDENTIFIER COLON type_expression SEMICOLON
  public static boolean ledger_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ledger_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, LEDGER_DECLARATION, "<ledger declaration>");
    r = ledger_declaration_0(b, l + 1);
    r = r && ledger_declaration_1(b, l + 1);
    r = r && consumeTokens(b, 0, LEDGER, IDENTIFIER, COLON);
    r = r && type_expression(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPORT?
  private static boolean ledger_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ledger_declaration_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // SEALED?
  private static boolean ledger_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ledger_declaration_1")) return false;
    consumeToken(b, SEALED);
    return true;
  }

  /* ********************************************************** */
  // EXPORT? MODULE IDENTIFIER gparams? LBRACE program_element* RBRACE
  public static boolean module_definition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "module_definition")) return false;
    if (!nextTokenIs(b, "<module definition>", EXPORT, MODULE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MODULE_DEFINITION, "<module definition>");
    r = module_definition_0(b, l + 1);
    r = r && consumeTokens(b, 0, MODULE, IDENTIFIER);
    r = r && module_definition_3(b, l + 1);
    r = r && consumeToken(b, LBRACE);
    r = r && module_definition_5(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPORT?
  private static boolean module_definition_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "module_definition_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // gparams?
  private static boolean module_definition_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "module_definition_3")) return false;
    gparams(b, l + 1);
    return true;
  }

  // program_element*
  private static boolean module_definition_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "module_definition_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!program_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "module_definition_5", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // DECIMAL_LITERAL
  static boolean nat_literal(PsiBuilder b, int l) {
    return consumeToken(b, DECIMAL_LITERAL);
  }

  /* ********************************************************** */
  // typed_pattern | pattern
  public static boolean optionally_typed_pattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "optionally_typed_pattern")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, OPTIONALLY_TYPED_PATTERN, "<optionally typed pattern>");
    r = typed_pattern(b, l + 1);
    if (!r) r = pattern(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER                                                    // pattern-id
  //   | LBRACKET (pattern? (COMMA pattern?)* COMMA?)? RBRACKET        // pattern-tuple  (OPT per slot)
  //   | LBRACE (pattern_struct_elt (COMMA pattern_struct_elt)* COMMA?)? RBRACE
  public static boolean pattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PATTERN, "<pattern>");
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = pattern_1(b, l + 1);
    if (!r) r = pattern_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // LBRACKET (pattern? (COMMA pattern?)* COMMA?)? RBRACKET
  private static boolean pattern_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACKET);
    r = r && pattern_1_1(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // (pattern? (COMMA pattern?)* COMMA?)?
  private static boolean pattern_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_1_1")) return false;
    pattern_1_1_0(b, l + 1);
    return true;
  }

  // pattern? (COMMA pattern?)* COMMA?
  private static boolean pattern_1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_1_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = pattern_1_1_0_0(b, l + 1);
    r = r && pattern_1_1_0_1(b, l + 1);
    r = r && pattern_1_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // pattern?
  private static boolean pattern_1_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_1_1_0_0")) return false;
    pattern(b, l + 1);
    return true;
  }

  // (COMMA pattern?)*
  private static boolean pattern_1_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_1_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!pattern_1_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "pattern_1_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA pattern?
  private static boolean pattern_1_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_1_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && pattern_1_1_0_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // pattern?
  private static boolean pattern_1_1_0_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_1_1_0_1_0_1")) return false;
    pattern(b, l + 1);
    return true;
  }

  // COMMA?
  private static boolean pattern_1_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_1_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // LBRACE (pattern_struct_elt (COMMA pattern_struct_elt)* COMMA?)? RBRACE
  private static boolean pattern_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && pattern_2_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // (pattern_struct_elt (COMMA pattern_struct_elt)* COMMA?)?
  private static boolean pattern_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_2_1")) return false;
    pattern_2_1_0(b, l + 1);
    return true;
  }

  // pattern_struct_elt (COMMA pattern_struct_elt)* COMMA?
  private static boolean pattern_2_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_2_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = pattern_struct_elt(b, l + 1);
    r = r && pattern_2_1_0_1(b, l + 1);
    r = r && pattern_2_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA pattern_struct_elt)*
  private static boolean pattern_2_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_2_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!pattern_2_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "pattern_2_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA pattern_struct_elt
  private static boolean pattern_2_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_2_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && pattern_struct_elt(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean pattern_2_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_2_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // LPAREN (typed_pattern (COMMA typed_pattern)* COMMA?)? RPAREN
  public static boolean pattern_parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_parameter_list")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && pattern_parameter_list_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, PATTERN_PARAMETER_LIST, r);
    return r;
  }

  // (typed_pattern (COMMA typed_pattern)* COMMA?)?
  private static boolean pattern_parameter_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_parameter_list_1")) return false;
    pattern_parameter_list_1_0(b, l + 1);
    return true;
  }

  // typed_pattern (COMMA typed_pattern)* COMMA?
  private static boolean pattern_parameter_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_parameter_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = typed_pattern(b, l + 1);
    r = r && pattern_parameter_list_1_0_1(b, l + 1);
    r = r && pattern_parameter_list_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA typed_pattern)*
  private static boolean pattern_parameter_list_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_parameter_list_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!pattern_parameter_list_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "pattern_parameter_list_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA typed_pattern
  private static boolean pattern_parameter_list_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_parameter_list_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && typed_pattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean pattern_parameter_list_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_parameter_list_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER COLON pattern   // pattern-struct-elt-pattern
  //   | IDENTIFIER
  public static boolean pattern_struct_elt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_struct_elt")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = pattern_struct_elt_0(b, l + 1);
    if (!r) r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, PATTERN_STRUCT_ELT, r);
    return r;
  }

  // IDENTIFIER COLON pattern
  private static boolean pattern_struct_elt_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern_struct_elt_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, COLON);
    r = r && pattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PRAGMA IDENTIFIER version_expr SEMICOLON
  public static boolean pragma_form(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pragma_form")) return false;
    if (!nextTokenIs(b, PRAGMA)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, PRAGMA, IDENTIFIER);
    r = r && version_expr(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, PRAGMA_FORM, r);
    return r;
  }

  /* ********************************************************** */
  // program_element* EOF
  public static boolean program(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "program")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PROGRAM, "<program>");
    r = program_0(b, l + 1);
    r = r && consumeToken(b, EOF);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // program_element*
  private static boolean program_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "program_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!program_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "program_0", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // pragma_form
  //   | module_definition
  //   | import_form
  //   | export_form
  //   | include_form
  //   | struct_declaration
  //   | enum_declaration
  //   | contract_declaration
  //   | implements_declaration
  //   | type_alias_declaration
  //   | ledger_declaration
  //   | witness_declaration
  //   | constructor_definition
  //   | circuit_definition
  public static boolean program_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "program_element")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PROGRAM_ELEMENT, "<program element>");
    r = pragma_form(b, l + 1);
    if (!r) r = module_definition(b, l + 1);
    if (!r) r = import_form(b, l + 1);
    if (!r) r = export_form(b, l + 1);
    if (!r) r = include_form(b, l + 1);
    if (!r) r = struct_declaration(b, l + 1);
    if (!r) r = enum_declaration(b, l + 1);
    if (!r) r = contract_declaration(b, l + 1);
    if (!r) r = implements_declaration(b, l + 1);
    if (!r) r = type_alias_declaration(b, l + 1);
    if (!r) r = ledger_declaration(b, l + 1);
    if (!r) r = witness_declaration(b, l + 1);
    if (!r) r = constructor_definition(b, l + 1);
    if (!r) r = circuit_definition(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // COLON type_expression
  public static boolean return_type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "return_type")) return false;
    if (!nextTokenIs(b, COLON)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && type_expression(b, l + 1);
    exit_section_(b, m, RETURN_TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // program
  static boolean root(PsiBuilder b, int l) {
    return program(b, l + 1);
  }

  /* ********************************************************** */
  // LPAREN (typed_id (COMMA typed_id)* COMMA?)? RPAREN
  public static boolean simple_parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_parameter_list")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && simple_parameter_list_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, SIMPLE_PARAMETER_LIST, r);
    return r;
  }

  // (typed_id (COMMA typed_id)* COMMA?)?
  private static boolean simple_parameter_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_parameter_list_1")) return false;
    simple_parameter_list_1_0(b, l + 1);
    return true;
  }

  // typed_id (COMMA typed_id)* COMMA?
  private static boolean simple_parameter_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_parameter_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = typed_id(b, l + 1);
    r = r && simple_parameter_list_1_0_1(b, l + 1);
    r = r && simple_parameter_list_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA typed_id)*
  private static boolean simple_parameter_list_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_parameter_list_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!simple_parameter_list_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "simple_parameter_list_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA typed_id
  private static boolean simple_parameter_list_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_parameter_list_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && typed_id(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean simple_parameter_list_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_parameter_list_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // IF LPAREN expr_seq RPAREN stmt   // statement-one-armed-if
  //        | stmt0
  public static boolean stmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STMT, "<stmt>");
    r = stmt_0(b, l + 1);
    if (!r) r = stmt0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IF LPAREN expr_seq RPAREN stmt
  private static boolean stmt_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IF, LPAREN);
    r = r && expr_seq(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    r = r && stmt(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // CONST cbinding (COMMA cbinding)* SEMICOLON                       // statement-const     (SEP+ #f = no trailing comma)
  //   | IF LPAREN expr_seq RPAREN stmt0 ELSE stmt                        // statement-if        (two-armed; consequent is stmt0)
  //   | FOR LPAREN CONST IDENTIFIER OF tsize RANGE tsize RPAREN stmt     // statement-for1      (range form: start..end)
  //   | FOR LPAREN CONST IDENTIFIER OF expr_seq RPAREN stmt              // statement-for2      (iterable form)
  //   | RETURN SEMICOLON                                                  // statement-return-no-value
  //   | RETURN expr_seq SEMICOLON                                         // statement-return-value
  //   | block                                                             // statement-block
  //   | expr_seq SEMICOLON
  public static boolean stmt0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STMT_0, "<stmt 0>");
    r = stmt0_0(b, l + 1);
    if (!r) r = stmt0_1(b, l + 1);
    if (!r) r = stmt0_2(b, l + 1);
    if (!r) r = stmt0_3(b, l + 1);
    if (!r) r = parseTokens(b, 0, RETURN, SEMICOLON);
    if (!r) r = stmt0_5(b, l + 1);
    if (!r) r = block(b, l + 1);
    if (!r) r = stmt0_7(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // CONST cbinding (COMMA cbinding)* SEMICOLON
  private static boolean stmt0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CONST);
    r = r && cbinding(b, l + 1);
    r = r && stmt0_0_2(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA cbinding)*
  private static boolean stmt0_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!stmt0_0_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "stmt0_0_2", c)) break;
    }
    return true;
  }

  // COMMA cbinding
  private static boolean stmt0_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && cbinding(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // IF LPAREN expr_seq RPAREN stmt0 ELSE stmt
  private static boolean stmt0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IF, LPAREN);
    r = r && expr_seq(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    r = r && stmt0(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && stmt(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // FOR LPAREN CONST IDENTIFIER OF tsize RANGE tsize RPAREN stmt
  private static boolean stmt0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, FOR, LPAREN, CONST, IDENTIFIER, OF);
    r = r && tsize(b, l + 1);
    r = r && consumeToken(b, RANGE);
    r = r && tsize(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    r = r && stmt(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // FOR LPAREN CONST IDENTIFIER OF expr_seq RPAREN stmt
  private static boolean stmt0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, FOR, LPAREN, CONST, IDENTIFIER, OF);
    r = r && expr_seq(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    r = r && stmt(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // RETURN expr_seq SEMICOLON
  private static boolean stmt0_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0_5")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, RETURN);
    r = r && expr_seq(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // expr_seq SEMICOLON
  private static boolean stmt0_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt0_7")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expr_seq(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER COLON expr                                   // struct-arg-named
  //              | SPREAD expr                                             // struct-arg-spread
  //              | expr
  public static boolean struct_arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_arg")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRUCT_ARG, "<struct arg>");
    r = struct_arg_0(b, l + 1);
    if (!r) r = struct_arg_1(b, l + 1);
    if (!r) r = expr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IDENTIFIER COLON expr
  private static boolean struct_arg_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_arg_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, COLON);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // SPREAD expr
  private static boolean struct_arg_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_arg_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SPREAD);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // EXPORT? STRUCT IDENTIFIER gparams? LBRACE (typed_id (SEMICOLON typed_id)* SEMICOLON?)? RBRACE SEMICOLON?   // structure-declaration/semicolons
  //   | EXPORT? STRUCT IDENTIFIER gparams? LBRACE (typed_id (COMMA    typed_id)* COMMA?   )? RBRACE SEMICOLON?
  public static boolean struct_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration")) return false;
    if (!nextTokenIs(b, "<struct declaration>", EXPORT, STRUCT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRUCT_DECLARATION, "<struct declaration>");
    r = struct_declaration_0(b, l + 1);
    if (!r) r = struct_declaration_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPORT? STRUCT IDENTIFIER gparams? LBRACE (typed_id (SEMICOLON typed_id)* SEMICOLON?)? RBRACE SEMICOLON?
  private static boolean struct_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = struct_declaration_0_0(b, l + 1);
    r = r && consumeTokens(b, 0, STRUCT, IDENTIFIER);
    r = r && struct_declaration_0_3(b, l + 1);
    r = r && consumeToken(b, LBRACE);
    r = r && struct_declaration_0_5(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    r = r && struct_declaration_0_7(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // EXPORT?
  private static boolean struct_declaration_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // gparams?
  private static boolean struct_declaration_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0_3")) return false;
    gparams(b, l + 1);
    return true;
  }

  // (typed_id (SEMICOLON typed_id)* SEMICOLON?)?
  private static boolean struct_declaration_0_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0_5")) return false;
    struct_declaration_0_5_0(b, l + 1);
    return true;
  }

  // typed_id (SEMICOLON typed_id)* SEMICOLON?
  private static boolean struct_declaration_0_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = typed_id(b, l + 1);
    r = r && struct_declaration_0_5_0_1(b, l + 1);
    r = r && struct_declaration_0_5_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (SEMICOLON typed_id)*
  private static boolean struct_declaration_0_5_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0_5_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!struct_declaration_0_5_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "struct_declaration_0_5_0_1", c)) break;
    }
    return true;
  }

  // SEMICOLON typed_id
  private static boolean struct_declaration_0_5_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0_5_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SEMICOLON);
    r = r && typed_id(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // SEMICOLON?
  private static boolean struct_declaration_0_5_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0_5_0_2")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  // SEMICOLON?
  private static boolean struct_declaration_0_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_0_7")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  // EXPORT? STRUCT IDENTIFIER gparams? LBRACE (typed_id (COMMA    typed_id)* COMMA?   )? RBRACE SEMICOLON?
  private static boolean struct_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = struct_declaration_1_0(b, l + 1);
    r = r && consumeTokens(b, 0, STRUCT, IDENTIFIER);
    r = r && struct_declaration_1_3(b, l + 1);
    r = r && consumeToken(b, LBRACE);
    r = r && struct_declaration_1_5(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    r = r && struct_declaration_1_7(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // EXPORT?
  private static boolean struct_declaration_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // gparams?
  private static boolean struct_declaration_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1_3")) return false;
    gparams(b, l + 1);
    return true;
  }

  // (typed_id (COMMA    typed_id)* COMMA?   )?
  private static boolean struct_declaration_1_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1_5")) return false;
    struct_declaration_1_5_0(b, l + 1);
    return true;
  }

  // typed_id (COMMA    typed_id)* COMMA?
  private static boolean struct_declaration_1_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = typed_id(b, l + 1);
    r = r && struct_declaration_1_5_0_1(b, l + 1);
    r = r && struct_declaration_1_5_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA    typed_id)*
  private static boolean struct_declaration_1_5_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1_5_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!struct_declaration_1_5_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "struct_declaration_1_5_0_1", c)) break;
    }
    return true;
  }

  // COMMA    typed_id
  private static boolean struct_declaration_1_5_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1_5_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && typed_id(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean struct_declaration_1_5_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1_5_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // SEMICOLON?
  private static boolean struct_declaration_1_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "struct_declaration_1_7")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER                                                    // term-ref
  //        | TRUE                                                          // term-true
  //        | FALSE                                                         // term-false
  //        | DECIMAL_LITERAL                                               // term-element-field
  //        | HEX_LITERAL
  //        | BINARY_LITERAL
  //        | OCTAL_LITERAL
  //        | STRING_LITERAL                                                // term-element-string
  //        | PAD LPAREN DECIMAL_LITERAL COMMA STRING_LITERAL RPAREN        // term-element-padded-string
  //        | DEFAULT LT type_expression GT                                            // term-element-default
  //        | LPAREN expr_seq RPAREN
  public static boolean term(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "term")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TERM, "<term>");
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, TRUE);
    if (!r) r = consumeToken(b, FALSE);
    if (!r) r = consumeToken(b, DECIMAL_LITERAL);
    if (!r) r = consumeToken(b, HEX_LITERAL);
    if (!r) r = consumeToken(b, BINARY_LITERAL);
    if (!r) r = consumeToken(b, OCTAL_LITERAL);
    if (!r) r = consumeToken(b, STRING_LITERAL);
    if (!r) r = parseTokens(b, 0, PAD, LPAREN, DECIMAL_LITERAL, COMMA, STRING_LITERAL, RPAREN);
    if (!r) r = term_9(b, l + 1);
    if (!r) r = term_10(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // DEFAULT LT type_expression GT
  private static boolean term_9(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "term_9")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, DEFAULT, LT);
    r = r && type_expression(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, null, r);
    return r;
  }

  // LPAREN expr_seq RPAREN
  private static boolean term_10(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "term_10")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && expr_seq(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER gargs?
  public static boolean tref(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tref")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    r = r && tref_1(b, l + 1);
    exit_section_(b, m, TREF, r);
    return r;
  }

  // gargs?
  private static boolean tref_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tref_1")) return false;
    gargs(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // DECIMAL_LITERAL   // type-size-field    (a literal field count / bit width)
  //         | IDENTIFIER
  public static boolean tsize(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tsize")) return false;
    if (!nextTokenIs(b, "<tsize>", DECIMAL_LITERAL, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TSIZE, "<tsize>");
    r = consumeToken(b, DECIMAL_LITERAL);
    if (!r) r = consumeToken(b, IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // SPREAD expr                                              // tuple-arg-spread
  //             | expr
  public static boolean tuple_arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tuple_arg")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TUPLE_ARG, "<tuple arg>");
    r = tuple_arg_0(b, l + 1);
    if (!r) r = expr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // SPREAD expr
  private static boolean tuple_arg_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tuple_arg_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SPREAD);
    r = r && expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // EXPORT? NEW? TYPE IDENTIFIER gparams? ASSIGN type_expression SEMICOLON
  public static boolean type_alias_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_alias_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_ALIAS_DECLARATION, "<type alias declaration>");
    r = type_alias_declaration_0(b, l + 1);
    r = r && type_alias_declaration_1(b, l + 1);
    r = r && consumeTokens(b, 0, TYPE, IDENTIFIER);
    r = r && type_alias_declaration_4(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && type_expression(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPORT?
  private static boolean type_alias_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_alias_declaration_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // NEW?
  private static boolean type_alias_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_alias_declaration_1")) return false;
    consumeToken(b, NEW);
    return true;
  }

  // gparams?
  private static boolean type_alias_declaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_alias_declaration_4")) return false;
    gparams(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // tref                                                    // type-ref
  //   | BOOLEAN_TYPE                                            // type-boolean
  //   | FIELD_TYPE                                              // type-field
  //   | JUBJUB_SCALAR_TYPE                                      // type-jubjub-scalar
  //   | SECP256K1_BASE_TYPE                                     // type-secp256k1-base
  //   | SECP256K1_SCALAR_TYPE                                   // type-secp256k1-scalar
  //   | UINT_TYPE  LT tsize RANGE tsize GT                      // type-unsigned-integer-max  ("..") — longer, try first
  //   | UINT_TYPE  LT tsize GT                                  // type-unsigned-integer-bits
  //   | BYTES_TYPE LT tsize GT                                  // type-bytes
  //   | OPAQUE_TYPE LT STRING_LITERAL GT                        // type-opaque  (str terminal → STRING_LITERAL)
  //   | VECTOR_TYPE LT tsize COMMA type_expression GT                      // type-vector
  //   | LBRACKET (type_expression (COMMA type_expression)* COMMA?)? RBRACKET
  public static boolean type_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_EXPRESSION, "<type expression>");
    r = tref(b, l + 1);
    if (!r) r = consumeToken(b, BOOLEAN_TYPE);
    if (!r) r = consumeToken(b, FIELD_TYPE);
    if (!r) r = consumeToken(b, JUBJUB_SCALAR_TYPE);
    if (!r) r = consumeToken(b, SECP256K1_BASE_TYPE);
    if (!r) r = consumeToken(b, SECP256K1_SCALAR_TYPE);
    if (!r) r = type_expression_6(b, l + 1);
    if (!r) r = type_expression_7(b, l + 1);
    if (!r) r = type_expression_8(b, l + 1);
    if (!r) r = parseTokens(b, 0, OPAQUE_TYPE, LT, STRING_LITERAL, GT);
    if (!r) r = type_expression_10(b, l + 1);
    if (!r) r = type_expression_11(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // UINT_TYPE  LT tsize RANGE tsize GT
  private static boolean type_expression_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, UINT_TYPE, LT);
    r = r && tsize(b, l + 1);
    r = r && consumeToken(b, RANGE);
    r = r && tsize(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, null, r);
    return r;
  }

  // UINT_TYPE  LT tsize GT
  private static boolean type_expression_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_7")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, UINT_TYPE, LT);
    r = r && tsize(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, null, r);
    return r;
  }

  // BYTES_TYPE LT tsize GT
  private static boolean type_expression_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_8")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, BYTES_TYPE, LT);
    r = r && tsize(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, null, r);
    return r;
  }

  // VECTOR_TYPE LT tsize COMMA type_expression GT
  private static boolean type_expression_10(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_10")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, VECTOR_TYPE, LT);
    r = r && tsize(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && type_expression(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, null, r);
    return r;
  }

  // LBRACKET (type_expression (COMMA type_expression)* COMMA?)? RBRACKET
  private static boolean type_expression_11(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_11")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACKET);
    r = r && type_expression_11_1(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // (type_expression (COMMA type_expression)* COMMA?)?
  private static boolean type_expression_11_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_11_1")) return false;
    type_expression_11_1_0(b, l + 1);
    return true;
  }

  // type_expression (COMMA type_expression)* COMMA?
  private static boolean type_expression_11_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_11_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type_expression(b, l + 1);
    r = r && type_expression_11_1_0_1(b, l + 1);
    r = r && type_expression_11_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA type_expression)*
  private static boolean type_expression_11_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_11_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!type_expression_11_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "type_expression_11_1_0_1", c)) break;
    }
    return true;
  }

  // COMMA type_expression
  private static boolean type_expression_11_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_11_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && type_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean type_expression_11_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_11_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER COLON type_expression
  public static boolean typed_id(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typed_id")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, COLON);
    r = r && type_expression(b, l + 1);
    exit_section_(b, m, TYPED_ID, r);
    return r;
  }

  /* ********************************************************** */
  // pattern COLON type_expression
  public static boolean typed_pattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typed_pattern")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPED_PATTERN, "<typed pattern>");
    r = pattern(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && type_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // VERSION_LITERAL
  //                | DECIMAL_LITERAL
  public static boolean version_atom(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_atom")) return false;
    if (!nextTokenIs(b, "<version atom>", DECIMAL_LITERAL, VERSION_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VERSION_ATOM, "<version atom>");
    r = consumeToken(b, VERSION_LITERAL);
    if (!r) r = consumeToken(b, DECIMAL_LITERAL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // version_expr0 (OROR version_expr0)*
  static boolean version_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_expr")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = version_expr0(b, l + 1);
    r = r && version_expr_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (OROR version_expr0)*
  private static boolean version_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!version_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "version_expr_1", c)) break;
    }
    return true;
  }

  // OROR version_expr0
  private static boolean version_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OROR);
    r = r && version_expr0(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // version_term (ANDAND version_term)*
  static boolean version_expr0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_expr0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = version_term(b, l + 1);
    r = r && version_expr0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ANDAND version_term)*
  private static boolean version_expr0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_expr0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!version_expr0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "version_expr0_1", c)) break;
    }
    return true;
  }

  // ANDAND version_term
  private static boolean version_expr0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_expr0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ANDAND);
    r = r && version_term(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // version_atom                        // version-term-atom
  //                | NOT     version_atom                // version-term-not  (#\!)
  //                | LT      version_atom                // version-term-lt   (#\<)
  //                | LTE     version_atom                // version-term-le   ("<=")
  //                | GTE     version_atom                // version-term-ge   (">=")
  //                | GT      version_atom                // version-term-gt   (#\>)
  //                | LPAREN  version_expr  RPAREN
  public static boolean version_term(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_term")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VERSION_TERM, "<version term>");
    r = version_atom(b, l + 1);
    if (!r) r = version_term_1(b, l + 1);
    if (!r) r = version_term_2(b, l + 1);
    if (!r) r = version_term_3(b, l + 1);
    if (!r) r = version_term_4(b, l + 1);
    if (!r) r = version_term_5(b, l + 1);
    if (!r) r = version_term_6(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // NOT     version_atom
  private static boolean version_term_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_term_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NOT);
    r = r && version_atom(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LT      version_atom
  private static boolean version_term_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_term_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LT);
    r = r && version_atom(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LTE     version_atom
  private static boolean version_term_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_term_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LTE);
    r = r && version_atom(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // GTE     version_atom
  private static boolean version_term_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_term_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, GTE);
    r = r && version_atom(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // GT      version_atom
  private static boolean version_term_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_term_5")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, GT);
    r = r && version_atom(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LPAREN  version_expr  RPAREN
  private static boolean version_term_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "version_term_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && version_expr(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // EXPORT? WITNESS IDENTIFIER gparams? simple_parameter_list COLON type_expression SEMICOLON
  public static boolean witness_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "witness_declaration")) return false;
    if (!nextTokenIs(b, "<witness declaration>", EXPORT, WITNESS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, WITNESS_DECLARATION, "<witness declaration>");
    r = witness_declaration_0(b, l + 1);
    r = r && consumeTokens(b, 0, WITNESS, IDENTIFIER);
    r = r && witness_declaration_3(b, l + 1);
    r = r && simple_parameter_list(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && type_expression(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPORT?
  private static boolean witness_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "witness_declaration_0")) return false;
    consumeToken(b, EXPORT);
    return true;
  }

  // gparams?
  private static boolean witness_declaration_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "witness_declaration_3")) return false;
    gparams(b, l + 1);
    return true;
  }

}
