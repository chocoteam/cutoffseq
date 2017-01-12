/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.sum;

import org.chocosolver.solver.constraints.Operator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

/**
 * A propagator for SUM(x_i) = y + b, where x_i are boolean variables
 * <br/>
 * Based on "Bounds Consistency Techniques for Long Linear Constraint" </br>
 * W. Harvey and J. Schimpf
 * <p>
 *
 * @author Charles Prud'homme
 * @since 18/03/11
 */
public class PropSumBool extends PropSum {

    /**
     * The resulting variable
     */
    protected final IntVar sum;

    /**
     * Creates a sum propagator: SUM(x_i) = sum + b, where x_i are boolean variables.
     * Coefficients are induced by <code>pos</code>:
     * those before <code>pos</code> (included) are equal to 1,
     * the other ones are equal to -1.
     * @param variables list of boolean variables
     * @param pos position of the last positive (induced) coefficient
     * @param o operator
     * @param sum resulting variable
     * @param b bound to respect
     * @param reactOnFineEvent set to <tt>true</tt> to react on fine events
     */
    protected PropSumBool(BoolVar[] variables, int pos, Operator o, IntVar sum, int b, boolean reactOnFineEvent) {
        super(ArrayUtils.append(variables, new IntVar[]{sum}), pos, o, b, PropagatorPriority.BINARY, reactOnFineEvent);
        this.sum = sum;
    }

    /**
     * Creates a sum propagator: SUM(x_i) = sum + b, where x_i are boolean variables.
     * Coefficients are induced by <code>pos</code>:
     * those before <code>pos</code> (included) are equal to 1,
     * the other ones are equal to -1.
     * @param variables list of boolean variables
     * @param pos position of the last positive (induced) coefficient
     * @param o operator
     * @param sum resulting variable
     * @param b bound to respect
     */
    public PropSumBool(BoolVar[] variables, int pos, Operator o, IntVar sum, int b) {
        this(variables, pos, o, sum, b, false);
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        switch (o) {
            case NQ:
                return IntEventType.INSTANTIATE.getMask();
            case LE:
                return IntEventType.combine(IntEventType.INSTANTIATE, vIdx == l - 1 ? IntEventType.DECUPP : IntEventType.VOID);
            case GE:
                return IntEventType.combine(IntEventType.INSTANTIATE, vIdx == l - 1 ? IntEventType.INCLOW : IntEventType.VOID);
            default:
                return IntEventType.boundAndInst();
        }
    }

    @Override
    protected void prepare() {
        int i = 0, k;
        int lb = 0, ub = 0;
        for (; i < pos; i++) { // first the positive coefficients
            if (vars[i].isInstantiated()) {
                k = vars[i].getLB();
                lb += k;
                ub += k;
            } else {
                ub++;
            }
        }
        for (; i < l - 1; i++) { // then the negative ones
            if (vars[i].isInstantiated()) {
                k = vars[i].getLB();
                lb -= k;
                ub -= k;
            } else {
                lb--;
            }
        }
        sumLB = lb - sum.getUB();
        sumUB = ub - sum.getLB();
    }

    @SuppressWarnings({"NullableProblems"})
    @Override
    protected void filterOnEq() throws ContradictionException {
        int F = b - sumLB;
        int E = sumUB - b;
        if (F < 0 || E < 0) {
            fails();
        }
        int lb, ub, i = 0;
        // deal with sum
        lb = -sum.getUB();
        ub = -sum.getLB();
        if (sum.updateLowerBound(-F - lb, this)) {
            int nub = -sum.getLB();
            E += nub - ub;
            ub = nub;
        }
        if (sum.updateUpperBound(-ub + E, this)) {
            int nlb = -sum.getUB();
            F -= nlb - lb;
        }
        if (F == 0 || E == 0) { // the main reason we implemented a dedicated version
            // positive coefficients first
            while (i < pos) {
                if (F == 0 && !vars[i].isInstantiated() && vars[i].instantiateTo(0, this)) {
                    E++;
                }
                if (E == 0 && !vars[i].isInstantiated() && vars[i].instantiateTo(1, this)) {
                    F++;
                }
                i++;
            }
            // then negative ones
            while (i < l - 1) {
                if (F == 0 && !vars[i].isInstantiated() && vars[i].instantiateTo(1, this)) {
                    E--;
                }
                if (E == 0 && !vars[i].isInstantiated() && vars[i].instantiateTo(0, this)) {
                    F--;
                }
                i++;
            }
        }
    }


