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
package com.evolveum.midpoint.model.controller;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Component
@Scope
public class ModelController {

	private static final Trace LOGGER = TraceManager.getTrace(ModelController.class);
	@Autowired(required = true)
	private ProvisioningPortType provisioning;
	@Autowired(required = true)
	private RepositoryPortType repository;

	public String addObject(ObjectType object, OperationResult result) {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(result, "Result type must not be null.");

		return null;
	}

	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectListType listObjects(String objectType, PagingType paging, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object modifyObject(ObjectModificationType change, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteObject(String oid, OperationResult result) {
		// TODO Auto-generated method stub

	}

	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public UserType listAccountShadowOwner(String accountOid, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResourceObjectShadowListType listResourceObjectShadows(String resourceOid,
			String resourceObjectShadowType, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectListType listResourceObjects(String resourceOid, String objectType, PagingType paging,
			OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResourceTestResultType testResource(String resourceOid, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public EmptyType launchImportFromResource(String resourceOid, String objectClass, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	public TaskStatusType getImportStatus(String resourceOid, OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}
}
