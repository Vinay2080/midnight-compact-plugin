---
name: srs-diagram-designer
description: Design, architect, and render all categories of formal software engineering diagrams in LaTeX using TikZ/PGF and text-based formats (Mermaid, PlantUML) for Software Requirements Specifications.
---

# SRS Diagram Designer Skill

## Purpose
Provide comprehensive standards, best practices, and production-ready code patterns for designing and embedding all types of software engineering diagrams into Software Requirements Specifications (SRS). Emphasizes native LaTeX `tikz` rendering to ensure deterministic, crisp, self-contained, publication-ready vector output without external image file dependencies.

---

## 1. Diagram Taxonomy for Software Requirements Specifications

A complete, industrial-grade SRS requires visual models across the following 8 fundamental architectural perspectives:

```
                                  ┌───────────────────────────────┐
                                  │      SRS Visual Taxonomy      │
                                  └──────────────┬────────────────┘
         ┌───────────────────────────────┬───────┴───────┬───────────────────────────────┐
         ▼                               ▼               ▼                               ▼
┌──────────────────┐           ┌──────────────────┐ ┌──────────────────┐           ┌──────────────────┐
│ 1. Context & Use │           │ 2. Structural &  │ │ 3. Dynamic &     │ │ 4. Process &     │
│    Case Models   │           │    Data Models   │ │    Behavioral    │ │    Deployment    │
├──────────────────┤           ├──────────────────┤ ├──────────────────┤ ├──────────────────┤
│• System Context  │           │• Layered Arch    │ │• Sequence Diags  │ │• Flowcharts      │
│• Use Case Diags  │           │• Component Diags │ │• State Machines  │ │• Activity Diags  │
│• User Workflows  │           │• Class / PSI AST │ │• Communication   │ │• DFD (L0, L1)    │
│                  │           │• Type Hierarchy  │ │• Event Lifecycles│ │• Deployment Diag │
└──────────────────┘           └──────────────────┘ └──────────────────┘ └──────────────────┘
```

---

## 2. Diagram Types & Specifications

### 2.1 System Context & External Boundary Diagram
- **Purpose**: Establishes system boundary, surrounding human actors, external services, compilers, operating systems, and target blockchains.
- **Key Elements**: System under specification (central box), external actors (developers, CI/CD), external tools (CLI compilers, WSL), network nodes (Midnight Testnet/Devnet).

### 2.2 Use Case Diagram
- **Purpose**: Maps system functional capabilities to user personas (e.g. Smart Contract Author, ZK Cryptographer, Security Auditor).
- **Key Elements**: Actors (stick figures or stylized icons), Use Case ovals (`<<include>>`, `<<extend>>`), System Boundary frame.

### 2.3 Layered Architecture & Component Diagram
- **Purpose**: Illustrates modular decomposition, API boundaries, and strict downward dependency directions.
- **Key Elements**: Subsystems, packages, interfaces, dependency arrows with invariant labels (e.g., "Downwards Only").

### 2.4 Class & Domain Model Diagram (PSI / AST & Type Hierarchy)
- **Purpose**: Defines object-oriented structure of syntax nodes, AST tokens, and type representations.
- **Key Elements**: UML class boxes (Stereotype, Attributes, Methods), Inheritance (`--|>`), Composition (`*--`), Association (`-->`).

### 2.5 Sequence Diagram (Dynamic Interactions)
- **Purpose**: Demonstrates chronological message passing between components for core user scenarios (e.g. Code Completion, Reference Resolution, 1-Click Compilation).
- **Key Elements**: Lifelines (`---`), synchronous calls (`->>`), asynchronous tasks (`-->>`), activations (`rectangle`), return flows (`-->`).

### 2.6 State Machine Diagram (Lifecycle Models)
- **Purpose**: Documents state transitions for reactive components (e.g. Compiler Toolchain Manager, Daemon Code Analyzer, Inplace Rename Refactoring session).
- **Key Elements**: Initial/Final states, state boxes, transition arrows with triggers, guards `[guard]`, and actions `/action`.

### 2.7 Activity & Flowchart Diagram (Algorithmic Logic)
- **Purpose**: Documents branching decision trees and computational algorithms (e.g. Pratt parsing precedence climbing, SemVer compatibility evaluation, Type unification).
- **Key Elements**: Start/End rounded nodes, Decision diamonds (`if/else`), Operation rectangles, Parallel forks/joins.

