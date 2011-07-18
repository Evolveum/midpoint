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
package com.evolveum.midpoint.model.api;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public interface ModelService {

	String addObject(ObjectType object, OperationResult result) throws ObjectAlreadyExistsException,
			ObjectNotFoundException;

	String addUser(UserType user, UserTemplateType userTemplate, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException;

	<T extends ObjectType> T getObject(String oid, PropertyReferenceListType resolve, OperationResult result,
			Class<T> clazz) throws ObjectNotFoundException;

	ObjectListType listObjects(Class<? extends ObjectType> objectType, PagingType paging,
			OperationResult result);

	ObjectListType searchObjectsInProvisioning(QueryType query, PagingType paging, OperationResult result);

	ObjectListType searchObjectsInRepository(QueryType query, PagingType paging, OperationResult result);

	void modifyObject(ObjectModificationType change, OperationResult result) throws ObjectNotFoundException;

	void modifyObjectWithExclusion(ObjectModificationType change, String accountOid, OperationResult result)
			throws ObjectNotFoundException;

	boolean deleteObject(String oid, OperationResult result) throws ObjectNotFoundException;

	PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult result);

	UserType listAccountShadowOwner(String accountOid, OperationResult result) throws ObjectNotFoundException;

	List<ResourceObjectShadowType> listResourceObjectShadows(String resourceOid,
			Class<? extends ObjectType> resourceObjectShadowType, OperationResult result)
			throws ObjectNotFoundException;

	ObjectListType listResourceObjects(String resourceOid, QName objectType, PagingType paging,
			OperationResult result);

	// This returns OperationResult instead of taking it as in/out argument.
	// This is different
	// from the other methods. The testResource method is not using
	// OperationResult to track its own
	// execution but rather to track the execution of resource tests (that in
	// fact happen in provisioning).
	OperationResult testResource(String resourceOid) throws ObjectNotFoundException;

	@Deprecated
	void launchImportFromResource(String resourceOid, QName objectClass, OperationResult result)
			throws ObjectNotFoundException;

	// Note: The result is in the task. No need to pass it explicitly
	void importFromResource(String resourceOid, QName objectClass, Task task) throws ObjectNotFoundException;

	@Deprecated
	TaskStatusType getImportStatus(String resourceOid, OperationResult result) throws ObjectNotFoundException;

	@SuppressWarnings("unchecked")
	<T extends ObjectType> T getObject(String oid, PropertyReferenceListType resolve, OperationResult result,
			Class<T> clazz, boolean fromProvisioning) throws ObjectNotFoundException;
}
