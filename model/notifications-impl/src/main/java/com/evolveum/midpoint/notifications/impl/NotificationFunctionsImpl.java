/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationFunctions;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Various useful functions. TODO decide what to do with this class.
 */
@Component
public class NotificationFunctionsImpl implements NotificationFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationFunctionsImpl.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired protected TextFormatter textFormatter;

    // beware, may return null if there's any problem getting sysconfig (e.g. during initial import)
    public static SystemConfigurationType getSystemConfiguration(RepositoryService repositoryService, OperationResult result) {
        return getSystemConfiguration(repositoryService, true, result);
    }

    public static SystemConfigurationType getSystemConfiguration(RepositoryService repositoryService, boolean errorIfNotFound, OperationResult result) {
        try {
            return repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                    null, result).asObjectable();
        } catch (ObjectNotFoundException|SchemaException e) {
            if (errorIfNotFound) {
                LoggingUtils.logException(LOGGER,
                        "Notification(s) couldn't be processed, because the system configuration couldn't be retrieved", e);
            } else {
                LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                        "Notification(s) couldn't be processed, because the system configuration couldn't be retrieved", e);
            }
            return null;
        }
    }

    public SystemConfigurationType getSystemConfiguration(OperationResult result) {
        return getSystemConfiguration(cacheRepositoryService, result);
    }

    public static String getResourceNameFromRepo(RepositoryService repositoryService, String oid, OperationResult result) {
        try {
            PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, oid, null, result);
            return PolyString.getOrig(resource.asObjectable().getName());
        } catch (ObjectNotFoundException | SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get resource", e);
            return null;
        }
    }

    public String getObjectName(String oid, OperationResult result) {
        try {
            PrismObject<? extends ObjectType> object = cacheRepositoryService.getObject(ObjectType.class, oid, null, result);
            return PolyString.getOrig(object.asObjectable().getName());
        } catch (CommonException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get resource", e);
            return null;
        }
    }

    public ObjectType getObjectType(SimpleObjectRef simpleObjectRef, boolean allowNotFound, OperationResult result) {
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

    public ObjectType getObjectType(ObjectReferenceType ref, boolean allowNotFound, OperationResult result) {
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
