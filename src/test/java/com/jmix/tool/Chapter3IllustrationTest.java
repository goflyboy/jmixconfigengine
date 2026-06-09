package com.jmix.tool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 为《配置器约束求解原理与冲突诊断设计文档》第3章生成系列配图。
 * <p>
 * 共6张图，对应章节 3.1 ~ 3.6：<br>
 * 01 - 什么是约束（三杯糖果问题）<br>
 * 02 - 逐步引入冲突（A=6 时无解过程）<br>
 * 03 - 松弛变量机制（给规则装开关）<br>
 * 04 - 松弛后的求解器视角<br>
 * 05 - 冲突诊断结果<br>
 * 06 - 六步流程全景图<br>
 *
 * @since 2025-06-03
 */
@Slf4j
public class Chapter3IllustrationTest {

    private static final String ASSETS_DIR = "persum/assets/chapter3/";

    private static final String API_KEY = "";
    private static final String BASE_URL = "https://code.codingplay.top";

    @Test
    void generateAllChapter3Images() throws Exception {
        Files.createDirectories(Paths.get(ASSETS_DIR));

        ImageBuilder builder = new ImageBuilder(API_KEY, BASE_URL);

        log.info("Starting Chapter 3 image generation...");

        generate01_whatIsConstraint(builder);
        generate02_conflictEscalation(builder);
        generate03_relaxationMechanism(builder);
        generate04_solverPerspective(builder);
        generate05_conflictDiagnosis(builder);
        generate06_sixStepSummary(builder);

        log.info("All Chapter 3 images generated successfully!");
    }

    // -------------------------------------------------------------------------
    // 01 - 3.1 什么是约束？（三杯糖果问题）
    // -------------------------------------------------------------------------
    private void generate01_whatIsConstraint(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: article section illustration
                Primary request: Generate an infographic that explains "what is a constraint" using a relatable everyday analogy.
                Scene/backdrop: clean white background, no clutter
                Subject: Three glasses labeled A, B, C on a wooden table, each containing sugar cubes. Above the table, five numbered rule labels float like sticky notes. Rule 1 shows A+B≥8 with a connecting arc between A and B. Rule 2 shows C≤3 with a max-line on glass C. Rule 3 shows a conditional arrow A>4 → C≥2. Rule 4 shows A+B+C≤12 with a balance scale. Rule 5 shows B>3 → A≤4 as an if-then gate. All rules drawn in a clean technical infographic style.
                Style/medium: modern technical editorial illustration, flat design with subtle depth, professional and calm
                Composition/framing: wide 16:9 layout, three glasses in the lower half, five rule cards in the upper half arranged in a clean grid
                Color palette: warm beige/cream base, teal for rule labels, amber/orange for sugar cubes, soft gray for glass outlines
                Text: no readable text, use only numbers and math symbols (1, 2, 3, 4, 5, ≥, ≤, +, →)
                Constraints: no brand, no company logo, no watermark, no cartoon style, no people
                """;
        builder.generateImage(prompt, ASSETS_DIR + "03-01-what-is-constraint.png");
        log.info("Image 01 generated: what-is-constraint");
    }

    // -------------------------------------------------------------------------
    // 02 - 3.2 逐步引入冲突
    // -------------------------------------------------------------------------
    private void generate02_conflictEscalation(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: article section illustration
                Primary request: Generate a three-stage escalation diagram showing how a solvable problem becomes unsolvable.
                Scene/backdrop: clean white background
                Subject: Stage 1 (left): three glasses A=4, B=4, C=2 with a large green checkmark above, labeled "Solver: SATISFIED". Stage 2 (center): user input arrow pointing to glass A=6, showing B and C adjusting, labeled "User input: A=6". Stage 3 (right): a red X over a "NO SOLUTION" label, with the constraint conflict shown as two arrows pushing against each other: B+C≤2 on one side and B≥2, C≥2 on the other. Arrows from stages show progression.
                Style/medium: clean technical infographic, three-column layout, arrows connecting stages left to right
                Composition/framing: horizontal three-stage flow, each stage clearly bounded by a light rounded rectangle
                Color palette: stage 1 in soft green, stage 2 in neutral blue, stage 3 in red/amber warning tones
                Text: no readable text, use only math symbols and stage labels (1, 2, 3, A=6, SATISFIED, NO SOLUTION)
                Constraints: no brand, no watermark, professional style
                """;
        builder.generateImage(prompt, ASSETS_DIR + "03-02-conflict-escalation.png");
        log.info("Image 02 generated: conflict-escalation");
    }

