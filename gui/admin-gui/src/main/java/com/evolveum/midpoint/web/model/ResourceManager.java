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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceObjectShadowDto;
import com.evolveum.midpoint.web.model.impl.ObjectManagerImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;

/**
 * 
 * @author semancik
 * @author lazyman
 * 
 */
public abstract class ResourceManager extends ObjectManagerImpl<ResourceDto> {

	private static final long serialVersionUID = -4183063295869675058L;

	public abstract <T extends ResourceObjectShadowType> List<ResourceObjectShadowDto<T>> listObjectShadows(
			String oid, Class<T> resourceObjectShadowType);

	public abstract OperationResult testConnection(String resourceOid);

	public abstract void launchImportFromResource(String resourceOid, QName objectClass);

	public abstract TaskStatusType getImportStatus(String resourceOid);

	public abstract Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> listResourceObjects(
			String resourceOid, QName objectClass, PagingType paging);
}
