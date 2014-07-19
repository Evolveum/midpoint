/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class NotificationsUtil {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationsUtil.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    // beware, may return null if there's any problem getting sysconfig (e.g. during initial import)
    public static SystemConfigurationType getSystemConfiguration(RepositoryService repositoryService, OperationResult result) {
        try {
            return repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
            		null, result).asObjectable();
        } catch (ObjectNotFoundException|SchemaException e) {
            LoggingUtils.logException(LOGGER, "Notification(s) couldn't be processed, because the system configuration couldn't be retrieved", e);
            return null;
        }
    }

    public static String getResourceNameFromRepo(RepositoryService repositoryService, String oid, OperationResult result) {
        try {
            PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, oid, null, result);
            return PolyString.getOrig(resource.asObjectable().getName());
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get resource", e);
            return null;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get resource", e);
            return null;
        }
    }

    public ObjectType getObjectType(SimpleObjectRef simpleObjectRef, OperationResult result) {
        if (simpleObjectRef == null) {
            return null;
        }
        if (simpleObjectRef.getObjectType() != null) {
            return simpleObjectRef.getObjectType();
        }
        if (simpleObjectRef.getOid() == null) {
            return null;
        }

        ObjectType objectType;
        try {
            objectType = cacheRepositoryService.getObject(ObjectType.class, simpleObjectRef.getOid(), null, result).asObjectable();
        } catch (ObjectNotFoundException e) {   // todo correct error handling
            throw new SystemException(e);
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
        simpleObjectRef.setObjectType(objectType);
        return objectType;
    }

    public static boolean isAmongHiddenPaths(ItemPath path, List<ItemPath> hiddenPaths) {
        if (hiddenPaths == null) {
            return false;
        }
        for (ItemPath hiddenPath : hiddenPaths) {
            if (hiddenPath.isSubPathOrEquivalent(path)) {
                return true;
            }
        }
        return false;
    }

    public String getShadowName(PrismObject<? extends ShadowType> shadow) {
        if (shadow == null) {
            return null;
        } else if (shadow.asObjectable().getName() != null) {
            return shadow.asObjectable().getName().getOrig();
        } else {
            Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
            LOGGER.trace("secondary identifiers: {}", secondaryIdentifiers);
            // first phase = looking for "name" identifier
            for (ResourceAttribute ra : secondaryIdentifiers) {
                if (ra.getElementName() != null && ra.getElementName().getLocalPart().contains("name")) {
                    LOGGER.trace("Considering {} as a name", ra);
                    return String.valueOf(ra.getAnyRealValue());
                }
            }
            // second phase = returning any value ;)
            if (!secondaryIdentifiers.isEmpty()) {
                return String.valueOf(secondaryIdentifiers.iterator().next().getAnyRealValue());
            } else {
                return null;
            }
        }
    }
}
