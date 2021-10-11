/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author Radovan Semancik
 *
 */
public interface ResourceObjectChangeListener extends ProvisioningListener {

    /**
     * Submits notification about a specific change that happened on the
     * resource.
     *
     * This describes the change that has already happened on the resource. The upper layers are
     * notified to take that change into an account (synchronize it).
     *
     * The call should return without a major delay. It means that the
     * implementation can do calls to repository, but it should not
     * (synchronously) initiate a long-running process or provisioning request.
     *
     * This operation may be called multiple times with the same change, e.g. in
     * case of failures in IDM or on the resource. The implementation must be
     * able to handle such duplicates.
     *
     * @param change
     *            change description
     */
     <F extends FocusType> void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult);

}
