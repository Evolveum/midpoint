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

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.TestResourceOpNames;

import com.evolveum.midpoint.prism.OriginMarker;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext.empty;

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
 * NOTE: Because there is currently only a single implementation of this interface, we have the luxury to provide some
 * implementation-level restrictions and comments here, without having to write them separately into the implementation
 * class. This may change in the future.
 *
 * @author Radovan Semancik
 */
public interface ProvisioningService {

    String OP_GET_OBJECT = ProvisioningService.class.getName() + ".getObject";
    String OP_SEARCH_OBJECTS = ProvisioningService.class.getName() + ".searchObjects";
    String OP_SEARCH_OBJECTS_ITERATIVE = ProvisioningService.class.getName() + ".searchObjectsIterative";
    String OP_COUNT_OBJECTS = ProvisioningService.class.getName() + ".countObjects";
    String OP_REFRESH_SHADOW = ProvisioningService.class.getName() + ".refreshShadow";
    String OP_DELETE_OBJECT = ProvisioningService.class.getName() + ".deleteObject";
    String OP_DISCOVER_CONFIGURATION = ProvisioningService.class.getName() + ".discoverConfiguration";
    String OP_EXPAND_CONFIGURATION_OBJECT = ProvisioningService.class.getName()
            + ".expandConfigurationObject";
    // TODO reconsider names of these operations
    String OP_TEST_RESOURCE = ProvisioningService.class.getName() + ".testResource";
    String OP_GET_NATIVE_CAPABILITIES = ProvisioningService.class.getName() + ".getNativeCapabilities";
    String OP_INITIALIZE = ProvisioningService.class.getName() + ".initialize";
    String OP_DISCOVER_CONNECTORS = ProvisioningService.class.getName() + ".discoverConnectors";

