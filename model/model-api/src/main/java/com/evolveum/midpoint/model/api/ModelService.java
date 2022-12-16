/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.DiscoveredConfiguration;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.CompareResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    String COMPARE_OBJECT = CLASS_NAME_WITH_DOT + "compareObject";
    String SEARCH_OBJECTS = CLASS_NAME_WITH_DOT + "searchObjects";
    String SEARCH_CONTAINERS = CLASS_NAME_WITH_DOT + "searchContainers";
    String COUNT_CONTAINERS = CLASS_NAME_WITH_DOT + "countContainers";
    String COUNT_OBJECTS = CLASS_NAME_WITH_DOT + "countObjects";
    String EXECUTE_CHANGES = CLASS_NAME_WITH_DOT + "executeChanges";
    String EXECUTE_CHANGE = CLASS_NAME_WITH_DOT + "executeChange";
    String RECOMPUTE = CLASS_NAME_WITH_DOT + "recompute";
    String LIST_ACCOUNT_SHADOW_OWNER = CLASS_NAME_WITH_DOT + "listAccountShadowOwner";
    String IMPORT_ACCOUNTS_FROM_RESOURCE = CLASS_NAME_WITH_DOT + "importAccountsFromResource";
    String IMPORT_OBJECTS_FROM_FILE = CLASS_NAME_WITH_DOT + "importObjectsFromFile";
    String IMPORT_OBJECTS_FROM_STREAM = CLASS_NAME_WITH_DOT + "importObjectsFromStream";
    String POST_INIT = CLASS_NAME_WITH_DOT + "postInit";
    String DISCOVER_CONNECTORS = CLASS_NAME_WITH_DOT + "discoverConnectors";
    String MERGE_OBJECTS = CLASS_NAME_WITH_DOT + "mergeObjects";
    String NOTIFY_CHANGE = CLASS_NAME_WITH_DOT + "notifyChange";

    String AUTZ_NAMESPACE = AuthorizationConstants.NS_AUTHORIZATION_MODEL;

    String OPERATION_LOGGER_NAME = "com.evolveum.midpoint.model.api.op";
    String CHECK_INDESTRUCTIBLE = CLASS_NAME_WITH_DOT + "checkIndestructible";

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
     * @param result
     *            parent OperationResult (in/out)
     * @param task
     *               Task instance. It gives context to the execution (e.g. security context)
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
    @NotNull
    <T extends ObjectType> PrismObject<T> getObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

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
     * @param result
     *            parent OperationResult (in/out)
     * @param task
     *               Task instance. It gives context to the execution (e.g. security context)
     * @return A collection of executed ObjectDeltaOperations (ObjectDelta + OperationResult). OIDs of newly created objects can be found
     *         in these ObjectDeltas (which may or may not be original ObjectDeltas passed to the method).
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
    default Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        return executeChanges(deltas, options, task, null, result);
    }

    Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options,
            Task task,
            Collection<ProgressListener> listeners,
            OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException;

    /**
     * Recomputes focal object with the specified OID. The operation considers all the applicable policies and
     * mapping and tries to re-apply them as necessary.
     *
     * @since 3.6
     *
     * @param type type (class) of an object to recompute
     * @param oid OID of the object to recompute
     * @param options execute options
     * @param parentResult parent OperationResult (in/out)
     * @param task Task instance. It gives context to the execution (e.g. security context)
     */
    <F extends ObjectType> void recompute(
            Class<F> type, String oid, ModelExecuteOptions options, Task task, OperationResult parentResult)
             throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * <p>
     * Returns the Focus object representing owner of specified shadow.
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
     * @param shadowOid
     *            OID of the shadow to look for an owner
     * @param task
     *               Task instance. It gives context to the execution (e.g. security context)
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
    PrismObject<? extends FocusType> searchShadowOwner(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException;

    /**
     * Search for objects.
     *
     * Searches through all object of a specified type. Returns a list of objects that match
     * search criteria.
     *
     * Note that this method has a very limited scaling capability
     * as all the results are stored in the memory. DO NOT USE on large datasets.
     * Recommended usage is only when using queries that cannot return large number
     * of results (e.g. queries for unique values) or when combined with paging capability.
     * For other cases use searchObjectsIterative instead.
     *
     * Returns empty list if object type is correct but there are no objects of
     * that type. Fails if object type is wrong. Should fail if unknown property is
     * specified in the query.
     *
     * When searching for objects of {@link ShadowType}, there are specific requirements related to the query. Please see
     * {@link ProvisioningService#searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)} for more information.
     *
     * @param type
     *            (class) of an object to search
     * @param query
     *            search query
     * @param options
     *            options influencing the retrieval and processing of the objects
     * @param task
     *               Task instance. It gives context to the execution (e.g. security context)
     * @param parentResult
     *            parent OperationResult (in/out)
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
    <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Search for "sub-object" structures, i.e. containers.
     * Supported types are: AccessCertificationCaseType, CaseWorkItemType.
     */
    <T extends Containerable> SearchResultList<T> searchContainers(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException;

    <T extends Containerable> Integer countContainers(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Search for objects in iterative fashion (using callback).
     *
     * Searches through all object of a specified type. A handler is invoked for each object found.
     *
     * The handler is not called at all if object type is correct but there are no objects of
     * that type. Fails if object type is wrong. Should fail if unknown property is
     * specified in the query.
     *
     * When searching for objects of {@link ShadowType}, there are specific requirements related to the query. Please see
     * {@link ProvisioningService#searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)} for more information.
     *
     * @param type
     *            (class) of an object to search
     * @param query
     *            search query
     * @param handler
     *             callback handler that will be called for each found object
     * @param options
     *            options influencing the retrieval and processing of the objects
     * @param task
     *               Task instance. It gives context to the execution (e.g. security context)
     * @param parentResult
     *            parent OperationResult (in/out)
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
    <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, Task task,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * Count objects.
     *
     * Searches through all object of a specified type and returns a count of such objects.
     * This method is usually much more efficient than equivalent search method. It is used mostly for
     * presentation purposes, e.g. displaying correct number of pages in the GUI listings.
     *
     * When counting objects of {@link ShadowType}, there are specific requirements related to the query. Please see
     * {@link ProvisioningService#searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)} for more information.
     *
     * @param type
     *            (class) of an object to search
     * @param query
     *            search query
     * @param options
     *            options influencing the retrieval and processing of the objects
     * @param task
     *               Task instance. It gives context to the execution (e.g. security context)
     * @param parentResult
     *            parent OperationResult (in/out)
     * @return number of objects of specified type that match search criteria (subject
     *         to paging). May return null if the number of objects is not known.
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
    <T extends ObjectType> Integer countObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult parentResult)
                    throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException;

    /**
     * <p>
     * Test the resource connection and basic resource connector functionality.
     * </p>
     * <p>
     * Work same as {@link ProvisioningService#testResource(PrismObject, Task, OperationResult)}.
     * </p>
     *
     * @param resourceOid OID of resource to test
     * @return results of executed tests
     * @throws ObjectNotFoundException specified object does not exist
     * @throws IllegalArgumentException wrong OID format
     */
    OperationResult testResource(String resourceOid, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException;

    /**
     * <p>
     * Test the resource connection and basic resource connector functionality.
     * </p>
     * <p>
     * Work same as {@link com.evolveum.midpoint.provisioning.api.ProvisioningService#testResource(PrismObject, Task, OperationResult)}.
     * </p>
     *
     * @param resource resource to test
     * @return results of executed tests
     * @throws ObjectNotFoundException specified object does not exist
     */
    OperationResult testResource(PrismObject<ResourceType> resource, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException;


    /**
     * <p>
     * Test partial resource connector configuration. Testing only basic connection.
     * </p>
     * <p>
     * Method work with OperationResult same as method
     * {@link ProvisioningService#testResource(PrismObject, Task, OperationResult)}.
     * </p>
     *
     * @param resource resource to test
     * @return results of executed partial test
     * @throws ObjectNotFoundException specified object does not exist
     */
    OperationResult testResourcePartialConfiguration(PrismObject<ResourceType> resource, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException;

    /**
     * <p>
     * Method work same as
     * {@link ProvisioningService#discoverConfiguration(PrismObject, OperationResult)}.
     * </p>
     *
     * @param resource resource with minimal connector configuration
     * @return Suggested configuration properties wrapped in DiscoveredConfiguration.
     */
    DiscoveredConfiguration discoverResourceConnectorConfiguration(PrismObject<ResourceType> resource, OperationResult result);

    /**
     * <p>
     * Method work same as
     * {@link com.evolveum.midpoint.provisioning.api.ProvisioningService#fetchSchema(PrismObject, OperationResult)}.
     * </p>
     *
     * @param resource resource with connector configuration
     * @return Resource schema fetched by connector
     */
    @Nullable ResourceSchema fetchSchema(@NotNull PrismObject<ResourceType> resource, @NotNull OperationResult parentResult);

    /**
     * <p>
     * Method work same as
     * {@link com.evolveum.midpoint.provisioning.api.ProvisioningService#getNativeCapabilities(String, OperationResult)}.
     * </p>
     *
     * EXPERIMENTAL feature.
     */
    @Experimental
    @NotNull CapabilityCollectionType getNativeCapabilities(@NotNull String connOid, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException;


    /**
     * <p>
     * Import accounts from resource.
     * </p>
     * <p>
     * Invocation of this method may be switched to background.
     * </p>
     * TODO: Better description
     */
    void importFromResource(String resourceOid, QName objectClass, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * <p>
     * Import single account from resource.
     * </p>
     * TODO: Better description
     */
    void importFromResource(String shadowOid, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Import objects from file.
     *
     * Invocation of this method may be switched to background.
     */
    void importObjectsFromFile(File input, ImportOptionsType options, Task task, OperationResult parentResult) throws FileNotFoundException;

    /**
     * Import object.
     *
     * The results will be provided in the task.
     */
    <O extends ObjectType> void importObject(PrismObject<O> object, ImportOptionsType options, Task task, OperationResult result);

    /**
     * Import objects from stream.
     *
     * Invocation of this method will happen in foreground, as the stream cannot
     * be serialized.
     *
     * The results will be provided in the task.
     */
    void importObjectsFromStream(InputStream input, String language, ImportOptionsType options, Task task, OperationResult parentResult);

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
    Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, Task task, OperationResult parentResult)
            throws CommunicationException, SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException;

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

    /**
     *  shutdown model and lower system components
     */
    void shutdown();

    /**
     * TODO
     */
    <O extends ObjectType> CompareResultType compareObject(PrismObject<O> object,
            Collection<SelectorOptions<GetOperationOptions>> readOptions, ModelCompareOptions compareOptions,
            @NotNull List<? extends ItemPath> ignoreItemPaths, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException;

    /**
     * Merge two objects into one.
     *
     * EXPERIMENTAL feature. The method signature is likely to change in the future.
     *
     * @param type object type
     * @param leftOid left-side object OID
     * @param rightOid  right-side object OID
     * @param mergeConfigurationName name of the merge configuration to use
     */
    <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> mergeObjects(Class<O> type, String leftOid, String rightOid,
            String mergeConfigurationName, Task task, OperationResult result)
                    throws ObjectNotFoundException, SchemaException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, PolicyViolationException, SecurityViolationException;

    void notifyChange(ResourceObjectShadowChangeDescriptionType changeDescription, Task task, OperationResult parentResult)
            throws CommonException;

    @NotNull
    PrismContext getPrismContext();
}
