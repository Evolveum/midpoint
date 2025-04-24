/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.expr;

import java.util.*;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ActivityCustomization;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchemaUtil;
import com.evolveum.midpoint.schema.query.PreparedQuery;
import com.evolveum.midpoint.schema.query.TypedQuery;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.CaseService;
import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

/**
 *
 */
@SuppressWarnings("unused")
public interface MidpointFunctions {

    Trace LOGGER = TraceManager.getTrace(MidpointFunctions.class);

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
     * Fails if object with the OID does not exist.
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
     *                 the object is not schema compliant
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws CommunicationException
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
     * Fails if object with the OID does not exist.
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
     *                 the object is not schema compliant
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws CommunicationException
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

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
     *                 evaluation of expression associated with the object has failed
     * @throws CommunicationException
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
     * @throws PolicyViolationException
     *                 Policy violation was detected during processing of the object
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
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
     *                 evaluation of expression associated with the object has failed
     * @throws CommunicationException
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
     * @throws PolicyViolationException
     *                 Policy violation was detected during processing of the object
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
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
     *                 evaluation of expression associated with the object has failed
     * @throws CommunicationException
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
     * @throws PolicyViolationException
     *                 Policy violation was detected during processing of the object
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws IllegalArgumentException
     *             wrong OID format, etc.
     * @throws SystemException
     *             unknown error from underlying layers or other unexpected state
     */
    @SuppressWarnings("unchecked")
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

