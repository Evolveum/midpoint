/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ObjectProcessingListener;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

public interface SimulationResultContext {

    @NotNull ObjectProcessingListener objectProcessingListener();

    /** OID of the {@link SimulationResultType} object. */
    @NotNull String getResultOid();

    /** Reference to the {@link SimulationResultType} object. */
    default ObjectReferenceType getResultRef() {
        return ObjectTypeUtil.createObjectRef(getResultOid(), ObjectTypes.SIMULATION_RESULT);
    }

    /** TEMPORARY. Retrieves stored deltas. May be replaced by something more general in the future. */
    default @NotNull Collection<ObjectDelta<?>> getStoredDeltas(OperationResult result) throws SchemaException {
        return getStoredProcessedObjects(result).stream()
                .map(ProcessedObject::getDelta)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** TEMPORARY. Retrieves stored processed objects. May be replaced by something more general in the future. */
    @NotNull Collection<ProcessedObject<?>> getStoredProcessedObjects(OperationResult result) throws SchemaException;
}
