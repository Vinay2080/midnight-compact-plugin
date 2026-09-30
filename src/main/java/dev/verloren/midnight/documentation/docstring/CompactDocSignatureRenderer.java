package dev.verloren.midnight.documentation.docstring;

import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactContractImplementsDeclaration;
import dev.verloren.midnight.psi.CompactEnumDefinition;
import dev.verloren.midnight.psi.CompactEnumMemberImpl;
import dev.verloren.midnight.psi.CompactExportDeclaration;
import dev.verloren.midnight.psi.CompactExternalContractDeclaration;
import dev.verloren.midnight.psi.CompactImportDeclarationImpl;
import dev.verloren.midnight.psi.CompactLedgerDeclaration;
import dev.verloren.midnight.psi.CompactModuleDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactParameterImpl;
import dev.verloren.midnight.psi.CompactPatternImpl;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.psi.CompactStructDefinition;
import dev.verloren.midnight.psi.CompactStructFieldImpl;
import dev.verloren.midnight.psi.CompactTypeDefinition;
import dev.verloren.midnight.psi.CompactTypeElement;
import dev.verloren.midnight.psi.CompactWitnessDeclaration;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer for Compact definition headers, method signatures, and structured section tables.
 */
public final class CompactDocSignatureRenderer {

  private CompactDocSignatureRenderer() {}

  public static @Nullable String getDefinitionHeader(@NotNull PsiElement element) {
    if (CompactDocParser.isParameter(element)) {
      String name = ((PsiNamedElement) element).getName();
      CompactType type = ((CompactTypeElement) element).getType();
      return "parameter " + (name != null ? name : "param") + ": " + type.name();
    }
    String topLevel = formatTopLevelHeader(element);
    if (topLevel != null) {
      return topLevel;
    }
    return formatMemberHeader(element);
  }

  private static @Nullable String formatTopLevelHeader(@NotNull PsiElement element) {
    return switch (element) {
      case CompactFile file -> formatFileHeader(file);
      case CompactImportDeclarationImpl importDecl -> formatImportHeader(importDecl);
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
      default -> null;
    };
  }

  private static @Nullable String formatMemberHeader(@NotNull PsiElement element) {
    return switch (element) {
      case CompactStructDefinition struct -> {
        String name = struct.getName();
        yield "struct " + (name != null ? name : "");
      }
      case CompactStructFieldImpl field -> formatStructFieldHeader(field);
      case CompactEnumDefinition enumDef -> {
        String name = enumDef.getName();
        yield "enum " + (name != null ? name : "");
      }
      case CompactEnumMemberImpl member -> formatEnumMemberHeader(member);
      case CompactTypeDefinition typeDef -> {
        String name = typeDef.getName() != null ? typeDef.getName() : "type";
        yield "type " + name + " = " + typeDef.getType().name();
      }
      case CompactPatternImpl pattern -> formatBindingHeader(pattern);
      case CompactConstBindingImpl constBinding -> formatBindingHeader(constBinding);
      case CompactLedgerDeclaration ledger -> ledger.getText().trim();
      case CompactPragmaForm pragma -> pragma.getText().trim();
      case CompactNamedElement named -> named.getName() != null ? named.getName() : named.getText();
      default -> null;
    };
  }

  private static @NotNull String formatFileHeader(@NotNull CompactFile file) {
    String name = file.getName();
    if ("standard-library.compact".equals(name)) {
      return "standard library CompactStandardLibrary";
    }
    if ("zkir-v3-library.compact".equals(name)) {
      return "standard library zkir-v3-library";
    }
    return "file " + name;
  }

  private static @NotNull String formatImportHeader(@NotNull CompactImportDeclarationImpl importDecl) {
    String mod = importDecl.getModuleName();
    if ("CompactStandardLibrary".equals(mod)) {
      return "standard library CompactStandardLibrary";
    }
    return "import " + (mod != null ? mod : (importDecl.getImportPath() != null ? importDecl.getImportPath() : ""));
  }

  private static @NotNull String formatStructFieldHeader(@NotNull CompactStructFieldImpl field) {
    String name = field.getName() != null ? field.getName() : "field";
    CompactType type = field.getType();
    CompactStructDefinition parentStruct = PsiTreeUtil.getParentOfType(field, CompactStructDefinition.class);
    String prefix = parentStruct != null && parentStruct.getName() != null ? parentStruct.getName() + "." : "";
    return "struct field " + prefix + name + ": " + type.name();
  }

  private static @NotNull String formatEnumMemberHeader(@NotNull CompactEnumMemberImpl member) {
    String name = member.getName() != null ? member.getName() : "member";
    CompactEnumDefinition parentEnum = PsiTreeUtil.getParentOfType(member, CompactEnumDefinition.class);
    String prefix = parentEnum != null && parentEnum.getName() != null ? parentEnum.getName() + "." : "";
    return "enum variant " + prefix + name;
  }

  public static @NotNull String formatBindingHeader(@NotNull CompactNamedElement element) {
    String name = element.getName();
    CompactType type = element.getType();
    boolean isLocal = PsiTreeUtil.getParentOfType(element, CompactBlock.class) != null;
    String kind = isLocal ? "local " : "const ";
    return kind + (name != null ? name : "const") + ": " + type.name();
  }