    <F extends FocusType> PrismObject<F> searchShadowOwner(String accountOid)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException, CommunicationException;

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
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
     *             callback handler that will be called for each found object
     * @param options
     *            options influencing the retrieval and processing of the objects
     * @throws SchemaException
     *             unknown property used in search query
     * @throws ObjectNotFoundException
     *             object required for a search was not found (e.g. resource definition)
     * @throws CommunicationException
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
     * @throws IllegalArgumentException
     *             wrong query format
     */
    <T extends ObjectType> void searchObjectsIterative(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException;

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
     *             callback handler that will be called for each found object
     * @throws SchemaException
     *             unknown property used in search query
     * @throws ObjectNotFoundException
     *             object required for a search was not found (e.g. resource definition)
     * @throws CommunicationException
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
     *                 Communication (network) error during retrieval. E.g. error communicating with the resource
     * @throws SecurityViolationException
     *                 Security violation during operation execution. May be caused either by midPoint internal
     *                 security mechanism but also by external mechanism (e.g. on the resource)
     * @throws ConfigurationException
     *                 Configuration error. E.g. misconfigured resource parameters, invalid policies, etc.
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
    OperationResult testResource(String resourceOid) throws ObjectNotFoundException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, CommunicationException;

    List<String> toList(String... s);

    /** Uses repository service directly, bypassing authorization checking. */
    long getSequenceCounter(String sequenceOid) throws ObjectNotFoundException, SchemaException;

    Collection<String> getManagersOids(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    Collection<String> getManagersOidsExceptUser(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException;

    Collection<String> getManagersOidsExceptUser(@NotNull Collection<ObjectReferenceType> userRefList)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ExpressionEvaluationException,
            ConfigurationException;

    Collection<UserType> getManagers(UserType user) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    Collection<UserType> getManagersByOrgType(UserType user, String orgType) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    Collection<UserType> getManagers(UserType user, String orgType, boolean allowSelf) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    /** Uses repository service directly, bypassing authorization checking. */
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
     * Returns parent org of the specified object that have a specific orgType.
     * @param object base object
     * @param orgType orgType to select
     * @return parent org of the specified object that have a specific orgType
     * @throws SchemaException Internal schema error
     * @throws SecurityViolationException Security violation
     */
    OrgType getParentOrgByOrgType(ObjectType object, String orgType) throws SchemaException, SecurityViolationException;

    /**
     * Returns parent org of the specified object that have a specific archetype.
     * @param object base object
     * @param archetypeOid archetype OID to select (null means "any archetype")
     * @return parent org of the specified object that have a specific archetype
     * @throws SchemaException Internal schema error
     * @throws SecurityViolationException Security violation
     */
    OrgType getParentOrgByArchetype(ObjectType object, String archetypeOid) throws SchemaException, SecurityViolationException;

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
     * Returns true if user is a manager of specified organizational unit.
     */
    boolean isManagerOf(UserType user, String orgOid);

    /**
     * Returns true if user is a manager of any organizational unit.
     */
    boolean isManager(UserType user);

    boolean isManagerOfOrgType(UserType user, String orgType) throws SchemaException;

    boolean isMemberOf(UserType user, String orgOid);

    String getPlaintextUserPassword(FocusType user) throws EncryptionException;

    String getPlaintext(ProtectedStringType user) throws EncryptionException;

    String getPlaintextAccountPassword(ShadowType account) throws EncryptionException;

    String getPlaintextAccountPasswordFromDelta(ObjectDelta<? extends ShadowType> delta) throws EncryptionException;

    String getPlaintextUserPasswordFromDeltas(List<ObjectDelta<? extends FocusType>> deltas) throws EncryptionException;

    Task getCurrentTask();

    OperationResult getCurrentResult();

    OperationResult getCurrentResult(String operationName);

    ModelContext<?> unwrapModelContext(LensContextType lensContextType)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException;

    LensContextType wrapModelContext(ModelContext<?> lensContext) throws SchemaException;

    <F extends ObjectType> boolean hasLinkedAccount(String resourceOid);

    /**
     * Returns `true` if the current clockwork operation causes the current projection to have `administrativeState` switched to
     * {@link ActivationStatusType#ENABLED}.
     *
     * Not always precise - the original value may not be known.
     */
    boolean isCurrentProjectionBeingEnabled();

    /**
     * Returns `true` if the current clockwork operation brings the projection into existence and being effectively enabled,
     * i.e. with `administrativeState` set to `null` or {@link ActivationStatusType#ENABLED}.
     * (So, previously the projection was either non-existent or effectively disabled.)
     *
     * Loads the full shadow if necessary.
     */
    boolean isCurrentProjectionActivated()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Returns `true` if the current clockwork operation causes the current projection to have `administrativeState` switched to
     * a disabled value (e.g. {@link ActivationStatusType#DISABLED} or {@link ActivationStatusType#ARCHIVED}).
     *
     * Not always precise - the original value may not be known.
     *
     * TODO what about deleting projections?
     */
    boolean isCurrentProjectionBeingDisabled();

    /**
     * Returns `true` if the current clockwork operation deletes the projection or effectively disables it,
     * i.e. sets `administrativeState` {@link ActivationStatusType#DISABLED} or {@link ActivationStatusType#ARCHIVED}.
     * (So, previously the projection existed and was effectively enabled.)
     *
     * Loads the full shadow if necessary.
     */
    boolean isCurrentProjectionDeactivated()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Returns `true` if `focus` has a `assignment` with `targetRef.OID` being equal to `targetOid`.
     * No other conditions are checked (e.g. validity, assignment condition, lifecycle states, and so on).
     */
    <F extends FocusType> boolean isDirectlyAssigned(F focus, String targetOid);

    boolean isDirectlyAssigned(String targetOid);

    boolean isDirectlyAssigned(ObjectType target);

    <F extends FocusType> boolean isDirectlyAssigned(F focusType, ObjectType target);

    default ShadowType getLinkedShadow(FocusType focus, String resourceOid)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return getLinkedShadow(focus, resourceOid, false);
    }

    @NotNull
    default List<ShadowType> getLinkedShadows(FocusType focus, String resourceOid)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return getLinkedShadows(focus, resourceOid, false);
    }

    default ShadowType getLinkedShadow(FocusType focus, String resourceOid, boolean repositoryObjectOnly)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        List<ShadowType> shadows = getLinkedShadows(focus, resourceOid, repositoryObjectOnly);
        if (shadows.isEmpty()) {
            return null;
        } else {
            return shadows.get(0);
        }
    }

    @NotNull
    List<ShadowType> getLinkedShadows(FocusType focus, String resourceOid, boolean repositoryObjectOnly)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException;

    default ShadowType getLinkedShadow(FocusType focus, ResourceType resource)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return getLinkedShadow(focus, resource.getOid());
    }

    default ShadowType getLinkedShadow(FocusType focus, ResourceType resource, boolean repositoryObjectOnly)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return getLinkedShadow(focus, resource.getOid(), repositoryObjectOnly);
    }

