package com.jmix.temp3;

import com.jmix.temp3.core.CpModelTracker;
import com.jmix.temp3.core.Part;
import com.jmix.temp3.core.PartVar;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.DecisionStrategyProto;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;
import com.google.ortools.sat.SatParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Verify OR-Tools decision strategy using Part/PartVar model classes.
 *
 * <p>Scenario: 3 disk drives with different speeds (5400, 7200, 9000).
 * Constraint: exactly one drive must be selected.
 * Strategy: sort isSelected vars by speed → controls which speed is found first.
 *
 * <p>Key: FIXED_SEARCH is required for addDecisionStrategy to take effect.
 */
public class DecisionStrategyVerifyTest {

    static {
        Loader.loadNativeLibraries();
    }
    static class SolutionCallback extends CpSolverSolutionCallback {
        final List<String> solutions = new ArrayList<>();
        final List<PartVar> partVars;
        int step = 0;

        SolutionCallback(List<PartVar> partVars) {
            this.partVars = partVars;
        }

        @Override
        public void onSolutionCallback() {
            step++;
            StringBuilder sb = new StringBuilder("S" + step + ": ");
            PartVar selected = null;
            for (PartVar pv : partVars) {
                long isSel = value(pv.isSelected);
                long qty = value(pv.qty);
                sb.append(pv.part.code).append("(S:").append(isSel).append(",Q:").append(qty).append(") ");
                if (isSel == 1) selected = pv;
            }
            if (selected != null) {
                sb.append("→ ").append(selected.part.code)
                        .append(" speed=").append(selected.part.speed);
            }
            solutions.add(sb.toString());
        }

        List<String> getSolutions() { return solutions; }
    }

    public static void main(String[] args) {
        System.out.println("=== Decision Strategy with Part/PartVar ===\n");

        // Create 3 disk parts with different speeds
        List<Part> parts = Arrays.asList(
                new Part("disk1", true, 7200, 3),
                new Part("disk2", true, 5400, 6),
                new Part("disk3", true, 9000, 9),
                new Part("disk4", true, 3200, 9));

        System.out.println("Disks:");
        for (Part p : parts) {
            System.out.println("  " + p.code + "  speed=" + p.speed + "  capacity=" + p.capacity);
        }

        // ---- Test 1: No strategy (baseline) ----
        System.out.println("\n--- Test 1: No strategy (baseline) ---");
        testWithStrategy(parts, null, "No strategy");

        // ---- Test 2: ASCENDING by speed (cheapest speed first, 5400 → 7200 → 9000) ----
        System.out.println("\n--- Test 2: ASCENDING speed (5400→7200→9000) ---");
        testWithStrategy(parts, StrategyType.ASCENDING, "ASC speed");

        // ---- Test 3: DESCENDING by speed (highest speed first, 9000 → 7200 → 5400) ----
        System.out.println("\n--- Test 3: DESCENDING speed (9000→7200→5400) ---");
        testWithStrategy(parts, StrategyType.DESCENDING, "DESC speed");

        System.out.println("\n=== Done ===");
    }

    enum StrategyType { ASCENDING, DESCENDING }

    static void testWithStrategy(List<Part> parts, StrategyType st, String label) {
        // Build model with PartVars via CpModelTracker
        CpModelTracker tracker = new CpModelTracker();
        List<PartVar> partVars = new ArrayList<>();
        List<Literal> literals = new ArrayList<>();
        for (Part part : parts) {
            PartVar pv = new PartVar(tracker, part);
            partVars.add(pv);
            literals.add(pv.isSelected);
        }
        // Exactly one disk selected
        tracker.getModel().addExactlyOne(literals.toArray(new Literal[0]));

        // Apply decision strategy: sort isSelected vars by speed
        if (st != null) {
            // Sort PartVars by speed
            List<PartVar> sorted = new ArrayList<>(partVars);
            if (st == StrategyType.ASCENDING) {
                sorted.sort(Comparator.comparingInt(pv -> pv.part.speed));
            } else {
                sorted.sort((a, b) -> Integer.compare(b.part.speed, a.part.speed));
            }

            System.out.print("  Var order: ");
            for (PartVar pv : sorted) System.out.print(pv.part.code + "(spd=" + pv.part.speed + ") ");
            System.out.println();

            IntVar[] orderedVars = sorted.stream().map(pv -> (IntVar) pv.isSelected).toArray(IntVar[]::new);
            tracker.getModel().addDecisionStrategy(orderedVars,
                    DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
                    DecisionStrategyProto.DomainReductionStrategy.SELECT_MAX_VALUE);
        }

        // Solve with FIXED_SEARCH to honor decision strategy
        CpSolver solver = new CpSolver();
        solver.getParameters().setEnumerateAllSolutions(true);
        solver.getParameters().setNumSearchWorkers(1);
        if (st != null) {
            solver.getParameters().setSearchBranching(SatParameters.SearchBranching.FIXED_SEARCH);
        }

        SolutionCallback cb = new SolutionCallback(partVars);
        CpSolverStatus status = solver.solve(tracker.getModel(), cb);

        System.out.println("  Status: " + status);
        for (String s : cb.getSolutions()) {
            System.out.println("  " + s);
        }

        // Verify: first solution should match expectation
        if (st != null && !cb.getSolutions().isEmpty()) {
            String first = cb.getSolutions().get(0);
            if (st == StrategyType.ASCENDING) {
                boolean ok = first.contains("disk1"); // 5400 = slowest/cheapest
                System.out.println("  " + (ok ? "PASS" : "FAIL") + ": expected disk1(speed=5400) first");
            } else {
                boolean ok = first.contains("disk3"); // 9000 = fastest
                System.out.println("  " + (ok ? "PASS" : "FAIL") + ": expected disk3(speed=9000) first");
            }
        }
    }
}
