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
   - **Calling Pipeline:** In the class-level JavaDoc, clearly document *which IntelliJ subsystem* calls this (e.g., `CodeCompletionHandlerBase`, `DaemonCodeAnalyzer`, `PsiDocumentManager`, `LookupManager`, `BlockSupport`).
   - **Threading Contract:** Explicitly state the execution thread and lock requirements:
     - Is it on the Event Dispatch Thread (EDT) or a pooled background thread?
     - Does it require a Read Action (`@RequiresReadLock`), a Write Action (`@RequiresWriteLock`), or a `WriteCommandAction`?
   - **Cancellation Support:** If the class performs tree traversal or resolution loops, document how `ProgressManager.checkCanceled()` or daemon interrupts are respected.

2. **Design Decisions & "Why" Rationale:**
   - **Singleton vs. Instance:** If a class uses a private constructor and a `static final INSTANCE`, explain why (e.g., statelessness, thread safety, eliminating allocation overhead on high-frequency typing events).
   - **Static vs. Non-Static:** Document why methods/constants are static (pure mathematical/parsing functions, in-memory caching, test facades).
   - **Visibility Bounds:** Clarify intentional visibility constraints (`private` vs package-private vs `public` test-facade helpers).
   - **Memory Leak Guards:** Document why UI/PSI objects (`Project`, `PsiElement`, `Editor`, `VirtualFile`) are NOT stored in static or persistent fields.
   - **Cache Invalidation:** If caching results, document the cache key and invalidation dependency (e.g., `PsiModificationTracker.MODIFICATION_COUNT`).

3. **Grammar & Edge-Case Mitigations:**
   - **Lookahead & Boundary Checking:** Document any forward lookahead logic (e.g., skipping whitespace to avoid inserting duplicate brackets `()`, `<>`, or colons `:`).
   - **Dummy Identifier Awareness:** Document interactions with the parser's incomplete syntax state and IntelliJ's completion placeholder (`IntellijIdeaRulezzz`).
   - **Template & UI Race Conditions:** Document `invokeLater`, `!editor.isDisposed()` checks, and live template synchronization (`TemplateState`) that prevent cursor displacement.

4. **Visual Diagrams (ASCII) for Complex Hierarchies:**
   - For classifiers, parser delegates, or multi-branch pipelines, include a compact 3-to-6 line ASCII diagram inside the class-level JavaDoc illustrating parent-child relationships, states, or request flow.

5. **Comment Structure & Clean Code Standards:**
   - **Class-Level JavaDoc:** 
     - Summary sentence.
     - HTML `<ul>` / `<li>` list detailing all functional capabilities and edge cases.
     - `@see` tags cross-referencing related IntelliJ platform classes and plugin components.
   - **Constants & Fields JavaDoc:** Purpose, scope, and immutability guarantees.
   - **Step-by-Step Inline Comments:** For non-trivial algorithmic steps (parsers, insert handlers, type checkers, resolve loops), use clear numbered comments (e.g., `// Step 1: Lookahead forward...`, `// Step 2: Determine caret offset...`).
   - **Zero Fluff:** Avoid self-evident comments (e.g., `// gets the offset`). Every comment must contribute architectural, lifecycle, or edge-case knowledge.

6. **Integrity Rule:**
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
