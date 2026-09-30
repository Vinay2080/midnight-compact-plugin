package dev.verloren.midnight.documentation.docstring;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactEnumDefinition;
import dev.verloren.midnight.psi.CompactExternalContractDeclaration;
import dev.verloren.midnight.psi.CompactModuleDefinition;
import dev.verloren.midnight.psi.CompactParameterImpl;
import dev.verloren.midnight.psi.CompactPatternImpl;
import dev.verloren.midnight.psi.CompactPsiUtil;
import dev.verloren.midnight.psi.CompactStructDefinition;
import dev.verloren.midnight.psi.CompactStructFieldImpl;
import dev.verloren.midnight.psi.CompactTypedPatternImpl;
import dev.verloren.midnight.psi.CompactWitnessDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser and markdown renderer for Compact documentation comments ({@code /** ... *&#47;} and {@code ///}).
 */
public final class CompactDocParser {

  private static final Pattern DOC_TAG_LINE_PATTERN =
      Pattern.compile("^@([a-zA-Z_][a-zA-Z0-9_:-]*)(?:\\s+(.*))?$");
  private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
  private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
  private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");
  private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");

  public record DocTag(@NotNull String name, @Nullable String target, @NotNull String description) {}

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

  private CompactDocParser() {}

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

  public static @NotNull List<String> extractRawCommentLines(@NotNull PsiElement element) {
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
        currentTag = parseTagLine(tagMatcher);
        tags.add(currentTag);
      } else {
        currentTag = appendToCurrentTagOrDesc(line, currentTag, tags, descriptionLines);
      }
    }

    return new ParsedDoc(descriptionLines, tags);
  }

  private static @NotNull DocTag parseTagLine(@NotNull Matcher tagMatcher) {
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
    return new DocTag(tagName, target, desc);
  }

  private static @Nullable DocTag appendToCurrentTagOrDesc(
      @NotNull String line,
      @Nullable DocTag currentTag,
      @NotNull List<DocTag> tags,
      @NotNull List<String> descriptionLines
  ) {
    if (currentTag != null) {
      if (!line.isEmpty()) {
        String updatedDesc = currentTag.description().isEmpty() ? line : currentTag.description() + " " + line;
        tags.set(tags.size() - 1, new DocTag(currentTag.name(), currentTag.target(), updatedDesc));
        return tags.getLast();
      }
      return currentTag;
    }
    descriptionLines.add(line);
    return null;
  }

  public static boolean isParameter(@NotNull PsiElement element) {
    if (element instanceof CompactParameterImpl) {
      return true;
    }
    if (element instanceof CompactPatternImpl pattern) {
      return PsiTreeUtil.getParentOfType(pattern, CompactParameterImpl.class) != null
          || PsiTreeUtil.getParentOfType(pattern, CompactTypedPatternImpl.class) != null
          || hasAncestorOfType(pattern, CompactElementTypes.PATTERN_PARAMETER_LIST)
          || hasAncestorOfType(pattern, CompactElementTypes.SIMPLE_PARAMETER_LIST)
          || hasAncestorOfType(pattern, CompactElementTypes.ARROW_PARAMETER_LIST);
    }
    return false;
  }

  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull IElementType type) {
    return CompactPsiUtil.hasAncestorOfType(element, type);
  }

  public static @Nullable ParsedDoc findParamDocFromEnclosing(@NotNull PsiElement param) {
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

  public static @Nullable ParsedDoc findFieldDocFromParentStruct(@NotNull CompactStructFieldImpl field) {
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

  public static @NotNull String formatInlineDoc(@NotNull String text) {
    String escaped = escapeHtml(text);
    escaped = INLINE_CODE_PATTERN.matcher(escaped).replaceAll("<code>$1</code>");
    escaped = BOLD_PATTERN.matcher(escaped).replaceAll("<b>$1</b>");
    escaped = ITALIC_PATTERN.matcher(escaped).replaceAll("<i>$1</i>");
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

  public static @NotNull String capitalize(@NotNull String str) {
    if (str.isEmpty()) return str;
    return Character.toUpperCase(str.charAt(0)) + str.substring(1);
  }

  public static PsiElement getPrev(@NotNull PsiElement element) {
    PsiElement target = element;
    while (target.getParent() != null) {
      PsiElement parent = target.getParent();
      switch (parent) {
        case PsiFile _,
             CompactBlock _,
             CompactStructDefinition _,
             CompactEnumDefinition _,
             CompactExternalContractDeclaration _,
             CompactModuleDefinition _ -> {
          return target.getPrevSibling();
        }
        default -> target = parent;
      }
    }
    return target.getPrevSibling();
  }
}
