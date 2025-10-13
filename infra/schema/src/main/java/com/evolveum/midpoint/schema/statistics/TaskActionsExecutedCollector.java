/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface TaskActionsExecutedCollector {

    void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid, ChangeType changeType, String channel, Throwable exception);

    <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass, String defaultOid, ChangeType changeType, String channel, Throwable exception);

    /**
     * Logs under default channel known to the current task.
     */
    void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, Throwable exception);

    void startCollectingActionsExecuted(ActionsExecutedCollector collector);

    void stopCollectingActionsExecuted();
}
