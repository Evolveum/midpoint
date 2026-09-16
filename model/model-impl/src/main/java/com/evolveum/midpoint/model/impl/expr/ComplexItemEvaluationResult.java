/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
 * Result of the evaluation of evaluators that produces triples for inner paths, in addition to the main triple.
 *
 * For example, this is used by:
 *
 * - {@link AssociationSynchronizationExpressionEvaluator}
 * - {@link ComplexAttributeSynchronizationExpressionEvaluator}
 *
 * Currently tailored for the use with inbound mappings only.
 */
@Experimental
public class ComplexItemEvaluationResult<V extends PrismValue> extends AbstractDelegatedPrismValueDeltaSetTriple<V> {

    /** Delta set triples for inner values. Keyed by absolute path. */
    @NotNull private final DeltaSetTripleIvwoMap innerDeltaSetTriplesMap = new DeltaSetTripleIvwoMap();

    /** Definitions for inner items. Keyed by absolute path. */
    @NotNull private final PathKeyedMap<ItemDefinition<?>> innerItemDefinitionsMap = new PathKeyedMap<>();

    /** Inbound mapping evaluation requests for inner items. Keyed by absolute path. */
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

    /** Merges the specified triple map into the map of inner triples, prefixing each entry with given path prefix. */
    void mergeIntoInnerTriples(ItemPath pathPrefix, DeltaSetTripleIvwoMap tripleMap) {
        innerDeltaSetTriplesMap.putOrMergeAll(pathPrefix, tripleMap);
    }

    void mergeIntoInnerItemDefinitionsMap(ItemPath pathPrefix, PathKeyedMap<ItemDefinition<?>> itemDefinitionsMap) {
        for (var entry : itemDefinitionsMap.entrySet()) {
            innerItemDefinitionsMap.put(pathPrefix.append(entry.getKey()), entry.getValue());
        }
    }

    void mergeIntoInnerMappingEvaluationRequestsMap(
            ItemPath pathPrefix, MappingEvaluationRequestsMap mappingEvaluationRequestsMap) {
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
