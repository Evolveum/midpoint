/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObject;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.Operation;

import org.jetbrains.annotations.Nullable;

/**
 * Operations to be executed on given resource object.
 *
 * @author semancik
 */
public class ResourceObjectOperations {

    /** Low-level (transformed, elementary) operations to be executed. */
    @NotNull private final Collection<Operation> ucfOperations = new ArrayList<>();

    /** We store the current state here if there is a need to avoid duplicate values. */
    private ExistingResourceObject currentResourceObject;

    /** The context in which the operations will be carried out. */
    @NotNull private final ProvisioningContext resourceObjectContext;

    public ResourceObjectOperations(@NotNull ProvisioningContext resourceObjectContext) {
        this.resourceObjectContext = resourceObjectContext;
    }

    public @Nullable ExistingResourceObject getCurrentResourceObject() {
        return currentResourceObject;
    }

    public void setCurrentResourceObject(ExistingResourceObject currentResourceObject) {
        this.currentResourceObject = currentResourceObject;
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
                + ", currentShadow=" + currentResourceObject
                + ", ctx=" + resourceObjectContext + ")";
    }
}
