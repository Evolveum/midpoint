/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.authentication;

import java.util.List;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;

/**
 * Takes care for clusterwide user session management.
 */
public interface ClusterwideUserSessionManager {

    /**
     * Terminates specified sessions (on local and remote nodes).
     */
    void terminateSessions(TerminateSessionEvent terminateSessionEvent, Task task, OperationResult result);

    /**
     * Collects logged in principals (on local and remote nodes).
     */
    List<UserSessionManagementType> getLoggedInPrincipals(Task task, OperationResult result);
}
