/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.TokenUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.async.UcfAsyncUpdateChangeListener;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.AsynchronousOperationQueryable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AsyncUpdateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

/**
 * Serves as a facade for accessing resource objects.
 *
 * Responsibilities (mostly delegated):
 *
 * . protected objects
 * . simulated activation (delegated to {@link ActivationConverter})
 * . simulated entitlements (delegated to {@link EntitlementReader} and {@link EntitlementConverter})
 * . script execution
 * . avoid duplicate values
 * . attributes returned by default/not returned by default
 *
 * Limitations (applies to the whole package):
 *
 * . must NOT access repository (only indirectly via {@link ResourceObjectReferenceResolver}) - TODO re-check
 * . does not know about OIDs
 *
 * @author Katarina Valalikova
 * @author Radovan Semancik
 */
@Component
public class ResourceObjectConverter {

    private static final String DOT_CLASS = ResourceObjectConverter.class.getName() + ".";
    static final String OPERATION_MODIFY_ENTITLEMENT = DOT_CLASS + "modifyEntitlement";
    private static final String OPERATION_ADD_RESOURCE_OBJECT = DOT_CLASS + "addResourceObject";
    private static final String OPERATION_MODIFY_RESOURCE_OBJECT = DOT_CLASS + "modifyResourceObject";
    private static final String OPERATION_DELETE_RESOURCE_OBJECT = DOT_CLASS + "deleteResourceObject";
    private static final String OPERATION_REFRESH_OPERATION_STATUS = DOT_CLASS + "refreshOperationStatus";
    private static final String OPERATION_HANDLE_CHANGE = DOT_CLASS + "handleChange";
    static final String OP_SEARCH_RESOURCE_OBJECTS = DOT_CLASS + "searchResourceObjects";
    static final String OP_COUNT_RESOURCE_OBJECTS = DOT_CLASS + "countResourceObjects";

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectConverter.class);

    /**
     * Fetches the resource object by its primary identifier(s).
     * The resource must have "full" reading capability (i.e., no caching-only).
     */
    public @NotNull CompleteResourceObject fetchResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.WithPrimary primaryIdentification,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return ResourceObjectFetchOperation.execute(
                ctx, primaryIdentification, fetchAssociations, shadowItemsToReturn, result);
    }

    /**
     * "Completes" the provided "raw" resource object, i.e. executes
     * {@link ResourceObjectCompleter#completeResourceObject(ProvisioningContext, ExistingResourceObjectShadow, boolean, OperationResult)}.
     */
    public @NotNull CompleteResourceObject completeResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ExistingResourceObjectShadow rawObject,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        return ResourceObjectCompleter.completeResourceObject(ctx, rawObject, fetchAssociations, result);
    }

    /**
     * Obtains the resource object:
     *
     * - Tries to get the object directly if primary identifiers are present.
     * - If they are not, tries to search for the object by arbitrary/first secondary identifier.
     *
     * Both cases are handled by the resource, i.e. not by the repository.
     * The returned object is in the "initialized" state.
     *
     * TODO
     *  Currently seems to be used for entitlements search.
     *  It is questionable whether we should do the full processing of activation etc here.
     */
    public CompleteResourceObject locateResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification<?> identification,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException {
        if (identification instanceof ResourceObjectIdentification.WithPrimary primary) {
            return ResourceObjectFetchOperation.execute(
                    ctx, primary, fetchAssociations,
                    ctx.createItemsToReturn(),
                    result);
        } else if (identification instanceof ResourceObjectIdentification.SecondaryOnly secondaryOnly) {
            return ResourceObjectLocateOperation.execute(ctx, secondaryOnly, fetchAssociations, result);
        } else {
            throw new AssertionError(identification);
        }
    }

    public SearchResultMetadata searchResourceObjects(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectHandler resultHandler,
            @Nullable ObjectQuery query,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return ResourceObjectSearchOperation.execute(
                ctx, resultHandler, query, fetchAssociations, errorReportingMethod, result);
    }

    public Integer countResourceObjects(
            @NotNull ProvisioningContext ctx,
            @Nullable ObjectQuery query,
            @NotNull OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return new ResourceObjectCountOperation(ctx, query)
                .execute(parentResult);
    }

    public @NotNull ResourceObjectAddReturnValue addResourceObject(
            ProvisioningContext ctx,
            ResourceObjectShadow shadowToAdd,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            boolean skipExplicitUniquenessCheck,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_ADD_RESOURCE_OBJECT);
        try {
            return ResourceObjectAddOperation.execute(ctx, shadowToAdd, scripts, connOptions, skipExplicitUniquenessCheck, result);
        } catch (Throwable t) {
            result.recordException("Could not create object on the resource: " + t.getMessage(), t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Deletes the object on the resource.
     *
     * For the majority of cases - typically, for ConnId connectors - the primary identifier of the object is required.
     * (The operation will fail if it is not present.)
     *
     * TODO consider making this obligatory for all cases.
     */
    public @NotNull ResourceObjectDeleteResult deleteResourceObject(
            ProvisioningContext ctx,
            RepoShadow shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_RESOURCE_OBJECT);
        try {
            return ResourceObjectDeleteOperation.execute(ctx, shadow, scripts, connOptions, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    static void updateQuantum(
            ProvisioningContext ctx,
            ConnectorInstance connectorUsedForOperation,
            ResourceObjectOperationResult aResult,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ConnectorInstance readConnector = ctx.getConnector(ReadCapabilityType.class, result);
        if (readConnector != connectorUsedForOperation) {
            // Writing by different connector that we are going to use for reading: danger of quantum effects
            aResult.setQuantumOperation(true); // TODO this information is currently unused
        }
    }

    /**
     * Returns known executed deltas as reported by {@link ConnectorInstance#modifyObject(
     * ResourceObjectIdentification.WithPrimary, PrismObject, Collection, ConnectorOperationOptions,
     * SchemaAwareUcfExecutionContext, OperationResult)}.
     */
    public @NotNull ResourceObjectModifyReturnValue modifyResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            Collection<? extends ItemDelta<?, ?>> itemDeltas,
            XMLGregorianCalendar now,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OPERATION_MODIFY_RESOURCE_OBJECT)
                .addParam("repoShadow", repoShadow.getBean())
                .addArbitraryObjectAsParam("connOptions", connOptions)
                .addArbitraryObjectCollectionAsParam("itemDeltas", itemDeltas)
                .addArbitraryObjectAsContext("ctx", ctx)
                .build();

        try {
            return ResourceObjectModifyOperation.execute(ctx, repoShadow, scripts, connOptions, itemDeltas, now, result);
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        } finally {
            result.recordEnd();
        }
    }

    public LiveSyncToken fetchCurrentToken(
            ProvisioningContext ctx, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.trace("Fetching current sync token for {}", ctx);

        UcfSyncToken lastToken;
        ConnectorInstance connector = ctx.getConnector(LiveSyncCapabilityType.class, parentResult);
        try {
            lastToken = connector.fetchCurrentToken(ctx.getObjectDefinition(), ctx.getUcfExecutionContext(), parentResult);
        } catch (CommunicationException ex) {
            throw communicationException(ctx, connector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, connector, ex);
        }

        LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));

        computeResultStatusAndAsyncOpReference(parentResult);

        return TokenUtil.fromUcf(lastToken);
    }

    public UcfFetchChangesResult fetchChanges(
            ProvisioningContext ctx,
            @NotNull LiveSyncToken initialToken,
            @Nullable Integer maxChangesConfigured,
            ResourceObjectLiveSyncChangeListener outerListener,
            OperationResult gResult)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, GenericFrameworkException, ObjectNotFoundException, ExpressionEvaluationException {

        LOGGER.trace("START fetch changes from {}, objectClass: {}", initialToken, ctx.getObjectClassDefinition());
        ShadowItemsToReturn attrsToReturn;
        if (ctx.isWildcard()) {
            attrsToReturn = null;
        } else {
            attrsToReturn = ctx.createItemsToReturn();
        }

        ConnectorInstance connector = ctx.getConnector(LiveSyncCapabilityType.class, gResult);
        Integer maxChanges = getMaxChanges(maxChangesConfigured, ctx);

        AtomicInteger processed = new AtomicInteger(0);
        UcfLiveSyncChangeListener localListener = (ucfChange, lParentResult) -> {
            int changeNumber = processed.getAndIncrement();

            OperationResult lResult = lParentResult.subresult(OPERATION_HANDLE_CHANGE)
                    .setMinor()
                    .addParam("number", changeNumber)
                    .addParam("localSequenceNumber", ucfChange.getLocalSequenceNumber())
                    .addArbitraryObjectAsParam("primaryIdentifier", ucfChange.getPrimaryIdentifierValue())
                    .addArbitraryObjectAsParam("token", ucfChange.getToken()).build();

            try {
                ResourceObjectLiveSyncChange change =
                        new ResourceObjectLiveSyncChange(ucfChange, ctx, attrsToReturn);
                // Intentionally not initializing the change here. Let us be flexible and let the ultimate caller decide.
                return outerListener.onChange(change, lResult);
            } catch (Throwable t) {
                lResult.recordFatalError(t);
                throw t;
            } finally {
                lResult.computeStatusIfUnknown();
            }
        };

        // get changes from the connector
        UcfFetchChangesResult fetchChangesResult = connector.fetchChanges(
                ctx.getObjectDefinition(),
                TokenUtil.toUcf(initialToken),
                attrsToReturn,
                maxChanges,
                ctx.getUcfExecutionContext(),
                localListener,
                gResult);

        computeResultStatusAndAsyncOpReference(gResult);

        LOGGER.trace("END fetch changes ({} changes); interrupted = {}; all fetched = {}, final token = {}",
                processed.get(), !ctx.canRun(), fetchChangesResult.isAllChangesFetched(), fetchChangesResult.getFinalToken());

        return fetchChangesResult;
    }

    @Nullable
    private Integer getMaxChanges(@Nullable Integer maxChangesConfigured, ProvisioningContext ctx) {
        LiveSyncCapabilityType capability = ctx.getCapability(LiveSyncCapabilityType.class); // TODO what if it's disabled?
        if (capability != null) {
            if (Boolean.TRUE.equals(capability.isPreciseTokenValue())) {
                return maxChangesConfigured;
            } else {
                checkMaxChanges(maxChangesConfigured, "LiveSync capability has preciseTokenValue not set to 'true'");
                return null;
            }
        } else {
            // Is this possible?
            checkMaxChanges(maxChangesConfigured, "LiveSync capability is not found or disabled");
            return null;
        }
    }

    private void checkMaxChanges(Integer maxChangesFromTask, String reason) {
        if (maxChangesFromTask != null && maxChangesFromTask > 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot apply %s because %s", LiveSyncWorkDefinitionType.F_BATCH_SIZE.getLocalPart(), reason));
        }
    }

    public void listenForAsynchronousUpdates(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectAsyncChangeListener outerListener,
            @NotNull OperationResult parentResult) throws SchemaException,
            CommunicationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {

        LOGGER.trace("Listening for async updates in {}", ctx);
        ConnectorInstance connector = ctx.getConnector(AsyncUpdateCapabilityType.class, parentResult);

        UcfAsyncUpdateChangeListener innerListener = (ucfChange, listenerTask, listenerResult) -> {
            ResourceObjectAsyncChange change = new ResourceObjectAsyncChange(ucfChange, ctx);
            // Intentionally not initializing the change here. Let us be flexible and let the ultimate caller decide.
            outerListener.onChange(change, listenerTask, listenerResult);
        };
        connector.listenForChanges(innerListener, ctx::canRun, parentResult);

        LOGGER.trace("Finished listening for async updates");
    }

    public @Nullable OperationResultStatus refreshOperationStatus(
            ProvisioningContext ctx, RepoShadow shadow, String asyncRef, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_REFRESH_OPERATION_STATUS);
        try {

            ResourceType resource = ctx.getResource();

            // TODO: not really correct. But good enough for now.
            ConnectorInstance connector = ctx.getConnector(UpdateCapabilityType.class, result);

            OperationResultStatus status;
            if (connector instanceof AsynchronousOperationQueryable queryableConnector) {

                LOGGER.trace("PROVISIONING REFRESH operation ref={} on {}, object: {}",
                        asyncRef, resource, shadow);

                status = queryableConnector.queryOperationStatus(asyncRef, result);

                LOGGER.debug("PROVISIONING REFRESH ref={} successful on {} {}, returned status: {}",
                        asyncRef, resource, shadow, status);
            } else {
                LOGGER.trace("Ignoring refresh of {}, because the connector is not async operation queryable", shadow);
                status = null;
                result.setNotApplicable();
            }

            // Here was "updateQuantum" call (actually useless). When reviving this feature, we could consider placing
            // it here again.

            return status;

        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Does _not_ close the operation result, just sets its status (and async operation reference).
     */
    static void computeResultStatusAndAsyncOpReference(OperationResult result) {
        if (result.isInProgress()) {
            return;
        }
        OperationResultStatus status = OperationResultStatus.SUCCESS;
        String asyncRef = null;
        for (OperationResult subresult : result.getSubresults()) {
            if (OPERATION_MODIFY_ENTITLEMENT.equals(subresult.getOperation()) && subresult.isError()) {
                status = OperationResultStatus.PARTIAL_ERROR;
            } else if (subresult.isError()) {
                status = OperationResultStatus.FATAL_ERROR;
            } else if (subresult.isInProgress()) {
                status = OperationResultStatus.IN_PROGRESS;
                asyncRef = subresult.getAsynchronousOperationReference();
            }
        }
        result.setStatus(status);
        result.setAsynchronousOperationReference(asyncRef);
    }

    static ObjectAlreadyExistsException objectAlreadyExistsException(
            String messagePrefix, ProvisioningContext ctx, ConnectorInstance connector, ObjectAlreadyExistsException ex) {
        return new ObjectAlreadyExistsException(
                String.format("%sObject already exists on the resource (%s): %s",
                        messagePrefix, ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    static GenericConnectorException genericConnectorException(
            ProvisioningContext ctx, ConnectorInstance connector, GenericFrameworkException ex) {
        return new GenericConnectorException(
                String.format("Generic error in connector (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    static CommunicationException communicationException(
            ProvisioningContext ctx, ConnectorInstance connector, CommunicationException ex) {
        return new CommunicationException(
                String.format("Error communicating with the resource (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    static ConfigurationException configurationException(
            ProvisioningContext ctx, ConnectorInstance connector, ConfigurationException ex) {
        return new ConfigurationException(
                String.format("Configuration error (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }
}
