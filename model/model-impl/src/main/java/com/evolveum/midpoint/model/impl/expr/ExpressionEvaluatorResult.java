/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.projector.focus.DeltaSetTripleIvwoMap;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.extensions.AbstractDelegatedPrismValueDeltaSetTriple;
import com.evolveum.midpoint.util.DebugUtil;

/** TODO */
public class ExpressionEvaluatorResult<V extends PrismValue> extends AbstractDelegatedPrismValueDeltaSetTriple<V> {

    @NotNull private final DeltaSetTripleIvwoMap otherDeltaSetTriplesMap = new DeltaSetTripleIvwoMap();

    public @NotNull DeltaSetTripleIvwoMap getOtherDeltaSetTriplesMap() {
        return otherDeltaSetTriplesMap;
    }

    void mergeIntoOtherTriples(DeltaSetTripleIvwoMap tripleMap) {
        otherDeltaSetTriplesMap.putOrMergeAll(tripleMap);
    }

    @Override
    public String debugDump(int indent) {
        return super.debugDump(indent)
                + "\n"
                + DebugUtil.debugDump(otherDeltaSetTriplesMap, indent + 1);
    }
}
