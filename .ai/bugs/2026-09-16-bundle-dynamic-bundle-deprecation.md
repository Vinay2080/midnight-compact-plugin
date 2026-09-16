# Bug Record: Deprecated DynamicBundle Constructor Usage

- **Date**: 2026-09-16
- **Subsystem**: Bundle / Platform
- **Affected Files**:
  - `src/main/java/dev/verloren/midnight/CompactBundle.java`
- **Related ADRs**: None
- **Severity**: Minor

---

## 1. Symptoms & Failure Behavior
- **Observed Defect**: During plugin verification and inspection, IntelliJ Platform Plugin Verifier flags a deprecated API usage:
  ```text
  1 deprecated API usage
  Midnight Compact Language 1.3.0 uses deprecated API, which may be removed in future releases leading to binary and source code incompatibilities

  Deprecated constructor usage (1)
  DynamicBundle.<init>(String) (1)
  ```
- **Reproduction Sequence**:
  1. Inspect `src/main/java/dev/verloren/midnight/CompactBundle.java`.
  2. Observe the constructor calling `super(BUNDLE)` where `BUNDLE` is `"messages.MyMessageBundle"`.
  3. Disassemble the compiled class bytecode using `javap -c -v CompactBundle.class`.
  4. Note the constant pool entry referencing `#5 = Methodref com/intellij/DynamicBundle."<init>":(Ljava/lang/String;)V`.
  5. Run IntelliJ Plugin Verifier or an automated bytecode assertion test verifying constructor signatures.

## 2. Root Cause Analysis
- In `CompactBundle.java` (lines 18–20 prior to fix):
  ```java
  private CompactBundle() {
    super(BUNDLE);
  }
  ```
- The single-argument constructor `com.intellij.DynamicBundle.<init>(String)` was deprecated in IntelliJ Platform SDK.
- JetBrains deprecated `DynamicBundle(String)` because relying on caller stack inspection or thread context classloader resolution fails in modern environments featuring dynamic plugin reloading, plugin classloader isolation, and localized language pack extensions.
- The IntelliJ Platform SDK requires passing the bundle's `Class<?>` token via `DynamicBundle(@NotNull Class<?> bundleClass, @NotNull String pathToBundle)` so the platform can deterministically and reliably resolve the plugin module's `ClassLoader`.

## 3. Investigation & Evaluated Approaches
- **Subsystem Search**: Checked `.ai/bugs/` for past records regarding `DynamicBundle` or `CompactBundle`; confirmed no previous issues had been logged.
- **Codebase Auditing**: Audited all usages of `CompactBundle` across the project (`CompactStatusBarWidget.java`, `CompactStatusBarPopup.java`, `CompactStatusBarWidgetFactory.java`). All callers invoke static helper methods (`CompactBundle.message(...)`) and do not rely on raw class inheritance or instance casting.
- **Evaluated Composition vs. Inheritance**: Evaluated switching to composition (`private static final DynamicBundle INSTANCE = new DynamicBundle(CompactBundle.class, BUNDLE);`) vs updating the `super` invocation to `super(CompactBundle.class, BUNDLE);`. Retained `extends DynamicBundle` with `super(CompactBundle.class, BUNDLE)` to preserve 100% binary and source compatibility for any existing subtyping expectations while directly eliminating the deprecated constructor call.
- **Lazy Message Supplier Addition**: Evaluated adding `messagePointer` returning `Supplier<@Nls String>` via `INSTANCE.getLazyMessage(key, params)` in alignment with modern IntelliJ Platform SDK message bundle standards.

## 4. Solution & Implementation
- In `src/main/java/dev/verloren/midnight/CompactBundle.java`:
  - Updated the private constructor to invoke `super(CompactBundle.class, BUNDLE);`.
  - Added public static method `messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params)` returning `@NotNull Supplier<@Nls String>` via `INSTANCE.getLazyMessage(key, params)`.
  - Maintained Java 25 standards, strict nullability annotations (`@NotNull`, `@Nls`, `@NonNls`, `@PropertyKey`), and zero raw types or unchecked suppressions.

## 5. Verification & Tests Added
- Created `src/test/java/dev/verloren/midnight/CompactBundleTest.java`:
  - `testResolvesExistingLocalizationKeys`: Validates runtime localization string retrieval and parameter formatting from `messages.MyMessageBundle`, as well as lazy `messagePointer` supplier resolution.
  - `testDoesNotInvokeDeprecatedDynamicBundleConstructor`: Inspects `CompactBundle.class` bytecode to confirm that `DynamicBundle.<init>(Class, String)` (`(Ljava/lang/Class;Ljava/lang/String;)V`) is invoked and that `DynamicBundle.<init>(String)` (`(Ljava/lang/String;)V`) is completely absent from constructor references.
- Confirmed test failure prior to fix (`testDoesNotInvokeDeprecatedDynamicBundleConstructor FAILED` with `AssertionError`).
- Confirmed test passing after fix (`BUILD SUCCESSFUL` in 25s).
- Verified `javap -c -v CompactBundle.class` output shows:
  ```text
  #5 = Methodref #6.#7 // com/intellij/DynamicBundle."<init>":(Ljava/lang/Class;Ljava/lang/String;)V
  ```
- Executed full Gradle test suite (`./gradlew test`) with 100% passing across all 62 test classes with 0 failures and 0 compiler warnings.

## 6. Prevention & Key Lessons Learned
- Never use the single-string `DynamicBundle(String)` constructor in IntelliJ plugin message bundles.
- Always pass the bundle class (`BundleClass.class`) to `DynamicBundle` so the platform runtime can reliably identify the plugin's classloader during dynamic plugin unloading, reloading, and localization pack resolution.
- Include automated bytecode/constant-pool assertion tests for platform deprecation requirements to catch binary incompatibilities during CI before plugin verification reports them.
