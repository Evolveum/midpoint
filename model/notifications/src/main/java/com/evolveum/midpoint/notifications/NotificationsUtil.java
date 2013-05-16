/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.notifications;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

/**
 * @author mederly
 */
public class NotificationsUtil {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationsUtil.class);

    public static PrismObject<SystemConfigurationType> getSystemConfiguration(RepositoryService repositoryService, OperationResult result) {
        PrismObject<SystemConfigurationType> systemConfiguration;
        try {
            systemConfiguration = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                    result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get system configuration", e);
            throw new SystemException("Couldn't get system configuration", e);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get system configuration", e);
            throw new SystemException("Couldn't get system configuration", e);
        }
        if (systemConfiguration == null) {
            throw new SystemException("Couldn't get system configuration");
        }
        return systemConfiguration;
    }

    public static String getResourceNameFromRepo(RepositoryService repositoryService, String oid, OperationResult result) {
        try {
            PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, oid, result);
            return PolyString.getOrig(resource.asObjectable().getName());
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get resource", e);
            return null;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get resource", e);
            return null;
        }
    }

}
