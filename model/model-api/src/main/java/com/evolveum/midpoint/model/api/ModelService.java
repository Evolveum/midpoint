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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * <p>
 * IDM Model Interface
 * </p>
 * <p>
 * IDM Model Interface provides access unified to the identity objects stored in
 * the repository and on the resources. It abstracts away the details about
 * where and how are the data stored, it hides all the low-level system
 * components.
 * </p>
 * <p>
 * Implementation of this interface are expected to enforce a consistency of
 * access control decisions and model, e.g. to enforce Role-Based Access Control
 * (RBAC). RBAC is only one of many possibly models and this interface may have
 * many implementations.
 * </p>
 * <p>
 * Implementations of this interface may automatically derive properties and
 * attributes for objects. E.g. RBAC models may automatically derive resource
 * accounts attributes based on user role membership.
 * </p>
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
public interface ModelService {

	// Constants for OperationResult
	String CLASS_NAME_WITH_DOT = ModelService.class.getName() + ".";
	String GET_OBJECT = CLASS_NAME_WITH_DOT + "getObject";
	String COUNT_OBJECTS = CLASS_NAME_WITH_DOT + "countObjects";
	String EXECUTE_CHANGES = CLASS_NAME_WITH_DOT + "executeChanges";
	String GET_PROPERTY_AVAILABLE_VALUES = CLASS_NAME_WITH_DOT + "getPropertyAvailableValues";
	String LIST_OBJECTS = CLASS_NAME_WITH_DOT + "listObjects";
	String LIST_ACCOUNT_SHADOW_OWNER = CLASS_NAME_WITH_DOT + "listAccountShadowOwner";
	String LIST_RESOURCE_OBJECT_SHADOWS = CLASS_NAME_WITH_DOT + "listResourceObjectShadows";
	String LIST_RESOURCE_OBJECTS = CLASS_NAME_WITH_DOT + "listResourceObjects";
	String TEST_RESOURCE = CLASS_NAME_WITH_DOT + "testResource";
	String IMPORT_ACCOUNTS_FROM_RESOURCE = CLASS_NAME_WITH_DOT + "importAccountsFromResource";
	String IMPORT_OBJECTS_FROM_FILE = CLASS_NAME_WITH_DOT + "importObjectsFromFile";
	String IMPORT_OBJECTS_FROM_STREAM = CLASS_NAME_WITH_DOT + "importObjectsFromStream";
	String POST_INIT = CLASS_NAME_WITH_DOT + "postInit";
	String DISCOVER_CONNECTORS = CLASS_NAME_WITH_DOT + "discoverConnectors";

