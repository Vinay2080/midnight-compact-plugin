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
  // LBRACE statement* RBRACE
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

  // statement*
  private static boolean block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // CIRCUIT IDENTIFIER parameter_list return_type? block
  public static boolean circuit_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "circuit_declaration")) return false;
    if (!nextTokenIs(b, CIRCUIT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, CIRCUIT, IDENTIFIER);
    r = r && parameter_list(b, l + 1);
    r = r && circuit_declaration_3(b, l + 1);
    r = r && block(b, l + 1);
    exit_section_(b, m, CIRCUIT_DECLARATION, r);
    return r;
  }

  // return_type?
  private static boolean circuit_declaration_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "circuit_declaration_3")) return false;
    return_type(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // item*
  static boolean compilation_unit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compilation_unit")) return false;
    while (true) {
      int c = current_position_(b);
      if (!item(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "compilation_unit", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // circuit_declaration
  static boolean declaration(PsiBuilder b, int l) {
    return circuit_declaration(b, l + 1);
  }

  /* ********************************************************** */
  // IDENTIFIER
  //                      | NUMBER
  //                      | STRING_LITERAL
  static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    boolean r;
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, STRING_LITERAL);
    return r;
  }

  /* ********************************************************** */
  // IMPORT STRING_LITERAL SEMICOLON
  public static boolean import_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_statement")) return false;
    if (!nextTokenIs(b, IMPORT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IMPORT, STRING_LITERAL, SEMICOLON);
    exit_section_(b, m, IMPORT_STATEMENT, r);
    return r;
  }

  /* ********************************************************** */
  // pragma_statement
  //                | import_statement
  //                | declaration
  static boolean item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item")) return false;
    boolean r;
    r = pragma_statement(b, l + 1);
    if (!r) r = import_statement(b, l + 1);
    if (!r) r = declaration(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER COLON type
  public static boolean parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, COLON);
    r = r && type(b, l + 1);
    exit_section_(b, m, PARAMETER, r);
    return r;
  }

  /* ********************************************************** */
  // LPAREN parameter (COMMA parameter)* RPAREN
  public static boolean parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && parameter(b, l + 1);
    r = r && parameter_list_2(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, PARAMETER_LIST, r);
    return r;
  }

  // (COMMA parameter)*
  private static boolean parameter_list_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parameter_list_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list_2", c)) break;
    }
    return true;
  }

  // COMMA parameter
  private static boolean parameter_list_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PRAGMA IDENTIFIER VERSION NUMBER SEMICOLON
  public static boolean pragma_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pragma_statement")) return false;
    if (!nextTokenIs(b, PRAGMA)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, PRAGMA, IDENTIFIER, VERSION, NUMBER, SEMICOLON);
    exit_section_(b, m, PRAGMA_STATEMENT, r);
    return r;
  }

  /* ********************************************************** */
  // RETURN expression SEMICOLON
  public static boolean return_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "return_statement")) return false;
    if (!nextTokenIs(b, RETURN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, RETURN);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, RETURN_STATEMENT, r);
    return r;
  }

  /* ********************************************************** */
  // COLON type
  public static boolean return_type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "return_type")) return false;
    if (!nextTokenIs(b, COLON)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && type(b, l + 1);
    exit_section_(b, m, RETURN_TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // compilation_unit
  static boolean root(PsiBuilder b, int l) {
    return compilation_unit(b, l + 1);
  }

  /* ********************************************************** */
  // return_statement
  static boolean statement(PsiBuilder b, int l) {
    return return_statement(b, l + 1);
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, TYPE, r);
    return r;
  }

}
