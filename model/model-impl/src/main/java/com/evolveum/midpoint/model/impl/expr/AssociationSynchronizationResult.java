/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequestsMap;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.projector.focus.DeltaSetTripleIvwoMap;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.extensions.AbstractDelegatedPrismValueDeltaSetTriple;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Result of the evaluation of {@link AssociationSynchronizationExpressionEvaluator}.
 * It contains the main result (triple of values) but also other results, namely triples for inner paths.
 *
 * Currently quite experimental. It can be generalized to contain inner delta set triples only, which would be
 * applicable to more expression evaluators than just association synchronization.
 */
@Experimental
public class AssociationSynchronizationResult<V extends PrismValue> extends AbstractDelegatedPrismValueDeltaSetTriple<V> {

    /** Delta set triples for inner values. Keyed by absolute path. */
    @NotNull private final DeltaSetTripleIvwoMap innerDeltaSetTriplesMap = new DeltaSetTripleIvwoMap();

    /** Definitions for inner items. Keyed by absolute path. */
    @NotNull private final PathKeyedMap<ItemDefinition<?>> innerItemDefinitionsMap = new PathKeyedMap<>();

    /** Evaluation requests for inner items. Keyed by absolute path. */
    @NotNull private final MappingEvaluationRequestsMap innerMappingEvaluationRequestsMap = new MappingEvaluationRequestsMap();

    public @NotNull DeltaSetTripleIvwoMap getInnerDeltaSetTriplesMap() {
        return innerDeltaSetTriplesMap;
    }

    public @NotNull PathKeyedMap<ItemDefinition<?>> getInnerItemDefinitionsMap() {
        return innerItemDefinitionsMap;
    }

    public @NotNull MappingEvaluationRequestsMap getInnerMappingEvaluationRequestsMap() {
        return innerMappingEvaluationRequestsMap;
    }

    /** Merges the specified triple map into the map of other triples, prefixing each entry with given path prefix. */
    void mergeIntoOtherTriples(ItemPath pathPrefix, DeltaSetTripleIvwoMap tripleMap) {
        innerDeltaSetTriplesMap.putOrMergeAll(pathPrefix, tripleMap);
    }

    void mergeIntoItemDefinitionsMap(ItemPath pathPrefix, PathKeyedMap<ItemDefinition<?>> itemDefinitionsMap) {
        for (var entry : itemDefinitionsMap.entrySet()) {
            innerItemDefinitionsMap.put(pathPrefix.append(entry.getKey()), entry.getValue());
        }
    }

    void mergeIntoMappingEvaluationRequestsMap(ItemPath pathPrefix, MappingEvaluationRequestsMap mappingEvaluationRequestsMap) {
        for (var entry : mappingEvaluationRequestsMap.entrySet()) {
            innerMappingEvaluationRequestsMap.put(pathPrefix.append(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public String debugDump(int indent) {
        return super.debugDump(indent)
                + "\n"
                + DebugUtil.debugDump(innerDeltaSetTriplesMap, indent + 1);
    }
}