	/**
	 * <p>
	 * Returns object for provided OID.
	 * </p>
	 * <p>
	 * Must fail if object with the OID does not exists.
	 * </p>
	 * 
	 * @param <T>
	 * @param oid
	 *            OID of the object to get
	 * @param resolve
	 *            list of properties to resolve in the fetched object
	 * @param type
	 *            (class) of an object to get
	 * @param result
	 *            parent OperationResult (in/out)
	 * @return Retrieved object
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws SchemaException 
	 * 				the object is not schema compliant
	 * @throws IllegalArgumentException
	 *             missing required parameter, wrong OID format, etc.
	 * @throws ClassCastException
	 *             OID represents object of a type incompatible with requested
	 *             type
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	<T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options,
			Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException;

	/**
	 * <p>
	 * Execute the provided object deltas.
	 * </p>
	 * <p>
	 * The operation executes the provided object deltas. All deltas must relate to analogous objects (e.g. user
	 * and linked accounts). The implementation may throw an error if the objects are not analogous. The implementation
	 * also implicitly links the objects (mark them to be analogous) if such a link is part of the data model.
	 * E.g. the implementation links all accounts to the user if they are passed in a single delta collection.
	 * This is especially useful if the account deltas are ADD deltas without OID and therefore cannot be linked
	 * explicitly. 
	 * </p>
	 * <p>
	 * There must be no more than one delta for each object.
	 * The order of execution is not defined and the implementation is free to determine the correct or most suitable ordering.
	 * </p>
	 * <p>
	 * The OID provided in ADD deltas may be empty. In that case the OID
	 * will be assigned by the implementation and the OIDs will be set in the
	 * deltas after the operation is completed.
	 * </p>
	 * <p>
	 * Execution of ADD deltas should fail if such object already exists (if object with
	 * the provided OID already exists). Execution of MODIFY and DELETE deltas should fail if
	 * such objects do not exist.
	 * </p>
	 * <p>
	 * The operation may fail if provided OIDs are in an unusable format for the
	 * storage. Generating own OIDs and providing them to this method is not
	 * recommended for normal operation.
	 * </p>
	 * <p>
	 * There are no explicit atomicity guarantees for the operations. Some of the operations may pass, some may fail
	 * or even fail partially. The consistency of the data and state are not based on operation atomicity but rather
	 * a data model that can "repair" inconsistencies.
	 * </p>
	 * <p>
	 * The operation may fail if any of the objects to be created or modified does not conform to
	 * the underlying schema of the storage system or the schema enforced by the implementation.
	 * </p>
	 * 
	 * @param deltas
	 *            Collection of object deltas to execute
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return OID assigned to the created object
	 * @throws ObjectAlreadyExistsException
	 *             object with specified identifiers already exists, cannot add
	 * @throws ObjectNotFoundException
	 *             object required to complete the operation was not found (e.g.
	 *             appropriate connector or resource definition)
	 * @throws SchemaException
	 *             error dealing with resource schema, e.g. created object does
	 *             not conform to schema
	 * @throws ExpressionEvaluationException 
	 * 				evaluation of expression associated with the object has failed
	 * @throws CommunicationException 
	 * @throws ConfigurationException 
	 * @throws PolicyViolationException
	 * 				Policy violation was detected during processing of the object
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	void executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult parentResult) 
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;
	
	/**
	 * <p>
	 * Returns the User object representing owner of specified account (account
	 * shadow).
	 * </p>
	 * <p>
	 * May return null if there is no owner specified for the account.
	 * </p>
	 * <p>
	 * Implements the backward "owns" association between account shadow and
	 * user. Forward association is implemented by property "account" of user
	 * object.
	 * </p>
	 * 
	 * @param accountOid
	 *            OID of the account to look for an owner
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return owner of the account or null
	 * @throws ObjectNotFoundException
	 *             specified account was not found
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	PrismObject<UserType> findShadowOwner(String accountOid, Task task, OperationResult parentResult)
			throws ObjectNotFoundException;

	/**
	 * <p>
	 * Search for resource object shadows of a specified type that belong to the
	 * specified resource.
	 * </p>
	 * <p>
	 * Returns a list of such object shadows or empty list if nothing was found.
	 * </p>
	 * <p>
	 * Returned shadows are "attached", that means they have OID and at they are
	 * stored in the repository.
	 * </p>
	 * <p>
	 * Implements the backward "has" association between resource and resource
	 * object shadows. Forward association is implemented by property "resource"
	 * of resource object shadow.
	 * </p>
	 * 
	 * TODO: add objectClass to be really usable ?
	 * 
	 * @param <T>
	 *            specific shadow type (class) to return
	 * @param resourceOid
	 *            OID of the resource that the shadows reside
	 * @param resourceObjectShadowType
	 *            specific shadow type (class) to return
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return list of found shadows
	 * @throws ObjectNotFoundException
	 */
	<T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException;

	/**
	 * <p>
	 * Returns all resource objects of specified type that are currently
	 * available to the system.
	 * </p>
	 * <p>
	 * Returns empty list if object type is correct but there are no objects of
	 * that type. The operation should fail if object type is wrong (e.g.
	 * specified type is not part of resource schema).
	 * </p>
	 * <p>
	 * This method does NOT use any repository shadow objects for reference or
	 * any other business objects in the local repository. It goes directly to
	 * the resource. The returned objects (indirectly) comply with the resource
	 * schema, but it is returned re-formated in a form of detached shadow
	 * object. Although the form is the same as shadow object, this is NOT
	 * really a shadow object because it is not stored in the repository (it is
	 * detached). It does NOT have OID.
	 * </p>
	 * <p>
	 * The objects are identified by whatever identification
	 * properties/attributes are defined by the resource schema.
	 * </p>
	 * <p>
	 * The purpose of this operation is diagnostics. It works directly with the
	 * resource without the potential problems of underlying implementation.
	 * E.g. it may be used to test resource connectivity or correctness of
	 * resource setup. It may also be used to reach object types that are not
	 * directly supported as "shadows" by the implementation. Therefore this
	 * method is not required to implement any form of caching, queuing,
	 * reference resolution or any other "smart" algorithm.
	 * </p>
	 * <p>
	 * 
	 * @param resourceOid
	 *            OID of the resource to fetch objects from
	 * @param objectClass
	 *            Object class of the objects to fetch
	 * @param paging
	 *            paging specification to limit operation result (optional)
	 * @param result
	 *            parent OperationResult (in/out)
	 * @return resource objects in a form of "detached shadows"
	 * @throws ObjectNotFoundException
	 *             specified resource object does not exist
	 * @throws SchemaException
	 *             error handling resource schema
	 * @throws CommunicationException
	 *             error communicating with the resource
	 */
	List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid, QName objectClass, ObjectPaging paging,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, 
			ConfigurationException, SecurityViolationException;

