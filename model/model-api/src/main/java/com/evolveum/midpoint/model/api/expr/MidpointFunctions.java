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

package com.evolveum.midpoint.model.api.expr;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author mederly
 */
public interface MidpointFunctions {
	
	/**
	 * <p>
	 * Creates empty prism object of specified type in memory. This is used to construct
	 * Java instances of object before they are added to the repository.
	 * </p>
	 * <p>
	 * Please note that this method constructs the object <b>in memory only</b>. If you
	 * intend to create an object in repository or on the resource you need to populate
	 * this object with data and then invoke the addObject or executeChanges method.
	 * </p>
	 * @param type Class of the object to create
	 * @return empty object in memory
	 * @throws SchemaException schema error instantiating the object (e.g. attempt to
	 *                         instantiate abstract type).
	 */
	<T extends ObjectType> T createEmptyObject(Class<T> type) throws SchemaException;

	/**
	 * <p>
	 * Creates empty prism object of specified type and with specified name in memory.
	 * This is used to construct Java instances of object before they are added to the
	 * repository. The 'name' property of the new object will be set to a specified value.
	 * </p>
	 * <p>
	 * Please note that this method constructs the object <b>in memory only</b>. If you
	 * intend to create an object in repository or on the resource you need to populate
	 * this object with data and then invoke the addObject or executeChanges method.
	 * </p>
	 * @param type Class of the object to create
	 * @param name Name of the object
	 * @return empty object in memory
	 * @throws SchemaException schema error instantiating the object (e.g. attempt to
	 *                         instantiate abstract type).
	 */
	<T extends ObjectType> T createEmptyObjectWithName(Class<T> type, String name) throws SchemaException;

