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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * <p>Provisioning Service Interface.</p>
 * <p>
 * Status: public
 * Stability: DRAFT, some changes are likely
 * @version 0.3
 * @author Radovan Semancik
 * </p>
 * <p>
 * This service retrieves information about resource objects and resources 
 * and handles changes to resource objects. Implementations of this interface
 * will apply the changes to accounts, groups and other similar objects to the
 * target resources. It also provides information about connectors and similar
 * configuration of access to the resources.
 * </p>
 * <p>
 * Supported object types:
 *   <ul>
 *      <li>Resource</li>
 *      <li>ResourceObjectShadow and all sub-types</li>
 *      <li>Connector</li>
 *   </ul>
 * Supported extra data types:
 *   <ul>
 *   	<li>Resource Objects (Resource Schema)</li>
 *   </ul>
 * </p>
 * <p>
 * TODO: better documentation
 * </p>
 */
public interface ProvisioningService {
	
	/**
	 * Prefix for all the "standardized" test resource tests.
	 */
	public static final String TEST_CONNECTION_OPERATION = ProvisioningService.class.getName()+".testResource";
	/**
	 * Check whether the configuration is valid e.g. well-formed XML, valid with regard to schema, etc.
	 */
	public static final String TEST_CONNECTION_CONNECTOR_VALIDATION_OPERATION = TEST_CONNECTION_OPERATION+".configurationValidation";
	/**
	 * Check whether the connector can be initialized.
	 * E.g. connector classes can be loaded, it can process configuration, etc.
	 */
	public static final String TEST_CONNECTION_CONNECTOR_INIT_OPERATION = TEST_CONNECTION_OPERATION+".connectorInitialization";
	/**
	 * Check whether the connector can be initialized.
	 * E.g. connector classes can be loaded, it can process configuration, etc.
	 */
	public static final String TEST_CONNECTION_CONNECTOR_CONNECTION_OPERATION = TEST_CONNECTION_OPERATION+".connectorConnection";
	/**
	 * Check whether the connector can fetch and process resource schema.
	 */
	public static final String TEST_CONNECTION_CONNECTOR_SCHEMA_OPERATION = TEST_CONNECTION_OPERATION+".connectorSchema";
	/**
	 * Check whether the connector can be used to fetch some mandatory objects (e.g. fetch a "root" user).
	 */
	public static final String TEST_CONNECTION_CONNECTOR_SANITY_OPERATION = TEST_CONNECTION_OPERATION+".connectorSanity";
	
