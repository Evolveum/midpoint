/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;

/**
 * @author semancik
 *
 */
public class ConnectorOperationOptions {

    /**
     * Run the operations on resource using the specified identity.
     * Provided identification should identify valid, active account.
     *
     * The identification currently has the primary identifier. But we don't require it here, as we don't need it downstream.
     */
    private ResourceObjectIdentification<?> runAsIdentification;

    public ResourceObjectIdentification<?> getRunAsIdentification() {
        return runAsIdentification;
    }

    public void setRunAsIdentification(ResourceObjectIdentification<?> runAsIdentification) {
        this.runAsIdentification = runAsIdentification;
    }

    @Override
    public String toString() {
        return "ConnectorOperationOptions{" +
                "runAsIdentification=" + runAsIdentification +
                '}';
    }
}
