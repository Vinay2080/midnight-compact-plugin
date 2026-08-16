package dev.verloren.midnight.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompactDocumentationProvider extends AbstractDocumentationProvider {

  @Override
  public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    if (element == null) {
      return null;
    }
    return getDefinitionHeader(element);
  }

  @Override
  public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element == null) {
      return null;
    }

    String header = getDefinitionHeader(element);
    if (header == null) {
      return null;
    }

    StringBuilder doc = new StringBuilder();
    doc.append("<div class='definition'><pre>");
    doc.append(escapeHtml(header));
    doc.append("</pre></div>");

    String docComments = extractDocComments(element);
    if (docComments != null && !docComments.isEmpty()) {
      doc.append("<div class='content'>");
      doc.append(docComments);
      doc.append("</div>");
    }

    String extraDetails = getExtraDetails(element);
    if (extraDetails != null && !extraDetails.isEmpty()) {
      doc.append("<div class='sections'>");
      doc.append(extraDetails);
      doc.append("</div>");
    }

    return doc.toString();
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

    // If context element or any ancestor has a reference, resolve it first
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      if (p.getReference() != null) {
        PsiElement resolved = p.getReference().resolve();
        if (resolved != null) {
          return resolved;
        }
      }
    }

    // Otherwise walk up to nearest named declaration element
    for (PsiElement p = contextElement; p != null && p != file; p = p.getParent()) {
      if (p instanceof CompactStructFieldImpl) {
        return p;
      }
      if (p instanceof CompactEnumMemberImpl) {
        return p;
      }
      if (p instanceof CompactParameterImpl && p.getParent() instanceof CompactStructFieldImpl) {
        return p.getParent();
      }
      if (p instanceof CompactPatternImpl) {
        return p;
      }
      if (p instanceof CompactNamedElement) {
        return p;
      }
    }
    return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
  }

  public static @Nullable String getDefinitionHeader(@NotNull PsiElement element) {
    if (element instanceof CompactCircuitDefinition circuit) {
      String name = circuit.getName() != null ? circuit.getName() : "circuit";
      return "circuit " + name + getSignatureSuffix(circuit);
    }
    if (element instanceof CompactWitnessDeclaration witness) {
      String name = witness.getName() != null ? witness.getName() : "witness";
      return "witness " + name + getSignatureSuffix(witness);
    }
    if (element instanceof CompactConstructorDeclaration) {
      return "constructor" + getSignatureSuffix(element);
    }
    if (element instanceof CompactExternalContractDeclaration) {
      String name = ((CompactExternalContractDeclaration) element).getName();
      return "contract " + (name != null ? name : "");
    }
    if (element instanceof CompactContractImplementsDeclaration) {
      return element.getText().trim();
    }
    if (element instanceof CompactModuleDefinition) {
      String name = ((CompactModuleDefinition) element).getName();
      return "module " + (name != null ? name : "");
    }
    if (element instanceof CompactStructDefinition) {
      String name = ((CompactStructDefinition) element).getName();
      return "struct " + (name != null ? name : "");
    }
    if (element instanceof CompactStructFieldImpl) {
      CompactStructFieldImpl field = (CompactStructFieldImpl) element;
      String name = field.getName() != null ? field.getName() : "field";
      CompactType type = field.getType();
      CompactStructDefinition parentStruct = PsiTreeUtil.getParentOfType(field, CompactStructDefinition.class);
      String prefix = parentStruct != null && parentStruct.getName() != null ? parentStruct.getName() + "." : "";
      return "struct field " + prefix + name + ": " + type.name();
    }
    if (element instanceof CompactEnumDefinition) {
      String name = ((CompactEnumDefinition) element).getName();
      return "enum " + (name != null ? name : "");
    }
    if (element instanceof CompactEnumMemberImpl member) {
      String name = member.getName() != null ? member.getName() : "member";
      CompactEnumDefinition parentEnum = PsiTreeUtil.getParentOfType(member, CompactEnumDefinition.class);
      String prefix = parentEnum != null && parentEnum.getName() != null ? parentEnum.getName() + "." : "";
      return "enum variant " + prefix + name;
    }
    if (element instanceof CompactTypeDefinition typeDef) {
      String name = typeDef.getName() != null ? typeDef.getName() : "type";
      return "type " + name + " = " + typeDef.getType().name();
    }
    if (element instanceof CompactPatternImpl || element instanceof CompactConstBindingImpl) {
      String name = ((PsiNamedElement) element).getName();
      CompactType type = ((CompactTypeElement) element).getType();
      return "const " + (name != null ? name : "const") + ": " + type.name();
    }
    if (element instanceof CompactParameterImpl param) {
      String name = param.getName() != null ? param.getName() : "param";
      CompactType type = param.getType();
      return "parameter " + name + ": " + type.name();
    }
    if (element instanceof CompactLedgerDeclaration) {
      return element.getText().trim();
    }
    if (element instanceof CompactPragmaForm) {
      return element.getText().trim();
    }
    if (element instanceof CompactNamedElement) {
      String name = ((CompactNamedElement) element).getName();
      return name != null ? name : element.getText();
    }
    return null;
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

  private static @Nullable String getExtraDetails(@NotNull PsiElement element) {
    if (element instanceof CompactStructDefinition) {
      List<CompactStructFieldImpl> fields = new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, CompactStructFieldImpl.class));
      if (!fields.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p><b>Fields:</b></p><ul>");
        for (CompactStructFieldImpl f : fields) {
          sb.append("<li><code>").append(escapeHtml(f.getName() != null ? f.getName() : "_"))
              .append(": ").append(escapeHtml(f.getType().name())).append("</code></li>");
        }
        sb.append("</ul>");
        return sb.toString();
      }
    }
    if (element instanceof CompactEnumDefinition) {
      List<CompactEnumMemberImpl> members = new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, CompactEnumMemberImpl.class));
      if (!members.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p><b>Variants:</b></p><ul>");
        for (CompactEnumMemberImpl m : members) {
          sb.append("<li><code>").append(escapeHtml(m.getName() != null ? m.getName() : "_")).append("</code></li>");
        }
        sb.append("</ul>");
        return sb.toString();
      }
    }
    return null;
  }

  private static @Nullable String extractDocComments(@NotNull PsiElement element) {
    List<String> commentLines = new ArrayList<>();

    PsiElement target = element;
    while (target != null && target.getParent() != null &&
           !(target.getParent() instanceof PsiFile) &&
           !(target.getParent() instanceof CompactBlock) &&
           !(target.getParent() instanceof CompactStructDefinition) &&
           !(target.getParent() instanceof CompactEnumDefinition) &&
           !(target.getParent() instanceof CompactExternalContractDeclaration) &&
           !(target.getParent() instanceof CompactModuleDefinition)) {
      target = target.getParent();
    }

    if (target == null) {
      target = element;
    }

    PsiElement prev = target.getPrevSibling();
    while (prev instanceof PsiWhiteSpace || prev instanceof PsiComment) {
      if (prev instanceof PsiComment) {
        String commentText = prev.getText().trim();
        if (commentText.startsWith("///") || commentText.startsWith("//")) {
          String cleaned = commentText.replaceFirst("^///?\\s*", "");
          commentLines.add(cleaned);
        } else if (commentText.startsWith("/*")) {
          String cleaned = commentText.replaceAll("^/\\*+\\s*", "").replaceAll("\\s*\\*+/$", "").trim();
          commentLines.add(cleaned);
        }
      }
      prev = prev.getPrevSibling();
    }
    if (commentLines.isEmpty()) {
      return null;
    }
    Collections.reverse(commentLines);
    StringBuilder html = new StringBuilder();
    for (String line : commentLines) {
      if (!html.isEmpty()) html.append("<br>");
      html.append(escapeHtml(line));
    }
    return html.toString();
  }

  private static @NotNull String escapeHtml(@NotNull String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