### 2.8 Data Flow Diagram (DFD Level 0 & Level 1)
- **Purpose**: Visualizes the transformation of data streams from raw source text to tokens, AST, semantic index, and ZK bytecode.
- **Key Elements**: External entities (rectangles), Processes (circles/rounded boxes), Data stores (open-ended rectangles), Data flows (labeled arrows).

### 2.9 Deployment Diagram (Runtime Physical Topology)
- **Purpose**: Shows physical execution environment, process boundaries, file system paths, and host operating system bindings (Windows, macOS, Linux, WSL).
- **Key Elements**: Host Node 3D boxes, Execution environments (JVM, IntelliJ Sandbox), Artifact archives (`~/.compact/versions/`).

---

## 3. LaTeX TikZ Implementation Standard

When implementing diagrams in LaTeX:

### 3.1 Mandatory TikZ Libraries
Include the following in the LaTeX preamble:
```latex
\usepackage{tikz}
\usetikzlibrary{
    shapes.geometric,
    shapes.misc,
    arrows.meta,
    positioning,
    fit,
    backgrounds,
    calc,
    decorations.pathreplacing,
    shadows,
    matrix
}
```

### 3.2 Standard Harmonious Color Palette
Use consistent, professional color tokens:
```latex
\definecolor{srsPrimary}{RGB}{41, 128, 185}     % Calming Slate Blue
\definecolor{srsSecondary}{RGB}{39, 174, 96}    % Emerald Green
\definecolor{srsAccent}{RGB}{142, 68, 173}      % Purple (Midnight Theme)
\definecolor{srsWarning}{RGB}{230, 126, 34}     % Amber Orange
\definecolor{srsDanger}{RGB}{192, 57, 43}       % Carmine Red
\definecolor{srsDark}{RGB}{44, 62, 80}          % Charcoal / Midnight Blue
\definecolor{srsLight}{RGB}{245, 247, 250}      % Background Gray
\definecolor{srsBorder}{RGB}{189, 195, 199}     % Subtle Border Silver
```

### 3.3 Production-Ready TikZ Style Definitions
```latex
\tikzset{
    srsBox/.style={
        rectangle, rounded corners=3pt,
        draw=srsDark, line width=0.8pt,
        fill=white, text=srsDark,
        align=center, font=\small\sffamily,
        inner sep=6pt, drop shadow={opacity=0.15, shadow xshift=1pt, shadow yshift=-1pt}
    },
    srsProcess/.style={
        rectangle, rounded corners=4pt,
        draw=srsPrimary, line width=1pt,
        fill=srsPrimary!10, text=srsDark,
        align=center, font=\small\sffamily,
        inner sep=6pt
    },
    srsDecision/.style={
        diamond, aspect=2,
        draw=srsWarning, line width=1pt,
        fill=srsWarning!15, text=srsDark,
        align=center, font=\footnotesize\sffamily,
        inner sep=3pt
    },
    srsDataStore/.style={
        cylinder, shape border rotate=90, aspect=0.25,
        draw=srsAccent, line width=1pt,
        fill=srsAccent!10, text=srsDark,
        align=center, font=\small\sffamily,
        inner sep=4pt
    },
    srsActor/.style={
        circle, draw=srsDark, line width=1pt,
        fill=srsLight, text=srsDark,
        align=center, font=\footnotesize\bfseries\sffamily,
        inner sep=4pt
    },
    srsArrow/.style={
        -{Stealth[scale=1.0]}, line width=1pt, draw=srsDark
    },
    srsArrowDashed/.style={
        -{Stealth[scale=1.0]}, dashed, line width=0.9pt, draw=srsDark!70
    }
}
```

---

## 4. Quality Checklist for SRS Diagrams
- [ ] **Legibility**: All text is rendered using document fonts at readable sizes ($\ge 8\text{pt}$).
- [ ] **Contrast**: Foreground text against background node fills meets WCAG AA contrast ratio ($\ge 4.5:1$).
- [ ] **Self-Contained**: Diagram is completely generated within `tikzpicture` — zero missing image or broken external asset errors.
- [ ] **Cross-Referenced**: Every diagram has a numbered `\caption{...}` and unique `\label{fig:...}` cited within the normative text.
- [ ] **Semantic Alignment**: Node names, flow directions, and data types match the textual requirements exactly.
