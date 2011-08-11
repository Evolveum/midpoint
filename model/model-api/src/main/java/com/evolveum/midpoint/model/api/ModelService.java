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

import java.io.File;
import java.io.InputStream;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public interface ModelService {

	String CLASS_NAME = ModelService.class.getName() + ".";
	String ADD_OBJECT = CLASS_NAME + "addObject";
	String ADD_USER = CLASS_NAME + "addUser";
	String GET_OBJECT = CLASS_NAME + "getObject";

	<T extends ObjectType> T getObject(String oid, PropertyReferenceListType resolve, Class<T> clazz,
			OperationResult result) throws ObjectNotFoundException;

	PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult result);

	ObjectListType listObjects(Class<? extends ObjectType> objectType, PagingType paging,
			OperationResult result);

	String addObject(ObjectType object, OperationResult result) throws ObjectAlreadyExistsException,
			ObjectNotFoundException;

	String addUser(UserType user, UserTemplateType userTemplate, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException;

	void modifyObject(ObjectModificationType change, OperationResult result) throws ObjectNotFoundException;

	boolean deleteObject(String oid, OperationResult result) throws ObjectNotFoundException;

	UserType listAccountShadowOwner(String accountOid, OperationResult result) throws ObjectNotFoundException;

	<T extends ResourceObjectShadowType> List<T> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult result) throws ObjectNotFoundException;

	ObjectListType listResourceObjects(String resourceOid, QName objectType, PagingType paging,
			OperationResult result);

	/**
	 * This returns OperationResult instead of taking it as in/out argument.
	 * This is different from the other methods. The testResource method is not
	 * using OperationResult to track its own execution but rather to track the
	 * execution of resource tests (that in fact happen in provisioning).
	 * 
	 * @param resourceOid
	 * @return
	 * @throws ObjectNotFoundException
	 */
	OperationResult testResource(String resourceOid) throws ObjectNotFoundException;

	// Note: The result is in the task. No need to pass it explicitly
	void importAccountsFromResource(String resourceOid, QName objectClass, Task task)
			throws ObjectNotFoundException;

	/**
	 * Import objects from file.
	 * 
	 * Invocation of this method may be switched to background.
	 * 
	 * The results will be provided in the task.
	 * 
	 * @param input
	 * @param task
	 */
	void importObjectsFromFile(File input, Task task, OperationResult parentResult);

	/**
	 * Import objects from stream.
	 * 
	 * Invocation of this method will happen in foreground, as the stream cannot
	 * be serialized.
	 * 
	 * The results will be provided in the task.
	 * 
	 * @param input
	 * @param task
	 */
	void importObjectsFromStream(InputStream input, Task task, OperationResult parentResult);

	/**
	 * Finish initialization of the model and lower system components
	 * (provisioning, repository, etc).
	 * 
	 * The implementation may execute resource-intensive tasks in this method.
	 * All the dependencies should be already constructed, properly wired and
	 * initialized. Also logging and other infrastructure should be already set
	 * up.
	 */
	public void postInit(OperationResult parentResult);

	ObjectListType searchObjectsInRepository(QueryType query, PagingType paging, OperationResult result);
}
