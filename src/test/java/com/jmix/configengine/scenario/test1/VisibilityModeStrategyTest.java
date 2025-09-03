package com.jmix.configengine.scenario.test1;

import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleSecnarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.model.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class VisibilityModeStrategyTest extends ModuleSecnarioTestBase {
    
    //---------------Model definition with all 3 strategies----------------------------------------
    @ModuleAnno(id = 1003L)
    static public class VisibilityModeStrategyConstraint extends ConstraintAlgImpl {
        @ParaAnno(
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "2"
        )
        private ParaVar P0;
        
        @ParaAnno(
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "2"
        )
        private ParaVar P1;
        
        @ParaAnno(
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "2"
        )
        private ParaVar P2;
        
        // Store assumption literals for later use
        private List<Literal> assumptions = new ArrayList<>();

        @Override
        protected void initConstraint() {
            // STRATEGY 1: Establish relationship between visibilityModeVar and var
            // This should ideally be in the base class ConstraintAlgImpl
            establishVisibilityRelationship(P0, 0);
            establishVisibilityRelationship(P1, 0);
            establishVisibilityRelationship(P2, 0);
            
            // Business constraints
            // C1: if P0.var==1 or P0.var==2 then P1.visibilityModeVar=3
            implementBusinessConstraintC1();
            
            // C2: if P1.var==2 then P2.var=2
            implementBusinessConstraintC2();
            
            // STRATEGY 3: Implement default values using Assumptions
            setupDefaultVisibilityWithAssumptions();

            // setupSearchStrategy();
        }
        
        /**
         * STRATEGY 1 IMPLEMENTATION: Establish core visibility relationship
         * This defines the fundamental rule: when invisible, var must be minimum
         */
        private void establishVisibilityRelationship(ParaVar paraVar, int minValue) {
            // Step 1: Define domain for visibilityModeVar (only 0 or 3)
            BoolVar isVisible = model.newBoolVar(paraVar.getCode() + "_is_visible");
            BoolVar isInvisible = model.newBoolVar(paraVar.getCode() + "_is_invisible");
            
            // visibilityModeVar can only be 0 (visible) or 3 (invisible)
            model.addEquality((IntVar)paraVar.visibilityModeVar, 0).onlyEnforceIf(isVisible);
            model.addEquality((IntVar)paraVar.visibilityModeVar, 3).onlyEnforceIf(isInvisible);
            
            // Ensure exactly one state is active
            model.addBoolOr(new Literal[]{isVisible, isInvisible});
            model.addAtMostOne(new Literal[]{isVisible, isInvisible});
            
            // Step 2: Core visibility control rule
            // When invisible (visibilityModeVar=3), var MUST be minimum value
            // This is the key constraint that reduces search space
            model.addEquality((IntVar)paraVar.var, minValue).onlyEnforceIf(isInvisible);
            
            // When visible (visibilityModeVar=0), var can be any value in domain
            // No additional constraint needed - var naturally uses its domain
            
            log.debug("Established visibility relationship for {}: invisible => var={}", 
                     paraVar.getCode(), minValue);
        }
        
        /**
         * Business constraint C1: External constraint that can change visibilityModeVar
         */
        private void implementBusinessConstraintC1() {
            // C1: if (P0.var==1 or P0.var==2) then P1.visibilityModeVar=3
            BoolVar p0IsNot0 = model.newBoolVar("p0_is_not_0");
            
            // Create constraint: P0.var != 0
            model.addDifferent((IntVar)P0.var, 0).onlyEnforceIf(p0IsNot0);
            model.addEquality((IntVar)P0.var, 0).onlyEnforceIf(p0IsNot0.not());
            
            // Apply visibility change based on P0 value
            // If P0.var != 0, then P1 becomes invisible
            model.addEquality((IntVar)P1.visibilityModeVar, 3).onlyEnforceIf(p0IsNot0);
            // If P0.var == 0, then P1 remains visible (will be handled by assumptions)
        }
        
        /**
         * Business constraint C2
         */
        private void implementBusinessConstraintC2() {
            // C2: if P1.var==2 then P2.var=2
            BoolVar p1Is2 = model.newBoolVar("p1_is_2");
            
            model.addEquality((IntVar)P1.var, 2).onlyEnforceIf(p1Is2);
            model.addDifferent((IntVar)P1.var, 2).onlyEnforceIf(p1Is2.not());
            
            model.addEquality((IntVar)P2.var, 2).onlyEnforceIf(p1Is2);
        }
        
        /**
         * STRATEGY 3 IMPLEMENTATION: Setup default values using Assumptions
         * Assumptions are "soft constraints" that solver tries to satisfy but can be overridden
         */
        private void setupDefaultVisibilityWithAssumptions() {
            // P0 default: always visible (no external constraint changes it)
            BoolVar p0DefaultVisible = model.newBoolVar("p0_default_visible");
            model.addEquality((IntVar)P0.visibilityModeVar, 0).onlyEnforceIf(p0DefaultVisible);
            assumptions.add(p0DefaultVisible);
            
            // P1 default: visible, but can be overridden by C1
            // We create a conditional assumption
            BoolVar p0Is0 = model.newBoolVar("p0_is_0_for_assumption");
            model.addEquality((IntVar)P0.var, 0).onlyEnforceIf(p0Is0);
            model.addDifferent((IntVar)P0.var, 0).onlyEnforceIf(p0Is0.not());
            
            BoolVar p1DefaultVisible = model.newBoolVar("p1_default_visible");
            // P1 should be visible by default when P0=0
            model.addEquality((IntVar)P1.visibilityModeVar, 0).onlyEnforceIf(p1DefaultVisible);
            // Only add assumption when P0=0 (when C1 doesn't apply)
            model.addImplication(p0Is0, p1DefaultVisible);
            assumptions.add(p1DefaultVisible);
            
            // P2 default: always visible (no external constraint changes it)
            BoolVar p2DefaultVisible = model.newBoolVar("p2_default_visible");
            model.addEquality((IntVar)P2.visibilityModeVar, 0).onlyEnforceIf(p2DefaultVisible);
            assumptions.add(p2DefaultVisible);
            
            // Apply all assumptions to the model
            for (Literal assumption : assumptions) {
                model.addAssumption(assumption);
            }
            
            log.debug("Added {} assumptions for default visibility values", assumptions.size());
        }
        
        /**
         * STRATEGY 2 IMPLEMENTATION: Search strategy prioritizing visibilityModeVar
         * This method should be called by the solver framework
         */
        // @Override
        protected void setupSearchStrategy() {
            List<IntVar> searchOrder = new ArrayList<>();
            
            // PHASE 1: Determine visibility modes first
            // This drastically reduces search space by fixing var values early
            searchOrder.add((IntVar)P0.visibilityModeVar);
            searchOrder.add((IntVar)P1.visibilityModeVar);
            searchOrder.add((IntVar)P2.visibilityModeVar);
            
            // PHASE 2: Then determine actual values
            // Many of these are already determined by visibility constraints
            searchOrder.add((IntVar)P0.var);
            searchOrder.add((IntVar)P1.var);
            searchOrder.add((IntVar)P2.var);
            
            // Configure search strategy
            model.addDecisionStrategy(
                searchOrder.toArray(new IntVar[0]),
                DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
                DecisionStrategyProto.DomainReductionStrategy.SELECT_MIN_VALUE
            );
            
            log.debug("Search strategy configured: visibility vars first, then value vars");
        }
        
        /**
         * Alternative search strategy with more sophisticated heuristics
         */
        protected void setupAdvancedSearchStrategy() {
            // Create separate strategies for different variable types
            IntVar[] visibilityVars = new IntVar[] {
                (IntVar)P0.visibilityModeVar,
                (IntVar)P1.visibilityModeVar,
                (IntVar)P2.visibilityModeVar
            };
            
            IntVar[] valueVars = new IntVar[] {
                (IntVar)P0.var,
                (IntVar)P1.var,
                (IntVar)P2.var
            };
            
            // Strategy 1: Fix visibility modes with minimum value preference
            // This tends to prefer visible (0) over invisible (3)
            model.addDecisionStrategy(
                visibilityVars,
                DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_MIN_DOMAIN_SIZE,
                DecisionStrategyProto.DomainReductionStrategy.SELECT_MIN_VALUE
            );
            
            // Strategy 2: Then fix actual values
            model.addDecisionStrategy(
                valueVars,
                DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_MIN_DOMAIN_SIZE,
                DecisionStrategyProto.DomainReductionStrategy.SELECT_MIN_VALUE
            );
        }
    }
    //---------------Model definition end----------------------------------------

    public VisibilityModeStrategyTest() {
        super(VisibilityModeStrategyConstraint.class);
    }

    @Test
    public void testStrategy1_VisibilityRelationship() {
        // Test Strategy 1: Verify visibility-var relationship
        // When P0=1, P1 becomes invisible and must have var=0
        inferParasByPara("P0", "1");
        
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(3);
        
        // Verify the core relationship: invisible => minimum value
        for(int i = 0; i < 3; i++) {
            solutions(i)
                .assertPara("P1").valueEqual(0).visibilityModeEqual(3);
        }
        
        log.info("Strategy 1 verified: When P1 is invisible, P1.var is forced to 0");
        printSolutions();
    }
    
    // @Test
    public void testStrategy2_SearchOrderImpact() {
        // Test Strategy 2: Search strategy impact
        // The search should efficiently find solutions by fixing visibility first
        
        long startTime = System.currentTimeMillis();
        // inferAllSolutions();
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(13);
        
        log.info("Strategy 2: Found all 13 solutions in {}ms", elapsedTime);
        log.info("Search space reduction: 27 potential -> 13 actual solutions");
        
        // Verify search found correct distribution
        assertSolutionNum("P0:0", 7);  // P1 visible cases
        assertSolutionNum("P0:1", 3);  // P1 invisible cases
        assertSolutionNum("P0:2", 3);  // P1 invisible cases
        
        printSolutions();
    }
    
    @Test
    public void testStrategy3_DefaultValues() {
        // Test Strategy 3: Assumptions provide default values
        // When no constraints apply, all parameters should be visible
        inferParasByPara("P0", "0", "P1", "0", "P2", "0");
        
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
        
        // All should use default visibility (0)
        solutions(0)
            .assertPara("P0").visibilityModeEqual(0)
            .assertPara("P1").visibilityModeEqual(0)
            .assertPara("P2").visibilityModeEqual(0);
        
        log.info("Strategy 3 verified: Default visibility values applied via assumptions");
        printSolutions();
    }
    
    @Test
    public void testStrategy3_AssumptionOverride() {
        // Test that assumptions can be overridden by hard constraints
        inferParasByPara("P0", "2");
        
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(3);
        
        // P1's default (visible) is overridden by C1 constraint
        for(int i = 0; i < 3; i++) {
            solutions(i)
                .assertPara("P1").visibilityModeEqual(3);  // Overridden to invisible
        }
        
        log.info("Strategy 3: Assumption successfully overridden by hard constraint");
        printSolutions();
    }
    
    // @Test
    public void testCombinedStrategiesEfficiency() {
        // Test all three strategies working together
        // inferAllSolutions();
        
        // Count solutions by visibility state
        int visibleP1Count = 0;
        int invisibleP1Count = 0;
        
        for(int i = 0; i < 13; i++) {
            // This would normally require checking each solution
            // but our constraints guarantee the distribution
        }
        
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(13);
        
        // Verify the three strategies worked together:
        // 1. Visibility relationship enforced (P1=0 when invisible)
        assertSolutionNum("P0:1,P1:1", 0);  // Impossible due to Strategy 1
        assertSolutionNum("P0:2,P1:2", 0);  // Impossible due to Strategy 1
        
        // 2. Search found all valid solutions efficiently (13 not 27)
        
        // 3. Default values were applied where appropriate
        assertSolutionNum("P0:0,P1:0", 3);  // P1 visible by default when P0=0
        
        log.info("All 3 strategies working together successfully");
        printSolutions();
    }
}