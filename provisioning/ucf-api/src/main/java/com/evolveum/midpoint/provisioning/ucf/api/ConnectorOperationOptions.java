/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
     */
    private ResourceObjectIdentification runAsIdentification;

    public ResourceObjectIdentification getRunAsIdentification() {
        return runAsIdentification;
    }

    public void setRunAsIdentification(ResourceObjectIdentification runAsIdentification) {
        this.runAsIdentification = runAsIdentification;
    }
}
