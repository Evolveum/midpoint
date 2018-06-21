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
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Connector instance configured for a specific resource.
 *
 * This is kind of connector facade. It is an API provided by
 * the "Unified Connector Framework" to the midPoint provisioning
 * component. There is no associated SPI yet. That may come in the
 * future when this interface stabilizes a bit.
 *
 * This interface provides an unified facade to a connector capabilities
 * in the Unified Connector Framework interface. The connector is configured
 * to a specific resource instance and therefore can execute operations on
 * resource.
 *
 * Calls to this interface always try to reach the resource and get the
 * actual state on resource. The connectors are not supposed to cache any
 * information. Therefore the methods do not follow get/set java convention
 * as the data are not regular javabean properties.
 *
 * @see ConnectorFactory
 *
 * @author Radovan Semancik
 *
 */
public interface ConnectorInstance {

	public static final String OPERATION_CONFIGURE = ConnectorInstance.class.getName() + ".configure";
	public static final String OPERATION_INITIALIZE = ConnectorInstance.class.getName() + ".initialize";

	/**
	 *
	 * The connector instance will be configured to the state that it can
	 * immediately access the resource. The resource configuration is provided as
	 * a parameter to this method.
	 *
	 * @param configuration
	 * @throws ConfigurationException
	 */
	void configure(PrismContainerValue<?> configuration, OperationResult parentResult) throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException;

	ConnectorOperationalStatus getOperationalStatus() throws ObjectNotFoundException;

