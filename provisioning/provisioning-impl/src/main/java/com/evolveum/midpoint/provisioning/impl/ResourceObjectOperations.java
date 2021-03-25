/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class ResourceObjectOperations {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectOperations.class);

    private final Collection<Operation> operations = new ArrayList<>();
    private PrismObject<ShadowType> currentShadow = null;
    private ProvisioningContext resourceObjectContext = null;
    private Collection<? extends ResourceAttribute<?>> allIdentifiers;

    public PrismObject<ShadowType> getCurrentShadow() {
        return currentShadow;
    }

    public void setCurrentShadow(PrismObject<ShadowType> currentShadow) {
        this.currentShadow = currentShadow;
    }

    public ProvisioningContext getResourceObjectContext() {
        return resourceObjectContext;
    }

    public void setResourceObjectContext(ProvisioningContext resourceObjectContext) {
        this.resourceObjectContext = resourceObjectContext;
    }

    @NotNull public Collection<Operation> getOperations() {
        return operations;
    }

    public void add(Operation operation) {
        if (!operations.contains(operation)) {
            operations.add(operation);
        }
    }

    public Collection<? extends ResourceAttribute<?>> getAllIdentifiers() {
        return allIdentifiers;
    }

    public void setAllIdentifiers(Collection<? extends ResourceAttribute<?>> allIdentifiers) {
        this.allIdentifiers = allIdentifiers;
    }

    @Override
    public String toString() {
        return "ResourceObjectOperations(operations=" + operations + ", currentShadow=" + currentShadow
                + ", ctx=" + resourceObjectContext + ")";
    }



}
