/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface ModelAuditRecorder {

    void auditLoginSuccess(FocusType focus, ConnectionEnvironment connEnv);

    void auditLoginFailure(String username, FocusType focus, ConnectionEnvironment connEnv, String message);

    void auditLogout(ConnectionEnvironment connEnv, Task task);
}
