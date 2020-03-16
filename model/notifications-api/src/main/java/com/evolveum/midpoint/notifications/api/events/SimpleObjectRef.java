/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *
 */
public interface SimpleObjectRef extends DebugDumpable {
    String getOid();
    void setOid(String oid);
    ObjectType getObjectType();
    void setObjectType(ObjectType objectType);
    ObjectType resolveObjectType(OperationResult result, boolean allowNotFound);
    ObjectType resolveObjectType();
}
