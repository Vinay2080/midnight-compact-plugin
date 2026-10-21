package dev.verloren.midnight.type;

import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactStructDefinitionImpl;

import java.util.List;
import java.util.Map;

/**
 * Multi-tier unit and mirror test suite for the first-class sealed {@link CompactType} hierarchy,
 * {@link CompactTypeVariable}, {@link CompactTypeSubstitutor}, and type inference utilities.
 */
public class CompactTypeHierarchyTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        dev.verloren.midnight.CompactLanguage.INSTANCE,
        new dev.verloren.midnight.parser.CompactParserDefinition()
    );
  }

  public void testTypeVariableCreationAndEquivalence() {
    CompactTypeVariable varT = new CompactTypeVariable("T");
    CompactTypeVariable varHashT = new CompactTypeVariable("#T");
    CompactTypeVariable varVal = new CompactTypeVariable("TVal");

    assertEquals("T", varT.name());
    assertEquals("T", varT.cleanName());
    assertEquals("#T", varHashT.name());
    assertEquals("T", varHashT.cleanName());

    // Structural equality normalizes leading # prefix
    assertEquals(varT, varHashT);
    assertEquals(varT.hashCode(), varHashT.hashCode());
    assertFalse(varT.equals(varVal));

    // Type variable is assignable to any concrete type (unification)
    assertTrue(varT.isAssignableTo(CompactPrimitiveType.FIELD));
    assertTrue(varT.isAssignableTo(CompactPrimitiveType.BOOLEAN));
  }

  public void testTypeSubstitutorDirectVariableReplacement() {
    CompactTypeSubstitutor substitutor = CompactTypeSubstitutor.builder()
        .put("T", CompactPrimitiveType.FIELD)
        .put("E", CompactPrimitiveType.BOOLEAN)
        .build();

    assertFalse(substitutor.isEmpty());
    assertEquals(4, substitutor.getMappings().size()); // Normalized with and without #

    CompactType replacedT = substitutor.substitute(new CompactTypeVariable("T"));
    assertEquals(CompactPrimitiveType.FIELD, replacedT);

    CompactType replacedHashT = substitutor.substitute(new CompactTypeVariable("#T"));
    assertEquals(CompactPrimitiveType.FIELD, replacedHashT);

    CompactType replacedE = substitutor.substitute(new CompactTypeVariable("E"));
    assertEquals(CompactPrimitiveType.BOOLEAN, replacedE);

    // Unmapped variable remains intact
    CompactType unmapped = substitutor.substitute(new CompactTypeVariable("Z"));
    assertEquals(new CompactTypeVariable("Z"), unmapped);
  }

  public void testParameterizedTypeSubstitutionStdlib() {
    CompactTypeSubstitutor substitutor = CompactTypeSubstitutor.builder()
        .put("A", CompactPrimitiveType.FIELD)
        .put("B", CompactPrimitiveType.BOOLEAN)
        .build();

    CompactType eitherType = new CompactParameterizedType("Either", List.of(
        new CompactTypeVariable("A"),
        new CompactTypeVariable("B")
    ));

    CompactType substituted = substitutor.substitute(eitherType);
    assertInstanceOf(substituted, CompactParameterizedType.class);
    assertEquals("Either<Field, Boolean>", substituted.name());
  }

  public void testParameterizedTypeSubstitutionUserDefinedMirror() {
    // Mandatory Mirror Test: User-defined generic struct Result<TVal, TErr>
    CompactTypeSubstitutor substitutor = CompactTypeSubstitutor.builder()
        .put("TVal", CompactPrimitiveType.FIELD)
        .put("TErr", CompactUintType.ofBits(32))
        .build();

    CompactType resultType = new CompactParameterizedType("Result", List.of(
        new CompactTypeVariable("TVal"),
        new CompactTypeVariable("TErr")
    ));

    CompactType substituted = substitutor.substitute(resultType);
    assertInstanceOf(substituted, CompactParameterizedType.class);
    assertEquals("Result<Field, Uint<32>>", substituted.name());
  }

  public void testNestedParameterizedTypeSubstitutionMirror() {
    // Nested mirror test: Pair<A, Box<B>> -> Pair<Field, Box<Boolean>>
    CompactTypeSubstitutor substitutor = CompactTypeSubstitutor.builder()
        .put("A", CompactPrimitiveType.FIELD)
        .put("B", CompactPrimitiveType.BOOLEAN)
        .build();

    CompactType boxB = new CompactParameterizedType("Box", List.of(new CompactTypeVariable("B")));
    CompactType pairType = new CompactParameterizedType("Pair", List.of(new CompactTypeVariable("A"), boxB));

    CompactType substituted = substitutor.substitute(pairType);
    assertEquals("Pair<Field, Box<Boolean>>", substituted.name());
  }

  public void testStructTypeSubstitutionMirror() {
    CompactTypeSubstitutor substitutor = CompactTypeSubstitutor.builder()
        .put("T", CompactPrimitiveType.FIELD)
        .build();

    CompactStructType rawStruct = new CompactStructType("CustomStruct", Map.of(
        "value", new CompactTypeVariable("T"),
        "flag", CompactPrimitiveType.BOOLEAN
    ));

    CompactType substituted = substitutor.substitute(rawStruct);
    assertInstanceOf(substituted, CompactStructType.class);
    CompactStructType substStruct = (CompactStructType) substituted;
    assertEquals(CompactPrimitiveType.FIELD, substStruct.fields().get("value"));
    assertEquals(CompactPrimitiveType.BOOLEAN, substStruct.fields().get("flag"));
  }

  public void testParseTypePrimitivesAndNumericLiterals() {
    assertEquals(CompactPrimitiveType.BOOLEAN, CompactTypeInferenceUtil.parseType("Boolean"));
    assertEquals(CompactPrimitiveType.FIELD, CompactTypeInferenceUtil.parseType("Field"));
    assertEquals(CompactPrimitiveType.OPAQUE, CompactTypeInferenceUtil.parseType("Opaque"));

    CompactType numType = CompactTypeInferenceUtil.parseType("42");
    assertInstanceOf(numType, CompactNumericLiteralType.class);

    CompactType uintType = CompactTypeInferenceUtil.parseType("Uint<32>");
    assertInstanceOf(uintType, CompactUintType.class);
    assertEquals("Uint<32>", uintType.name());
  }

  public void testParseTypeVariablesAndGenerics() {
    CompactType varT = CompactTypeInferenceUtil.parseType("T");
    assertInstanceOf(varT, CompactTypeVariable.class);

    CompactType varHashT = CompactTypeInferenceUtil.parseType("#T");
    assertInstanceOf(varHashT, CompactTypeVariable.class);

    CompactType paramType = CompactTypeInferenceUtil.parseType("Either<Field, Boolean>");
    assertInstanceOf(paramType, CompactParameterizedType.class);
    assertEquals("Either<Field, Boolean>", paramType.name());

    // User-defined mirror test
    CompactType userType = CompactTypeInferenceUtil.parseType("Result<TVal, TErr>");
    assertInstanceOf(userType, CompactParameterizedType.class);
    assertEquals("Result<TVal, TErr>", userType.name());
  }

  public void testBuildSubstitutorFromPsiDeclaration() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Result<TVal, TErr> {
          value: TVal,
          error: TErr
        }
        """
    );

    CompactStructDefinitionImpl structDef = PsiTreeUtil.findChildOfType(
        myFixture.getFile(),
        CompactStructDefinitionImpl.class
    );
    assertNotNull("Struct definition should be found", structDef);

    CompactTypeSubstitutor substitutor = CompactTypeInferenceUtil.buildSubstitutor(
        structDef,
        List.of(CompactPrimitiveType.FIELD, CompactPrimitiveType.BOOLEAN)
    );

    assertFalse(substitutor.isEmpty());
    assertEquals(
        CompactPrimitiveType.FIELD,
        substitutor.substitute(new CompactTypeVariable("TVal"))
    );
    assertEquals(
        CompactPrimitiveType.BOOLEAN,
        substitutor.substitute(new CompactTypeVariable("TErr"))
    );
  }

  public void testParameterizedTypeAssignabilityWithVariables() {
    CompactType concreteResult = new CompactParameterizedType("Result", List.of(
        CompactPrimitiveType.FIELD,
        CompactPrimitiveType.BOOLEAN
    ));
    CompactType genericResult = new CompactParameterizedType("Result", List.of(
        new CompactTypeVariable("TVal"),
        new CompactTypeVariable("TErr")
    ));

    // A concrete instantiation unifies with the generic signature
    assertTrue(concreteResult.isAssignableTo(genericResult));

    // Struct type with matching name is compatible
    CompactStructType structType = new CompactStructType("Result", Map.of());
    assertTrue(structType.isAssignableTo(genericResult));
  }
}
