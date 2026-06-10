# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build (Java 21, Maven 3.6+)
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=FilterExpressionExecutorTest

# Run a specific test method
mvn test -Dtest=CalculateRuleIfThenTest#testCalculateRule

# Run with exec plugin
mvn exec:java -Dexec.mainClass="com.jmix.configengine.Main"
```

Lombok is used throughout — ensure annotation processing is enabled in your IDE. Checkstyle config exists at `checkstyle.xml` but is commented out in `pom.xml`.

## Architecture

This is a **constraint-based configuration engine** that uses Google OR-Tools (CP-SAT solver) to reason about product configurations. The system models modules as Modules with Params, Parts, and Rules, then uses the CP-SAT solver to find valid configurations or infer parameter values.

### Layer Model

```
config data (Module/Para/Part/Rule)
       |
       v
Constraint solver (OR-Tools CP-SAT wrapper)
       |
       v
Solution enumeration (ModuleInst/ParaInst/PartInst)
```

### Package Structure

**`com.jmix.executor`** — Runtime constraint solving engine:
- `bmodel/` — Business domain model: `Module`, `ModuleBase`, `Para`, `Part`, `PartCategory`, `Rule`, and rule schemas (`CompatiableRuleSchema`, `CalculateRuleSchema`, `CodeRuleSchema`, `PriorityRuleSchema`). `ModuleBase` extends `Onto` and serves as the common base for both `Module` and `PartCategory` (they share the same structure of params + parts + rules).
- `cmodel/` — Configuration instance model: `ModuleInst`, `ParaInst`, `PartInst`, `SolverResult` — these represent solver output, not input.
- `impl/algmodel/` — OR-Tools CP-SAT wrapper: `AlgCPModel`, `AlgCPConstraint`, `AlgCPLinearExpr`, plus variable types (`ParaVar`, `PartVar`, `PartCategoryVar`, `RelaxVar`). `ModuleAlgImpl` extends `ModuleBaseAlgImpl` and is the key class that builds CP-SAT constraints from Module data.
- `southinf/` — "Southbound interfaces" that generated code implements: `IModuleAlg`, `IConstraintFunction`, `IPartCategoryFunction`.
- `ModuleConstraintExecutor` — Singleton entry point (via `INST` field). Only interface external callers should use. Provides `init/fini/addModule/removeModule/inferParas/registerExtensible`.
- `ModuleConstraintExecutorImpl` — Implementation with priority-based layered solving, incremental model loading, and conflict detection via relax variables.

**`com.jmix.tool`** — Code generation tooling:
- `artbuilder/ModuleAlgArtifactGenerator` — Uses FreeMarker to generate Java constraint code from Module definitions using `ModuleConstraintTemplate.ftl`.
- `bbuilder/ModuleGenneratorByAnno` — Builds `Module` instances from annotation-decorated classes at runtime.
- `impl/ModuleGenerator` — LLM-based code generation (via Spring AI).
- `impl/ModulePacker` / `ModuleCompiler` / `ModuleRunner` — Dynamic compilation and execution pipeline.

### Key Patterns

- **Annotation-driven module definition in tests**: Use `@ModuleAnno`, `@ParaAnno`, `@PartAnno`, `@CodeRuleAnno`, `@CompatiableRuleAnno`, `@PriorityRuleAnno` on a class extending `ConstraintAlgImplTestBase`. These annotations auto-generate the `Module` object.
- **Test base classes**: `ModuleScenarioTestBase` for integration tests (full init → solve → assert pipeline), `ConstraintAlgImplTestBase` for constraint algorithm definition.
- **PartCategory** shares the same structure as Module (extends `ModuleBase`), supporting nested constraint hierarchies and multi-instance (MultiInstPartCategory) scenarios.
- **Solver supports**: single solution, all solutions enumeration (`enumerateAllSolution`), priority-layered solving, and conflict detection via relax variables.

## Code Style

- **Log messages MUST be in English** — this is a hard rule from `.cursorrules`. Use `log.info("User authentication successful")` not `log.info("用户认证成功")`.
- **Existing Chinese comments and natural-language metadata MUST stay Chinese** — do not translate existing JavaDoc, inline comments, annotation text such as `normalNaturalCode`, or JSON natural-language fields into English.
- **Edit Chinese text as UTF-8 only** — never save Java/JSON/Markdown files through a non-UTF-8 encoding path. Before committing, scan for mojibake markers such as replacement character `U+FFFD`, private-use characters, and common mojibake code points such as `U+9205`, `U+9227`, `U+95BA`, `U+940E`, `U+9346`, `U+95B0`, `U+6D60`, `U+93C4`, `U+7459`, `U+59AF`, `U+7029`, `U+95AE`, `U+9359`, `U+9352`, `U+93C1`, `U+9422`, `U+93AC`.
- **Comments may use Chinese** — JavaDoc and inline comments can use Chinese for explaining complex business logic.
- Communication with this user should be in simplified Chinese.
- Java source level: 21. Max line length: 120 (per `checkstyle.xml` and `CodeFormatterFromIdea.xml`).
- Use Lombok (`@Data`, `@Slf4j`, `@EqualsAndHashCode`) for boilerplate reduction.

## Git Commit

- **Default branch: `main`** — always commit directly to `main` unless user explicitly requests a feature branch.
- **Include all modified files** — `.specstory/`, AI session histories, and other non-standard files are **included by default** unless user explicitly says to ignore them.
