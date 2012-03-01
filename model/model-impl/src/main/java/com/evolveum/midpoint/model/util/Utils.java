/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.util;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public final class Utils {

    private Utils() {
    }

    public static void resolveResource(ResourceObjectShadowType shadow, ProvisioningService provisioning,
            OperationResult result) throws CommunicationException, SchemaException, ObjectNotFoundException, ConfigurationException {

        Validate.notNull(shadow, "Resource object shadow must not be null.");
        Validate.notNull(provisioning, "Provisioning service must not be null.");

        ResourceType resource = getResource(shadow, provisioning, result);
        shadow.setResourceRef(null);
        shadow.setResource(resource);
    }

    public static ResourceType getResource(ResourceObjectShadowType shadow, ProvisioningService provisioning,
            OperationResult result) throws CommunicationException, SchemaException, ObjectNotFoundException, ConfigurationException {

        if (shadow.getResource() != null) {
            return shadow.getResource();
        }

        if (shadow.getResourceRef() == null) {
            throw new IllegalArgumentException("Couldn't resolve resource. Resource object shadow doesn't" +
                    " contain resource nor resource ref.");
        }

        ObjectReferenceType resourceRef = shadow.getResourceRef();
        return provisioning.getObject(ResourceType.class, resourceRef.getOid(), null, result).asObjectable();
    }
}
