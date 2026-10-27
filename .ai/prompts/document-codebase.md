# Codebase Documentation & Architectural Javadoc Generator

Use this prompt whenever you want to generate high-depth, production-grade Javadocs and inline architectural comments for any file or directory in this IntelliJ plugin project.

---

## 📋 Prompt Template (Copy & Paste below)

```markdown
Please review and enrich the following file(s) or directory with comprehensive Javadocs and inline architectural comments:
Target: {{TARGET_PATH_OR_FILE}}

### Objective:
Document this code with "every drop of knowledge a new developer needs to understand the working, lifecycle, threading model, and design decisions." Explain not just *what* the code does, but *why* it was architected this way, *who* calls it, and *what subtle edge cases* it mitigates.

### Detailed Requirements & Guidelines:

1. **IntelliJ Platform Lifecycle, Callers & Threading Model:**
   - **Extension Point & Subsystem Role:** Identify whether this class is an Extension Point (`plugin.xml`), a Service, an Action, a PSI Element, an Annotator, an Inspection, or a Completion/Refactoring Contributor.
   - **Calling Pipeline:** In the class-level Javadoc, clearly document *which IntelliJ subsystem* calls this (e.g., `CodeCompletionHandlerBase`, `DaemonCodeAnalyzer`, `PsiDocumentManager`, `LookupManager`, `BlockSupport`).
   - **Threading Contract:** Explicitly state the execution thread and lock requirements:
     - Is it on the Event Dispatch Thread (EDT) or a pooled background thread?
     - Does it require a Read Action (`@RequiresReadLock`), a Write Action (`@RequiresWriteLock`), or a `WriteCommandAction`?
   - **Cancellation Support:** If the class performs tree traversal or resolution loops, document how `ProgressManager.checkCanceled()` or daemon interrupts are respected.

2. **IntelliJ Platform SDK Reliance & Method/Constant Deconstruction:**
   - **SDK Classes & Base Abstractions:** Our codebase heavily relies on IntelliJ SDK base classes (e.g., `CreateFileFromTemplateAction`, `PsiReferenceContributor`), marker interfaces (e.g., `DumbAware`), validators (e.g., `InputValidatorEx`), and service singletons (`PropertiesComponent`, `FileTemplateManager`, `CodeStyleManager`). Document the specific platform contract these classes enforce.
   - **SDK Method Calls & Constants:** Document non-obvious SDK method calls and platform constants (e.g., why `getInternalTemplate` vs `getTemplate`, why `WriteCommandAction.runWriteCommandAction` is chosen, why `PropertiesComponent` is used for MRU persistence, why `ProcessCanceledException` must be rethrown immediately).
   - **Method Calling Context & Triggers:** Document *when* and *by whom* each method is invoked (e.g. platform action subsystem, dialogue lifecycle, template expansion engine, background daemon) directly in method-level Javadocs when needed, explaining preconditions without generating document mess.

3. **Design Decisions & "Why" Rationale:**
   - **Singleton vs. Instance:** If a class uses a private constructor and a `static final INSTANCE`, explain why (e.g., statelessness, thread safety, eliminating allocation overhead on high-frequency typing events).
   - **Static vs. Non-Static:** Document why methods/constants are static (pure mathematical/parsing functions, in-memory caching, test facades).
   - **Visibility Bounds:** Clarify intentional visibility constraints (`private` vs package-private vs `public` test-facade helpers).
   - **Memory Leak Guards:** Document why UI/PSI objects (`Project`, `PsiElement`, `Editor`, `VirtualFile`) are NOT stored in static or persistent fields.
   - **Cache Invalidation:** If caching results, document the cache key and invalidation dependency (e.g., `PsiModificationTracker.MODIFICATION_COUNT`).

4. **Grammar & Edge-Case Mitigations:**
   - **Lookahead & Boundary Checking:** Document any forward lookahead logic (e.g. skipping whitespace to avoid inserting duplicate brackets `()`, `<>`, or colons `:`).
   - **Dummy Identifier Awareness:** Document interactions with the parser's incomplete syntax state and IntelliJ's completion placeholder (`IntellijIdeaRulezzz`).
   - **Template & UI Race Conditions:** Document `invokeLater`, `!editor.isDisposed()` checks, and live template synchronization (`TemplateState`) that prevent cursor displacement.

5. **Visual Diagrams (ASCII) for Complex Hierarchies:**
   - For classifiers, parser delegates, or multi-branch pipelines, include a compact 3-to-6 line ASCII diagram inside the class-level Javadoc illustrating parent-child relationships, states, or request flow.

6. **Comment Structure, Step-by-Step Fragments & Clean Code Standards:**
   - **Class-Level JavaDoc:** 
     - Summary sentence.
     - Subsystem role, calling pipeline, and threading contract.
     - HTML `<ul>` / `<li>` list detailing all functional capabilities and edge cases.
     - `@see` tags cross-referencing related IntelliJ platform classes and plugin components.
   - **Method-Level JavaDoc:**
     - Purpose, parameter roles, return value contract, and exceptions (especially `ProcessCanceledException` or `IncorrectOperationException`).
     - Calling context (who triggers this method and on what thread/under what lock).
   - **Constants & Fields Javadoc:** Purpose, scope, immutability guarantees, and platform configuration keys.
   - **Step-by-Step Fragment Comments & Deep-Dive Logic (Explain "What" AND "How"):**
     - For non-trivial methods, validation pipelines, insert handlers, resolution loops, or multi-branch switches (like annotator passes), never stop at high-level summary labels (e.g. `// 8. Struct literal expressions`).
     - Go deeper into the code mechanics and explain **how** the logic operates step-by-step:
       * **AST Traversal:** Detail how tokens are located (e.g. finding identifier leaves via `findChildByType(CompactTokenTypes.IDENTIFIER)`, scanning sibling nodes via `getTreeNext()`).
       * **PSI Resolution & Alias Unwrapping:** Explain how references are resolved via `ref.resolve()` under a ReadAction and how synthetic/import aliases (such as `CompactImportElementImpl`) are unwrapped via `CompactResolveUtil` to reach the canonical declaration.
       * **Branching & Fallback Strategies:** Explain why branching decisions are made (e.g. why user-defined structs take precedence, how built-in type fallbacks apply, why unresolved symbols default gracefully, or how mutating assignment LHS writes are distinguished from read usages).
       * **SDK Construct Interaction:** Document the effect of platform API calls (e.g. `newSilentAnnotation(HighlightSeverity.INFORMATION)`, `pack(...)`, `TextRange.create(...)`, `PsiTreeUtil.getParentOfType(...)`).
   - **Zero Fluff & Zero Document Mess:** Avoid self-evident comments (e.g. `// gets the offset`). Every comment must contribute architectural, lifecycle, SDK-contract, or algorithmic knowledge. Keep comments dense with insight, concise, and cleanly structured without noisy boilerplate.

7. **Integrity Rule:**
   - Do NOT alter any runtime code, method signatures, or logic.
   - Preserve all existing functionality while adding clear, educational documentation.
```

---

## 💡 Examples of How to Use This Prompt:

### For a single file:
> "Please review and enrich the following file with comprehensive Javadocs and inline architectural comments:  
> Target: `../../src/main/java/dev/verloren/midnight/completion/CompactParameterizedTypeInsertHandler.java`"

### For an entire package/feature:
> "Please review and enrich the following directory with comprehensive Javadocs and inline architectural comments:  
> Target: `../../src/main/java/dev/verloren/midnight/completion/providers`"

### For a complex classifier/resolver:
> "Please review and enrich `CompactCompletionContext.java` focusing on the AST lookahead mechanisms, classification states, and how it handles broken syntax during active typing."
