/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.async.UcfAsyncUpdateChangeListener;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Connector instance configured for a specific resource.
 *
 * This is kind of connector facade. It is an API provided by the "Unified Connector Framework" to the midPoint provisioning
 * component. There is no associated SPI yet. That may come in the future when this interface stabilizes a bit.
 *
 * This interface provides an unified facade to a connector capabilities in the Unified Connector Framework interface.
 * The connector is configured to a specific resource instance and therefore can execute operations on resource.
 *
 * Calls to this interface always try to reach the resource and get the actual state on resource. The connectors are
 * not supposed to cache any information. Therefore the methods do not follow get/set java convention as the data
 * are not regular javabean properties.
 *
 * This instance must know the most current version of the resource schema, even with the refinements.
 *
 * @see ConnectorFactory
 *
 * @author Radovan Semancik
 *
 */
public interface ConnectorInstance {

    String OP_FETCH_OBJECT = ConnectorInstance.class.getName() + ".fetchObject";
    String OP_FETCH_RESOURCE_SCHEMA = ConnectorInstance.class.getName() + ".fetchResourceSchema";
    String OP_FETCH_CAPABILITIES = ConnectorInstance.class.getName() + ".fetchCapabilities";
    String OP_ADD_OBJECT = ConnectorInstance.class.getName() + ".addObject";
    String OP_MODIFY_OBJECT = ConnectorInstance.class.getName() + ".modifyObject";
    String OP_DELETE_OBJECT = ConnectorInstance.class.getName() + ".deleteObject";
    String OP_FETCH_CURRENT_TOKEN = ConnectorInstance.class.getName() + ".fetchCurrentToken";
    String OP_TEST = ConnectorInstance.class.getName() + ".test";
    String OP_DISCOVER_CONFIGURATION = ConnectorInstance.class.getName() + ".discoverConfiguration";
    String OP_SEARCH = ConnectorInstance.class.getName() + ".search";
    String OP_COUNT = ConnectorInstance.class.getName() + ".count";
    String OP_EXECUTE_SCRIPT = ConnectorInstance.class.getName() + ".executeScript";
    String OP_FETCH_CHANGES = ConnectorInstance.class.getName() + ".fetchChanges";
    String OPERATION_CONFIGURE = ConnectorInstance.class.getName() + ".configure";
    String OPERATION_INITIALIZE = ConnectorInstance.class.getName() + ".initialize";
    String OPERATION_DISPOSE = ConnectorInstance.class.getName() + ".dispose";

