package dev.verloren.midnight.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick Documentation provider for Compact declarations, references, and doc comments.
 *
 * <p>Extends {@link AbstractDocumentationProvider} to format rich HTML documentation popups displayed
 * when the user hovers over an identifier or invokes Quick Documentation (Ctrl+Q / F1).
 * Extracts signatures, types, field/variant listings, preceding doc comments ({@code /** ... *&#47;}, {@code ///}),
 * parses Javadoc/doc tags ({@code @param}, {@code @return}, {@code @throws}, {@code @see}, {@code @since}, {@code @deprecated}, etc.),
 * formats inline Markdown, and provides in-editor rendered documentation for Reader Mode.</p>
 */
public class CompactDocumentationProvider extends AbstractDocumentationProvider {

  private static final Pattern DOC_TAG_LINE_PATTERN =
      Pattern.compile("^@([a-zA-Z_][a-zA-Z0-9_:-]*)(?:\\s+(.*))?$");
  private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
  private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
  private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");
  private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");

  @Override
  public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    if (element == null) {
      return null;
    }
    if (element instanceof PsiComment comment) {
      PsiElement target = findTargetDeclarationForComment(comment);
      if (target != null) {
        return getDefinitionHeader(target);
      }
    }
    return getDefinitionHeader(element);
  }

  @Override
  public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element == null) {
      return null;
    }

    if (element instanceof PsiComment comment) {
      PsiElement target = findTargetDeclarationForComment(comment);
      if (target != null) {
        return generateDoc(target, originalElement);
      }
      return renderDocCommentOnly(comment);
    }

    String header = getDefinitionHeader(element);
    if (header == null) {
      return null;
    }

    StringBuilder doc = new StringBuilder();
    doc.append(DocumentationMarkup.DEFINITION_START);
    doc.append(escapeHtml(header));
    doc.append(DocumentationMarkup.DEFINITION_END);

    ParsedDoc docData = extractAndParseDoc(element);

    // If element is a parameter and has no own doc, look up @param from enclosing function
    if ((docData == null || docData.isEmpty()) && isParameter(element)) {
      docData = findParamDocFromEnclosing(element);
    }
    // If element is a struct field and has no own doc, look up @param from parent struct
    if ((docData == null || docData.isEmpty()) && element instanceof CompactStructFieldImpl field) {
      docData = findFieldDocFromParentStruct(field);
    }

    if (docData != null && docData.hasDescription()) {
      doc.append(DocumentationMarkup.CONTENT_START);
      doc.append(docData.renderDescriptionHtml());
      doc.append(DocumentationMarkup.CONTENT_END);
    }

    String sectionsHtml = renderSections(element, docData);
    if (sectionsHtml != null && !sectionsHtml.isEmpty()) {
      doc.append(DocumentationMarkup.SECTIONS_START);
      doc.append(sectionsHtml);
      doc.append(DocumentationMarkup.SECTIONS_END);
    }

    return doc.toString();
  }

  @Override
  public void collectDocComments(@NotNull PsiFile file, @NotNull Consumer<? super PsiDocCommentBase> sink) {
    if (!(file instanceof CompactFile)) {
      return;
    }
    for (PsiComment comment : PsiTreeUtil.findChildrenOfType(file, PsiComment.class)) {
      String text = comment.getText();
      if (text.startsWith("/**") || text.startsWith("///")) {
        sink.accept(new CompactDocComment(comment));
      }
    }
  }

  @Override
  public @Nullable PsiDocCommentBase findDocComment(@NotNull PsiFile file, @NotNull TextRange range) {
    if (!(file instanceof CompactFile)) {
      return null;
    }
    PsiElement element = file.findElementAt(range.getStartOffset());
    while (element != null && !(element instanceof PsiComment)) {
      element = element.getParent();
    }
    if (element instanceof PsiComment comment) {
      String text = comment.getText();
      if (text.startsWith("/**") || text.startsWith("///")) {
        return new CompactDocComment(comment);
      }
    }
    return null;
  }

  @Override
  public @Nullable String generateRenderedDoc(@NotNull PsiDocCommentBase comment) {
    if (comment instanceof CompactDocComment docComment) {
      return renderDocCommentOnly(docComment.getDelegate());
    }
    return renderDocCommentOnly(comment);
  }

  public @Nullable String renderDocCommentOnly(@NotNull PsiComment comment) {
    ParsedDoc docData = parseDocCommentText(comment.getText());
    if (docData == null || docData.isEmpty()) {
      return null;
    }

    StringBuilder doc = new StringBuilder();
    if (docData.hasDescription()) {
      doc.append(DocumentationMarkup.CONTENT_START);
      doc.append(docData.renderDescriptionHtml());
      doc.append(DocumentationMarkup.CONTENT_END);
    }

    String sectionsHtml = renderDocTagSectionsOnly(docData);
    if (sectionsHtml != null && !sectionsHtml.isEmpty()) {
      doc.append(DocumentationMarkup.SECTIONS_START);
      doc.append(sectionsHtml);
      doc.append(DocumentationMarkup.SECTIONS_END);
    }

    return doc.isEmpty() ? null : doc.toString();
  }

  @Override
  public @Nullable PsiElement getCustomDocumentationElement(
      @NotNull Editor editor,
      @NotNull PsiFile file,
      @Nullable PsiElement contextElement,
      int targetOffset
  ) {
    if (contextElement == null) {
      return null;
    }

    // If contextElement is whitespace, check previous non-whitespace sibling
    if (contextElement instanceof PsiWhiteSpace) {
      PsiElement prev = contextElement.getPrevSibling();
      if (prev != null) {
        contextElement = prev;
      }
    }

    // If context element or any ancestor has a reference, resolve it first
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      if (p.getReference() != null) {
        PsiElement resolved = p.getReference().resolve();
        if (resolved != null) {
          return resolved;
        }
      }
    }

    // If contextElement is a doc comment or child of a comment, resolve to documented declaration
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      if (p instanceof PsiComment comment) {
        PsiElement target = findTargetDeclarationForComment(comment);
        if (target != null) {
          return target;
        }
        return comment;
      }
    }

    // Otherwise walk up to nearest declaration element
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      switch (p) {
        case CompactStructFieldImpl _, CompactEnumMemberImpl _ -> {
          return p;
        }
        case CompactParameterImpl param when param.getParent() instanceof CompactStructFieldImpl -> {
          return param.getParent();
        }
        case CompactPatternImpl _, CompactPragmaForm _, CompactNamedElement _ -> {
          return p;
        }
        default -> {
        }
      }
    }
    return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
  }

  public static @Nullable PsiElement findTargetDeclarationForComment(@NotNull PsiComment comment) {
    if (comment instanceof CompactDocComment docComment) {
      comment = docComment.getDelegate();
    }
    PsiElement next = comment.getNextSibling();
    while (next instanceof PsiWhiteSpace || next instanceof PsiComment) {
      next = next.getNextSibling();
    }
    if (next instanceof CompactNamedElement
        || next instanceof CompactConstructorDeclaration
        || next instanceof CompactContractImplementsDeclaration
        || next instanceof CompactPragmaForm) {
      return next;
    }
    if (next instanceof CompactExportDeclaration exportDecl) {
      for (PsiElement child : exportDecl.getChildren()) {
        if (child instanceof CompactNamedElement) {
          return child;
        }
      }
      return exportDecl;
    }
    if (next != null) {
      return PsiTreeUtil.findChildOfType(next, CompactNamedElement.class);
    }
    return null;
  }

  public static @Nullable String getDefinitionHeader(@NotNull PsiElement element) {
    if (isParameter(element)) {
      String name = ((PsiNamedElement) element).getName();
      CompactType type = ((CompactTypeElement) element).getType();
      return "parameter " + (name != null ? name : "param") + ": " + type.name();
    }

    return switch (element) {
      case CompactCircuitDefinition circuit -> {
        String name = circuit.getName() != null ? circuit.getName() : "circuit";
        yield "circuit " + name + getSignatureSuffix(circuit);
      }
      case CompactWitnessDeclaration witness -> {
        String name = witness.getName() != null ? witness.getName() : "witness";
        yield "witness " + name + getSignatureSuffix(witness);
      }
      case CompactConstructorDeclaration ctor -> "constructor" + getSignatureSuffix(ctor);
      case CompactExternalContractDeclaration contract -> {
        String name = contract.getName();
        yield "contract " + (name != null ? name : "");
      }
      case CompactContractImplementsDeclaration impl -> impl.getText().trim();
      case CompactModuleDefinition module -> {
        String name = module.getName();
        yield "module " + (name != null ? name : "");
      }
      case CompactStructDefinition struct -> {
        String name = struct.getName();
        yield "struct " + (name != null ? name : "");
      }
      case CompactStructFieldImpl field -> {
        String name = field.getName() != null ? field.getName() : "field";
        CompactType type = field.getType();
        CompactStructDefinition parentStruct = PsiTreeUtil.getParentOfType(field, CompactStructDefinition.class);
        String prefix = parentStruct != null && parentStruct.getName() != null ? parentStruct.getName() + "." : "";
        yield "struct field " + prefix + name + ": " + type.name();
      }
      case CompactEnumDefinition enumDef -> {
        String name = enumDef.getName();
        yield "enum " + (name != null ? name : "");
      }
      case CompactEnumMemberImpl member -> {
        String name = member.getName() != null ? member.getName() : "member";
        CompactEnumDefinition parentEnum = PsiTreeUtil.getParentOfType(member, CompactEnumDefinition.class);
        String prefix = parentEnum != null && parentEnum.getName() != null ? parentEnum.getName() + "." : "";
        yield "enum variant " + prefix + name;
      }
      case CompactTypeDefinition typeDef -> {
        String name = typeDef.getName() != null ? typeDef.getName() : "type";
        yield "type " + name + " = " + typeDef.getType().name();
      }
      case CompactPatternImpl pattern -> formatBindingHeader(pattern);
      case CompactConstBindingImpl constBinding -> formatBindingHeader(constBinding);
      case CompactLedgerDeclaration ledger -> ledger.getText().trim();
      case CompactPragmaForm pragma -> pragma.getText().trim();
      case CompactNamedElement named -> {
        String name = named.getName();
        yield name != null ? name : named.getText();
      }
      default -> null;
    };
  }

  private static @NotNull String formatBindingHeader(@NotNull CompactNamedElement element) {
    String name = element.getName();
    CompactType type = element.getType();
    boolean isLocal = PsiTreeUtil.getParentOfType(element, CompactBlock.class) != null;
    String kind = isLocal ? "local " : "const ";
    return kind + (name != null ? name : "const") + ": " + type.name();
  }

  private static @NotNull String getSignatureSuffix(@NotNull PsiElement element) {
    StringBuilder sb = new StringBuilder();
    for (PsiElement child : element.getChildren()) {
      if (child instanceof CompactParameterImpl || child instanceof CompactBlock) {
        continue;
      }
      String text = child.getText().trim();
      if (text.startsWith("(") && text.endsWith(")")) {
        sb.append(text);
      }
    }
    if (sb.isEmpty()) {
      List<CompactParameterImpl> params = new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, CompactParameterImpl.class));
      sb.append("(");
      for (int i = 0; i < params.size(); i++) {
        if (i > 0) sb.append(", ");
        CompactParameterImpl p = params.get(i);
        sb.append(p.getName() != null ? p.getName() : "_");
        sb.append(": ");
        sb.append(p.getType().name());
      }
      sb.append(")");
    }
    if (element instanceof CompactTypeElement) {
      CompactType type = ((CompactTypeElement) element).getType();
      if (!"Unknown".equals(type.name()) && !"void".equalsIgnoreCase(type.name())) {
        sb.append(": ").append(type.name());
      }
    }
    return sb.toString();
  }

  // =========================================================================
  // Sections Rendering (IntelliJ Standard Two-Column Sections Table)
  // =========================================================================

  private static @Nullable String renderSections(@NotNull PsiElement element, @Nullable ParsedDoc docData) {
    StringBuilder sb = new StringBuilder();

    // 1. Doc tag sections (Params, Returns, Throws, etc.)
    if (docData != null) {
      appendDocTagSections(sb, docData);
    }

    // 2. Struct fields (if not already covered by params or as extra AST details)
    if (element instanceof CompactStructDefinition) {
      List<CompactStructFieldImpl> fields = new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, CompactStructFieldImpl.class));
      if (!fields.isEmpty()) {
        sb.append(DocumentationMarkup.SECTION_HEADER_START)
            .append("Fields:")
            .append(DocumentationMarkup.SECTION_SEPARATOR);
        for (CompactStructFieldImpl field : fields) {
          String fieldName = field.getName() != null ? field.getName() : "_";
          String fieldType = field.getType().name();
          sb.append("<p><code>").append(escapeHtml(fieldName)).append(": ").append(escapeHtml(fieldType)).append("</code></p>");
        }
        sb.append(DocumentationMarkup.SECTION_END);
      }
    }

    // 3. Enum variants
    if (element instanceof CompactEnumDefinition) {
      List<CompactEnumMemberImpl> members = new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, CompactEnumMemberImpl.class));
      if (!members.isEmpty()) {
        sb.append(DocumentationMarkup.SECTION_HEADER_START)
            .append("Variants:")
            .append(DocumentationMarkup.SECTION_SEPARATOR);
        for (CompactEnumMemberImpl member : members) {
          String memberName = member.getName() != null ? member.getName() : "_";
          sb.append("<p><code>").append(escapeHtml(memberName)).append("</code></p>");
        }
        sb.append(DocumentationMarkup.SECTION_END);
      }
    }

    return sb.isEmpty() ? null : sb.toString();
  }

  private static @Nullable String renderDocTagSectionsOnly(@NotNull ParsedDoc docData) {
    StringBuilder sb = new StringBuilder();
    appendDocTagSections(sb, docData);
    return sb.isEmpty() ? null : sb.toString();
  }

  private static void appendDocTagSections(@NotNull StringBuilder sb, @NotNull ParsedDoc docData) {
    // Params
    List<DocTag> params = docData.getTags("param", "parameter");
    if (!params.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Params:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (DocTag tag : params) {
        sb.append("<p><code>").append(escapeHtml(tag.target() != null ? tag.target() : "_")).append("</code>");
        if (!tag.description().isEmpty()) {
          sb.append(" &ndash; ").append(formatInlineDoc(tag.description()));
        }
        sb.append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }

    // Returns
    List<DocTag> returns = docData.getTags("return", "returns");
    if (!returns.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Returns:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (DocTag tag : returns) {
        sb.append("<p>").append(formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }

    // Throws
    List<DocTag> throwsList = docData.getTags("throws", "throw");
    if (!throwsList.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Throws:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (DocTag tag : throwsList) {
        sb.append("<p>");
        if (tag.target() != null && !tag.target().isEmpty()) {
          sb.append("<code>").append(escapeHtml(tag.target())).append("</code> &ndash; ");
        }
        sb.append(formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }

    // See also
    List<DocTag> seeList = docData.getTags("see");
    if (!seeList.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("See also:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (DocTag tag : seeList) {
        sb.append("<p>").append(formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }

    // Since
    List<DocTag> sinceList = docData.getTags("since");
    if (!sinceList.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Since:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (DocTag tag : sinceList) {
        sb.append("<p>").append(formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }

    // Deprecated
    List<DocTag> deprecatedList = docData.getTags("deprecated");
    if (!deprecatedList.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Deprecated:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (DocTag tag : deprecatedList) {
        sb.append("<p><span class='deprecated'>").append(formatInlineDoc(tag.description())).append("</span></p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }

    // Notice
    List<DocTag> noticeList = docData.getTags("notice");
    if (!noticeList.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Notice:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (DocTag tag : noticeList) {
        sb.append("<p>").append(formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }

    // Dev
    List<DocTag> devList = docData.getTags("dev");
    if (!devList.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Dev:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (DocTag tag : devList) {
        sb.append("<p>").append(formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }

    // Other tags (e.g., @author, @type, @module, custom)
    for (DocTag tag : docData.tags()) {
      if (isHandledTag(tag.name())) {
        continue;
      }
      String sectionTitle = capitalize(tag.name()) + ":";
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append(escapeHtml(sectionTitle))
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      sb.append("<p>");
      if (tag.target() != null && !tag.target().isEmpty()) {
        sb.append("<code>").append(escapeHtml(tag.target())).append("</code> &ndash; ");
      }
      sb.append(formatInlineDoc(tag.description())).append("</p>");
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  private static boolean isHandledTag(@NotNull String name) {
    return switch (name.toLowerCase()) {
      case "param", "parameter", "return", "returns", "throws", "throw", "see", "since", "deprecated", "notice", "dev" -> true;
      default -> false;
    };
  }

  // =========================================================================
  // Doc Extraction & Parsing
  // =========================================================================

  public static @Nullable ParsedDoc extractAndParseDoc(@NotNull PsiElement element) {
    List<String> rawLines = extractRawCommentLines(element);
    if (rawLines.isEmpty()) {
      return null;
    }
    return parseLines(rawLines);
  }

  public static @Nullable ParsedDoc parseDocCommentText(@NotNull String text) {
    List<String> lines = parseCommentTextIntoLines(text);
    if (lines.isEmpty()) {
      return null;
    }
    return parseLines(lines);
  }

  private static @NotNull List<String> extractRawCommentLines(@NotNull PsiElement element) {
    List<String> result = new ArrayList<>();
    List<PsiComment> comments = new ArrayList<>();

    PsiElement prev = getPrev(element);
    while (prev instanceof PsiWhiteSpace || prev instanceof PsiComment) {
      if (prev instanceof PsiComment comment) {
        comments.add(comment);
      }
      prev = prev.getPrevSibling();
    }

    if (comments.isEmpty()) {
      return result;
    }

    Collections.reverse(comments);
    for (PsiComment comment : comments) {
      result.addAll(parseCommentTextIntoLines(comment.getText()));
    }

    return result;
  }

  public static @NotNull List<String> parseCommentTextIntoLines(@NotNull String text) {
    List<String> lines = new ArrayList<>();
    String trimmed = text.trim();

    if (trimmed.startsWith("///") || trimmed.startsWith("//")) {
      String cleaned = trimmed.replaceFirst("^///?\\s*", "");
      lines.add(cleaned);
      return lines;
    }

    if (trimmed.startsWith("/*")) {
      String content = trimmed.replaceAll("^/\\*+\\s*", "").replaceAll("\\s*\\*+/$", "");
      String[] split = content.split("\\r?\\n");
      for (String rawLine : split) {
        String l = rawLine.trim();
        if (l.startsWith("*")) {
          l = l.substring(1).trim();
        }
        lines.add(l);
      }

      // Trim leading and trailing empty lines using SequencedCollection methods
      while (!lines.isEmpty() && lines.getFirst().isEmpty()) {
        lines.removeFirst();
      }
      while (!lines.isEmpty() && lines.getLast().isEmpty()) {
        lines.removeLast();
      }
    }

    return lines;
  }

  public static @NotNull ParsedDoc parseLines(@NotNull List<String> lines) {
    List<String> descriptionLines = new ArrayList<>();
    List<DocTag> tags = new ArrayList<>();
    DocTag currentTag = null;

    for (String line : lines) {
      Matcher tagMatcher = DOC_TAG_LINE_PATTERN.matcher(line);
      if (tagMatcher.find()) {
        String tagName = tagMatcher.group(1).toLowerCase();
        String remainder = tagMatcher.group(2) != null ? tagMatcher.group(2).trim() : "";

        String target = null;
        String desc = remainder;

        if (tagName.equals("param") || tagName.equals("parameter") || tagName.equals("type")
            || tagName.equals("throws") || tagName.equals("throw") || tagName.equals("module")) {
          int firstSpace = remainder.indexOf(' ');
          if (firstSpace != -1) {
            target = remainder.substring(0, firstSpace).trim();
            desc = remainder.substring(firstSpace + 1).trim();
          } else if (!remainder.isEmpty()) {
            target = remainder;
            desc = "";
          }
        }

        currentTag = new DocTag(tagName, target, desc);
        tags.add(currentTag);
      } else {
        if (currentTag != null) {
          if (!line.isEmpty()) {
            String updatedDesc = currentTag.description().isEmpty() ? line : currentTag.description() + " " + line;
            tags.set(tags.size() - 1, new DocTag(currentTag.name(), currentTag.target(), updatedDesc));
            currentTag = tags.getLast();
          }
        } else {
          descriptionLines.add(line);
        }
      }
    }

    return new ParsedDoc(descriptionLines, tags);
  }

  // =========================================================================
  // Parameter & Field Doc Inheritance
  // =========================================================================

  public static boolean isParameter(@NotNull PsiElement element) {
    if (element instanceof CompactParameterImpl) {
      return true;
    }
    if (element instanceof CompactPatternImpl pattern) {
      return PsiTreeUtil.getParentOfType(pattern, CompactParameterImpl.class) != null
          || PsiTreeUtil.getParentOfType(pattern, CompactTypedPatternImpl.class) != null
          || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
          || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.SIMPLE_PARAMETER_LIST)
          || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST);
    }
    return false;
  }

  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType type) {
    return CompactPsiUtil.hasAncestorOfType(element, type);
  }

  private static @Nullable ParsedDoc findParamDocFromEnclosing(@NotNull PsiElement param) {
    if (!(param instanceof PsiNamedElement named)) {
      return null;
    }
    String paramName = named.getName();
    if (paramName == null) {
      return null;
    }

    PsiElement enclosing = PsiTreeUtil.getParentOfType(param,
        CompactCircuitDefinition.class,
        CompactWitnessDeclaration.class,
        CompactConstructorDeclaration.class,
        CompactStructDefinition.class);
    if (enclosing == null) {
      return null;
    }

    ParsedDoc parentDoc = extractAndParseDoc(enclosing);
    if (parentDoc == null) {
      return null;
    }

    for (DocTag tag : parentDoc.getTags("param", "parameter")) {
      if (paramName.equals(tag.target()) && !tag.description().isEmpty()) {
        return new ParsedDoc(List.of(tag.description()), Collections.emptyList());
      }
    }
    return null;
  }

  private static @Nullable ParsedDoc findFieldDocFromParentStruct(@NotNull CompactStructFieldImpl field) {
    String fieldName = field.getName();
    if (fieldName == null) {
      return null;
    }

    CompactStructDefinition parentStruct = PsiTreeUtil.getParentOfType(field, CompactStructDefinition.class);
    if (parentStruct == null) {
      return null;
    }

    ParsedDoc parentDoc = extractAndParseDoc(parentStruct);
    if (parentDoc == null) {
      return null;
    }

    for (DocTag tag : parentDoc.getTags("param", "parameter", "field")) {
      if (fieldName.equals(tag.target()) && !tag.description().isEmpty()) {
        return new ParsedDoc(List.of(tag.description()), Collections.emptyList());
      }
    }
    return null;
  }

  // =========================================================================
  // Formatting & Markdown Helpers
  // =========================================================================

  public static @NotNull String formatInlineDoc(@NotNull String text) {
    String escaped = escapeHtml(text);

    // Code: `code`
    escaped = INLINE_CODE_PATTERN.matcher(escaped).replaceAll("<code>$1</code>");

    // Bold: **text**
    escaped = BOLD_PATTERN.matcher(escaped).replaceAll("<b>$1</b>");

    // Italic: *text*
    escaped = ITALIC_PATTERN.matcher(escaped).replaceAll("<i>$1</i>");

    // Markdown Links: [text](url)
    escaped = LINK_PATTERN.matcher(escaped).replaceAll("<a href=\"$2\">$1</a>");

    return escaped;
  }

  public static @NotNull String escapeHtml(@NotNull String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private static @NotNull String capitalize(@NotNull String str) {
    if (str.isEmpty()) return str;
    return Character.toUpperCase(str.charAt(0)) + str.substring(1);
  }

  private static PsiElement getPrev(@NonNull PsiElement element) {
    PsiElement target = element;
    while (target.getParent() != null
        && !(target.getParent() instanceof PsiFile)
        && !(target.getParent() instanceof CompactBlock)
        && !(target.getParent() instanceof CompactStructDefinition)
        && !(target.getParent() instanceof CompactEnumDefinition)
        && !(target.getParent() instanceof CompactExternalContractDeclaration)
        && !(target.getParent() instanceof CompactModuleDefinition)) {
      target = target.getParent();
    }

    return target.getPrevSibling();
  }

  // =========================================================================
  // ParsedDoc & DocTag Records (Java 16+)
  // =========================================================================

  public record ParsedDoc(@NotNull List<String> descriptionLines, @NotNull List<DocTag> tags) {
    public boolean isEmpty() {
      return descriptionLines.isEmpty() && tags.isEmpty();
    }

    public boolean hasDescription() {
      for (String line : descriptionLines) {
        if (!line.trim().isEmpty()) {
          return true;
        }
      }
      return false;
    }

    public @NotNull String renderDescriptionHtml() {
      List<String> paragraphs = new ArrayList<>();
      StringBuilder currentPara = new StringBuilder();

      for (String line : descriptionLines) {
        if (line.trim().isEmpty()) {
          if (!currentPara.isEmpty()) {
            paragraphs.add(currentPara.toString().trim());
            currentPara.setLength(0);
          }
        } else {
          if (!currentPara.isEmpty()) {
            currentPara.append(" ");
          }
          currentPara.append(line.trim());
        }
      }
      if (!currentPara.isEmpty()) {
        paragraphs.add(currentPara.toString().trim());
      }

      StringBuilder html = new StringBuilder();
      for (String p : paragraphs) {
        html.append("<p>").append(formatInlineDoc(p)).append("</p>");
      }
      return html.toString();
    }

    public @NotNull List<DocTag> getTags(String... names) {
      List<DocTag> result = new ArrayList<>();
      for (DocTag tag : tags) {
        for (String name : names) {
          if (tag.name().equalsIgnoreCase(name)) {
            result.add(tag);
            break;
          }
        }
      }
      return result;
    }
  }

  public record DocTag(@NotNull String name, @Nullable String target, @NotNull String description) {}
}