	/**
	 * Returns object for provided OID.
	 * 
	 * Must fail if object with the OID does not exists.
	 * 
	 * Resource Object Shadows: The resource object shadow attributes may be
	 * retrieved from the local database, directly form the resource or a
	 * combination of both. The retrieval may fail due to resource failure,
	 * network failure or similar external cases. The retrieval may also take
	 * relatively long time (e.g. until it times out).
	 * 
	 * @param type the type (class) of object to get
	 * @param oid
	 *            OID of the object to get
	 * @param resolve
	 *            list of properties to resolve in the fetched object
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return Object fetched from repository and/or resource
	 * 
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws CommunicationException
	 *             error communicating with the resource
	 * @throws SchemaException
	 *             error dealing with resource schema
	 * @throws ConfigurationException 
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, PropertyReferenceListType resolve, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException;
	
	/**
	 * Add new object.
	 * 
	 * The OID provided in the input message may be empty. In that case the OID
	 * will be assigned by the implementation of this method and it will be
	 * provided as return value.
	 * 
	 * This operation should fail if such object already exists (if object with
	 * the provided OID already exists).
	 * 
	 * The operation may fail if provided OID is in an unusable format for the
	 * storage. Generating own OIDs and providing them to this method is not
	 * recommended for normal operation.
	 * 
	 * Should be atomic. Should not allow creation of two objects with the same
	 * OID (even if created in parallel).
	 * 
	 * The operation may fail if the object to be created does not conform to
	 * the underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * 
	 * @param object
	 *            object to create
	 * @param scripts
	 *            scripts to execute before/after the operation
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return OID assigned to the created object
	 * 
	 * @throws ObjectAlreadyExistsException
	 *             object with specified identifiers already exists, cannot add
	 * @throws SchemaException
	 *             error dealing with resource schema, e.g. schema violation
	 * @throws CommunicationException
	 *             error communicating with the resource
	 * @throws ObjectNotFoundException appropriate connector object was not found
	 * @throws ConfigurationException 
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public <T extends ObjectType> String addObject(PrismObject<T> object, ScriptsType scripts, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException;

	/**
	 * Collect external changes on a resource and call the business logic with
	 * the accumulated change data.
	 * 
	 * This method will be invoked by scheduler/sync thread.
	 * 
	 * TODO: Better description
	 * 
	 * @param resourceOid
	 *            OID of the resource for which to attempt synchronization
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return the number of processed changes
	 * @throws ObjectNotFoundException
	 *             specified resource definition (OID) does not exist
	 * @throws CommunicationException
	 *             error communicating with the resource
	 * @throws SchemaException
	 *             error dealing with resource schema
	 * @throws ConfigurationException 
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public int synchronize(String resourceOid, Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException;

	/**
	 * Returns all objects of specified type that are available to the
	 * implementation.
	 * 
	 * This can be considered as a simplified search operation.
	 * 
	 * Returns empty list if object type is correct but there are no objects of
	 * that type.
	 * 
	 * Should fail if object type is wrong.
	 * 
	 * @param objectType
	 * @param paging
	 *            paging specification to limit operation result (optional)
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return all objects of specified type (subject to paging)
	 * 
	 * @throws IllegalArgumentException
	 *             wrong object type
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	@Deprecated
	public <T extends ObjectType> List<PrismObject<T>> listObjects(Class<T> objectType, PagingType paging,
			OperationResult parentResult);

	/**
	 * Search for objects. Searches through all object types. Returns a list of
	 * objects that match search criteria.
	 * 
	 * Returns empty list if object type is correct but there are no objects of
	 * that type.
	 * 
	 * Should fail if object type is wrong. Should fail if unknown property is
	 * specified in the query.
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
	 * @throws IllegalArgumentException
	 *             wrong object type
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ConfigurationException 
	 */
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException;
	
	
	public <T extends ObjectType> int countObjects(Class<T> type, QueryType query, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException;
	
	/**
	 * Search for objects iteratively. Searches through all object types. Calls a
	 * specified handler for each object found.
	 * 
	 * If nothing is found the handler is not called and the operation returns.
	 * 
	 * Should fail if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * 
	 * @param query
	 *            search query
	 * @param paging
	 *            paging specification to limit operation result (optional)
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @param handler
	 *            result handler
	 * 
	 * @throws IllegalArgumentException
	 *             wrong object type
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException appropriate connector object was not found
	 * @throws ConfigurationException 
	 */
	public <T extends ObjectType> void searchObjectsIterative(Class<T> type, QueryType query, PagingType paging, 
			final ResultHandler<T> handler, final OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException;

	/**
	 * Modifies object using relative change description. Must fail if user with
	 * provided OID does not exists. Must fail if any of the described changes
	 * cannot be applied. Should be atomic.
	 * 
	 * If two or more modify operations are executed in parallel, the operations
	 * should be merged. In case that the operations are in conflict (e.g. one
	 * operation adding a value and the other removing the same value), the
	 * result is not deterministic.
	 * 
	 * The operation may fail if the modified object does not conform to the
	 * underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * 
	 * TODO: optimistic locking
	 * 
	 * @param scripts
	 *            scripts that should be executed before of after operation
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws SchemaException
	 *             resulting object would violate the schema
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			ScriptsType scripts, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, 
			CommunicationException, ConfigurationException;

	/**
	 * <p>Deletes object with specified OID.</p>
	 * <p>
	 * Must fail if object with specified OID
	 * does not exists. Should be atomic.
	 * </p>
	 * 
	 * @param oid
	 *            OID of object to delete
	 * @param scripts
	 *            scripts that should be executed before of after operation
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws ConfigurationException 
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, ScriptsType scripts, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException;

	/**
	 * Test the resource connection and basic resource connector functionality.
	 * 
	 * This operation will NOT throw exception in case the resource connection
	 * fails. It such case it will indicate the failure in the return message,
	 * but the operation itself succeeds. The operations fails only if the
	 * provided arguments are wrong, in case of system error, system
	 * misconfiguration, etc.
	 * 
	 * @param resourceOid
	 *            OID of resource to test
	 * @return results of executed tests
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public OperationResult testResource(String resourceOid) throws ObjectNotFoundException;

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
	 * @throws CommunicationException error connecting to a remote host
	 */
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult) throws CommunicationException;
	
	/**
	 * Lists resource objects.
	 * 
	 * This method differs from other method in this interface as it works with resource objects directly.
	 * It returns resource objects in a form of "detached shadow", that means fully-populated shadow objects
	 * with no OID. The results of this method may not be stored in the repository.
	 * 
	 * The purpose of this method is to work directly with the resource without the potential problems of
	 * provisioning implementation. E.g. it may be used to test resource connectivity or correctness of resource
	 * setup. It may also be used to reach object types that are not directly supported as "shadows" by the provisioning
	 * implementation.
	 * 
	 * @param resourceOid OID of the resource to fetch objects from
	 * @param objectClass Object class of the objects to fetch
	 * @param paging paging specification to limit operation result (optional)
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return resource objects in a form of "detached shadows"
	 * @throws ObjectNotFoundException specified resource object does not exist
	 * @throws SchemaException error handling resource schema
	 * @throws CommunicationException error communicating with the resource
	 */
	public List<PrismObject<? extends ResourceObjectShadowType>> listResourceObjects(String resourceOid, QName objectClass, PagingType paging,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException;
	
	/**
	 * Finish initialization of provisioning system.
	 * 
	 * The implementation may execute resource-intensive tasks in this method. All the dependencies should be already
	 * constructed, properly wired and initialized. Also logging and other infrastructure should be already set up.
	 */
	public void postInit(OperationResult parentResult);
}