    // -------------------------------------------------------------------------
    // 03 - 3.3 松弛变量机制：给规则装开关
    // -------------------------------------------------------------------------
    private void generate03_relaxationMechanism(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: article section illustration
                Primary request: Generate an illustration showing the relaxation variable concept — a "switch" attached to each rule.
                Scene/backdrop: clean white background
                Subject: Five rule cards stacked vertically. Each card has a left section showing the original constraint (e.g. "A+B+C≤8") and a right section showing the same constraint with a toggle switch labeled r_i. When switch is OFF (r=0): constraint is tight and glowing red. When switch is ON (r=1): constraint becomes faded and loose "A+B+C≤108". A large central label reads "minimize Σ(r_i)" with a small objective function icon. The Big-M concept shown as M=100 in the background.
                Style/medium: modern flat infographic, technical but approachable
                Composition/framing: vertical list of 5 rule cards, each with an ON/OFF toggle switch on the right, objective function shown prominently at top center
                Color palette: dark navy background for rule cards, white text, red for tight constraints, green for relaxed, amber for switches
                Text: no readable text, only math formulas and switch labels (r1, r2, r3, r4, r5, ON, OFF, M=100)
                Constraints: no brand, no watermark
                """;
        builder.generateImage(prompt, ASSETS_DIR + "03-03-relaxation-mechanism.png");
        log.info("Image 03 generated: relaxation-mechanism");
    }

    // -------------------------------------------------------------------------
    // 04 - 3.4 松弛后的求解器视角
    // -------------------------------------------------------------------------
    private void generate04_solverPerspective(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: article section illustration
                Primary request: Generate an illustration showing what the solver sees after relaxation — the variable model.
                Scene/backdrop: clean technical blueprint-style background, subtle grid
                Subject: Two columns. Left column shows VARIABLES: A=6 (fixed, highlighted in yellow), B∈[0,5], C∈[0,5], and five relaxation switches r1..r5 each as a small toggle (all OFF except r4 which is ON). Right column shows CONSTRAINTS: the five relaxed rules written as mathematical expressions, with the one containing r4 highlighted differently. Below both columns, an OBJECTIVE function shown as "minimize (r1+r2+r3+r4+r5)" with a solver gear icon. A subtle thought bubble from a robot icon labeled "Solver" shows the model.
                Style/medium: clean technical diagram, blueprint aesthetic
                Composition/framing: two-column layout with header "Solver View", objective function at bottom
                Color palette: dark blue-gray background, white text, yellow for fixed A=6, green for active constraints, muted gray for satisfied ones, purple highlight for r4
                Text: no readable text, only math notation
                Constraints: no brand, no watermark
                """;
        builder.generateImage(prompt, ASSETS_DIR + "03-04-solver-perspective.png");
        log.info("Image 04 generated: solver-perspective");
    }

    // -------------------------------------------------------------------------
    // 05 - 3.5 冲突诊断
    // -------------------------------------------------------------------------
    private void generate05_conflictDiagnosis(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: article section illustration
                Primary request: Generate a conflict diagnosis report illustration — the result of the relaxation solver.
                Scene/backdrop: clean white background
                Subject: A diagnostic report card style. Top section: "Relaxation Result" with a summary showing r4=1 (highlighted in red/orange) and r1=r2=r3=r5=0 (all in green checkmarks). Middle section: "Partial Solution" showing the three glasses with values A=6, B=2, C=2, and a checkmark showing total=10. Bottom section: "Diagnosis Report" with Rule 4 highlighted in red, showing it was bypassed and the bound raised from 8 to 10. A small user figure with a thought bubble showing decision options. Arrows pointing to specific rule violations.
                Style/medium: clean diagnostic report style, professional
                Composition/framing: three-tier vertical report layout, with visual callouts pointing to specific elements
                Color palette: white background, red for violated rule, green for satisfied rules, amber for partial solution, teal for report headers
                Text: no readable text, only numbers and status symbols
                Constraints: no brand, no watermark
                """;
        builder.generateImage(prompt, ASSETS_DIR + "03-05-conflict-diagnosis.png");
        log.info("Image 05 generated: conflict-diagnosis");
    }

    // -------------------------------------------------------------------------
    // 06 - 3.6 六步流程全景图
    // -------------------------------------------------------------------------
    private void generate06_sixStepSummary(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: article section illustration
                Primary request: Generate a six-step process flow diagram summarizing the complete constraint solving workflow.
                Scene/backdrop: clean white background with subtle grid
                Subject: Six steps arranged in a horizontal flowchart with curved connecting arrows. Step 1: "Model" icon with formula blocks. Step 2: "Solve" icon with X mark (no solution). Step 3: "Add relax vars" icon showing switches appearing on rules. Step 4: "Re-solve" icon with checkmark. Step 5: "Diagnose" icon with magnifier glass over r=1. Step 6: "Present" icon with report card. Each step is a rounded card with a number badge. Connecting arrows flow left to right, then wrapping to a second row of three cards. A summary bar at bottom shows "minimize Σr → Σr=1 → partial solution found".
                Style/medium: clean modern process flow, professional infographic
                Composition/framing: two-row horizontal process, 3 cards per row, curved arrows between steps
                Color palette: alternating teal and amber cards, dark navy arrows, green accent for the final summary bar
                Text: no readable text, only step numbers and symbolic icons
                Constraints: no brand, no watermark
                """;
        builder.generateImage(prompt, ASSETS_DIR + "03-06-six-step-summary.png");
        log.info("Image 06 generated: six-step-summary");
    }
}
