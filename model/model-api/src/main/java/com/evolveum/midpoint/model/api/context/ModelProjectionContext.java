/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.CompositeObjectDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author semancik
 *
 */
public interface ModelProjectionContext extends ModelElementContext<ShadowType> {

    /**
     * Returns synchronization delta.
     *
     * Synchronization delta describes changes that have recently happened. MidPoint reacts to these
     * changes by "pulling them in" (e.g. using them in inbound mappings).
     */
    ObjectDelta<ShadowType> getSyncDelta();

    void setSyncDelta(ObjectDelta<ShadowType> syncDelta);

    @NotNull ProjectionContextKey getKey();

    /**
     * Initial intent regarding the account. It indicates what the initiator of the operation
     * _wants to do_ with the context.
     *
     * If set to null then the decision is left to "the engine". Null is also a typical value
     * when the context is created. It may be pre-set under some circumstances, e.g. if an account is being unlinked.
     */
    SynchronizationIntent getSynchronizationIntent();

    /**
     * Decision regarding the account. It describes the overall situation of the account e.g. whether account
     * is added, is to be deleted, unliked, etc.
     *
     * If set to null no decision was made yet. Null is also a typical value when the context is created.
     *
     * @see SynchronizationPolicyDecision
     */
    SynchronizationPolicyDecision getSynchronizationPolicyDecision();

    /**
     * Returns delta suitable for execution. The primary and secondary deltas may not make complete sense all by themselves.
     * E.g. they may both be MODIFY deltas even in case that the account should be created. The deltas begin to make sense
     * only if combined with sync decision. This method provides the deltas all combined and ready for execution.
     */
    ObjectDelta<ShadowType> getExecutableDelta() throws SchemaException, ConfigurationException;

    boolean isFullShadow();

    Boolean isLegal();

    boolean isExists();

    boolean isGone();

    @Deprecated default boolean isTombstone() {
        return isGone();
    }

    /**
     * Dependencies returned are "almost complete": resource OID and kind are non-null.
     * Intent may be null; in that case, the default value has to be used.
     *
     * TODO decide what to return if the dependency configuration cannot be obtained; currently it's an empty list
     */
    Collection<ResourceObjectTypeDependencyType> getDependencies() throws SchemaException, ConfigurationException;

    CompositeObjectDefinition getCompositeObjectDefinition() throws SchemaException, ConfigurationException;

    ResourceType getResource();
}
