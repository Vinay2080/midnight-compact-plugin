package dev.verloren.midnight.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.documentation.docstring.CompactDocParser;
import dev.verloren.midnight.documentation.docstring.CompactDocSignatureRenderer;
import dev.verloren.midnight.documentation.docstring.CompactStandardLibraryDoc;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactContractImplementsDeclaration;
import dev.verloren.midnight.psi.CompactElementFactory;
import dev.verloren.midnight.psi.CompactEnumMemberImpl;
import dev.verloren.midnight.psi.CompactExportDeclaration;
import dev.verloren.midnight.psi.CompactImportDeclarationImpl;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactParameterImpl;
import dev.verloren.midnight.psi.CompactPatternImpl;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.psi.CompactStructFieldImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Quick Documentation provider for Compact declarations, references, and doc comments.
 *
 * <p>Extends {@link AbstractDocumentationProvider} to format rich HTML documentation popups.
 * Delegates parsing to {@link CompactDocParser}, signature rendering to
 * {@link CompactDocSignatureRenderer}, and standard library documentation to
 * {@link CompactStandardLibraryDoc}.</p>
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

    PsiElement resolved = findResolvedTarget(contextElement, file);
    if (resolved != null) {
      return resolved;
    }
    return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
  }

  private static @Nullable PsiElement findResolvedTarget(@NotNull PsiElement contextElement, @NotNull PsiFile file) {
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      if (p.getReference() != null) {
        PsiElement target = p.getReference().resolve();
        if (target != null) return target;
      }
    }
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      if (p instanceof PsiComment comment) {
        PsiElement target = findTargetDeclarationForComment(comment);
        return target != null ? target : comment;
      }
    }
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      switch (p) {
        case CompactStructFieldImpl _, CompactEnumMemberImpl _ -> { return p; }
        case CompactParameterImpl param when param.getParent() instanceof CompactStructFieldImpl -> {
          return param.getParent();
        }
        case CompactImportDeclarationImpl importDecl -> { return importDecl; }
        case CompactPatternImpl _, CompactPragmaForm _, CompactNamedElement _ -> { return p; }
        default -> {}
      }
    }
    return null;
  }

  public static @Nullable PsiElement findTargetDeclarationForComment(@NotNull PsiComment comment) {
    if (comment instanceof CompactDocComment docComment) {
      comment = docComment.getDelegate();
    }
    PsiElement next = comment.getNextSibling();
    while (next instanceof PsiWhiteSpace || next instanceof PsiComment) {
      next = next.getNextSibling();
    }
    return switch (next) {
      case CompactNamedElement _,
           CompactConstructorDeclaration _,
           CompactContractImplementsDeclaration _,
           CompactPragmaForm _ -> next;
      case CompactExportDeclaration exportDecl -> {
        for (PsiElement child : exportDecl.getChildren()) {
          if (child instanceof CompactNamedElement) {
            yield child;
          }
        }
        yield exportDecl;
      }
      case null -> null;
      default -> PsiTreeUtil.findChildOfType(next, CompactNamedElement.class);
    };
  }

  public static @Nullable String getDefinitionHeader(@NotNull PsiElement element) {
    return CompactDocSignatureRenderer.getDefinitionHeader(element);
  }
}