  public static @NotNull String getSignatureSuffix(@NotNull PsiElement element) {
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

  public static @Nullable String renderSections(@NotNull PsiElement element, @Nullable CompactDocParser.ParsedDoc docData) {
    StringBuilder sb = new StringBuilder();
    if (docData != null) {
      appendDocTagSections(sb, docData);
    }
    if (element instanceof CompactStructDefinition) {
      appendStructFieldsSection(sb, element);
    }
    if (element instanceof CompactEnumDefinition) {
      appendEnumVariantsSection(sb, element);
    }
    return sb.isEmpty() ? null : sb.toString();
  }

  private static void appendStructFieldsSection(@NotNull StringBuilder sb, @NotNull PsiElement element) {
    List<CompactStructFieldImpl> fields = new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, CompactStructFieldImpl.class));
    if (!fields.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Fields:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (CompactStructFieldImpl field : fields) {
        String fieldName = field.getName() != null ? field.getName() : "_";
        String fieldType = field.getType().name();
        sb.append("<p><code>").append(CompactDocParser.escapeHtml(fieldName)).append(": ").append(CompactDocParser.escapeHtml(fieldType)).append("</code></p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  private static void appendEnumVariantsSection(@NotNull StringBuilder sb, @NotNull PsiElement element) {
    List<CompactEnumMemberImpl> members = new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, CompactEnumMemberImpl.class));
    if (!members.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Variants:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (CompactEnumMemberImpl member : members) {
        String memberName = member.getName() != null ? member.getName() : "_";
        sb.append("<p><code>").append(CompactDocParser.escapeHtml(memberName)).append("</code></p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  public static @Nullable String renderDocTagSectionsOnly(@NotNull CompactDocParser.ParsedDoc docData) {
    StringBuilder sb = new StringBuilder();
    appendDocTagSections(sb, docData);
    return sb.isEmpty() ? null : sb.toString();
  }

  public static void appendDocTagSections(@NotNull StringBuilder sb, @NotNull CompactDocParser.ParsedDoc docData) {
    appendParamsSection(sb, docData);
    appendReturnsSection(sb, docData);
    appendThrowsSection(sb, docData);
    appendCommonTagSection(sb, "See also:", docData.getTags("see"));
    appendCommonTagSection(sb, "Since:", docData.getTags("since"));
    appendDeprecatedSection(sb, docData.getTags("deprecated"));
    appendCommonTagSection(sb, "Notice:", docData.getTags("notice"));
    appendCommonTagSection(sb, "Dev:", docData.getTags("dev"));
    appendOtherTagsSection(sb, docData);
  }

  private static void appendParamsSection(@NotNull StringBuilder sb, @NotNull CompactDocParser.ParsedDoc docData) {
    List<CompactDocParser.DocTag> params = docData.getTags("param", "parameter");
    if (!params.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Params:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (CompactDocParser.DocTag tag : params) {
        sb.append("<p><code>").append(CompactDocParser.escapeHtml(tag.target() != null ? tag.target() : "_")).append("</code>");
        if (!tag.description().isEmpty()) {
          sb.append(" &ndash; ").append(CompactDocParser.formatInlineDoc(tag.description()));
        }
        sb.append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  private static void appendReturnsSection(@NotNull StringBuilder sb, @NotNull CompactDocParser.ParsedDoc docData) {
    List<CompactDocParser.DocTag> returns = docData.getTags("return", "returns");
    if (!returns.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Returns:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (CompactDocParser.DocTag tag : returns) {
        sb.append("<p>").append(CompactDocParser.formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  private static void appendThrowsSection(@NotNull StringBuilder sb, @NotNull CompactDocParser.ParsedDoc docData) {
    List<CompactDocParser.DocTag> throwsList = docData.getTags("throws", "throw");
    if (!throwsList.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Throws:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (CompactDocParser.DocTag tag : throwsList) {
        sb.append("<p>");
        if (tag.target() != null && !tag.target().isEmpty()) {
          sb.append("<code>").append(CompactDocParser.escapeHtml(tag.target())).append("</code> &ndash; ");
        }
        sb.append(CompactDocParser.formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  private static void appendDeprecatedSection(@NotNull StringBuilder sb, @NotNull List<CompactDocParser.DocTag> tags) {
    if (!tags.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append("Deprecated:")
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (CompactDocParser.DocTag tag : tags) {
        sb.append("<p><span class='deprecated'>").append(CompactDocParser.formatInlineDoc(tag.description())).append("</span></p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  private static void appendCommonTagSection(@NotNull StringBuilder sb, @NotNull String title, @NotNull List<CompactDocParser.DocTag> tags) {
    if (!tags.isEmpty()) {
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append(title)
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      for (CompactDocParser.DocTag tag : tags) {
        sb.append("<p>").append(CompactDocParser.formatInlineDoc(tag.description())).append("</p>");
      }
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  private static void appendOtherTagsSection(@NotNull StringBuilder sb, @NotNull CompactDocParser.ParsedDoc docData) {
    for (CompactDocParser.DocTag tag : docData.tags()) {
      if (isHandledTag(tag.name())) {
        continue;
      }
      String sectionTitle = CompactDocParser.capitalize(tag.name()) + ":";
      sb.append(DocumentationMarkup.SECTION_HEADER_START)
          .append(CompactDocParser.escapeHtml(sectionTitle))
          .append(DocumentationMarkup.SECTION_SEPARATOR);
      sb.append("<p>");
      if (tag.target() != null && !tag.target().isEmpty()) {
        sb.append("<code>").append(CompactDocParser.escapeHtml(tag.target())).append("</code> &ndash; ");
      }
      sb.append(CompactDocParser.formatInlineDoc(tag.description())).append("</p>");
      sb.append(DocumentationMarkup.SECTION_END);
    }
  }

  public static boolean isHandledTag(@NotNull String name) {
    return switch (name.toLowerCase()) {
      case "param", "parameter", "return", "returns", "throws", "throw", "see", "since", "deprecated", "notice", "dev" -> true;
      default -> false;
    };
  }
}
