/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Listens for notifications about shadow death events i.e. when shadow is converted from live to dead and then
 * eventually to deleted.
 */
public interface ShadowDeathListener extends ProvisioningListener {

    /**
     * The caller is notified about particular shadow death event. It should react quickly.
     * The usual reaction is just an update of the state in the repository.
     */
    void notify(ShadowDeathEvent event, Task task, OperationResult result);

}