    default ShadowType getLinkedShadow(FocusType focus, String resourceOid, ShadowKindType kind, String intent)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return getLinkedShadow(focus, resourceOid, kind, intent, false);
    }

    /**
     * Null values of resource oid, kind, and intent mean "any".
     */
    ShadowType getLinkedShadow(
            FocusType focus, String resourceOid, ShadowKindType kind, String intent, boolean repositoryObjectOnly)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException;

    /**
     * Returns aggregated delta that is to be executed on a given resource.
     * @param context model context
     * @param resourceOid OID of the resource in question
     */
    ObjectDeltaType getResourceDelta(ModelContext<?> context, String resourceOid) throws SchemaException;

    Protector getProtector();


    /**
     * Returns a map from the translated xml attribute - value pairs.
     *
     * @param xml A string representation of xml formatted data.
     * @throws SystemException when a xml stream exception occurs
     */
    Map<String, String> parseXmlToMap(String xml);

    boolean isFullShadow();

    /**
     * Returns {@code true} if the attribute is available for processing. It must either be freshly loaded
     * (in the {@link #isFullShadow()} sense) or it must be cached *and* the use of cache for computations
     * must be allowed.
     */
    @Experimental
    boolean isAttributeLoaded(QName attrName) throws SchemaException, ConfigurationException;

    @Experimental
    default boolean isAttributeLoaded(String attrName) throws SchemaException, ConfigurationException {
        return isAttributeLoaded(new QName(NS_RI, attrName));
    }

    boolean isProjectionExists();

    /**
     * Returns list of memebers of an organization, specified by OID.
     * Only works for organizations.
     */
    // Should be perhaps renamed to getOrgMembers?
    List<UserType> getMembers(String orgOid)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, ExpressionEvaluationException;

    /**
     * Returns list of references to memebers of an organization, specified by OID.
     * Only works for organizations.
     */
    // Should be perhaps renamed to getOrgMembersAsReferences?
    List<ObjectReferenceType> getMembersAsReferences(String orgOid)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, ExpressionEvaluationException;

    /**
     * Lists all user members of a role (specified by OID), with respect to specified relation.
     * This method is suitable for listing role owners, approvers and similar "governance" users.
     */
    List<UserType> getRoleMemberUsers(String roleOid, QName relation) throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Lists all user members of a service (specified by OID), with respect to specified relation.
     * This method is suitable for listing owners, approvers and similar "governance" users of a service (e.g.an application).
     */
    List<UserType> getServiceMemberUsers(String serviceOid, QName relation) throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;
    /**
     * Default function used to compute projection purpose. It is provided here so it can be explicitly
     * invoked from a custom expression and then the result can be changed for special cases.
     */
    <F extends FocusType> ShadowPurposeType computeDefaultProjectionPurpose(F focus, ShadowType shadow, ResourceType resource);

    /**
     * Returns principal representing the user whose identity is used to execute the expression.
     */
    MidPointPrincipal getPrincipal() throws SecurityViolationException;

    /**
     * Returns OID of the current principal. After login is complete, the returned OID is the same as
     * getPrincipal().getOid(). However, during login process, this method returns the OID of the user that is
     * being authenticated/logged-in.
     */
    String getPrincipalOid();

    String getChannel();

    CaseService getWorkflowService();

    /**
     * Used for account activation notifier to collect all shadows which are going to be activated.
     * Currently it simply collects all accounts with the purpose of {@link ShadowPurposeType#INCOMPLETE}.
     */
    List<ShadowType> getShadowsToActivate(Collection<? extends ModelProjectionContext> projectionContexts);

    String createRegistrationConfirmationLink(UserType userType);

    String createInvitationLink(UserType userType);

    String createPasswordResetLink(UserType userType);

    /**
     * Returns a link where given work item can be completed.
     *
     * @return null if such a link cannot be created
     */
    @Nullable String createWorkItemCompletionLink(@NotNull WorkItemId workItemId);

    String createAccountActivationLink(UserType userType);

    String getConst(String name);

    /**
     * Returns cached entitlement (target object) for given association value. Returns a value only if the object is present
     * in the lens context cache.
     */
    ShadowType resolveEntitlement(ShadowAssociationValueType associationValue);

    ExtensionType collectExtensions(AssignmentPathType path, int startAt)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;

    /** Use {@link #submitTaskFromTemplate(String, ActivityCustomization)} instead. */
    @Deprecated
    TaskType submitTaskFromTemplate(String templateTaskOid, List<Item<?, ?>> extensionItems)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    /** Use {@link #submitTaskFromTemplate(String, ActivityCustomization)} instead. */
    @Deprecated
    TaskType submitTaskFromTemplate(String templateTaskOid, Map<QName, Object> extensionValues)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    /**
     * Submits a task from template (pointed to by `templateOid`), customizing it according to given
     * {@link ActivityCustomization}. The template must be compatible with the customization required.
     *
     * The currently logged-in user must possess {@link ModelAuthorizationAction#USE} authorization applicable for
     * the specified task template.
     */
    @NotNull String submitTaskFromTemplate(@NotNull String templateOid, @NotNull ActivityCustomization customization)
            throws CommonException;

    TaskType executeChangesAsynchronously(Collection<ObjectDelta<?>> deltas, ModelExecuteOptions options, String templateTaskOid) throws SecurityViolationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    TaskType executeChangesAsynchronously(Collection<ObjectDelta<?>> deltas, ModelExecuteOptions options,
            String templateTaskOid, Task opTask,
            OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    String translate(String key, Objects... args);

    String translate(LocalizableMessage message);

    /**
     * Translates message parameter.
     *
     * @param useDefaultLocale If true, default JVM locale will be used for translation - {@link Locale#getDefault()}.
     * If false, Midpoint will check principal object for more appropriate login - {@link MidPointPrincipal#getLocale()},
     * if no locale available {@link Locale#getDefault()} will be used.
     * @return translated string
     */
    String translate(LocalizableMessage message, boolean useDefaultLocale);

    String translate(LocalizableMessageType message);

    /**
     * Translates message parameter.
     *
     * @param useDefaultLocale If true, default JVM locale will be used for translation - {@link Locale#getDefault()}.
     * If false, Midpoint will check principal object for more appropriate login - {@link MidPointPrincipal#getLocale()},
     * if no locale available {@link Locale#getDefault()} will be used.
     * @return translated string
     */
    String translate(LocalizableMessageType message, boolean useDefaultLocale);

    /**
     * Counts accounts having `attributeValue` of `attributeName`.
     *
     * Note that this method uses the default definition of {@link ShadowKindType#ACCOUNT} objects, if present.
     */
    <T> Integer countAccounts(String resourceOid, QName attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /** A variant of {@link #countAccounts(String, QName, Object)}. */
    <T> Integer countAccounts(ResourceType resourceType, QName attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /** A variant of {@link #countAccounts(String, QName, Object)}. Attribute name is assumed to be in the `ri:` namespace. */
    <T> Integer countAccounts(ResourceType resourceType, String attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    <T> boolean isUniquePropertyValue(ObjectType objectType, String propertyPathString, T propertyValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(O objectType, String propertyPathString,
            T propertyValue, boolean getAllConflicting)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * Checks if the value `attributeValue` of `attributeName` in given shadow is unique on given resource.
     *
     * A search on resource is invoked, and any occurrences (except the one in `shadowType`) are reported as violations.
     *
     * Notes:
     *
     * 1. `shadowType` should have an OID;
     * 2. when constructing the query, the default `account` definition is used to provide attribute definitions.
     */
    <T> boolean isUniqueAccountValue(ResourceType resourceType, ShadowType shadowType, String attributeName,
            T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    <F extends ObjectType> ModelContext<F> getModelContext();

    <F extends ObjectType> ModelElementContext<F> getFocusContext();

    ModelProjectionContext getProjectionContext();

    <V extends PrismValue, D extends ItemDefinition<?>> Mapping<V, D> getMapping();

    Object executeAdHocProvisioningScript(ResourceType resource, String language, String code)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException, ObjectAlreadyExistsException;

    Object executeAdHocProvisioningScript(String resourceOid, String language, String code)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException, ObjectAlreadyExistsException;

    /**
     * Returns indication whether a script code is evaluating a new value.
     * If this method returns true value then the script code is evaluating "new" value.
     * That means a value that is going to be set to target, e.g.
     * value from "add" or "replace" parts of the delta.
     * If the script evaluates existing value that is not being modified (e.g. during
     * a recomputation), that is also considered as "new" (going to be set to target) value.
     * If this method returns false value then the script code is evaluating "old" value.
     * That means a value that is used for the purposes of removing values from target,
     * e.g. value from "delete" part of the delta or values from an existing object.
     * If this method returns null then the old/new status cannot be determined or this
     * method is invoked in a situation when such a distinction is not applicable.
     */
    Boolean isEvaluateNew();

    /** Just a convenience method to allow writing `midpoint.evaluateNew` even after Groovy upgrade in 4.8. */
    Boolean getEvaluateNew();

    /**
     * Returns all non-negative values from all focus mappings (targeted to given path)
     * from all non-negative evaluated assignments.
     *
     * Highly experimental. Use at your own risk.
     */
    @NotNull
    Collection<PrismValue> collectAssignedFocusMappingsResults(@NotNull ItemPath path) throws SchemaException;

    /**
     * Legacy name for {@link #findCandidateOwners(Class, ShadowType, String, ShadowKindType, String)}.
     *
     * There are some slight changes in semantics comparing to midPoint 4.5:
     *
     * 1. If `kind` is `null`, `ACCOUNT` is assumed.
     * 2. If `intent` is `null`, `default` is assumed.
     * 3. The use of `type` parameter is a bit different.
     * 4. Instead of being limited to correlation/confirmation expressions, the method now invokes
     * the standard correlation mechanism. See {@link #findCandidateOwners(Class, ShadowType, String, ShadowKindType, String)}
     * for more information.
     * 5. The original method returns `null` e.g. if there's no synchronization policy, etc.
     * So, this method treats all exceptions (except for {@link SchemaException}) by returning `null`.
     */
    @Deprecated
    default <F extends FocusType> List<F> getFocusesByCorrelationRule(
            Class<F> type,
            String resourceOid,
            ShadowKindType kind,
            String intent,
            ShadowType shadow) throws SchemaException {
        try {
            return findCandidateOwners(
                    type,
                    shadow,
                    Objects.requireNonNull(resourceOid, "no resource OID"),
                    Objects.requireNonNullElse(kind, ShadowKindType.ACCOUNT),
                    Objects.requireNonNullElse(intent, SchemaConstants.INTENT_DEFAULT));
        } catch (ExpressionEvaluationException | CommunicationException | SecurityViolationException
                | ConfigurationException | ObjectNotFoundException e) {
            LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't find focus objects by correlation rule", e);
            return null;
        }
    }

    /**
     * Finds candidate owners using defined correlation rules.
     * (To be used e.g. in synchronization sorter expressions.)
     *
     * Limitations/notes:
     *
     * 1. Fully supported only for simple correlators: query, expression, and item. Other correlators may or may not work here.
     * 2. The method call encompasses execution of "before correlation" mappings.
     *
     * @param focusType Type of the owner looked for. It is merged with the type defined for given kind/intent;
     * the more specific of the two is used.
     * @param resourceObject Resource object we want to correlate
     * @param resourceOid OID of the resource we want to do the correlation for
     * @param kind Pre-determined (or assumed) kind of the resource object
     * @param intent Pre-determined (or assumed) intent of the resource object
     */
    @Experimental
    <F extends FocusType> @NotNull List<F> findCandidateOwners(
            @NotNull Class<F> focusType,
            @NotNull ShadowType resourceObject,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException;

    <F extends ObjectType> ModelContext<F> previewChanges(Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SchemaException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    <F extends ObjectType> ModelContext<F> previewChanges(Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SchemaException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    PrismContext getPrismContext();

    RelationRegistry getRelationRegistry();

    <T extends ObjectType> void applyDefinition(T object)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException;

    <C extends Containerable> S_ItemEntry deltaFor(Class<C> objectClass) throws SchemaException;

    /**
     * Returns {@code true} if the specified object has an effective mark with the specified OID.
     *
     * Use only in situations where you know the provided object has effective marks computed.
     */
    default boolean hasEffectiveMark(@Nullable ObjectType object, @NotNull String markOid) {
        return object != null && ObjectTypeUtil.hasEffectiveMarkRef(object, markOid);
    }

    default <O extends ObjectType> boolean hasArchetype(O object, String archetypeOid) {
        return getArchetypeOids(object).contains(archetypeOid);
    }

    /**
     * Assumes single archetype. May throw error if used on object that has more than one archetype.
     */
    @Deprecated
    <O extends ObjectType> ArchetypeType getArchetype(O object) throws SchemaException;

    /**
     * Returns the structural archetype for the object, possibly `null`.
     */
    @Nullable <O extends AssignmentHolderType> ArchetypeType getStructuralArchetype(O object) throws SchemaException;

    @NotNull <O extends ObjectType> List<ArchetypeType> getArchetypes(O object) throws SchemaException;

    /**
     * Assumes single archetype. May throw error if used on object that has more than one archetype.
     */
    @Deprecated
    <O extends ObjectType> String getArchetypeOid(O object) throws SchemaException;

    /**
     * Returns a list of archetype OIDs for given object.
     *
     * Currently, those OIDs are taken from archetype assignments.
     * ArchetypeRef values are ignored.
     */
    @NotNull List<String> getArchetypeOids(ObjectType object);

    default <O extends ObjectType> void addRecomputeTrigger(O object, Long timestamp)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        addRecomputeTrigger(object, timestamp, null);
    }

    default <O extends ObjectType> void addRecomputeTrigger(O object, Long timestamp,
            TriggerCustomizer triggerCustomizer)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        addRecomputeTrigger(object.asPrismObject(), timestamp, triggerCustomizer);
    }

    default <O extends ObjectType> void addRecomputeTrigger(PrismObject<O> object, Long timestamp)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        addRecomputeTrigger(object, timestamp, null);
    }

    <O extends ObjectType> void addRecomputeTrigger(PrismObject<O> object, Long timestamp,
            TriggerCustomizer triggerCustomizer)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException;

    RepositoryService getRepositoryService();

    /** Creates an {@link OptimizingTriggerCreator} that is able to create triggers only if they are not already present. */
    @NotNull
    OptimizingTriggerCreator getOptimizingTriggerCreator(long fireAfter, long safetyMargin);

    @NotNull
    <T> ShadowSimpleAttributeDefinition<T> getAttributeDefinition(
            PrismObject<ResourceType> resource, QName objectClassName, QName attributeName)
            throws SchemaException, ConfigurationException;

    @NotNull
    <T> ShadowSimpleAttributeDefinition<T> getAttributeDefinition(
            PrismObject<ResourceType> resource, String objectClassName, String attributeName)
            throws SchemaException, ConfigurationException;

    /**
     * Goes directly to repository service.
     */
    @Experimental
    void createRecomputeTrigger(Class<? extends ObjectType> type, String oid) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException;

    /**
     * Returns a correlation case for given shadow. (Need not be full.)
     */
    @Experimental
    @Nullable CaseType getCorrelationCaseForShadow(@Nullable ShadowType shadow) throws SchemaException;

    <T> TypedQuery<T> queryFor(Class<T> type, String query) throws SchemaException;

    <T> PreparedQuery<T> preparedQueryFor(Class<T> type, String query) throws SchemaException;

    <T extends ObjectType> List<T> searchObjects(TypedQuery<T> query) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    default <T extends ObjectType> List<T> searchObjects(TypedQuery<T> query, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return searchObjects(query.getType(), query.toObjectQuery(), options);
    }

    default <T extends ObjectType> void searchObjectsIterative(TypedQuery<T> query, ResultHandler<T> handler) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        searchObjectsIterative(query.getType(), query.toObjectQuery(), handler);
    }

    default <T extends ObjectType> void searchObjectsIterative(TypedQuery<T> query, ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        searchObjectsIterative(query.getType(), query.toObjectQuery(), handler, options);
    }


    @FunctionalInterface
    interface TriggerCustomizer {
        void customize(TriggerType trigger) throws SchemaException;
    }

    /**
     * Returns longer version of human-readable description of the resource object set:
     *
     * . resource name
     * . object type display name (if known)
     * . object type ID (kind, intent)
     * . flag whether we are using default type definition
     * . object class
     *
     * Currently, object types are resolved and named using the
     * {@link ResourceSchemaUtil#findDefinitionForBulkOperation(ResourceType, ShadowKindType, String, QName)} method
     * that is used for import and reconciliation activities. Hence, the name should be quite precise in the context
     * of these activities.
     */
    String describeResourceObjectSetLong(ResourceObjectSetType set) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException;

    /**
     * Short version of {@link #describeResourceObjectSetLong(ResourceObjectSetType)}:
     *
     * . only one of object type display name and type ID is shown;
     * . object class is omitted when type is present
     */
    String describeResourceObjectSetShort(ResourceObjectSetType set) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException;

    /** Synonym for {@link #describeResourceObjectSetShort(ResourceObjectSetType)}, mainly for compatibility reasons. */
    default String describeResourceObjectSet(ResourceObjectSetType set) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        return describeResourceObjectSetShort(set);
    }


    /**
     * Selects specified values from all relevant identity data.
     *
     * @param identities a collection of identities where we search for the data
     * @param source specification of a source we are looking for; `null` means "all sources"
     * The source is currently matched using resource OID, kind, intent, and tag.
     * See {@link FocusIdentityTypeUtil#matches(FocusIdentitySourceType, FocusIdentitySourceType)} method.
     * @param itemPath item that should be provided
     *
     * @see FocusIdentityTypeUtil#matches(FocusIdentitySourceType, FocusIdentitySourceType)
     */
    Collection<PrismValue> selectIdentityItemValues(
            @Nullable Collection<FocusIdentityType> identities,
            @Nullable FocusIdentitySourceType source,
            @NotNull ItemPath itemPath);

    /**
     * Returns true if the object is effectively enabled.
     *
     * For convenience, if the object is not present, the method returns `false`.
     * If the object is not a {@link FocusType}, the method returns `true` (as there is no activation there).
     * For {@link FocusType} objects, it assumes that the object underwent standard computation and
     * `activation/effectiveStatus` is set.
     *
     * If the `activation/effectiveStatus` is not present, the return value of the method is undefined.
     */
    default boolean isEffectivelyEnabled(@Nullable ObjectType object) {
        return object != null
                && (!(object instanceof FocusType)
                || FocusTypeUtil.getEffectiveStatus((FocusType) object) == ActivationStatusType.ENABLED);
    }

    /**
     * Does the current clockwork operation bring the focus into existence and being effectively enabled?
     * (So, previously it was either non-existent or effectively disabled.)
     */
    boolean isFocusActivated();

    /**
     * Does the current clockwork operation delete or effectively disable the focus?
     * (So, previously it existed and was effectively enabled.)
     */
    boolean isFocusDeactivated();

    /** Does the current clockwork operation delete the focus? */
    boolean isFocusDeleted();

    /**
     * Returns the object reference for a given association value.
     * Assumes that the association has no content and exactly one object.
     * (It it does not hold, the method returns null.)
     */
    @Nullable ObjectReferenceType getObjectRef(@Nullable ShadowAssociationValueType associationValueBean);

    /** Returns the OID of the reference returned by {@link #getObjectRef(ShadowAssociationValueType)}. */
    default @Nullable String getObjectOid(@Nullable ShadowAssociationValueType associationValueBean) {
        return Referencable.getOid(
                getObjectRef(associationValueBean));
    }

    /**
     * Returns the name of the object of given (no-content) association value (if present there).
     *
     * @see #getObjectRef(ShadowAssociationValueType)
     */
    default @Nullable PolyStringType getObjectName(@Nullable ShadowAssociationValueType associationValueBean) {
        var ref = getObjectRef(associationValueBean);
        return ref != null ? ref.getTargetName() : null;
    }

    default @Nullable ValueMetadataType getMetadata(@NotNull ObjectType object) {
        return ValueMetadataTypeUtil.getMetadata(object);
    }

    default @Nullable ValueMetadataType getMetadata(@NotNull AbstractCredentialType credential) {
        return ValueMetadataTypeUtil.getMetadata(credential);
    }

    default @Nullable XMLGregorianCalendar getCreateTimestamp(@NotNull ObjectType object) {
        return ValueMetadataTypeUtil.getCreateTimestamp(object);
    }

    default @Nullable XMLGregorianCalendar getCreateTimestamp(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getCreateTimestamp(assignment);
    }

    default @Nullable XMLGregorianCalendar getModifyTimestamp(@NotNull ObjectType object) {
        return ValueMetadataTypeUtil.getModifyTimestamp(object);
    }

    default @Nullable XMLGregorianCalendar getLastChangeTimestamp(@NotNull ObjectType object) {
        return ValueMetadataTypeUtil.getLastChangeTimestamp(object);
    }

    default @NotNull List<ObjectReferenceType> getCreateApproverRefs(@NotNull ObjectType object) {
        return ValueMetadataTypeUtil.getCreateApproverRefs(object);
    }

    default @NotNull Collection<ObjectReferenceType> getCreateApproverRefs(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getCreateApproverRefs(assignment);
    }

    default Collection<ObjectReferenceType> getModifyApproverRefs(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getModifyApproverRefs(assignment);
    }

    default Collection<String> getModifyApprovalComments(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getModifyApprovalComments(assignment);
    }

    default @Nullable XMLGregorianCalendar getRequestTimestamp(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getRequestTimestamp(assignment);
    }

    default Collection<ObjectReferenceType> getRequestorRefs(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getRequestorRefs(assignment) ;
    }

    default Collection<String> getRequestorComments(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getRequestorComments(assignment);
    }

    default @NotNull Collection<ObjectReferenceType> getCertifierRefs(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getCertifierRefs(assignment);
    }

    default @NotNull Collection<String> getCertifierComments(@NotNull AssignmentType assignment) {
        return ValueMetadataTypeUtil.getCertifierComments(assignment);
    }

    default GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return GetOperationOptionsBuilder.create();
    }

    default @NotNull Collection<SelectorOptions<GetOperationOptions>> onlyBaseObject() {
        return getOperationOptionsBuilder()
                .item(ItemPath.EMPTY_PATH).dontRetrieve().build();
    }



}
