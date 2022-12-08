package com.evolveum.midpoint.model.api.simulation;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.AggregatedObjectProcessingListener;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface SimulationResultContext {

    AggregatedObjectProcessingListener aggregatedObjectProcessingListener();

    /** OID of the {@link SimulationResultType} object that can be used to retrieve the data. */
    @NotNull String getResultOid();

    /** TEMPORARY. Retrieves stored deltas. May be replaced by something more general in the future. */
    @NotNull Collection<ObjectDelta<?>> getStoredDeltas(OperationResult result) throws SchemaException;
}
