/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ProvisioningDiag;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * <p>Provisioning Service Interface.</p>
 * <p>
 * Status: public
 * Stability: STABLE, only compatible changes are expected
 * @version 3.4
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
	 * 				Wrong resource or connector configuration 
	 * @throws SecurityViolationException 
	 * 				Security violation while communicating with the connector or processing provisioning policies
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	<T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, 
			SecurityViolationException, ExpressionEvaluationException;
	
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
	 * @throws SecurityViolationException 
	 * 				Security violation while communicating with the connector or processing provisioning policies
	 */
	<T extends ObjectType> String addObject(PrismObject<T> object, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options,
			Task task, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException, 
			ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

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
	 *             some of key objects (resource, task, ...) do not exist
	 * @throws CommunicationException
	 *             error communicating with the resource
	 * @throws SchemaException
	 *             error dealing with resource schema
	 * @throws ConfigurationException 
	 * @throws SecurityViolationException 
	 * 				Security violation while communicating with the connector or processing provisioning policies
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	int synchronize(ResourceShadowDiscriminator shadowCoordinates, Task task, OperationResult parentResult) throws ObjectNotFoundException, 
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;


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
	 * @param paging
	 *            paging specification to limit operation result (optional)
	 * @param query
	 *            search query
	 * @param task
	 *@param parentResult
	 *            parent OperationResult (in/out)  @return all objects of specified type that match search criteria (subject
	 *         to paging)
	 * 
	 * @throws IllegalArgumentException
	 *             wrong object type
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ConfigurationException 
	 * @throws SecurityViolationException 
	 * 				Security violation while communicating with the connector or processing provisioning policies
	 */
	<T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
			SecurityViolationException, ExpressionEvaluationException;

	/**
	 * Options: if noFetch or raw, we count only shadows from the repository.
	 */
	<T extends ObjectType> Integer countObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException;
	
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
	 * @param handler
	 *            result handler
	 * @param task
	 *@param parentResult
	 *            parent OperationResult (in/out)
	 *  @throws IllegalArgumentException
	 *             wrong object type
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException appropriate connector object was not found
	 * @throws ConfigurationException 
	 * @throws SecurityViolationException 
	 * 				Security violation while communicating with the connector or processing provisioning policies
	 */
	<T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
																			  final ResultHandler<T> handler, Task task, final OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

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
	 * @throws SecurityViolationException 
	 * 				Security violation while communicating with the connector or processing provisioning policies
	 * @throws ObjectAlreadyExistsException
     *             if resulting object would have name which already exists in another object of the same type
	 */
	<T extends ObjectType> String modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, 
			CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException;

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
	 * @throws SecurityViolationException 
	 * 				Security violation while communicating with the connector or processing provisioning policies
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 * @throws GenericConnectorException
	 *             unknown connector framework error
	 */
	<T extends ObjectType> void deleteObject(Class<T> type, String oid, ProvisioningOperationOptions option, OperationProvisioningScriptsType scripts, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

	/**
	 * Executes a single provisioning script.
	 * 
	 * @param script
	 *            script to execute
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
	 * @throws SecurityViolationException 
	 * 				Security violation while communicating with the connector or processing provisioning policies
	 * @throws ObjectAlreadyExistsException
     *             if resulting object would have name which already exists in another object of the same type
	 */
	<T extends ObjectType> void executeScript(String resourceOid, ProvisioningScriptType script, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, 
			CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException;
	
	/**
	 * Test the resource connection and basic resource connector functionality.
	 * 
	 * This operation will NOT throw exception in case the resource connection
	 * fails. It such case it will indicate the failure in the return message,
	 * but the operation itself succeeds. The operations fails only if the
	 * provided arguments are wrong, in case of system error, system
	 * misconfiguration, etc.
	 * 
	 * The operation codes in the return value are defined by ConnectorTestOperation enumeration class.
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
	 *             
	 * @see ConnectorTestOperation
	 */
	OperationResult testResource(String resourceOid, Task task) throws ObjectNotFoundException;

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
	Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult) throws CommunicationException;
	
	List<ConnectorOperationalStatus> getConnectorOperationalStatus(String resourceOid, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
	
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
	 * @param task
	 *@param parentResult
	 *            parent OperationResult (in/out)  @return resource objects in a form of "detached shadows"
	 * @throws ObjectNotFoundException specified resource object does not exist
	 * @throws SchemaException error handling resource schema
	 * @throws CommunicationException error communicating with the resource
	 */
	@Deprecated
	List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid, QName objectClass, ObjectPaging paging,
				Task task, OperationResult parentResult) 
						throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;
	
	
	/**
	 * Makes sure that the shadow is in accord with the reality. If there are any unfinished operations associated with the shadow
	 * then this method will try to finish them. If there are pending (async) operations then this method will update their status.
	 * And so on. However, this is NOT reconciliation function that will make sure that the resource object attributes are OK
	 * with all the policies. This is just a provisioning-level operation.
	 */
	void refreshShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			ObjectAlreadyExistsException, SecurityViolationException, ExpressionEvaluationException;

	/**
	 * Applies appropriate definition to the shadow/resource delta.
	 */
	<T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Task task, OperationResult parentResult) 
		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Applies appropriate definition to the shadow/resource delta (uses provided object to get necessary information)
     */
    <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Objectable object, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
	 * Applies appropriate definition to the shadow.
	 */
	<T extends ObjectType> void applyDefinition(PrismObject<T> object, Task task, OperationResult parentResult)
		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
	
	/**
	 * Applies appropriate definition to the query.
	 */
	<T extends ObjectType> void applyDefinition(Class<T> type, ObjectQuery query, Task task, OperationResult parentResult) 
		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
	
	/**
	 * Runs a short, non-destructive internal provisioning test. It tests provisioning framework and
	 * general setup. Use ModelService.testResource for testing individual resource configurations.
	 */
	void provisioningSelfTest(OperationResult parentTestResult, Task task);

    /**
     * Returns a diagnostic information.
     * @see com.evolveum.midpoint.schema.ProvisioningDiag
     * @return
     */
    ProvisioningDiag getProvisioningDiag();

    /**
	 * Finish initialization of provisioning system.
	 * 
	 * The implementation may execute resource-intensive tasks in this method. All the dependencies should be already
	 * constructed, properly wired and initialized. Also logging and other infrastructure should be already set up.
	 */
	void postInit(OperationResult parentResult);

	ConstraintsCheckingResult checkConstraints(RefinedObjectClassDefinition shadowDefinition,
											   PrismObject<ShadowType> shadowObject,
											   ResourceType resourceType,
											   String shadowOid,
											   ResourceShadowDiscriminator resourceShadowDiscriminator,
											   ConstraintViolationConfirmer constraintViolationConfirmer,
											   Task task, OperationResult parentResult) 
			   throws CommunicationException, ObjectAlreadyExistsException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException;

	void enterConstraintsCheckerCache();

	void exitConstraintsCheckerCache();

	void shutdown();

}