	/**
	 * <p>
	 * Returns all objects of specified type that are available to the
	 * implementation.
	 * </p>
	 * <p>
	 * This can be considered as a simplified search operation.
	 * </p>
	 * <p>
	 * Returns empty list if object type is correct but there are no objects of
	 * that type.
	 * </p>
	 * <p>
	 * Should fail if object type is wrong.
	 * </p>
	 * 
	 * @param objectType
	 * @param paging
	 *            paging specification to limit operation result (optional)
	 * @param result
	 *            parent OperationResult (in/out)
	 * @return all objects of specified type (subject to paging)
	 * 
	 * @throws IllegalArgumentException
	 *             wrong object type
	 */

	/**
	 * <p>
	 * Search for objects.
	 * </p>
	 * <p>
	 * Searches through all object types. Returns a list of objects that match
	 * search criteria.
	 * </p>
	 * <p>
	 * Returns empty list if object type is correct but there are no objects of
	 * that type.
	 * </p>
	 * <p>
	 * Should fail if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * </p>
	 * 
	 * @param query
	 *            search query
	 * @param paging
	 *            paging specification to limit operation result (optional)
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return all objects of specified type that match search criteria (subject
	 *         to paging)
	 * 
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource
	 *             definition)
	 * @throws CommunicationException 
	 * 				error communicating with the resource
	 * @throws ConfigurationException 
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException;
	

	<T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult parentResult) 
            		throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException;

	/**
	 * <p>
	 * Test the resource connection and basic resource connector functionality.
	 * </p>
	 * <p>
	 * This operation will NOT throw exception in case the resource connection
	 * fails. It such case it will indicate the failure in the return message,
	 * but the operation itself succeeds. The operations fails only if the
	 * provided arguments are wrong, in case of system error, system
	 * misconfiguration, etc.
	 * </p>
	 * <p>
	 * This returns OperationResult instead of taking it as in/out argument.
	 * This is different from the other methods. The testResource method is not
	 * using OperationResult to track its own execution but rather to track the
	 * execution of resource tests (that in fact happen in provisioning).
	 * </p>
	 * 
	 * @param resourceOid
	 *            OID of resource to test
	 * @return results of executed tests
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format
	 */
	OperationResult testResource(String resourceOid, Task task) throws ObjectNotFoundException;

	/**
	 * <p>
	 * Import accounts from resource.
	 * </p>
	 * <p>
	 * Invocation of this method may be switched to background.
	 * </p>
	 * TODO: OperationResult
	 * @throws SchemaException 
	 */
	void importFromResource(String resourceOid, QName objectClass, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, SecurityViolationException;

	/**
	 * Import objects from file.
	 * 
	 * Invocation of this method may be switched to background.
	 * 
	 * @param input
	 * @param task
	 */
	void importObjectsFromFile(File input, ImportOptionsType options, Task task, OperationResult parentResult);

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
	void importObjectsFromStream(InputStream input, ImportOptionsType options, Task task, OperationResult parentResult);

	/**
	 * Discovers local or remote connectors.
	 * 
	 * The operation will try to search for new connectors. It works either on
	 * local host (hostType is null) or on a remote host (hostType is not null).
	 * All discovered connectors are stored in the repository.
	 * 
	 * It returns connectors that were discovered: those that were not in the
	 * repository before invocation of this operation.
	 * 
	 * @param hostType
	 *            definition of a connector host or null
	 * @param parentResult
	 *            parentResult parent OperationResult (in/out)
	 * @return discovered connectors
	 * @throws CommunicationException error communicating with the connector host
	 */
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult) throws CommunicationException;

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
}
