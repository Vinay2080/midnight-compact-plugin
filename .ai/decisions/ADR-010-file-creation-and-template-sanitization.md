# ADR-010: File Creation Action, Nested MkDirs, & Template Sanitization

## Status
Accepted (Phases 18–19 / Complete)

## Context & Problem Statement
Creating new files via the IDE's **New -> Compact File** context menu is the primary entry point for contract authoring.
In naive implementations of `CreateFileFromTemplateAction`:
1. Users typing relative paths (e.g. `models/Token`) caused IntelliJ to throw `IncorrectOperationException` because `directory.createFile()` does not support directory slashes.
2. The Velocity template engine parameter `${NAME}` captured the raw user string (e.g. `models/Token.compact`), causing template generation to emit invalid syntax like `contract models/Token.compact { ... }`.
3. In modern IntelliJ Platform versions (2024.1+ / 2026.2), bundled file templates (`.ft`) not explicitly declared under `<internalFileTemplate>` in `plugin.xml` trigger assertion errors during telemetry collection in unit tests.
4. Entering invalid Compact identifiers (e.g. containing hyphens or starting with numbers) allowed creation of files with immediately broken contract syntax.

## Authoritative References
1. **Compact Identifier Grammar** ([`compact/compiler/parser.ss:105-117`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/compact/compiler/parser.ss#L105-L117)):
   - Identifiers must start with an ASCII letter or underscore, followed by letters, digits, or underscores.
   - Keywords (`contract`, `circuit`, `pure`, `witness`, `ledger`, etc.) cannot be used as contract names.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: `RsCreateFileAction.kt` — utilizes `CreateFileAction.findOrCreateSubdirectory` to recursively build parent folders and extracts basenames for template parameters.
   - `Rplugin`: `RCreateFileAction.kt`.

## Decision
1. **Nested Directory Traversal (`findOrCreateSubdirectory`)**:
   In `CompactCreateFileAction.createFileFromTemplate`, path separators (`/` and `\`) are parsed into path components, recursively invoking `findOrCreateSubdirectory` to ensure all intermediate directories exist before file instantiation.
2. **Velocity `${NAME}` Extraction & Sanitization**:
   - Strip file extensions (`.compact`).
   - Extract only the simple file basename for `${NAME}` (e.g. `foo/bar/Token.compact` -> `Token`).
   - Inject the sanitized name into the Velocity template context properties map so templates render clean declarations (`contract Token { ... }`).
3. **Real-Time Input Validation (`CompactNamesValidator`)**:
   Integrate `InputValidatorEx` with `CompactNamesValidator.isIdentifier(cleanName, project)` to prevent entering keywords (`contract contract`), numbers, or special characters.
4. **Mandatory `<internalFileTemplate>` Registration**:
   Register all bundled `.ft` files (`Compact Contract.compact.ft`, `Compact Module.compact.ft`, `Compact Interface.compact.ft`, `Compact File.compact.ft`) in `plugin.xml`.
5. **Post-Creation Lifecycle Hooks**:
   Automatically open the created file in the editor, run `CodeStyleManager.reformat()`, and move the caret to the first contract element.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Path extraction is platform-agnostic (handles `/` and `\`), and validation relies on the formal lexer token stream via `CompactNamesValidator`.
- **Is it resilient?**: Yes. File creation gracefully handles existing files, read-only filesystems, and deep directory hierarchies.

## Feature Implementation Map
- Action: [`CompactCreateFileAction.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/actions/CompactCreateFileAction.java)
- Validator: [`CompactNamesValidator.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/refactoring/CompactNamesValidator.java)
- Templates:
  - `src/main/resources/fileTemplates/internal/Compact Contract.compact.ft`
  - `src/main/resources/fileTemplates/internal/Compact Module.compact.ft`
  - `src/main/resources/fileTemplates/internal/Compact Interface.compact.ft`
  - `src/main/resources/fileTemplates/internal/Compact File.compact.ft`
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<internalFileTemplate>` and `<action id="Compact.NewFile">`)
- Unit Tests: [`CompactFileTemplateTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/ide/fileTemplates/CompactFileTemplateTest.java)

## Consequences & Future Maintenance
- When adding new file templates (e.g. test files or ZK scripts), register them under `<internalFileTemplate>` in `plugin.xml` and add a corresponding entry to `CompactFileTemplateGroupFactory`.
