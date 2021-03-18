/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.namespace.QName;

/**
 * Records information about actions on repository objects.
 *
 * TODO better description
 */
public interface ObjectActionsCollector {

    void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid, ChangeType changeType, String channel, Throwable exception);

    void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, Throwable exception);

    <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass, String defaultOid, ChangeType changeType, String channel, Throwable exception);

    void markObjectActionExecutedBoundary();

    void resetActionsExecutedInformation(ActionsExecutedInformationType value);
}
