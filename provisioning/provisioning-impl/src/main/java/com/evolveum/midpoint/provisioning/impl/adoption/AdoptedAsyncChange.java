/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.adoption;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectAsyncChange;

import com.evolveum.midpoint.schema.AcknowledgementSink;

import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.sync.ChangeProcessingBeans;

/**
 * Adopted "async update" change.
 */
public class AdoptedAsyncChange
        extends AdoptedAcknowledgeableChange<ResourceObjectAsyncChange>
        implements AcknowledgementSink {

    protected AdoptedAsyncChange(@NotNull ResourceObjectAsyncChange resourceObjectChange, ChangeProcessingBeans beans) {
        super(resourceObjectChange, false, beans);
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        resourceObjectChange.acknowledge(release, result);
    }
}
