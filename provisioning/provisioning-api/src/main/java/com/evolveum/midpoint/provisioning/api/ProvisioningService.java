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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;

/**
 * WORK IN PROGRESS
 * 
 * There be dragons. Beware the dog. Do not trespass.
 * 
 * This is supposed to replace provisioning-1.wsdl
 * 
 * @author Radovan Semancik
 */
public interface ProvisioningService {
	
	public static final String TEST_CONNECTION_INIT_OPERATION = ProvisioningService.class.getName()+".testResource.initialization";
	public static final String TEST_CONNECTION_OPERATION = ProvisioningService.class.getName()+".testResource";

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
	 * @param oid
	 *            OID of the object to get
	 * @param resolve
	 *            list of properties to resolve in the fetched object
	 * @param result
	 *            parent OperationResult (in/out)
	 * @return Object fetched from repository and/or resource
	 * 
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws CommunicationException
	 *             error communicating with the resource
	 * @throws SchemaException
	 *             error dealing with resource schema
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException;

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
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public String addObject(ObjectType object, ScriptsType scripts, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException;

	/**
	 * Collect external changes on a resource and call the business logic with
	 * the accumulated change data.
	 * 
	 * This method will be invoked by scheduler/sync thread.
	 * 
	 * TODO: Better description
	 * 
	 * @param oid
	 *            OID of the resource for which to attempt synchronization
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * 
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public void synchronize(String oid, OperationResult parentResult);

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
	public ObjectListType listObjects(Class<? extends ObjectType> objectType, PagingType paging,
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
	 */
	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException;
	
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
	 */
	public void searchObjectsIterative(QueryType query, PagingType paging, final ResultHandler handler, final OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException;

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
	 * @param objectChange
	 *            specification of object changes
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
	public void modifyObject(ObjectModificationType objectChange, ScriptsType scripts,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException;

	/**
	 * Deleted object with provided OID. Must fail if object with specified OID
	 * does not exists. Should be atomic.
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
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public void deleteObject(String oid, ScriptsType scripts, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException;

	/**
	 * Returns list of available values for specified properties.
	 * 
	 * The returned values can be used as valid values for properties of the
	 * specific object. The provided values can be used e.g. for listing them in
	 * GUI list boxes, for early validation (pre-validation), displaying help
	 * messages, auto-complete, etc.
	 * 
	 * In case the list of available values is too big or it is not available,
	 * the empty list should be returned, setting the "closed" flag to false.
	 * 
	 * @param oid
	 *            OID of the object for which to determine values
	 * @param properties
	 *            list of properties for which to determine values
	 * @param parentResult
	 *            parentResult parent OperationResult (in/out)
	 * @return list of available values
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult parentResult)
			throws ObjectNotFoundException;

	/**
	 * Test the resouce connection and basic resource connector functionality.
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

	public ObjectListType listResourceObjects(String resourceOid, QName objectType, PagingType paging,
			OperationResult parentResult);
}
