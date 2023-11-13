/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Operations to be executed on a resource object.
 *
 * @author semancik
 */
public class ResourceObjectOperations {

    /** Low-level (transformed, elementary) operations to be executed. */
    @NotNull private final Collection<Operation> ucfOperations = new ArrayList<>();

    /** TODO */
    private ShadowType currentShadow = null;

    /** The context in which the operations will be carried out. */
    @NotNull private final ProvisioningContext resourceObjectContext;

    public ResourceObjectOperations(@NotNull ProvisioningContext resourceObjectContext) {
        this.resourceObjectContext = resourceObjectContext;
    }

    public ShadowType getCurrentShadow() {
        return currentShadow;
    }

    public void setCurrentShadow(ShadowType currentShadow) {
        this.currentShadow = currentShadow;
    }

    public @NotNull ProvisioningContext getResourceObjectContext() {
        return resourceObjectContext;
    }

    public @NotNull Collection<Operation> getUcfOperations() {
        return ucfOperations;
    }

    public void add(@NotNull Operation operation) {
        if (!ucfOperations.contains(operation)) {
            ucfOperations.add(operation);
        }
    }

    @Override
    public String toString() {
        return "ResourceObjectOperations("
                + "operations=" + ucfOperations
                + ", currentShadow=" + currentShadow
                + ", ctx=" + resourceObjectContext + ")";
    }
}