	/**
	 * <p>
	 * Creates empty prism object of specified type and with specified name in memory.
	 * This is used to construct Java instances of object before they are added to the
	 * repository. The 'name' property of the new object will be set to a specified value.
	 * </p>
	 * <p>
	 * Please note that this method constructs the object <b>in memory only</b>. If you
	 * intend to create an object in repository or on the resource you need to populate
	 * this object with data and then invoke the addObject or executeChanges method.
	 * </p>
	 * @param type Class of the object to create
	 * @param name Name of the object
	 * @return empty object in memory
	 * @throws SchemaException schema error instantiating the object (e.g. attempt to
	 *                         instantiate abstract type).
	 */
	<T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyString name) throws SchemaException;
	
	/**
	 * <p>
	 * Creates empty prism object of specified type and with specified name in memory.
	 * This is used to construct Java instances of object before they are added to the
	 * repository. The 'name' property of the new object will be set to a specified value.
	 * </p>
	 * <p>
	 * Please note that this method constructs the object <b>in memory only</b>. If you
	 * intend to create an object in repository or on the resource you need to populate
	 * this object with data and then invoke the addObject or executeChanges method.
	 * </p>
	 * @param type Class of the object to create
	 * @param name Name of the object
	 * @return empty object in memory
	 * @throws SchemaException schema error instantiating the object (e.g. attempt to
	 *                         instantiate abstract type).
	 */
	<T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyStringType name) throws SchemaException;

	<T extends ObjectType> T resolveReference(ObjectReferenceType reference)
            throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

	<T extends ObjectType> T resolveReferenceIfExists(ObjectReferenceType reference)
            throws SchemaException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

	/**
	 * <p>
	 * Returns object for provided OID. It retrieves the object from an appropriate source
	 * for an object type (e.g. internal repository, resource or both), merging data as necessary,
	 * processing any policies, caching mechanisms, etc. This can be influenced by using options.
	 * </p>
	 * <p>
	 * Fails if object with the OID does not exists.
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to get
	 * @param oid
	 *            OID of the object to get
	 * @param options
	 *            options influencing the retrieval and processing of the object
	 * @return Retrieved object
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws SchemaException 
	 * 				the object is not schema compliant
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws CommunicationException
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             missing required parameter, wrong OID format, etc.
	 * @throws ClassCastException
	 *             OID represents object of a type incompatible with requested
	 *             type
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	<T extends ObjectType> T getObject(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

	/**
	 * <p>
	 * Returns object for provided OID. It retrieves the object from an appropriate source
	 * for an object type (e.g. internal repository, resource or both), merging data as necessary,
	 * processing any policies, caching mechanisms, etc.
	 * </p>
	 * <p>
	 * Fails if object with the OID does not exists.
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to get
	 * @param oid
	 *            OID of the object to get
	 * @return Retrieved object
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws SchemaException 
	 * 				the object is not schema compliant
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws CommunicationException
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             missing required parameter, wrong OID format, etc.
	 * @throws ClassCastException
	 *             OID represents object of a type incompatible with requested
	 *             type
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	<T extends ObjectType> T getObject(Class<T> type, String oid)
			throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;
	
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
	 * @param options
	 *            options influencing processing of the deltas
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
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws PolicyViolationException
	 * 				Policy violation was detected during processing of the object
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected state
	 */
	void executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options) 
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;

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
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws PolicyViolationException
	 * 				Policy violation was detected during processing of the object
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected state
	 */
	void executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas) 
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;

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
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws PolicyViolationException
	 * 				Policy violation was detected during processing of the object
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected state
	 */
	void executeChanges(ObjectDelta<? extends ObjectType>... deltas) 
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;

	<T extends ObjectType> String addObject(PrismObject<T> newObject, ModelExecuteOptions options)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;
	
	<T extends ObjectType> String addObject(PrismObject<T> newObject)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;
	
	<T extends ObjectType> String addObject(T newObject, ModelExecuteOptions options)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;
	
	<T extends ObjectType> String addObject(T newObject)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;

	<T extends ObjectType> void modifyObject(ObjectDelta<T> modifyDelta, ModelExecuteOptions options)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;
	
	<T extends ObjectType> void modifyObject(ObjectDelta<T> modifyDelta)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;
	
	<T extends ObjectType> void deleteObject(Class<T> type, String oid, ModelExecuteOptions options)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;

	<T extends ObjectType> void deleteObject(Class<T> type, String oid)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;
	
	/**
	 * Recomputes focal object with the specified OID. The operation considers all the applicable policies and
	 * mapping and tries to re-apply them as necessary.
	 * 
	 * @param type type (class) of an object to recompute
	 * @param oid OID of the object to recompute
	 */
	<F extends FocusType> void recompute(Class<F> type, String oid)
			 throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException;

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
	 * @return owner of the account or null
	 * @throws ObjectNotFoundException
	 *             specified account was not found
	 * @throws SchemaException 
	 * @throws SecurityViolationException  
	 * @throws CommunicationException 
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 * @throws SystemException
	 *             unknown error from underlying layers or other unexpected
	 *             state
	 */
	PrismObject<UserType> findShadowOwner(String accountOid) throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException;

	/**
	 * <p>
	 * Search for objects.
	 * </p>
	 * <p>
	 * Searches through all object of a specified type. Returns a list of objects that match
	 * search criteria. 
	 * </p>
	 * <p>
	 * Note that this method has a very limited scaling capability
	 * as all the results are stored in the memory. DO NOT USE on large datasets.
	 * Recommended usage is only when using queries that cannot return large number
	 * of results (e.g. queries for unique values) or when combined with paging capability.
	 * For other cases use searchObjectsIterative instead.
	 * </p>
	 * <p>
	 * Returns empty list if object type is correct but there are no objects of
	 * that type. Fails if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param query
	 *            search query
	 * @param options
	 *            options influencing the retrieval and processing of the objects
	 * @return all objects of specified type that match search criteria (subject
	 *         to paging)
	 * 
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> List<T> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

	/**
	 * <p>
	 * Search for objects.
	 * </p>
	 * <p>
	 * Searches through all object of a specified type. Returns a list of objects that match
	 * search criteria. 
	 * </p>
	 * <p>
	 * Note that this method has a very limited scaling capability
	 * as all the results are stored in the memory. DO NOT USE on large datasets.
	 * Recommended usage is only when using queries that cannot return large number
	 * of results (e.g. queries for unique values) or when combined with paging capability.
	 * For other cases use searchObjectsIterative instead.
	 * </p>
	 * <p>
	 * Returns empty list if object type is correct but there are no objects of
	 * that type. Fails if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param query
	 *            search query
	 * @return all objects of specified type that match search criteria (subject
	 *         to paging)
	 * 
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> List<T> searchObjects(Class<T> type, ObjectQuery query) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

	/**
	 * <p>
	 * Search for objects in iterative fashion (using callback).
	 * </p>
	 * <p>
	 * Searches through all object of a specified type. A handler is invoked for each object found.
	 * </p>
	 * <p>
	 * The handler is not called at all if object type is correct but there are no objects of
	 * that type. Fails if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param query
	 *            search query
	 * @param handler
	 * 			callback handler that will be called for each found object
	 * @param options
	 *            options influencing the retrieval and processing of the objects
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> void searchObjectsIterative(Class<T> type, ObjectQuery query,
			ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

	/**
	 * <p>
	 * Search for objects in iterative fashion (using callback).
	 * </p>
	 * <p>
	 * Searches through all object of a specified type. A handler is invoked for each object found.
	 * </p>
	 * <p>
	 * The handler is not called at all if object type is correct but there are no objects of
	 * that type. Fails if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param query
	 *            search query
	 * @param handler
	 * 			callback handler that will be called for each found object
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> void searchObjectsIterative(Class<T> type, ObjectQuery query, ResultHandler<T> handler) 
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

	/**
	 * <p>
	 * Search for objects by name.
	 * </p>
	 * <p>
	 * Searches through all object of a specified type for an object with specified name.
	 * Returns that object if it is found, return null otherwise. The method fails if more than
	 * one object is found therefore it cannot be reliably used on types with non-unique names
	 * (such as Shadows). 
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param name
	 *            Name of the object to look for
	 * @return an object of specified type with a matching name or null
	 * 
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> T searchObjectByName(Class<T> type, String name) throws SecurityViolationException, 
					ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException, ExpressionEvaluationException;
	
	/**
	 * <p>
	 * Search for objects by name.
	 * </p>
	 * <p>
	 * Searches through all object of a specified type for an object with specified name.
	 * Returns that object if it is found, return null otherwise. The method fails if more than
	 * one object is found therefore it cannot be reliably used on types with non-unique names
	 * (such as Shadows). 
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param name
	 *            Name of the object to look for
	 * @return an object of specified type with a matching name or null
	 * 
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> T searchObjectByName(Class<T> type, PolyString name) throws SecurityViolationException, 
					ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException, ExpressionEvaluationException;
	
	/**
	 * <p>
	 * Search for objects by name.
	 * </p>
	 * <p>
	 * Searches through all object of a specified type for an object with specified name.
	 * Returns that object if it is found, return null otherwise. The method fails if more than
	 * one object is found therefore it cannot be reliably used on types with non-unique names
	 * (such as Shadows). 
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param name
	 *            Name of the object to look for
	 * @return an object of specified type with a matching name or null
	 * 
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> T searchObjectByName(Class<T> type, PolyStringType name) throws SecurityViolationException, 
					ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException, ExpressionEvaluationException;
	
	/**
	 * <p>
	 * Count objects.
	 * </p>
	 * <p>
	 * Searches through all object of a specified type and returns a count of such objects.
	 * This method is usually much more efficient than equivalent search method. It is used mostly for
	 * presentation purposes, e.g. displaying correct number of pages in the GUI listings. 
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param query
	 *            search query
	 * @param options
	 *            options influencing the retrieval and processing of the objects
	 * @return number of objects of specified type that match search criteria (subject
	 *         to paging)
	 * 
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) 
            		throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException;

	/**
	 * <p>
	 * Count objects.
	 * </p>
	 * <p>
	 * Searches through all object of a specified type and returns a count of such objects.
	 * This method is usually much more efficient than equivalent search method. It is used mostly for
	 * presentation purposes, e.g. displaying correct number of pages in the GUI listings. 
	 * </p>
	 * 
	 * @param type
	 *            (class) of an object to search
	 * @param query
	 *            search query
	 * @return number of objects of specified type that match search criteria (subject
	 *         to paging)
	 * 
	 * @throws SchemaException
	 *             unknown property used in search query
	 * @throws ObjectNotFoundException
	 *             object required for a search was not found (e.g. resource definition)
	 * @throws CommunicationException 
	 * 				Communication (network) error during retrieval. E.g. error communicating with the resource
	 * @throw SecurityViolationException
	 * 				Security violation during operation execution. May be caused either by midPoint internal
	 * 				security mechanism but also by external mechanism (e.g. on the resource)
	 * @throws ConfigurationException
	 * 				Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
	 * @throws IllegalArgumentException
	 *             wrong query format
	 */
	<T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query) 
    		throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException;

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
	OperationResult testResource(String resourceOid) throws ObjectNotFoundException;
	
	

    List<String> toList(String... s);

    Collection<String> getManagersOids(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    Collection<String> getManagersOidsExceptUser(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException;

    Collection<String> getManagersOidsExceptUser(@NotNull Collection<ObjectReferenceType> userRefList)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ExpressionEvaluationException,
			ConfigurationException;

    Collection<UserType> getManagers(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException;
    
    Collection<UserType> getManagersByOrgType(UserType user, String orgType) throws SchemaException, ObjectNotFoundException, SecurityViolationException;
    
    Collection<UserType> getManagers(UserType user, String orgType, boolean allowSelf) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException;

    // todo here we could select "functional" org.units in order to filter out e.g. project managers from the list of managers
    // however, the syntax of orgType attribute is not standardized
    Collection<String> getOrgUnits(UserType user);

	Collection<String> getOrgUnits(UserType user, QName relation);

	OrgType getOrgByOid(String oid) throws SchemaException;

    OrgType getOrgByName(String name) throws SchemaException, SecurityViolationException;

    /**
     * Returns parent orgs of the specified object that have a specific relation and orgType.
     * @param object base object
     * @param relation local part of the relation (in the String form)
     * @param orgType orgType to select
     * @return parent orgs of the specified object that have a specific relation and orgType
     * @throws SchemaException Internal schema error
     * @throws SecurityViolationException Security violation
     */
    Collection<OrgType> getParentOrgs(ObjectType object, String relation, String orgType) throws SchemaException, SecurityViolationException;
    
    /**
     * Returns parent orgs of the specified object that have a specific relation and orgType.
     * @param object base object
     * @param relation relation in the QName form
     * @param orgType orgType to select
     * @return parent orgs of the specified object that have a specific relation and orgType
     * @throws SchemaException Internal schema error
     * @throws SecurityViolationException Security violation
     */
    Collection<OrgType> getParentOrgs(ObjectType object, QName relation, String orgType) throws SchemaException, SecurityViolationException;
    
    /**
     * Returns parent orgs of the specified object that have a specific orgType.
     * @param object base object
     * @param orgType orgType to select
     * @return parent orgs of the specified object that have a specific orgType
     * @throws SchemaException Internal schema error
     * @throws SecurityViolationException Security violation
     */
    OrgType getParentOrgByOrgType(ObjectType object, String orgType) throws SchemaException, SecurityViolationException;

    /**
     * Returns parent orgs of the specified object that have a specific relation.
     * @param object base object
     * @param relation relation in the QName form
     * @return parent orgs of the specified object that have a specific relation
     * @throws SchemaException Internal schema error
     * @throws SecurityViolationException Security violation
     */
    Collection<OrgType> getParentOrgsByRelation(ObjectType object, QName relation) throws SchemaException, SecurityViolationException;

    /**
     * Returns parent orgs of the specified object that have a specific relation.
     * @param object base object
     * @param relation local part of the relation (in the String form)
     * @return parent orgs of the specified object that have a specific relation
     * @throws SchemaException Internal schema error
     * @throws SecurityViolationException Security violation
     */
    Collection<OrgType> getParentOrgsByRelation(ObjectType object, String relation) throws SchemaException, SecurityViolationException;
    
    /**
     * Returns all parent orgs of the specified object.
     * @param object base object
     * @return all parent orgs
     * @throws SchemaException Internal schema error
     * @throws SecurityViolationException Security violation
     */
    Collection<OrgType> getParentOrgs(ObjectType object) throws SchemaException, SecurityViolationException;
    
    Collection<UserType> getManagersOfOrg(String orgOid) throws SchemaException, SecurityViolationException;

    /**
     * Returns true if user is a manager of specified organiational unit. 
     */
    boolean isManagerOf(UserType user, String orgOid);
    
    /**
     * Returns true if user is a manager of any organizational unit.
     */
    boolean isManager(UserType user);
    
    boolean isManagerOfOrgType(UserType user, String orgType) throws SchemaException;

    boolean isMemberOf(UserType user, String orgOid);

    String getPlaintextUserPassword(UserType user) throws EncryptionException;
    
    String getPlaintext(ProtectedStringType user) throws EncryptionException;

    String getPlaintextAccountPassword(ShadowType account) throws EncryptionException;

    String getPlaintextAccountPasswordFromDelta(ObjectDelta<? extends ShadowType> delta) throws EncryptionException;

    String getPlaintextUserPasswordFromDeltas(List<ObjectDelta<UserType>> deltas) throws EncryptionException;

	Task getCurrentTask();

	ModelContext unwrapModelContext(LensContextType lensContextType) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    <F extends FocusType> boolean isDirectlyAssigned(F focusType, String targetOid);
    
    boolean isDirectlyAssigned(String targetOid);
    
    boolean isDirectlyAssigned(ObjectType target);

    <F extends FocusType> boolean isDirectlyAssigned(F focusType, ObjectType target);

    ShadowType getLinkedShadow(FocusType focus, String resourceOid)  throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
    
    ShadowType getLinkedShadow(FocusType focus, String resourceOid, boolean repositoryObjectOnly)  throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
    
    ShadowType getLinkedShadow(FocusType focus, ResourceType resource)  throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
    
    ShadowType getLinkedShadow(FocusType focus, ResourceType resource, boolean repositoryObjectOnly)  throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    ShadowType getLinkedShadow(FocusType focus, String resourceOid, ShadowKindType kind, String intent) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
    
    ShadowType getLinkedShadow(FocusType focus, String resourceOid, ShadowKindType kind, String intent, boolean repositoryObjectOnly) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
    
    /**
     * Returns aggregated delta that is to be executed on a given resource.
     * @param context model context
     * @param resourceOid OID of the resource in question
     * @return
     */
    ObjectDeltaType getResourceDelta(ModelContext context, String resourceOid) throws SchemaException;

	Protector getProtector();
	
	
	/**
	 * Returns a map from the translated xml attribute - value pairs.
	 *
	 * @param A string representation of xml formated data. 
	 * @return
	 * @throws SystemException when an xml stream exception occurs 
	 */
	Map<String, String> parseXmlToMap(String xml);
	
	boolean isFullShadow();
	
	boolean isProjectionExists();

	List<UserType> getMembers(String orgOid)
			throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
			ObjectNotFoundException, ExpressionEvaluationException;

	List<ObjectReferenceType> getMembersAsReferences(String orgOid)
			throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
			ObjectNotFoundException, ExpressionEvaluationException;
	
	/**
	 * Default function used to compute projection lifecycle. It is provided here so it can be explicitly
	 * invoked from a custom expression and then the result can be changed for special cases.
	 */
	<F extends FocusType> String computeProjectionLifecycle(F focus, ShadowType shadow, ResourceType resource);
	
	/**
	 * Returns principal representing the user whose identity is used to execute the expression.
	 */
	MidPointPrincipal getPrincipal() throws SecurityViolationException;

	String getChannel();

	WorkflowService getWorkflowService();
	
	/**
	 * Used for account activation notifier to collect all shadows which are going to be activated.
	 */
	List<ShadowType> getShadowsToActivate(Collection<ModelElementContext> projectionContexts);
	
	String createRegistrationConfirmationLink(UserType userType);
	
	String createPasswordResetLink(UserType userType);
	
	String createAccountActivationLink(UserType userType);
	
	String getConst(String name);

	ExtensionType collectExtensions(AssignmentPathType path, int startAt)
			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
			ConfigurationException, ExpressionEvaluationException;

	TaskType submitTaskFromTemplate(String templateTaskOid, List<Item<?, ?>> extensionItems)
			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
			ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

	TaskType submitTaskFromTemplate(String templateTaskOid, Map<QName, Object> extensionValues)
			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
			ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

	<F extends ObjectType> ModelContext<F> getModelContext();
	
	<F extends ObjectType> ModelElementContext<F> getFocusContext();
	
	ModelProjectionContext getProjectionContext();

}