    @SuppressWarnings({"NullableProblems"})
    @Override
    protected void filterOnLeq() throws ContradictionException {
        int F = b - sumLB;
        int E = sumUB - b;
        if (F < 0) {
            fails();
        }
        int lb, ub, i = 0;
        // deal with sum
        lb = -sum.getUB();
        ub = -sum.getLB();
        if (sum.updateLowerBound(-F - lb, this)) {
            int nub = -sum.getLB();
            E += nub - ub;
        }
        if (F == 0) { // the main reason we implemented a dedicated version
            // positive coefficients first
            while (i < pos) {
                if (!vars[i].isInstantiated() && vars[i].instantiateTo(0, this)) {
                    E++;
                }
                i++;
            }
            // then negative ones
            while (i < l - 1) {
                if (!vars[i].isInstantiated() && vars[i].instantiateTo(1, this)) {
                    E--;
                }
                i++;
            }
        }
        if (E <= 0) {
            this.setPassive();
        }
    }

    @SuppressWarnings({"NullableProblems"})
    @Override
    protected void filterOnGeq() throws ContradictionException {
        int F = b - sumLB;
        int E = sumUB - b;
        if (E < 0) {
            fails();
        }
        int lb, ub, i = 0;
        // deal with sum
        lb = -sum.getUB();
        ub = -sum.getLB();
        if (sum.updateUpperBound(-ub + E, this)) {
            int nlb = -sum.getUB();
            F -= nlb - lb;
        }
        if (E == 0) { // the main reason we implemented a dedicated version
            // positive coefficients first
            while (i < pos) {
                if (!vars[i].isInstantiated() && vars[i].instantiateTo(1, this)) {
                    F++;
                }
                i++;
            }
            // then negative ones
            while (i < l - 1) {
                if (!vars[i].isInstantiated() && vars[i].instantiateTo(0, this)) {
                    F--;
                }
                i++;
            }
        }
        if (F <= 0) {
            this.setPassive();
        }
    }

    @Override
    public ESat isEntailed() {
        int sumUB = 0, sumLB = 0, i = 0;
        for (; i < pos; i++) { // first the positive coefficients
            sumLB += vars[i].getLB();
            sumUB += vars[i].getUB();
        }
        for (; i < l; i++) { // then the negative ones
            sumLB -= vars[i].getUB();
            sumUB -= vars[i].getLB();
        }
        return check(sumLB, sumUB);
    }

    @Override
    public String toString() {
        StringBuilder linComb = new StringBuilder(20);
        linComb.append(pos == 0 ? "-" : "").append(vars[0].getName());
        int i = 1;
        for (; i < pos; i++) {
            linComb.append(" + ").append(vars[i].getName());
        }
        for (; i < l - 1; i++) {
            linComb.append(" - ").append(vars[i].getName());
        }
        linComb.append(" ").append(o).append(" ");
        linComb.append(vars[i].getName()).append(" ").append(b < 0 ? "- " : "+ ").append(Math.abs(b));
        return linComb.toString();
    }

    @Override
    protected PropSum opposite(){
        BoolVar[] bvars = new BoolVar[vars.length-1];
        System.arraycopy(vars, 0, bvars, 0, bvars.length);
        return new PropSumBool(bvars, pos, nop(o), vars[vars.length-1], b + nb(o), reactToFineEvt);
    }
}