    /**
     * Returns the object with specified OID. (It must fail if there is no object with that OID in the repository.)
     * The functionality vary vastly by the type of object requested.
     *
     * == {@link ShadowType} objects
     *
     * The resource object shadow may be retrieved from the repository, directly from the resource or a combination of both.
     * The retrieval may fail due to resource failure, network failure or similar external cases. The retrieval may also
     * take relatively long time (e.g. until it times out).
     *
     * === Options support
     *
     * [%autowidth]
     * [%header]
     * |===
     * | Option                     | Support
     * | retrieve                   | only as far as attributes are concerned - ignored e.g. for the associations (why?)
     * | resolve                    | do not use (effects are unclear)
     * | resolveNames               | do not use (effects are unclear)
     * | noFetch                    | yes
     * | raw                        | yes
     * | tolerateRawData            | do not use
     * | doNotDiscovery             | yes
     * | relationalValueSearchQuery | ignored
     * | allowNotFound              | partially supported
     * | readOnly                   | ignored (shadows are heavily updated after fetching from repo, anyway)
     * | pointInTimeType            | yes
     * | staleness                  | yes
     * | forceRefresh               | yes
     * | forceRetry                 | yes
     * | distinct                   | ignored
     * | attachDiagData             | do not use (effects are unclear)
     * | definitionProcessing       | ignored
     * | iterationMethod            | ignored
     * | executionPhase             | ignored
     * | errorHandling              | partially supported
     * |===
     *
     * === The `raw`, `noFetch`, and regular modes
     *
     * [%autowidth]
     * [%header]
     * |===
     * | Mode    | Shadow LC state set* | Shadow refresh | Read from resource | Classification   | Futurization (see below)
     * | raw     | no                   | none           | no                 | from repository  | no
     * | noFetch | yes                  | quick          | no                 | from repository  | yes
     * | regular | yes                  | depends        | depends            | depends          | yes
     * |===
     *
     * (*) This is `shadowLifecycleState` property that does not exist in the repository (yet). It is computed by
     * the implementation of this method.
     *
     * === Required resource capabilities
     *
     * The "read" capability of resource and relevant object type (if applicable) is checked. Operation fails if it is not
     * present or enabled. If "caching-only" is set, reading from resource is avoided.
     *
     * === Shadow refresh
     *
     * The shadow is refreshed after it's fetched from the repository in the following modes:
     *
     * [%autowidth]
     * [%header]
     * |===
     * | Condition                | Refresh mode
     * | resource in maintenance  | none (may change in future)
     * | forceRefresh option      | full
     * | forceRetry option        | full
     * | refreshOnRead is set     | full
     * | otherwise                | quick
     * |===
     *
     * - "Full refresh" is equivalent to {@link #refreshShadow(PrismObject, ProvisioningOperationOptions, Task, OperationResult)}.
     * - "Quick refresh" is a subset of it: deleting expired pending operations and deleting the expired dead shadow.
     * This may change in the future.
     *
     * === Avoiding reading from the resource
     *
     * Reading from the resource is avoided in specific conditions, namely if:
     *
     * . The resource or object type has "caching-only" read capability.
     * . Resource is in maintenance mode.
     * . A shadow is in specific near-birth or near-death state. See the implementation (`GetOperation`) for details.
     * . Using the cached version is required by the staleness and point-in-time options.
     *
     * In specific conditions the implementation may decide to handle the fact that the object does not exist on the resource.
     *
     * === Post-processing of the resource object fetched
     *
     * When a resource object is successfully read from the resource, the following should occur:
     *
     * . Shadow is classified - either if it was not classified yet, or the resource is in "development mode" (see below).
     * . Repository shadow is updated with known data obtained from the resource. This includes e.g. cached attributes.
     *
     * === Shadow futurization
     *
     * When `future` point of time is requested, the pending operations are applied to the last known state of the shadow.
     * (Fetched either from the resource or from repository.) The current implementation is not 100% correct, as it tries
     * to apply all attribute deltas even if the base object does not have all the attributes available.
     *
     * === Shadow fetch result specifics
     *
     * When the resource object is being fetched from the resource, and:
     *
     * . {@link CommunicationException} occurs, the status is `PARTIAL_ERROR` (and repo shadow is returned),
     * . resource is in maintenance, the status is `PARTIAL_ERROR` (and repo shadow is returned),
     * . {@link ObjectNotFoundException} occurs, the status is `HANDLED_ERROR` (and repo shadow with the `dead` flag is returned).
     * The reason is that the exception was, in fact, handled. The current state of the object is correctly reported.
     *
     * === Potential side effects
     *
     * . Quick or full shadow refresh - before the GET issued against resource (or after the repo load if noFetch is set).
     * The shadow may be even deleted by the refresh.
     * . Discovery process (an event is sent to the listener, typically to model).
     * . Shadow is updated with the information obtained from the resource: cached identifiers and/or other attributes,
     * probably `dead` and `exists` properties.
     * . ...
     *
     * TODO think about compatibility of these side effects with the limitations prescribed by development/execution mode.
     *
     * === Effects of development and execution mode
     *
     * The following configuration items should be respected:
     *
     * * `lifecycleState` in resource and/or object type - drives e.g. the classification process
     * * shadow production/non-production flag - also drives the classification
     *
     * TODO specify in mode details
     *
     * == ResourceType objects
     *
     * The returned object will conform to the following:
     *
     * [%autowidth]
     * [%header]
     * |===
     * | State/condition                        | Is expanded | Configuration is resolved | Has capabilities and schema | Is put into cache
     * | current version is in cache            | yes         | yes                       | yes (taken from cache)      | it is already there
     * | is abstract                            | yes         | no                        | if present in repo          | no
     * | is complete in repo                    | yes         | yes                       | yes (taken from repo)       | yes
     * | is incomplete in repo, noFetch = false | yes         | yes                       | yes (fetched)               | yes (if no problems)
     * | is incomplete in repo, noFetch = true  | yes         | yes                       | if present in repo          | no
     * | raw = true                             | no          | only the definitions      | if present in repo          | no
     * |===
     *
     * Notes:
     *
     * . If the current version of the object is in the resource cache, it is returned right from it.
     * . "Is expanded" = the references to super-resources are resolved.
     * . "Configuration is resolved" = definitions of individual configuration properties (from the connector schema)
     * are applied; also, the expressions in these properties are evaluated.
     * . "Has capabilities and schema" = whether native capabilities and schema information is present in the returned object.
     * "If present in repo" means that the information from the repository are returned.
     * . "Is put into cache" = whether the resource is put into the resource cache, so it can be cheaply retrieved afterwards.
     * . "Is complete in repo" means that both capabilities and schema are fetched - see
     * {@link ResourceTypeUtil#isComplete(ResourceType)}.
     *
     * === Options support
     *
     * [%autowidth]
     * [%header]
     * |===
     * | Option                     | Support
     * | retrieve                   | do not use (effects are unclear)
     * | resolve                    | do not use (effects are unclear)
     * | resolveNames               | do not use (effects are unclear)
     * | noFetch                    | yes
     * | raw                        | yes
     * | tolerateRawData            | do not use
     * | doNotDiscovery             | ignored
     * | relationalValueSearchQuery | ignored
     * | allowNotFound              | partially supported
     * | readOnly                   | yes
     * | pointInTimeType            | ignored
     * | staleness                  | ignored
     * | forceRefresh               | ignored
     * | forceRetry                 | ignored
     * | distinct                   | ignored
     * | attachDiagData             | do not use (effects are unclear)
     * | definitionProcessing       | ignored
     * | iterationMethod            | ignored
     * | executionPhase             | ignored
     * | errorHandling              | ignored
     * |===
     *
     * === Potential side effects
     *
     * . If a resource undergoes completion, the capabilities and/or schema are updated in the repository.
     * . The complete resource may be put into the cache.
     *
     * === Effects of development and execution mode
     *
     * They are none. The definition of the resource is the same, regardless of the mode we run in. For example, it is cached
     * regardless of the mode. However, the interpretation of the definition differs, but that is outside the scope of this
     * method.
     *
     * (This may change if we would e.g. apply some "patches" specific to individual modes.)
     *
     * == ConnectorType and other objects
     *
     * These objects are just retrieved from the repository. They are not treated in any special way now.
     *
     * === Potential side effects
     *
     * None.
     *
     * === Effects of development and execution mode
     *
     * None.
     *
     * == Notes
     *
     * . Concrete type of object (`ShadowType`, `ResourceType`, and so on) must be provided by the client.
     * Using generic `ObjectType` will not work.
     * . The operation result is cleaned up before returning.
     * . The fetch result ({@link ObjectType#getFetchResult()}) is stored into object being returned. It reflects the result
     * of the whole operation: fetching from the resource, if applicable, but also e.g. application of definitions to an
     * object retrieved in raw mode. Storing of "success" result may be skipped.
     *
     * == Limitations / Known issues
     *
     * . Getting shadows: Definitions of associations (identifiers) are not always refined (in noFetch mode).
     *
     * @param type the type (class) of object to get
     * @param oid OID of the object to get
     * @param parentResult parent OperationResult (in/out)
     * @return Object fetched from repository and/or resource
     *
     * @throws ObjectNotFoundException requested object (does not need to be the shadow we are looking for!) does not exist
     * @throws CommunicationException error communicating with the resource
     * @throws SchemaException error dealing with resource schema
     * @throws ConfigurationException wrong resource or connector configuration
     * @throws SecurityViolationException security violation while communicating with the connector
     * or processing provisioning policies
     * @throws IllegalArgumentException wrong OID format, etc.
     * @throws GenericConnectorException unknown connector framework error
     */
    @NotNull <T extends ObjectType> PrismObject<T> getObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /** A convenience method. */
    default @NotNull AbstractShadow getShadow(
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return AbstractShadow.of(
                getObject(ShadowType.class, oid, options, new ProvisioningOperationContext(), task, result));
    }

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    default @NotNull <T extends ObjectType> PrismObject<T> getObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return getObject(type, oid, options, new ProvisioningOperationContext(), task, parentResult);
    }

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
    <T extends ObjectType> String addObject(
            @NotNull PrismObject<T> object,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    default <T extends ObjectType> String addObject(
            @NotNull PrismObject<T> object,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        return addObject(object, scripts, options, new ProvisioningOperationContext(), task, parentResult);
    }

    /**
     * Fetches synchronization change events ({@link LiveSyncEvent}) from a resource and passes them into specified
     * {@link LiveSyncEventHandler}. Uses provided {@link LiveSyncTokenStorage} to get and update the token
     * that indicates the current position in the stream of live sync change events.
     *
     * It is typically invoked from a live sync activity (task).
     *
     * Notes regarding the `shadowCoordinates` parameter:
     *
     * * Resource OID is obligatory.
     * * If neither object class nor kind are specified, all object classes on the resource are synchronized
     * (if supported by the connector/resource).
     * * If object class name is specified (but kind and intent are not), the object class to synchronize is determined
     * using object class name.
     * * If both kind and intent are specified, object type is determined based on them. It is then checked against
     * object class name - the object class for given object type must match the specified object class name,
     * if it's provided.
     * * If only kind is specified (without intent), the default object type is found; and checked against
     * object class name just like above.
     *
     * See {@link ResourceSchemaUtil#findDefinitionForBulkOperation(ResourceType, ShadowKindType, String, QName)} for
     * the details.
     *
     * Note that it's not possible to specify intent without kind.
     * Also, `unknown` values for kind or intent are not supported.
     *
     * @param coordinates Where to attempt synchronization. See description above.
     * @param options Options driving the synchronization process (execution mode, batch size, ...)
     * @param tokenStorage Interface for getting and setting the token for the activity
     * @param handler Handler that processes live sync events
     * @param context Provisioning context used to pass information from upper layers
     * @param parentResult Parent OperationResult to where we write our own subresults.
     * @throws ObjectNotFoundException Some of key objects (resource, task, ...) do not exist
     * @throws CommunicationException Error communicating with the resource
     * @throws SchemaException Error dealing with resource schema
     * @throws SecurityViolationException Security violation while communicating with the connector
     *         or processing provisioning policies
     * @throws GenericConnectorException Unknown connector framework error
     */
    @NotNull SynchronizationResult synchronize(
            @NotNull ResourceOperationCoordinates coordinates,
            @Nullable LiveSyncOptions options,
            @NotNull LiveSyncTokenStorage tokenStorage,
            @NotNull LiveSyncEventHandler handler,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException;

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    default @NotNull SynchronizationResult synchronize(
            @NotNull ResourceOperationCoordinates coordinates,
            @Nullable LiveSyncOptions options,
            @NotNull LiveSyncTokenStorage tokenStorage,
            @NotNull LiveSyncEventHandler handler,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
        return synchronize(coordinates, options, tokenStorage, handler, new ProvisioningOperationContext(), task, parentResult);
    }

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
     * @param coordinates
     *
     *          What objects to synchronize. Note that although it is possible to specify other parameters in addition
     *          to resource OID (e.g. objectClass), these settings are not supported now.
     */
    void processAsynchronousUpdates(@NotNull ResourceOperationCoordinates coordinates,
            @NotNull AsyncUpdateEventHandler handler, @NotNull Task task, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException;

    /**
     * Searches for objects. Returns a list of objects that match search criteria.
     * The list is never null. It is empty if there are no matching objects.
     *
     * The method call should fail if object type is wrong. Should fail if the query is wrong, e.g. if it contains
     * a reference to an unknown attribute.
     *
     * == Processing of {@link ShadowType} objects
     *
     * === Options support
     *
     * [%autowidth]
     * [%header]
     * |===
     * | Option                     | Support
     * | retrieve                   | for attributes and associations
     * | resolve                    | do not use (effects are unclear)
     * | resolveNames               | do not use (effects are unclear)
     * | noFetch                    | yes
     * | raw                        | yes
     * | tolerateRawData            | do not use
     * | doNotDiscovery             | ignored (TODO check if it is really ignored)
     * | relationalValueSearchQuery | ignored
     * | allowNotFound              | ignored
     * | readOnly                   | ignored (shadows are heavily updated after fetching from repo, anyway)
     * | pointInTimeType            | ignored (no futurization)
     * | staleness                  | limited support
     * | forceRefresh               | ignored
     * | forceRetry                 | ignored
     * | distinct                   | supported for repository search, ignored for resource search
     * | attachDiagData             | do not use (effects are unclear)
     * | definitionProcessing       | ignored
     * | iterationMethod            | ignored
     * | executionPhase             | ignored
     * | errorHandling              | yes
     * |===
     *
     * === Specifying the "coordinates"
     *
     * When dealing with shadow queries in non-raw mode, there are the following requirements on the query:
     *
     * . there must be exactly one `resourceRef` obtainable from the query (i.e. present in the conjunction at the root level),
     * . and
     * .. either `kind` is specified (optionally with `intent` and/or `objectClass`),
     * .. or `objectClass` is specified (and both `kind` and `intent` are not specified).
     *
     * See also {@link ObjectQueryUtil#getOperationCoordinates(ObjectFilter)}.
     *
     * === Interpreting the query for on-resource search
     *
     * ==== If `kind` is specified
     * In this case, a specific _object type_ definition is looked up: either by `kind` and `intent` (if the latter is present),
     * or - if `intent` is not specified - by looking for a type marked as "default for its kind". The search is then executed
     * against this object type, taking its delineation (object class, base context, additional filters) into account.
     *
     * If `objectClass` is specified as well, it is just checked against the value in given object type definition. (Hence it
     * is not necessary nor advisable to specify object class when kind is specified.)
     *
     * ==== If only `objectClass` is specified
     * Here the implementation searches for all objects of given `objectClass`. However, there are few things that must be done
     * during the search, for example determining "attributes to get" i.e. what attributes should be explicitly requested.
     * These things depend on object class or object type definition. Therefore, the implementation has to look up appropriate
     * definition first.
     *
     * It does so by looking up raw or refined object class definition. Also, if there is a type definition marked as "default for
     * object class", it is used. However, even if such a type definition is found, the delineation (base context, filters, and
     * so on) are ignored: as stated above, all objects of given object class are searched for.
     *
     * See {@link ResourceSchemaUtil#findDefinitionForBulkOperation(ResourceType, ShadowKindType, String, QName)}
     * and {@link ResourceSchemaUtil#findObjectDefinitionPrecisely(ResourceType, ShadowKindType, String, QName)} for
     * the details.
     *
     * === Extra resource objects
     *
     * Note that when using kind and/or intent, the method may return objects that do not match these conditions. It depends
     * on how precise is the respective type definition, namely if its delineation: base context, hierarchy scope,
     * and/or object filter(s) precisely describe objects that belong to this type. If these features do not describe
     * the type adequately (e.g. if the classification has to be done using conditions written in Groovy), then the
     * returned set of objects may contain ones that are not of requested type. It is then the responsibility of the
     * caller to sort these extra objects out.
     *
     * === Potential side effects
     *
     * . Shadow is updated with the information obtained from the resource: cached identifiers and/or other attributes,
     * probably `dead` and `exists` properties.
     * . TODO
     *
     * === Effects of development and execution mode
     *
     * TODO
     *
     * == Processing of {@link ResourceType} objects
     *
     * Just like the {@link #getObject(Class, String, Collection, Task, OperationResult)} method, the resources returned from
     * this one are processed according to the inheritance rules, unless the `raw` mode is applied. Beware that - obviously -
     * the search query is applied to find the original resource objects, not the "processed" ones.
     *
     * - TODO expand this part
     * - TODO side effects
     *
     * === Effects of development and execution mode
     *
     * TODO
     *
     * == Fetch result
     *
     * The fetch result ({@link ObjectType#getFetchResult()}) should be present in objects returned (namely, if the processing
     * was not entirely successful). Beware that the details of storing it may differ between this method and
     * {@link #getObject(Class, String, Collection, Task, OperationResult)}.
     *
     * == Issues
     *
     * . Currently, we apply the object definition when fetching the object in raw mode.
     * This is a bit inconsistent with the processing in `get` operation, and also a bit unexpected.
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
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /** A convenience method. */
    default @NotNull SearchResultList<? extends AbstractShadow> searchShadows(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return searchObjects(ShadowType.class, query, options, empty(), task, result)
                .transform(AbstractShadow::of);
    }

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    @NotNull
    default <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return searchObjects(type, query, options, new ProvisioningOperationContext(), task, parentResult);
    }

    /**
     * Counts the objects of the respective type.
     *
     * == {@link ShadowType} objects
     *
     * These are counted in repository or on resource (if it has appropriate capability).
     * #TODO the description, including the options#
     *
     * === Potential side effects
     *
     * #TODO#
     *
     * === Effects of development and execution mode
     *
     * #TODO#
     *
     * == Other object types
     *
     * These are counted simply in the repository service.
     *
     * @param query See {@link #searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)} description.
     * @param options If noFetch or raw, we count only shadows from the repository.
     */
    <T extends ObjectType> Integer countObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    default <T extends ObjectType> Integer countObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return countObjects(type, query, options, new ProvisioningOperationContext(), task, parentResult);
    }

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
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    default <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ResultHandler<T> handler,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return searchObjectsIterative(type, query, options, handler, new ProvisioningOperationContext(), task, parentResult);
    }

    /** A convenience method. */
    @Experimental
    default <T extends ObjectType> SearchResultMetadata searchShadowsIterative(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ObjectHandler<AbstractShadow> handler,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return searchObjectsIterative(
                ShadowType.class,
                query,
                options,
                (object, lResult) -> handler.handle(AbstractShadow.of(object), lResult),
                new ProvisioningOperationContext(),
                task,
                result);
    }

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
     * TODO decide if it's OK that the callee modifies the `modifications` collection by adding side-effects provided
     *  by the connector.
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
    <T extends ObjectType> String modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException;

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    default <T extends ObjectType> String modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        return modifyObject(type, oid, modifications, scripts, options, new ProvisioningOperationContext(), task, parentResult);
    }

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
            OperationProvisioningScriptsType scripts, ProvisioningOperationContext context, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
            PolicyViolationException, ExpressionEvaluationException;

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    default <T extends ObjectType> PrismObject<T> deleteObject(Class<T> type, String oid, ProvisioningOperationOptions option,
            OperationProvisioningScriptsType scripts, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {
        return deleteObject(type, oid, option, scripts, new ProvisioningOperationContext(), task, parentResult);
    }

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
     * Test partial resource connection.
     *
     * Actually, this is a convenience method for calling {@link #testResource(PrismObject, Task, OperationResult)} with
     * the {@link ResourceTestOptions#testMode(ResourceTestOptions.TestMode)} set to {@link ResourceTestOptions.TestMode#PARTIAL}
     * (more detailed explanation is in the `PARTIAL` value documentation).
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
                ResourceTestOptions.partial(),
                task,
                parentResult);
    }

    /**
     * Discovers additional configuration properties. The resource object should contain minimal connector
     * configuration properties to connect to the resource, then use the connection
     * to discover additional configuration properties. Discovered configuration properties are returned
     * from this method as Prism properties wrapped in DiscoveredConfiguration.
     * DiscoveredConfiguration will be empty if it does not exist.
     *
     * @param resource resource with minimal connector configuration
     * @return Suggested configuration properties wrapped in DiscoveredConfiguration.
     */
    @NotNull DiscoveredConfiguration discoverConfiguration(
            @NotNull PrismObject<ResourceType> resource, @NotNull OperationResult parentResult);

    /**
     * The operation fetches the resource schema using the connector configuration from provided resource object.
     *
     * @param resource resource with connector configuration
     * @return Resource schema fetched by connector
     */
    @Nullable BareResourceSchema fetchSchema(@NotNull PrismObject<ResourceType> resource, @NotNull OperationResult parentResult);

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
    void refreshShadow(
            @NotNull PrismObject<ShadowType> shadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ObjectAlreadyExistsException, SecurityViolationException, ExpressionEvaluationException, EncryptionException;

    /**
     * This is method doesn't take {@link ProvisioningOperationContext} as a parameter to simplify backward compatibility for now.
     * It shouldn't be used, will be deprecated and removed after tests were updated accordingly.
     */
    default void refreshShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ObjectAlreadyExistsException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        refreshShadow(shadow, options, new ProvisioningOperationContext(), task, parentResult);
    }

    /**
     * Applies appropriate definition to the shadow/resource delta.
     */
    <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Task task, OperationResult parentResult)
        throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Applies appropriate definition to the shadow/resource delta. The provided object is used to get necessary information,
     * and it's (sometimes) updated as well. TODO this should be specified better
     */
    <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Objectable object, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException;

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
     * Determines effective shadow marks and policies, updating the shadow object in memory.
     *
     * This is to be used for externally acquired shadows, to make them look like all other shadows returned from the provisioning
     * module. Usually called along with {@link #determineShadowState(PrismObject, Task, OperationResult)}.
     *
     * TODO consider merging these methods
     *
     * The `isNew` parameter should be true if we believe the shadow does not exist yet (on the resource nor in the repo).
     *
     * TODO reconsider the `isNew` parameter
     */
    void updateShadowMarksAndPolicies(PrismObject<ShadowType> shadow, boolean isNew, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException;

    /**
     * Applies appropriate definition to the query.
     *
     * The query (for shadows) must comply with requirements similar to ones of {@link #searchObjects(Class, ObjectQuery,
     * Collection, Task, OperationResult)} method. See also {@link ResourceSchemaUtil#findObjectDefinitionPrecisely(ResourceType,
     * ShadowKindType, String, QName)}.
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
            ConstraintViolationConfirmer constraintViolationConfirmer,
            ConstraintsCheckingStrategyType strategy,
            ProvisioningOperationContext context,
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
     * Provides a sorter evaluator to the provisioning service.
     */
    void setSynchronizationSorterEvaluator(SynchronizationSorterEvaluator evaluator);

    /**
     * Classifies resource object, i.e. determines its kind and intent (not the tag!).
     *
     * . Ignores existing shadow classification.
     * . Invokes synchronization sorter, if it's defined for the resource.
     * . Even if new classification is determined, does _not_ update shadow in the repository.
     *
     * If you need to classify an unclassified (and not fetched yet) shadow, it may be generally better
     * to call {@link #getObject(Class, String, Collection, Task, OperationResult)} method. It attempts
     * to classify any unclassified objects retrieved.
     *
     * @param combinedObject Resource object combined with its shadow. Full "shadowization" is not required.
     */
    @NotNull ResourceObjectClassification classifyResourceObject(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @Nullable ObjectSynchronizationDiscriminatorType existingSorterResult,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException;

    /**
     * Generates shadow tag (for multi-account scenarios).
     *
     * . Ignores existing shadow tag.
     * . Does _not_ update shadow in the repository.
     */
    @Nullable String generateShadowTag(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectDefinition definition,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException;

    /**
     * Expands (in-memory) configuration object by (e.g.) resolving references to super/template objects, and so on.
     *
     * The expansion is done in-place i.e. the object is directly modified by the operation.
     * Provenance metadata are filled-in, see the description in {@link OriginMarker}.
     *
     * Assumes that there are no value metadata on entry!
     *
     * Currently implemented for resources.
     */
    @Experimental
    void expandConfigurationObject(
            @NotNull PrismObject<? extends ObjectType> configurationObject,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException;

    /**
     * Method create collection of capabilities which connector support.
     *
     * EXPERIMENTAL feature.
     *
     * @return Return supported operations for connector.
     */
    @Experimental
    @NotNull CapabilityCollectionType getNativeCapabilities(@NotNull String connOid, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException;

    /** Returns the default operation policy for given object type (if specified). */
    @Nullable ObjectOperationPolicyType getDefaultOperationPolicy(
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification typeIdentification,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException;
}
