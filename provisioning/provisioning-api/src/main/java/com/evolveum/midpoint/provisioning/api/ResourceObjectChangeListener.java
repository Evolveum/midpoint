/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

/**
 * Listens for notifications about resource object changes.
 * Typically used to implement synchronization of resource objects.
 *
 * @author Radovan Semancik
 *
 * @see ResourceObjectShadowChangeDescription
 */
public interface ResourceObjectChangeListener extends ProvisioningListener {

    /**
     * Processes a notification about a specific change that happened on the resource.
     *
     * The change has already happened on the resource. The upper layers (implementing this interface) are
     * notified to take that change into an account i.e. synchronize it.
     *
     * This operation may be called multiple times with the same change, e.g. in
     * case of failures in IDM or on the resource. The implementation must be
     * able to handle such duplicates.
     *
     * @param change change description
     */
    void notifyChange(@NotNull ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult);
}
