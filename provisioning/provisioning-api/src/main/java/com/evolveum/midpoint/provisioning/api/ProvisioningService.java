/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.schema.constants.TestResourceOpNames;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Provisioning Service Interface
 *
 * * Status: public
 * * Stability: STABLE, only compatible changes are expected
 *
 * This service retrieves information about resource objects and resources
 * and handles changes to resource objects. Implementations of this interface
 * will apply the changes to accounts, groups and other similar objects to the
 * target resources. It also provides information about connectors and similar
 * configuration of access to the resources.
 *
 * Supported object types:
 *
 *  * Resource
 *  * Shadow
 *  * Connector
 *
 *
 * TODO: better documentation
 *
 * @author Radovan Semancik
 */
public interface ProvisioningService {

    /**
     * Returns object for provided OID.
     *
     * Must fail if object with the OID does not exist.
     *
     * Resource Object Shadows: The resource object shadow attributes may be
     * retrieved from the local database, directly form the resource or a
     * combination of both. The retrieval may fail due to resource failure,
     * network failure or similar external cases. The retrieval may also take
     * relatively long time (e.g. until it times out).
     *
     * Retrieving `ResourceType` objects:
     *
     * 1. In `raw` mode, they are just fetched from the repository and the definitions are applied
     * (ignoring most of the exceptions in the definition application process). No super-resource resolution
     * is attempted!
     *
     * 2. in `noFetch` mode: TODO
     *
     * Retrieving `ShadowType` objects:
     *
     * ... TODO ...
     *
     * Notes:
     *
     * 1. The operation result is cleaned up before returning.
     * 2. The fetch result ({@link ObjectType#getFetchResult()}) is stored into object. It reflects the result
     * of the "fetch from resource" operation, but also e.g. application of definitions to an object retrieved in raw mode.
     * The exception is if the `raw` mode was used and the result is successful (because of performance).
     *
     * (TODO What for non-shadow/non-resource objects that are always taken from repository only?)
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
     *                 Wrong resource or connector configuration
     * @throws SecurityViolationException
     *                 Security violation while communicating with the connector or processing provisioning policies
     * @throws IllegalArgumentException
     *             wrong OID format, etc.
     * @throws GenericConnectorException
     *             unknown connector framework error
     */
    @NotNull <T extends ObjectType> PrismObject<T> getObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
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
     * @throws IllegalArgumentException
     *             wrong OID format, etc.
     * @throws GenericConnectorException
     *             unknown connector framework error
     * @throws SecurityViolationException
     *                 Security violation while communicating with the connector or processing provisioning policies
     */
    <T extends ObjectType> String addObject(PrismObject<T> object, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options,
            Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    /**
     * Fetches synchronization change events ({@link LiveSyncEvent}) from a resource and passes them into specified
     * {@link LiveSyncEventHandler}. Uses provided {@link LiveSyncTokenStorage} to get and update the token
     * that indicates the current position in the stream of live sync change events.
     *
     * It is typically invoked from a live sync activity (task).
     *
     * TODO review the following
     *
     * Notes regarding the `shadowCoordinates` parameter:
     *
     * * Resource OID is obligatory.
     * * If both object class and kind are left unspecified, all object classes on the resource are synchronized
     * (if supported by the connector/resource).
     * * If kind is specified, the object class to synchronize is determined using kind + intent pair.
     * * If kind is not specified, the object class to synchronize is determined using object class name.
     * (Currently, the default refined object class having given object class name is selected. But this should
     * be no problem, because we need just the object class name for live synchronization.)
     *
     * FIXME See also link ResourceSchema#determineCompositeObjectClassDefinition(ResourceShadowDiscriminator).
     *
     * @param shadowCoordinates Where to attempt synchronization. See description above.
     * @param options Options driving the synchronization process (execution mode, batch size, ...)
     * @param tokenStorage Interface for getting and setting the token for the activity
     * @param handler Handler that processes live sync events
     * @param parentResult Parent OperationResult to where we write our own subresults.
     * @throws ObjectNotFoundException Some of key objects (resource, task, ...) do not exist
     * @throws CommunicationException Error communicating with the resource
     * @throws SchemaException Error dealing with resource schema
     * @throws SecurityViolationException Security violation while communicating with the connector
     *         or processing provisioning policies
     * @throws GenericConnectorException Unknown connector framework error
     */
    @NotNull SynchronizationResult synchronize(@NotNull ResourceShadowDiscriminator shadowCoordinates,
            @Nullable LiveSyncOptions options, @NotNull LiveSyncTokenStorage tokenStorage, @NotNull LiveSyncEventHandler handler,
            @NotNull Task task, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException;

    /**
     * Processes asynchronous updates for a given resource.
     *
     * The control is not returned to the caller until processing is finished. The end of processing is usually triggered from
     * the outside: by stopping the owning task. (So the implementor of this method should keep an eye on task.canRun() state.)
     * Processing can be also finished when the resource encounters a fatal error. This behaviour should be configurable in the
     * future.
     *
     * If the task is not of RunningTask type, the only way how to stop processing is to interrupt the thread or to close the
     * asynchronous updates data source.
     *
     * Execution of updates is done in the context of the task worker threads (i.e. lightweight asynchronous
     * subtask), if there are any. If there are none, execution is done in the thread that receives the message.
     *
     * @param shadowCoordinates
     *
     *          What objects to synchronize. Note that although it is possible to specify other parameters in addition
     *          to resource OID (e.g. objectClass), these settings are not supported now.
     */
    void processAsynchronousUpdates(@NotNull ResourceShadowCoordinates shadowCoordinates,
            @NotNull AsyncUpdateEventHandler handler, @NotNull Task task, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException;

    /**
     * Search for objects. Returns a list of objects that match search criteria (may be empty if there are no matching objects).
     *
     * Should fail if object type is wrong. Should fail if unknown property is specified in the query.
     *
     * TODO review the following
     *
     * When dealing with shadow queries in non-raw mode, there are the following requirements:
     *
     * - there must be exactly one `resourceRef` obtainable from the query (i.e. present in the conjunction at the root level),
     * - there must be either `objectclass` or `kind` (optionally with `intent`) obtainable from the query.
     *
     * (For the raw mode the requirements are currently the same; however, we may relax them in the future.)
     *
     * The object class used for on-resource search is then determined like this:
     *
     * - if `kind` is specified, a combination of `kind` and `intent` is used to find refined object class definition,
     * - if `kind` is not specified, `objectclass` is used to find the default refined OC definition with this name
     * (i.e. it is _not_ so that the object class name is directly used for search on the resource!)
     *
     * See also MID-7470.
     *
     * Note that when using kind and/or intent, the method may return objects that do not match these conditions.
     * (The reason is that the connector does not know about kind+intent. It gets just the object class and
     * optionally an attribute query. So the search will return all members of that object class.)
     * It is the responsibility of the caller to sort these extra objects out.
     *
     * FIXME @see ObjectQueryUtil#getCoordinates(ObjectFilter, PrismContext)
     * FIXME @see ResourceSchema#determineCompositeObjectClassDefinition(ResourceShadowDiscriminator)
     *
     * == Processing of {@link ResourceType} objects
     *
     * Just like the {@link #getObject(Class, String, Collection, Task, OperationResult)} method, the resources returned from
     * this one are processed according to the inheritance rules, unless the `raw` mode is applied. Beware that - obviously -
     * the search query is applied to find the original resource objects, not the "processed" ones.
     *
     * == Fetch result
     *
     * The fetch result ({@link ObjectType#getFetchResult()}) should be present in objects returned (namely, if the processing
     * was not entirely successful). Beware that the details of storing it may differ between this method and
     * {@link #getObject(Class, String, Collection, Task, OperationResult)}.
     *
     * @return all objects of specified type that match search criteria (subject to paging)
     *
     * @throws IllegalArgumentException wrong object type
     * @throws GenericConnectorException unknown connector framework error
     * @throws SchemaException unknown property used in search query
     * @throws SecurityViolationException Security violation while communicating with the connector or processing provisioning
     * policies
     */
    @NotNull
    <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * @param query See {@link #searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)} description.
     * @param options If noFetch or raw, we count only shadows from the repository.
     */
    <T extends ObjectType> Integer countObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * Search for objects iteratively. Searches through all object types. Calls a specified handler for each object found.
     *
     * If nothing is found the handler is not called and the operation returns.
     *
     * Should fail if object type is wrong. Should fail if unknown property is specified in the query.
     *
     * See {@link #searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)} description for more information.
     *
     * @param query search query
     * @param handler result handler
     * @param parentResult parent OperationResult (in/out)
     * @throws IllegalArgumentException wrong object type
     * @throws GenericConnectorException unknown connector framework error
     * @throws SchemaException unknown property used in search query
     * @throws ObjectNotFoundException appropriate connector object was not found
     * @throws SecurityViolationException Security violation while communicating with the connector or processing provisioning
     * policies
     *
     * @see #searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)
     */
    <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ResultHandler<T> handler,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * Modifies object using relative change description. Must fail if user with
     * provided OID does not exist. Must fail if any of the described changes
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
     *                 Security violation while communicating with the connector or processing provisioning policies
     * @throws ObjectAlreadyExistsException
     *             if resulting object would have name which already exists in another object of the same type
     */
    <T extends ObjectType> String modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta<?, ?>> modifications,
            OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException;

    /**
     * Deletes object with specified OID.
     *
     * Delete operation always deletes the resource object - or at least tries to. But this operation may
     * or may not delete  the repository shadow. The shadow may remain in a dead (tombstone) state.
     * In that case the delete operation returns such shadow to indicate that repository shadow was not deleted.
     *
     * Must fail if object with specified OID does not exist. Should be atomic.
     *
     * @param oid
     *            OID of object to delete
     * @param scripts
     *            scripts that should be executed before of after operation
     * @param parentResult
     *            parent OperationResult (in/out)
     *
     * @return Current (usually dead) repository shadow - if it exists after delete. Otherwise returns null.
     *         For objects different from shadows (and when using raw deletion) returns null.
     *
     * @throws ObjectNotFoundException
     *             specified object does not exist
     * @throws SecurityViolationException
     *             security violation while communicating with the connector or processing provisioning policies
     * @throws IllegalArgumentException
     *             wrong OID format, described change is not applicable
     * @throws GenericConnectorException
     *             unknown connector framework error
     */
    <T extends ObjectType> PrismObject<T> deleteObject(Class<T> type, String oid, ProvisioningOperationOptions option,
            OperationProvisioningScriptsType scripts, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException;

    /**
     * Executes a single provisioning script.
     *
     * @param script
     *            script to execute
     * @param parentResult
     *            parent OperationResult (in/out)
     * @throws ObjectNotFoundException
     *             specified object does not exist
     * @throws SchemaException
     *             resulting object would violate the schema
     * @throws IllegalArgumentException
     *             wrong OID format, described change is not applicable
     * @throws GenericConnectorException
     *             unknown connector framework error
     * @throws SecurityViolationException
     *                 Security violation while communicating with the connector or processing provisioning policies
     * @throws ObjectAlreadyExistsException
     *             if resulting object would have name which already exists in another object of the same type
     */
    Object executeScript(String resourceOid, ProvisioningScriptType script, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException;

    /**
     * Tests the resource connection and basic resource connector functionality.
     *
     * This operation will NOT throw exception in case the resource connection fails. It such case it will indicate
     * the failure in the return message, but the operation itself succeeds. The operations fails only if the
     * provided arguments are wrong, in case of system error, system misconfiguration, etc.
     *
     * Operation result handling: The method records its operation into the provided `parentResult` in a usual way. However,
     * as the client is usually interested in details of the processing, the method returns the reference to the relevant
     * part of the operation result tree, representing the actual "test connection" operation. Care is taken to ensure that
     * part is nicely displayable to the user. The operation codes in the returned {@link OperationResult} are defined by
     * {@link TestResourceOpNames} enumeration class.
     *
     * See {@link ResourceTestOptions} for an explanation of the options and their default values.
     *
     * @param resourceOid OID of resource to test
     * @return results of executed tests
     * @throws ObjectNotFoundException resource or other required object (e.g. parent resource) does not exist
     * @throws IllegalArgumentException wrong OID format
     * @throws GenericConnectorException unknown connector framework error
     * @see TestResourceOpNames
     */
    @NotNull OperationResult testResource(
            @NotNull String resourceOid,
            @Nullable ResourceTestOptions options,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ConfigurationException;

    @NotNull default OperationResult testResource(
            @NotNull String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return testResource(resourceOid, null, task, parentResult);
    }

    /**
     * Test the resource connection and basic resource connector functionality.
     *
     * This operation will *not* throw exception in case the resource connection fails. For more information about operation
     * result handling please see {@link #testResource(String, Task, OperationResult)} method description.
     *
     * TODO describe the difference to {@link #testResource(String, Task, OperationResult)} and expected use of this method
     *
     * Notes:
     *
     * 1. The resource object must be mutable.
     * 2. Normally it is expected that it will not have OID. But it may have one. The resource is _not_ updated
     * in the repository, though, unless {@link ResourceTestOptions#updateInRepository(Boolean)} is explicitly set
     * to {@link Boolean#TRUE}.
     *
     * @param resource resource to test
     * @return results of executed tests
     * @throws GenericConnectorException unknown connector framework error
     * @throws ObjectNotFoundException some of required objects (like the parent resource) does not exist
     * @see TestResourceOpNames
     */
    @NotNull OperationResult testResource(
            @NotNull PrismObject<ResourceType> resource,
            @Nullable ResourceTestOptions options,
            @NotNull Task task,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException;

    default @NotNull OperationResult testResource(
            @NotNull PrismObject<ResourceType> resource,
            @NotNull Task task,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return testResource(resource, null, task, parentResult);
    }

    /**
     * Test basic resource connection.
     *
     * Actually, this is a convenience method for calling {@link #testResource(PrismObject, Task, OperationResult)} with
     * the {@link ResourceTestOptions#testMode(ResourceTestOptions.TestMode)} set to {@link ResourceTestOptions.TestMode#BASIC}
     * (more detailed explanation is in the `BASIC` value documentation).
     *
     * @param resource resource to test
     * @return results of executed tests
     * @throws GenericConnectorException unknown connector framework error
     * @see TestResourceOpNames
     */
    default @NotNull OperationResult testPartialConfiguration(
            @NotNull PrismObject<ResourceType> resource,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        return testResource(
                resource,
                ResourceTestOptions.basic(),
                task,
                parentResult);
    }

    /**
     * TODO please document this method
     */
    @NotNull DiscoveredConfiguration discoverConfiguration(
            @NotNull PrismObject<ResourceType> resource, @NotNull OperationResult parentResult);

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
     * Determines shadow lifecycle state (shadow state for short), updating the shadow object.
     */
    void determineShadowState(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult)
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
     */
    ProvisioningDiag getProvisioningDiag();

    /**
     * Finish initialization of provisioning system.
     *
     * The implementation may execute resource-intensive tasks in this method. All the dependencies should be already
     * constructed, properly wired and initialized. Also logging and other infrastructure should be already set up.
     */
    void postInit(OperationResult parentResult);

    /**
     * TODO description
     */
    ConstraintsCheckingResult checkConstraints(
            ResourceObjectDefinition objectTypeDefinition,
            PrismObject<ShadowType> shadowObject,
            PrismObject<ShadowType> shadowObjectOld,
            ResourceType resource,
            String shadowOid,
            ResourceShadowCoordinates shadowCoordinates,
            ConstraintViolationConfirmer constraintViolationConfirmer,
            ConstraintsCheckingStrategyType strategy,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectAlreadyExistsException, SchemaException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException;

    void enterConstraintsCheckerCache();

    void exitConstraintsCheckerCache();

    /**
     * Compare value on the resource with the provided value. This method is used to compare resource attributes
     * or passwords, e.g. for the purposes of password policy.
     * Note: comparison may be quite an expensive and heavy weight operation, e.g. it may try authenticating the user
     * on the resource.
     */
    <O extends ObjectType, T> ItemComparisonResult compare(Class<O> type, String oid, ItemPath path, T expectedValue,
            Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException;

    void shutdown();

    /**
     * Temporary and quick hack. TODO fix this
     */
    SystemConfigurationType getSystemConfiguration();

    /**
     * Provides a classifier to the provisioning service.
     */
    void setResourceObjectClassifier(ResourceObjectClassifier classifier);

    /**
     * Provides a shadow tag generator to the provisioning service.
     */
    void setShadowTagGenerator(ShadowTagGenerator generator);
}
