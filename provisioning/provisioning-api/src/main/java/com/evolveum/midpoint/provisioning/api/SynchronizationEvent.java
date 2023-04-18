/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a synchronization change event (obtained typically by live synchronization or asynchronous update)
 * that needs to be processed.
 *
 * It is comparable on the sequential number.
 */
public interface SynchronizationEvent extends AcknowledgementSink, DebugDumpable, Comparable<SynchronizationEvent> {

    /**
     * The description of the change.
     *
     * BEWARE! Can be null for erroneous changes that failed in such a crude way that no repo shadow was created.
     */
    ResourceObjectShadowChangeDescription getChangeDescription();

    /**
     * Sequential number of this event.
     *
     * It is unique at least in the context of the current synchronization operation
     * ({@link ProvisioningService#synchronize(com.evolveum.midpoint.schema.ResourceShadowCoordinates, LiveSyncOptions,
     * LiveSyncTokenStorage, LiveSyncEventHandler, Task, OperationResult)}
     * or {@link ProvisioningService#processAsynchronousUpdates(com.evolveum.midpoint.schema.ResourceShadowCoordinates,
     * AsyncUpdateEventHandler, Task, OperationResult)}).
     */
    int getSequentialNumber();

    /**
     * Value against which the events are to be ordered: events A and B having `A.sequentialNumber`
     * less than `B.sequentialNumber` must be processed in that order (A then B) if their correlation value is the
     * same. (Which means that they refer to the same resource object.)
     */
    Object getCorrelationValue();

    /**
     * Is the event ready to be processed? Events can be incomplete e.g. in the case of errors during pre-processing.
     *
     * TODO reconsider the name
     */
    boolean isComplete();

    /**
     * Is the event irrelevant, and therefore should be skipped?
     * This means no error has occurred, but simply there is nothing to do.
     * Like a deletion of already-deleted account.
     */
    boolean isNotApplicable();

    /**
     * Has the event encountered an error during pre-processing?
     * Such events should be reported but not processed in the regular way.
     */
    boolean isError();

    /**
     * Error message related to the pre-processing of this event.
     * Should be non-null only if {@link #isError()} is `true`.
     *
     * Temporary feature. Most probably it will be replaced by something more serious.
     */
    @Experimental
    String getErrorMessage();

    /**
     * OID of the shadow corresponding to the resource object in question.
     * Should be non-null if the change was pre-processed correctly.
     */
    String getShadowOid();

    /**
     * The resulting combination of resource object and its repo shadow.
     *
     * TODO declare as non-null
     *
     * TODO clarify this description; see `ShadowedChange.shadowedObject`
     */
    PrismObject<ShadowType> getShadowedObject();

    default @NotNull PrismObject<ShadowType> getShadowedObjectRequired() {
        return Objects.requireNonNull(getShadowedObject(), "no shadowed object");
    }
}