    /**
     * The connector instance will be configured to the state that it can
     * immediately access the resource. The resource configuration is provided as
     * a parameter to this method.
     *
     * This method may be invoked on connector instance that is already configured.
     * In that case re-configuration of the connector instance is requested.
     * The connector instance must be operational at all times, even during re-configuration.
     * Operations cannot be interrupted or refused due to missing configuration.
     *
     * Returns the same instance (`this`) to allow chained calls.
     *
     * @param configuration new connector configuration (prism container value)
     */
    ConnectorInstance configure(
            @NotNull ConnectorConfiguration configuration,
            @NotNull ConnectorConfigurationOptions options,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException;

    /**
     * Returns the current configuration (if the instance is configured), or {@code null} otherwise.
     * The object returned must be equivalent to the one passed to the {@link #configure(ConnectorConfiguration,
     * ConnectorConfigurationOptions, OperationResult)} method.
     */
    @Nullable ConnectorConfiguration getCurrentConfiguration();

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
     * Returns `this`.
     *
     * NOTE: the capabilities and schema that are used here are NOT necessarily those that are detected by the resource.
     *       The detected schema will come later. The schema here is the one that is stored in the resource
     *       definition (ResourceType). This may be schema that was detected previously. But it may also be a schema
     *       that was manually defined. This is needed to be passed to the connector in case that the connector
     *       cannot detect the schema and needs schema/capabilities definition to establish a connection.
     *       Most connectors will just ignore the schema and capabilities that are provided here.
     *       But some connectors may need it (e.g. CSV connector working with CSV file without a header).
     */
    @NotNull ConnectorInstance initialize(
            @Nullable NativeResourceSchema lastKnownResourceSchema,
            @Nullable CapabilityCollectionType lastKnownNativeCapabilities,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException;

    /**
     * Updates stored resource schema.
     */
    void updateSchema(NativeResourceSchema resourceSchema);

    /**
     * Retrieves native connector capabilities.
     *
     * The capabilities specify what the connector can do without any kind of simulation or other workarounds.
     * The set of capabilities may depend on the connector configuration (e.g. if a "disable" or password attribute
     * was specified in the configuration or not).
     *
     * TODO currently - for ConnId - this method also fetches the schema ... are we OK with this?
     *
     * - The `null` return value means the capabilities cannot be determined.
     * - Empty {@link CapabilityCollectionType} return value means there are no native capabilities.
     */
    @Nullable CapabilityCollectionType fetchCapabilities(OperationResult result)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException;

    /**
     * Returns capabilities natively supported by the connector.
     * Works also on unconfigured and uninitialized connectors.
     *
     * Limitations:
     *
     * - ignores the configuration even if present;
     * - does not take schema-derived capabilities (like activation, password, or paging) into account
     *
     * Can be seen as a simplified version of {@link #fetchCapabilities(OperationResult)}.
     *
     * @return Return supported operations for connector.
     */
    @NotNull CapabilityCollectionType getNativeCapabilities(OperationResult result)
            throws CommunicationException, GenericFrameworkException, ConfigurationException;

    /**
     * Retrieves the schema from the resource.
     *
     * The schema may be considered to be an XSD schema, but it is returned in a
     * "parsed" format and it is in fact a bit stricter and richer midPoint
     * schema.
     *
     * It may return null. Such case means that the schema cannot be determined.
     *
     * The method may return a schema that was fetched previously, e.g., if the fetch operation was executed
     * during connector initialization or when fetching native capabilities.
     *
     * @return Up-to-date resource schema. Only raw information should be there, no refinements. May be immutable.
     * @throws CommunicationException error in communication to the resource - nothing was fetched.
     */
    NativeResourceSchema fetchResourceSchema(@NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException;

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
     * TODO: object not found error
     *
     * @param resourceObjectIdentification objectClass+primary identifiers of the object to fetch
     * @return object fetched from the resource (no schema)
     * @throws CommunicationException error in communication to the resource - nothing was fetched.
     * @throws SchemaException error converting object from native (connector) format
     */
    UcfResourceObject fetchObject(
            @NotNull ResourceObjectIdentification.WithPrimary resourceObjectIdentification,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult result)
        throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
        SecurityViolationException, ConfigurationException;

    /**
     * Execute iterative search operation.
     *
     * This method will execute search operation on the resource and will pass
     * any objects that are found. A "handler" callback will be called for each
     * of the objects found (depending also on the error reporting method).
     *
     * The call to this method will return only after all the callbacks were
     * called, therefore it is not asynchronous in a strict sense.
     *
     * If nothing is found the method should behave as if there is an empty result set
     * (handler is never called) and the call should result in a success. So the ObjectNotFoundException
     * should be thrown only if there is an error in search parameters, e.g. if search base points to an non-existent object.
     *
     * BEWARE: The implementation of the handler should be consistent with the value of errorReportingMethod parameter:
     * if the method is FETCH_RESULT, the handler must be ready to process also incomplete/malformed objects (flagged
     * by appropriate fetchResult).
     *
     * *For clients*: Please create your own {@link OperationResult} in the `handler`, unless there's only a negligible
     * amount of processing. See {@link ResultHandler#providingOwnOperationResult(String)} docs for more information.
     *
     * *For implementors*: Please create a separate {@link OperationResult} object for each resource object found,
     * with the parent being the result passed from the caller to this method in `result` parameter (or its descendant).
     * Do not forget to keep the list of subresults short e.g. by calling {@link OperationResult#summarize()} method.
     *
     * @param objectDefinition Definition of the object class of the objects being searched for. May be class or type scoped.
     * @param query Object query to be used.
     * @param handler Handler that is called for each object found.
     * @param shadowItemsToReturn Attributes that are to be returned; TODO describe exact semantics
     * @param pagedSearchConfiguration Configuration (capability) describing how paged searches are to be done.
     * @param searchHierarchyConstraints Specifies in what parts of hierarchy the search should be executed.
     * @param errorReportingMethod How should errors during processing individual objects be reported.
     *                             If EXCEPTION (the default), an appropriate exception is thrown.
     *                             If FETCH_RESULT, the error is reported within the shadow affected.
     * @throws SchemaException if the search couldn't be executed because of a problem with the schema; or there is a schema
     *                         problem with an object returned (and error reporting method is EXCEPTION).
     * @throws ObjectNotFoundException if something from the search parameters refers non-existent object,
     *                                 e.g. if search base points to an non-existent object.
     */
    SearchResultMetadata search(
            @NotNull ResourceObjectDefinition objectDefinition,
            @Nullable ObjectQuery query,
            @NotNull UcfObjectHandler handler,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @Nullable PagedSearchCapabilityType pagedSearchConfiguration,
            @Nullable SearchHierarchyConstraints searchHierarchyConstraints,
            @Nullable UcfFetchErrorReportingMethod errorReportingMethod,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, SchemaException, SecurityViolationException,
            ObjectNotFoundException;

    /**
     * Counts objects on resource.
     *
     * This method will count objects on the resource by executing a paged search operation,
     * returning the "estimated objects count" information.
     *
     * If paging is not available, it throws an exception.
     */
    int count(ResourceObjectDefinition objectDefinition, ObjectQuery query,
            PagedSearchCapabilityType pagedSearchConfigurationType, UcfExecutionContext ctx, OperationResult result)
            throws CommunicationException, GenericFrameworkException, SchemaException, UnsupportedOperationException;

    /**
     * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
     *  and what were not determined.
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
     * @return created object attributes. May be null.
     * @throws SchemaException resource schema violation
     * @throws ObjectAlreadyExistsException object already exists on the resource
     */
    @NotNull UcfAddReturnValue addObject(
            @NotNull PrismObject<? extends ShadowType> object,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, SchemaException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException;

    /**
     * TODO the meaning of the `shadow` parameter ... it seems to be required e.g. by manual connectors
     *
     * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
     *  and what results are we not sure about.
     *
     * Returns modifications that the connector reports as executed as part of the operation.
     * These reported modifications MAY include the following:
     *
     * 1. Modifications that were requested and executed.
     * 2. Any other modifications that resulted from the operation, i.e. side effects. An example is UID change
     * stemming from object rename. Or DN change stemming from CN change.
     *
     * The exact content of the returned set depends on the actual connector used. Some connectors return requested
     * and executed operations, some do not. Also some connectors return side effects, some only part of them,
     * and some none of them.
     *
     * The exception should be thrown only if the connector is sure that nothing was done on the resource.
     * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
     * exception as something might have been done already.
     *
     * @throws ObjectAlreadyExistsException in case that the modified object conflicts with another existing object (e.g. while renaming an object)
     */
    @NotNull UcfModifyReturnValue modifyObject(
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            PrismObject<ShadowType> shadow,
            @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ConfigurationException;

    /**
     * Deletes the specified object.
     *
     * Currently, some implementations may accept secondary-only identification. Some (e.g. ConnId) may not.
     */
    @NotNull UcfDeleteResult deleteObject(
            @NotNull ResourceObjectIdentification<?> identification,
            @Nullable PrismObject<ShadowType> shadow,
            @NotNull UcfExecutionContext ctx,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException;

    Object executeScript(
            ExecuteProvisioningScriptOperation scriptOperation,
            UcfExecutionContext ctx,
            OperationResult result)
            throws CommunicationException, GenericFrameworkException;

    /**
     * Returns the latest token. In other words, returns a token that
     * corresponds to a current state of the resource. If fetchChanges
     * is immediately called with this token, nothing should be returned
     * (Figuratively speaking, neglecting concurrent resource modifications).
     */
    default UcfSyncToken fetchCurrentToken(
            ResourceObjectDefinition objectDefinition,
            @NotNull UcfExecutionContext ctx,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException {
        return null;
    }

    /**
     * Token may be null. That means "from the beginning of history".
     */
    UcfFetchChangesResult fetchChanges(
            @Nullable ResourceObjectDefinition objectDefinition,
            @Nullable UcfSyncToken lastToken,
            @Nullable ShadowItemsToReturn attrsToReturn,
            @Nullable Integer maxChanges,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull UcfLiveSyncChangeListener changeHandler,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException;

    /**
     * This method is not expected to throw any exceptions. It should record any issues to provided operation result
     * instead.
     *
     * Maybe this should be moved to ConnectorManager? In that way it can also test connector instantiation.
     */
    void test(OperationResult result);

    /**
     * Test the very minimal configuration set, which is usually just a set of mandatory configuration properties.
     * For most connectors this will be probably just a hostname, username and password.
     * For example, the test performs a simple connection and authentication to the resource.
     *
     * This method is not expected to throw any exceptions. It should record any issues to provided operation result
     * instead.
     */
    void testPartialConfiguration(OperationResult result);

    /**
     * Discovers additional configuration properties.
     * The connector is supposed to use minimal configuration to connect to the resource,
     * then use the connection to discover additional configuration properties.
     * Discovered configuration properties are returned from this method (if any).
     *
     * @return suggested attributes as prism properties
     */
    @NotNull Collection<PrismProperty<?>> discoverConfiguration(OperationResult result);

    /**
     * Dispose of the connector instance. Dispose is a brutal operation. Once the instance is disposed of, it cannot execute
     * any operation, it may not be (re)configured, any operation in progress may fail.
     * Dispose is usually invoked only if the system is shutting down. It is not invoked while the system is running, not even
     * if the connector instance configuration is out of date. There still may be some operations running. Disposing of the instance
     * will make those operations to fail.
     * MidPoint prefers to re-configure existing connector instance instead of disposing of it.
     * However, this approach may change in the future.
     */
    void dispose();

    /**
     * Listens for asynchronous updates. The connector should (indefinitely) listen for updates coming from the resource.
     * Caller thread can be used for listening to and processing of changes. Or, the connector is free to create its own
     * threads to listen and/or to execute changeListener in. But the control should return back to the caller after the
     * listening is over.
     *
     * This method should be called only in a single thread per the connector instance.
     *
     * @param changeListener Listener to invoke when a change arrives
     * @param canRunSupplier Supplier of "canRun" information. If it returns false we should stop listening.
     * @param result Operation result to use for listening for changes.
     *
     * @throws IllegalStateException If another listener is already present (or was successfully started in parallel).
     */
    default void listenForChanges(
            @NotNull UcfAsyncUpdateChangeListener changeListener,
            @NotNull Supplier<Boolean> canRunSupplier,
            @NotNull OperationResult result) throws SchemaException {
        throw new UnsupportedOperationException();
    }

    /** Get description usable e.g. in exception messages. */
    default String getHumanReadableDescription() {
        return toString();
    }
}
