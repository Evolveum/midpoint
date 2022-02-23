/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.transport.impl.TransportUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Various utility functions, also provided as `notificationFunctions` for notification expressions.
 */
@Component
public class NotificationFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationFunctions.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired protected TextFormatter textFormatter;

    public SystemConfigurationType getSystemConfiguration(OperationResult result) {
        return TransportUtil.getSystemConfiguration(cacheRepositoryService, result);
    }

    public String getObjectName(String oid, OperationResult result) {
        try {
            PrismObject<? extends ObjectType> object = cacheRepositoryService.getObject(ObjectType.class, oid, null, result);
            return PolyString.getOrig(object.asObjectable().getName());
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get resource", e);
            return null;
        }
    }

    public ObjectType getObject(SimpleObjectRef simpleObjectRef, boolean allowNotFound, OperationResult result) {
        if (simpleObjectRef == null) {
            return null;
        }
        if (simpleObjectRef.getObjectType() != null) {
            return simpleObjectRef.getObjectType();
        }
        if (simpleObjectRef.getOid() == null) {
            return null;
        }

        ObjectType objectType = getObjectFromRepo(simpleObjectRef.getOid(), allowNotFound, result);
        simpleObjectRef.setObjectType(objectType);
        return objectType;
    }

    public ObjectType getObject(ObjectReferenceType ref, boolean allowNotFound, OperationResult result) {
        if (ref == null) {
            return null;
        }
        if (ref.asReferenceValue().getObject() != null) {
            return (ObjectType) ref.asReferenceValue().getObject().asObjectable();
        }
        if (ref.getOid() == null) {
            return null;
        }

        return getObjectFromRepo(ref.getOid(), allowNotFound, result);
    }

    @Nullable
    private ObjectType getObjectFromRepo(String oid, boolean allowNotFound, OperationResult result) {
        ObjectType objectType;
        try {
            objectType = cacheRepositoryService.getObject(ObjectType.class, oid, null, result).asObjectable();
        } catch (ObjectNotFoundException e) {   // todo correct error handling
            if (allowNotFound) {
                return null;
            } else {
                throw new SystemException(e);
            }
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
        return objectType;
    }
}
