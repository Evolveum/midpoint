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

import java.util.List;


import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public final class Utils {

    private Utils() {
    }

    public static void resolveResource(ShadowType shadow, ProvisioningService provisioning,
            OperationResult result) throws CommunicationException, SchemaException, ObjectNotFoundException, ConfigurationException, 
            SecurityViolationException {

        Validate.notNull(shadow, "Resource object shadow must not be null.");
        Validate.notNull(provisioning, "Provisioning service must not be null.");

        ResourceType resource = getResource(shadow, provisioning, result);
        shadow.setResourceRef(null);
        shadow.setResource(resource);
    }

    public static ResourceType getResource(ShadowType shadow, ProvisioningService provisioning,
            OperationResult result) throws CommunicationException, SchemaException, ObjectNotFoundException, ConfigurationException, 
            SecurityViolationException {

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
    
	public static <T extends ObjectType> void searchIterative(RepositoryService repositoryService, Class<T> type, ObjectQuery query, 
			Handler<PrismObject<T>> handler, int blockSize, OperationResult opResult) throws SchemaException {
		ObjectQuery myQuery = query.clone();
		// TODO: better handle original values in paging
		ObjectPaging myPaging = ObjectPaging.createPaging(0, blockSize);
		myQuery.setPaging(myPaging);
		boolean cont = true;
		while (cont) {
			List<PrismObject<T>> objects = repositoryService.searchObjects(type, myQuery, opResult);
			for (PrismObject<T> object: objects) {
				handler.handle(object);
			}
			cont = objects.size() == blockSize;
			myPaging.setOffset(myPaging.getOffset() + blockSize);
		}
	}
}
