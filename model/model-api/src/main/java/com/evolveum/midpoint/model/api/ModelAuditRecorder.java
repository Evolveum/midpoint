/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface ModelAuditRecorder {

    void auditLoginSuccess(ObjectType object, ConnectionEnvironment connEnv);

    void auditLoginFailure(String username, FocusType focus, ConnectionEnvironment connEnv, String message);

    void auditLogout(ConnectionEnvironment connEnv, Task task, OperationResult result);
}
