package dev.verloren.midnight.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.documentation.docstring.CompactDocParser;
import dev.verloren.midnight.documentation.docstring.CompactDocSignatureRenderer;
import dev.verloren.midnight.documentation.docstring.CompactStandardLibraryDoc;
import dev.verloren.midnight.psi.CompactElementFactory;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.psi.CompactStructFieldImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Quick Documentation and Doc Comment provider for Compact smart contract files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.lang.documentationProvider} extension
 * point for {@link dev.verloren.midnight.CompactLanguage}. Powers all interactive documentation features:
 * <ul>
 *   <li><b>Quick Documentation (Ctrl+Q / Cmd+J / Hover):</b> Formats rich HTML documentation popups displaying
 *       colorized code definitions, description text, {@code @param}/{@code @return} sections, and throws specifications.</li>
 *   <li><b>Quick Navigate Info (Ctrl + Mouse Hover):</b> Generates concise one-line signature tooltips via {@link #getQuickNavigateInfo}.</li>
 *   <li><b>In-Editor Rendered Documentation:</b> Implements {@link #collectDocComments}, {@link #findDocComment},
 *       and {@link #generateRenderedDoc} to support Reader Mode and gutter documentation toggles (Ctrl+Alt+Q).</li>
 *   <li><b>Standard Library Grounding:</b> Supplies built-in documentation for core ZKIR and standard library
 *       constructs via {@link CompactStandardLibraryDoc}.</li>
 *   <li><b>Completion Item Documentation:</b> Generates on-the-fly documentation for code completion lookup items
 *       (e.g. pragma version directives) via {@link #getDocumentationElementForLookupItem}.</li>
 *   <li><b>Pragma Directives:</b> Provides dedicated documentation explanations and constraints for
 *       {@code language_version} and {@code compiler_version} pragmas.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by {@code com.intellij.codeInsight.documentation.DocumentationManager}:
 * <ol>
 *   <li>The platform identifies the target symbol via {@link #getCustomDocumentationElement}.</li>
 *   <li>{@link #generateDoc} resolves the definition signature via {@link CompactDocSignatureRenderer},
 *       parses doc comments via {@link CompactDocParser}, and inspects inherited docs ({@link #resolveInheritedDoc}).</li>
 *   <li>Formats the resulting HTML adhering to IntelliJ's standard {@link DocumentationMarkup} structure.</li>
 * </ol>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Query methods run either on pooled background worker threads (during hover or async doc fetching)
 *       or on the Event Dispatch Thread (EDT).</li>
 *   <li><b>Read Action:</b> All methods require a platform Read Action ({@code @RequiresReadLock}) while inspecting
 *       PSI elements, traversing siblings, and resolving references.</li>
 *   <li><b>Write Actions:</b> Pure read-only documentation formatting; never acquires write actions.</li>
 * </ul>
 *
 * <h3>Documentation Rendering Pipeline</h3>
 * <pre>
 *   [User presses Ctrl+Q or hovers over symbol/doc]
 *                          │
 *                          ▼
 *            [CompactDocumentationProvider]
 *                          │
 *          ┌───────────────┼───────────────┐
 *          ▼               ▼               ▼
 *   [Definition Header] [Description]   [Doc Sections]
 *   CompactDocSignature  CompactDoc     Parameters, Returns,
 *   Renderer             Parser/Stdlib  Pragma constraints
 *          │               │               │
 *          └───────────────┼───────────────┘
 *                          ▼
 *         Rich HTML Popup (DocumentationMarkup)
 * </pre>
 *
 * @see dev.verloren.midnight.documentation.CompactDocComment
 * @see dev.verloren.midnight.documentation.docstring.CompactDocParser
 * @see dev.verloren.midnight.documentation.docstring.CompactDocSignatureRenderer
 * @see dev.verloren.midnight.documentation.docstring.CompactStandardLibraryDoc
 * @see com.intellij.lang.documentation.AbstractDocumentationProvider
 */
public class CompactDocumentationProvider extends AbstractDocumentationProvider {

  public record ParsedDoc(CompactDocParser.ParsedDoc delegate) {
    public boolean isEmpty() {
      return delegate.isEmpty();
    }
  }

  public record DocTag(CompactDocParser.DocTag delegate) {}

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
  public @Nullable PsiElement getDocumentationElementForLookupItem(
      @NotNull PsiManager psiManager,
      @Nullable Object object,
      @Nullable PsiElement element
  ) {
    if (object instanceof String str && ("language_version".equals(str) || "compiler_version".equals(str))) {
      return CompactElementFactory.createPragmaForm(psiManager.getProject(), "pragma " + str + ";");
    }
    return super.getDocumentationElementForLookupItem(psiManager, object, element);
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

    CompactDocParser.ParsedDoc docData = CompactDocParser.extractAndParseDoc(element);
    if (element instanceof CompactPragmaForm pragma) {
      return generatePragmaDoc(pragma, docData);
    }

    return assembleFullDocumentation(element, header, docData);
  }

  private static @NotNull String assembleFullDocumentation(
      @NotNull PsiElement element,
      @NotNull String header,
      @Nullable CompactDocParser.ParsedDoc docData
  ) {
    StringBuilder doc = new StringBuilder();
    doc.append(DocumentationMarkup.DEFINITION_START);
    doc.append(CompactDocParser.escapeHtml(header));
    doc.append(DocumentationMarkup.DEFINITION_END);

    CompactDocParser.ParsedDoc effectiveDoc = resolveInheritedDoc(element, docData);
    if (effectiveDoc != null && effectiveDoc.hasDescription()) {
      doc.append(DocumentationMarkup.CONTENT_START);
      doc.append(effectiveDoc.renderDescriptionHtml());
      doc.append(DocumentationMarkup.CONTENT_END);
    } else {
      String stdlibDesc = CompactStandardLibraryDoc.getStandardLibraryDescription(element);
      if (stdlibDesc != null) {
        doc.append(DocumentationMarkup.CONTENT_START);
        doc.append(stdlibDesc);
        doc.append(DocumentationMarkup.CONTENT_END);
      }
    }

    String sectionsHtml = CompactDocSignatureRenderer.renderSections(element, effectiveDoc);
    if (sectionsHtml != null && !sectionsHtml.isEmpty()) {
      doc.append(DocumentationMarkup.SECTIONS_START);
      doc.append(sectionsHtml);
      doc.append(DocumentationMarkup.SECTIONS_END);
    }

    return doc.toString();
  }

  private static @Nullable CompactDocParser.ParsedDoc resolveInheritedDoc(
      @NotNull PsiElement element,
      @Nullable CompactDocParser.ParsedDoc docData
  ) {
    if ((docData == null || docData.isEmpty()) && CompactDocParser.isParameter(element)) {
      return CompactDocParser.findParamDocFromEnclosing(element);
    }
    if ((docData == null || docData.isEmpty()) && element instanceof CompactStructFieldImpl field) {
      return CompactDocParser.findFieldDocFromParentStruct(field);
    }
    return docData;
  }

  private static @NotNull String generatePragmaDoc(@NotNull CompactPragmaForm pragma, @Nullable CompactDocParser.ParsedDoc docData) {
    StringBuilder doc = new StringBuilder();
    doc.append(DocumentationMarkup.DEFINITION_START);
    doc.append(CompactDocParser.escapeHtml(pragma.getText().trim()));
    doc.append(DocumentationMarkup.DEFINITION_END);

    String pragmaName = pragma.getPragmaName();
    doc.append(DocumentationMarkup.CONTENT_START);
    if (docData != null && docData.hasDescription()) {
      doc.append(docData.renderDescriptionHtml());
    }
    appendPragmaDescription(doc, pragmaName);
    doc.append(DocumentationMarkup.CONTENT_END);

    appendPragmaSections(doc, pragma, pragmaName, docData);
    return doc.toString();
  }

  private static void appendPragmaDescription(@NotNull StringBuilder doc, @Nullable String pragmaName) {
    if ("language_version".equals(pragmaName)) {
      doc.append("<p>Specifies the required version of the Compact language specification for this contract. ")
          .append("The compiler validates this constraint against the language version supported by the toolchain.</p>");
    } else if ("compiler_version".equals(pragmaName)) {
      doc.append("<p>Specifies the required version of the Compact compiler (<code>compactc</code>) for this contract. ")
          .append("The compiler validates this constraint against its own build version.</p>");
    } else {
      doc.append("<p>Pragma directive configuring compilation settings for this contract. ")
          .append("Allowed pragma directives in Compact are <code>language_version</code> and <code>compiler_version</code>.</p>");
    }
  }

  private static void appendPragmaSections(
      @NotNull StringBuilder doc,
      @NotNull CompactPragmaForm pragma,
      @Nullable String pragmaName,
      @Nullable CompactDocParser.ParsedDoc docData
  ) {
    doc.append(DocumentationMarkup.SECTIONS_START);
    if (pragmaName != null) {
      doc.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Directive:")
          .append(DocumentationMarkup.SECTION_SEPARATOR)
          .append("<p><code>").append(CompactDocParser.escapeHtml(pragmaName)).append("</code></p>")
          .append(DocumentationMarkup.SECTION_END);
    }
    String constraint = pragma.getConstraintText();
    if (constraint != null) {
      doc.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Constraint:")
          .append(DocumentationMarkup.SECTION_SEPARATOR)
          .append("<p><code>").append(CompactDocParser.escapeHtml(constraint)).append("</code></p>")
          .append(DocumentationMarkup.SECTION_END);
    }
    doc.append(DocumentationMarkup.SECTION_HEADER_START)
        .append("Allowed Settings:")
        .append(DocumentationMarkup.SECTION_SEPARATOR)
        .append("<p><code>language_version</code>, <code>compiler_version</code></p>")
        .append(DocumentationMarkup.SECTION_END);

    if (docData != null) {
      CompactDocSignatureRenderer.appendDocTagSections(doc, docData);
    }
    doc.append(DocumentationMarkup.SECTIONS_END);
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
    CompactDocParser.ParsedDoc docData = CompactDocParser.parseDocCommentText(comment.getText());
    if (docData == null || docData.isEmpty()) {
      return null;
    }

    StringBuilder doc = new StringBuilder();
    if (docData.hasDescription()) {
      doc.append(DocumentationMarkup.CONTENT_START);
      doc.append(docData.renderDescriptionHtml());
      doc.append(DocumentationMarkup.CONTENT_END);
    }

    String sectionsHtml = CompactDocSignatureRenderer.renderDocTagSectionsOnly(docData);
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
    if (contextElement instanceof PsiWhiteSpace) {
      PsiElement prev = contextElement.getPrevSibling();
      if (prev != null) {
        contextElement = prev;
      }
    }

    PsiElement resolved = CompactDocumentationTargetResolver.findResolvedTarget(contextElement, file);
    if (resolved != null) {
      return resolved;
    }
    return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
  }

  public static @Nullable PsiElement findTargetDeclarationForComment(@NotNull PsiComment comment) {
    return CompactDocumentationTargetResolver.findTargetDeclarationForComment(comment);
  }

  public static @Nullable String getDefinitionHeader(@NotNull PsiElement element) {
    return CompactDocSignatureRenderer.getDefinitionHeader(element);
  }
}
