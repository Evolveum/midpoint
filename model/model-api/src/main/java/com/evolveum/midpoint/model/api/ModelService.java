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
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
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
	String GET_PROPERTY_AVAILABLE_VALUES = CLASS_NAME + "getPropertyAvailableValues";
	String LIST_OBJECTS = CLASS_NAME + "listObjects";
	String MODIFY_OBJECT = CLASS_NAME + "modifyObject";
	String DELETE_OBJECT = CLASS_NAME + "deleteObject";
	String LIST_ACCOUNT_SHADOW_OWNER = CLASS_NAME + "listAccountShadowOwner";
	String LIST_RESOURCE_OBJECT_SHADOWS = CLASS_NAME + "listResourceObjectShadows";
	String LIST_RESOURCE_OBJECTS = CLASS_NAME + "listResourceObjects";
	String TEST_RESOURCE = CLASS_NAME + "testResource";
	String IMPORT_ACCOUNTS_FROM_RESOURCE = CLASS_NAME + "importAccountsFromResource";
	String IMPORT_OBJECTS_FROM_FILE = CLASS_NAME + "importObjectsFromFile";
	String IMPORT_OBJECTS_FROM_STREAM = CLASS_NAME + "importObjectsFromStream";
	String POST_INIT = CLASS_NAME + "postInit";
	String SEARCH_OBJECTS_IN_REPOSITORY = CLASS_NAME + "searchObjectsInRepository";

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

	ObjectListType listResourceObjects(String resourceOid, QName objectClass, PagingType paging,
			OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException;

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
	void importObjectsFromStream(InputStream input, Task task, Boolean overwrite, OperationResult parentResult);
	
	/**
	 * Discovers local or remote connectors.
	 * 
	 * The operation will try to search for new connectors. It works either on local host (hostType is null)
	 * or on a remote host (hostType is not null). All discovered connectors are stored in the repository.
	 * 
	 * It returns connectors that were discovered: those that were not in the repository before invocation
	 * of this operation. 
	 * 
	 * @param hostType definition of a connector host or null
	 * @param parentResult parentResult parent OperationResult (in/out)
	 * @return discovered connectors
	 */
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult);

	/**
	 * Finish initialization of the model and lower system components
	 * (provisioning, repository, etc).
	 * 
	 * The implementation may execute resource-intensive tasks in this method.
	 * All the dependencies should be already constructed, properly wired and
	 * initialized. Also logging and other infrastructure should be already set
	 * up.
	 */
	void postInit(OperationResult parentResult);

	ObjectListType searchObjectsInRepository(QueryType query, PagingType paging, OperationResult result);
}
