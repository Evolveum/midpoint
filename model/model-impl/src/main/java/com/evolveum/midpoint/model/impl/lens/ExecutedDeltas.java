/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Deltas that have been executed; sorted according to waves and delta types.
 */
class ExecutedDeltas<O extends ObjectType> implements Serializable {

    /** Lists of executed deltas, indexed by wave. */
    @NotNull private final List<List<LensObjectDeltaOperation<O>>> operationsByWave = new ArrayList<>();

    ObjectDelta<O> getSummaryExecutedDelta() throws SchemaException {
        return ObjectDeltaCollectionsUtil.summarize(
                getDeltasStream()
                        .map(odo -> odo.getObjectDelta())
                        .toList());
    }

    @NotNull Stream<LensObjectDeltaOperation<O>> getDeltasStream() {
        return operationsByWave.stream()
                .flatMap(odoList -> odoList.stream());
    }

    @NotNull List<LensObjectDeltaOperation<O>> getDeltas() {
        return getDeltasStream().toList();
    }

    @NotNull List<LensObjectDeltaOperation<O>> getDeltas(int wave) {
        return operationsByWave.size() > wave ? operationsByWave.get(wave) : List.of();
    }

    void add(LensObjectDeltaOperation<O> odo) {
        int wave = odo.getWave();
        while (operationsByWave.size() <= wave) {
            operationsByWave.add(new ArrayList<>());
        }
        operationsByWave
                .get(wave)
                .add(odo);
    }

    void fillClonedFrom(@NotNull ExecutedDeltas<O> executedDeltas) {
        stateCheck(operationsByWave.isEmpty(), "Attempt to fill clonedFrom into non-empty ExecutedDeltas");
        executedDeltas.operationsByWave.forEach(
                operationsForWave ->
                        operationsByWave.add(CloneUtil.cloneCollectionMembers(operationsForWave)));
    }
}
