/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.model;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceObjectShadowDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;

/**
 * 
 * @author lazyman
 * 
 */
public interface ResourceManager extends ObjectManager<ResourceDto> {

	String CLASS_NAME = ResourceManager.class.getName();
	String LIST_OBJECT_SHADOWS = CLASS_NAME + "listObjectShadows";
	String TEST_CONNECTION = CLASS_NAME + "testConnection";
	String IMPORT_FROM_RESOURCE = CLASS_NAME + "importFromResource";
	String LIST_RESOURCE_OBJECTS = CLASS_NAME + "listResourceObjects";
	String GET_IMPORT_STATUS = CLASS_NAME + "getImportStatus";
	String LIST_CONNECTORS = CLASS_NAME + "listConnectors";

	<T extends ResourceObjectShadowType> List<ResourceObjectShadowDto<T>> listObjectShadows(String oid,
			Class<T> resourceObjectShadowType);

	OperationResult testConnection(String resourceOid);

	void importFromResource(String resourceOid, QName objectClass);

	TaskStatusType getImportStatus(String resourceOid);

	Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> listResourceObjects(String resourceOid,
			QName objectClass, PagingType paging);

	Collection<ConnectorDto> listConnectors();
}
