/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.casemgmt.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * Dispatcher for case creation events. It is put on quite low level to be accessible from built-in manual connector.
 *
 * We might consider moving this to another module, e.g. to schema, if needed.
 */
public interface CaseEventDispatcher {

    void registerCaseCreationEventListener(CaseCreationListener listener);

    void unregisterCaseCreationEventListener(CaseCreationListener listener);

    void dispatchCaseCreationEvent(CaseType aCase, Task task, OperationResult result);
}