	/**
	 * Get necessary information from the remote system.
	 *
	 * This method will initialize the configured connector. It may contact the remote system in order to do so,
	 * e.g. to download the schema. It will cache the information inside connector instance until this method
	 * is called again. It must be called after configure() and before any other method that is accessing the
	 * resource.
	 *
	 * If resource schema and capabilities are already cached by midPoint they may be passed to the connector instance.
	 * Otherwise the instance may need to fetch them from the resource which may be less efficient.
	 * 
	 * NOTE: the capabilities and schema that are used here are NOT necessarily those that are detected by the resource.
     *       The detected schema will come later. The schema here is the one that is stored in the resource
     *       definition (ResourceType). This may be schema that was detected previously. But it may also be a schema
     *       that was manually defined. This is needed to be passed to the connector in case that the connector
     *       cannot detect the schema and needs schema/capabilities definition to establish a connection.
     *       Most connectors will just ignore the schema and capabilities that are provided here.
     *       But some connectors may need it (e.g. CSV connector working with CSV file without a header).
	 *
	 * TODO: caseIgnoreAttributeNames is probably not correct here. It should be provided in schema or capabilities?
	 *
	 * @param caseIgnoreAttributeNames
	 * @param parentResult
	 * @throws CommunicationException
	 * @throws GenericFrameworkException
	 * @throws ConfigurationException
	 */
	void initialize(ResourceSchema previousResourceSchema, Collection<Object> previousCapabilities, boolean caseIgnoreAttributeNames, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, ConfigurationException;

	/**
	 * Retrieves native connector capabilities.
	 *
	 * The capabilities specify what the connector can do without any kind of simulation or other workarounds.
	 * The set of capabilities may depend on the connector configuration (e.g. if a "disable" or password attribute
	 * was specified in the configuration or not).
	 *
	 * It may return null. Such case means that the capabilities cannot be determined.
	 *
	 * @param parentResult
	 * @return
	 * @throws CommunicationException
	 * @throws GenericFrameworkException
	 * @throws ConfigurationException
	 */
	Collection<Object> fetchCapabilities(OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ConfigurationException;

    /**
	 * Retrieves the schema from the resource.
	 *
	 * The schema may be considered to be an XSD schema, but it is returned in a
	 * "parsed" format and it is in fact a bit stricter and richer midPoint
	 * schema.
	 *
	 * It may return null. Such case means that the schema cannot be determined.
	 *
	 * @see PrismSchema
	 *
	 * @return Up-to-date resource schema.
	 * @throws CommunicationException error in communication to the resource
	 *				- nothing was fetched.
     * @throws ConfigurationException
	 */
	ResourceSchema fetchResourceSchema(List<QName> generateObjectClasses, OperationResult parentResult) throws CommunicationException, GenericFrameworkException, ConfigurationException;

	/**
	 * Retrieves a specific object from the resource.
	 *
	 * This method is fetching an object from the resource that is identified
	 * by its primary identifier. It is a "targeted" method in this aspect and
	 * it will fail if the object is not found.
	 *
	 * The objectClass provided as a parameter to this method must correspond
	 * to one of the object classes in the schema. The object class must match
	 * the object. If it does not, the behavior of this operation is undefined.
	 *
	 * The returned ResourceObject is "disconnected" from schema. It means that
	 * any call to the getDefinition() method of the returned object will
	 * return null.
	 *
	 * TODO: object not found error
	 *
	 * @param objectClass objectClass of the object to fetch (QName).
	 * @param identifiers primary identifiers of the object.
	 * @return object fetched from the resource (no schema)
	 * @throws CommunicationException error in communication to the resource
	 *				- nothing was fetched.
	 * @throws SchemaException error converting object from native (connector) format
	 */
	PrismObject<ShadowType> fetchObject(ResourceObjectIdentification resourceObjectIdentification, AttributesToReturn attributesToReturn, StateReporter reporter,
															 OperationResult parentResult)
		throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
		SecurityViolationException, ConfigurationException;

	/**
	 * Execute iterative search operation.
	 *
	 * This method will execute search operation on the resource and will pass
	 * any objects that are found. A "handler" callback will be called for each
	 * of the objects found.
	 *
	 * The call to this method will return only after all the callbacks were
	 * called, therefore it is not asynchronous in a strict sense.
	 *
	 * If nothing is found the method should behave as if there is an empty result set
	 * (handler is never called) and the call should result in a success.
	 * The ObjectNotFoundException should be throws only if there is an error in search
	 * parameters, e.g. if search base points to an non-existent object.
	 *
	 * @param objectClass
	 * @param handler
	 * @return
	 * @throws CommunicationException
	 * @throws SchemaException error converting object from the native (connector) format
	 * @throws ObjectNotFoundException if something from the search parameters refers non-existent object.
	 * 									e.g. if search base points to an non-existent object.
	 */
    SearchResultMetadata search(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query, ShadowResultHandler handler, AttributesToReturn attributesToReturn, PagedSearchCapabilityType pagedSearchConfigurationType, SearchHierarchyConstraints searchHierarchyConstraints, StateReporter reporter,
															  OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, SecurityViolationException,
            		ObjectNotFoundException;

    /**
     * Counts objects on resource.
     *
     * This method will count objects on the resource by executing a paged search operation,
     * returning the "estimated objects count" information.
     *
     * If paging is not available, it throws an exception.
     *
     * @param objectClassDefinition
     * @param query
     * @param pagedSearchConfigurationType
     * @param parentResult
     * @throws CommunicationException
     * @throws SchemaException
     * @throws java.lang.UnsupportedOperationException
     */
    int count(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query, PagedSearchCapabilityType pagedSearchConfigurationType, StateReporter reporter,
					 OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, UnsupportedOperationException;

    /**
	 * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
	 * and what were not determined.
	 *
	 * The exception should be thrown only if the connector is sure that nothing was done on the resource.
	 * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
	 * exception as something might have been done already.
	 *
	 * The connector may return some (or all) of the attributes of created object. The connector should do
	 * this only such operation is efficient, e.g. in case that the created object is normal return value from
	 * the create operation. The connector must not execute additional operation to fetch the state of
	 * created resource. In case that the new state is not such a normal result, the connector must
	 * return null. Returning empty set means that the connector supports returning of new state, but nothing
	 * was returned (e.g. due to a limiting configuration). Returning null means that connector does not support
	 * returning of new object state and the caller should explicitly invoke fetchObject() in case that the
	 * information is needed.
	 *
	 * @param object
	 * @param additionalOperations
	 * @throws CommunicationException
	 * @throws SchemaException resource schema violation
	 * @return created object attributes. May be null.
	 * @throws ObjectAlreadyExistsException object already exists on the resource
	 */
    AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(PrismObject<? extends ShadowType> object, Collection<Operation> additionalOperations, StateReporter reporter,
													  OperationResult parentResult) throws CommunicationException, GenericFrameworkException, SchemaException,
			ObjectAlreadyExistsException, ConfigurationException;

	/**
	 * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
	 * and what results are we not sure about.
	 *
	 * Returns a set of attributes that were changed as a result of the operation. This may include attributes
	 * that were changed as a side effect of the operations, e.g. attributes that were not originally specified
	 * in the "changes" parameter.
	 *
	 * The exception should be thrown only if the connector is sure that nothing was done on the resource.
	 * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
	 * exception as something might have been done already.
	 *
	 * @param identifiers The set of identifiers. Their values may change as a result of the operation, e.g. when the resource object is renamed.
	 * @param changes
	 * @throws CommunicationException
	 * @throws SchemaException
	 * @throws ObjectAlreadyExistsException in case that the modified object conflicts with another existing object (e.g. while renaming an object)
	 */
    AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(ObjectClassComplexTypeDefinition objectClass, PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, Collection<Operation> changes, StateReporter reporter,
																  OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
			SecurityViolationException, ObjectAlreadyExistsException, ConfigurationException;
	
    AsynchronousOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass, Collection<Operation> additionalOperations, PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter,
							 OperationResult parentResult)
					throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException;

	Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, StateReporter reporter, OperationResult parentResult) throws CommunicationException, GenericFrameworkException;

	/**
	 * Creates a live Java object from a token previously serialized to string.
	 *
	 * Serialized token is not portable to other connectors or other resources.
	 * However, newer versions of the connector should understand tokens generated
	 * by previous connector version.
	 *
	 * @param serializedToken
	 * @return
	 */
	PrismProperty<?> deserializeToken(Object serializedToken);

	/**
	 * Returns the latest token. In other words, returns a token that
	 * corresponds to a current state of the resource. If fetchChanges
	 * is immediately called with this token, nothing should be returned
	 * (Figuratively speaking, neglecting concurrent resource modifications).
	 *
	 * @return
	 * @throws CommunicationException
	 */
	<T> PrismProperty<T> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClass, StateReporter reporter, OperationResult parentResult) throws CommunicationException, GenericFrameworkException;

	/**
	 * Token may be null. That means "from the beginning of history".
	 *
	 * @param lastToken
	 * @return
	 */
	List<Change> fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken, AttributesToReturn attrsToReturn, StateReporter reporter,
															   OperationResult parentResult) throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException;

	//public ValidationResult validateConfiguration(ResourceConfiguration newConfiguration);

	//public void applyConfiguration(ResourceConfiguration newConfiguration) throws MisconfigurationException;

	// Maybe this should be moved to ConnectorManager? In that way it can also test connector instantiation.
	void test(OperationResult parentResult);


	void dispose();

}
