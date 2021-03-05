/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.*;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.async.UcfAsyncUpdateChangeListener;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.*;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 * Responsibilities:
 *
 * 1. protected objects
 * 2. simulated activation (delegated to ActivationConverter)
 * 3. script execution
 * 4. avoid duplicate values
 * 5. attributes returned by default/not returned by default
 *
 * Limitations:
 *
 * 1. must NOT access repository (only indirectly via {@link ResourceObjectReferenceResolver})
 * 2. does not know about OIDs
 *
 * @author Katarina Valalikova
 * @author Radovan Semancik
 *
 */
@Component
public class ResourceObjectConverter {

    private static final String DOT_CLASS = ResourceObjectConverter.class.getName() + ".";
    private static final String OPERATION_MODIFY_ENTITLEMENT = DOT_CLASS + "modifyEntitlement";
    private static final String OPERATION_ADD_RESOURCE_OBJECT = DOT_CLASS + "addResourceObject";
    private static final String OPERATION_MODIFY_RESOURCE_OBJECT = DOT_CLASS + "modifyResourceObject";
    private static final String OPERATION_DELETE_RESOURCE_OBJECT = DOT_CLASS + "deleteResourceObject";
    private static final String OPERATION_REFRESH_OPERATION_STATUS = DOT_CLASS + "refreshOperationStatus";
    private static final String OPERATION_HANDLE_CHANGE = DOT_CLASS + "handleChange";

    @Autowired private EntitlementConverter entitlementConverter;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private ResourceObjectReferenceResolver resourceObjectReferenceResolver;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private Tracer tracer;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ResourceObjectsLocalBeans localBeans;
    @Autowired private CommonBeans commonBeans;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectConverter.class);

    public static final String FULL_SHADOW_KEY = ResourceObjectConverter.class.getName()+".fullShadow";

    public @NotNull PrismObject<ShadowType> getResourceObject(ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers, boolean fetchAssociations, OperationResult parentResult)
                    throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
                    SecurityViolationException, GenericConnectorException, ExpressionEvaluationException {

        LOGGER.trace("Getting resource object {}", identifiers);

        AttributesToReturn attributesToReturn = ProvisioningUtil.createAttributesToReturn(ctx);

        PrismObject<ShadowType> resourceShadow = fetchResourceObject(ctx, identifiers,
                attributesToReturn, fetchAssociations, parentResult);            // todo consider whether it is always necessary to fetch the entitlements

        LOGGER.trace("Got resource object\n{}", resourceShadow.debugDumpLazily());

        return resourceShadow;

    }

    /**
     * Tries to get the object directly if primary identifiers are present. Tries to search for the object if they are not.
     */
    public PrismObject<ShadowType> locateResourceObject(ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers, OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, GenericConnectorException, ExpressionEvaluationException {

        LOGGER.trace("Locating resource object {}", identifiers);

        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, parentResult);

        AttributesToReturn attributesToReturn = ProvisioningUtil.createAttributesToReturn(ctx);

        if (hasAllIdentifiers(identifiers, ctx.getObjectClassDefinition())) {
            return fetchResourceObject(ctx, identifiers,
                    attributesToReturn, true, parentResult);    // todo consider whether it is always necessary to fetch the entitlements
        } else {
            // Search
            Collection<? extends RefinedAttributeDefinition> secondaryIdentifierDefs = ctx.getObjectClassDefinition().getSecondaryIdentifiers();
            // Assume single secondary identifier for simplicity
            if (secondaryIdentifierDefs.size() > 1) {
                throw new UnsupportedOperationException("Composite secondary identifier is not supported yet");
            } else if (secondaryIdentifierDefs.isEmpty()) {
                throw new SchemaException("No secondary identifier defined, cannot search");
            }
            RefinedAttributeDefinition<String> secondaryIdentifierDef = secondaryIdentifierDefs.iterator().next();
            ResourceAttribute<?> secondaryIdentifier = null;
            for (ResourceAttribute<?> identifier: identifiers) {
                if (identifier.getElementName().equals(secondaryIdentifierDef.getItemName())) {
                    secondaryIdentifier = identifier;
                }
            }
            if (secondaryIdentifier == null) {
                throw new SchemaException("No secondary identifier present, cannot search. Identifiers: "+identifiers);
            }

            final ResourceAttribute<?> finalSecondaryIdentifier = secondaryIdentifier;

            List<PrismPropertyValue<String>> secondaryIdentifierValues = (List) secondaryIdentifier.getValues();
            PrismPropertyValue<String> secondaryIdentifierValue;
            if (secondaryIdentifierValues.size() > 1) {
                throw new IllegalStateException("Secondary identifier has more than one value: " + secondaryIdentifier.getValues());
            } else if (secondaryIdentifierValues.size() == 1) {
                secondaryIdentifierValue = secondaryIdentifierValues.get(0).clone();
            } else {
                secondaryIdentifierValue = null;
            }
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .itemWithDef(secondaryIdentifierDef, ShadowType.F_ATTRIBUTES, secondaryIdentifierDef.getItemName()).eq(secondaryIdentifierValue)
                    .build();
            final Holder<PrismObject<ShadowType>> shadowHolder = new Holder<>();
            ObjectHandler handler = ucfObject -> {
                if (!shadowHolder.isEmpty()) {
                    throw new IllegalStateException("More than one value found for secondary identifier "+finalSecondaryIdentifier);
                }
                shadowHolder.setValue(ucfObject.getResourceObject());
                return true;
            };
            try {
                connector.search(ctx.getObjectClassDefinition(), query, handler, attributesToReturn, null, null,
                        UcfFetchErrorReportingMethod.EXCEPTION, ctx, parentResult);
                if (shadowHolder.isEmpty()) {
                    throw new ObjectNotFoundException("No object found for secondary identifier "+secondaryIdentifier);
                }
                PrismObject<ShadowType> shadow = shadowHolder.getValue();
                postProcessResourceObjectRead(ctx, shadow, true, parentResult);
                LOGGER.trace("Located resource object {}", shadow);
                return shadow;
            } catch (GenericFrameworkException e) {
                throw new GenericConnectorException(e.getMessage(), e);
            }
        }
    }

    private boolean hasAllIdentifiers(Collection<? extends ResourceAttribute<?>> attributes,
            RefinedObjectClassDefinition objectClassDefinition) {
        Collection<? extends RefinedAttributeDefinition> identifierDefs = objectClassDefinition.getPrimaryIdentifiers();
        for (RefinedAttributeDefinition identifierDef: identifierDefs) {
            boolean found = false;
            for(ResourceAttribute<?> attribute: attributes) {
                if (attribute.getElementName().equals(identifierDef.getItemName()) && !attribute.isEmpty()) {
                    found = true;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public AsynchronousOperationReturnValue<PrismObject<ShadowType>> addResourceObject(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow, OperationProvisioningScriptsType scripts, ConnectorOperationOptions connOptions,
            boolean skipExplicitUniquenessCheck, OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException,
                    ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_ADD_RESOURCE_OBJECT);
        boolean specificExceptionRecorded = false;
        try {
            ResourceType resource = ctx.getResource();

            LOGGER.trace("Adding resource object {}", shadow);

            // We might be modifying the shadow (e.g. for simulated capabilities). But we do not want the changes
            // to propagate back to the calling code. Hence the clone.
            PrismObject<ShadowType> shadowClone = shadow.clone();
            ShadowType shadowType = shadowClone.asObjectable();

            Collection<ResourceAttribute<?>> resourceAttributesAfterAdd;

            if (ProvisioningUtil.isProtectedShadow(ctx.getProtectedAccountPatterns(expressionFactory, parentResult), shadowClone, matchingRuleRegistry,
                    relationRegistry)) {
                LOGGER.error("Attempt to add protected shadow " + shadowType + "; ignoring the request");
                SecurityViolationException e = new SecurityViolationException("Cannot get protected shadow " + shadowType);
                result.recordFatalError(e);
                throw e;
            }

            if (!skipExplicitUniquenessCheck) {
                checkForAddConflicts(ctx, shadow, result);
            }

            checkForCapability(ctx, CreateCapabilityType.class, result);

            executeProvisioningScripts(ctx, ProvisioningOperationTypeType.ADD, BeforeAfterType.BEFORE, scripts, result);

            entitlementConverter.processEntitlementsAdd(ctx, shadowClone);

            ConnectorInstance connector = ctx.getConnector(CreateCapabilityType.class, result);
            AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> connectorAsyncOpRet;
            try {
                LOGGER.debug("PROVISIONING ADD operation on resource {}\n ADD object:\n{}\n",
                        resource, shadowType.asPrismObject().debugDumpLazily());

                new ActivationConverter(ctx, commonBeans)
                        .transformActivationOnAdd(shadowType, result);

                connectorAsyncOpRet = connector.addObject(shadowClone, ctx, result);
                resourceAttributesAfterAdd = connectorAsyncOpRet.getReturnValue();

                LOGGER.debug("PROVISIONING ADD successful, returned attributes:\n{}",
                        SchemaDebugUtil.prettyPrintLazily(resourceAttributesAfterAdd));

                // Be careful not to apply this to the cloned shadow. This needs to be propagated
                // outside this method.
                applyAfterOperationAttributes(shadow, resourceAttributesAfterAdd);
            } catch (CommunicationException ex) {
                result.recordFatalError(
                        "Could not create object on the resource. Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
                specificExceptionRecorded = true;
                throw new CommunicationException("Error communicating with the connector " + connector + ": "
                        + ex.getMessage(), ex);
            } catch (GenericFrameworkException ex) {
                result.recordFatalError("Could not create object on the resource. Generic error in connector: " + ex.getMessage(), ex);
                specificExceptionRecorded = true;
                throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
            } catch (ObjectAlreadyExistsException ex) {
                result.recordFatalError(
                        "Could not create object on the resource. Object already exists on the resource: " + ex.getMessage(), ex);
                specificExceptionRecorded = true;
                throw new ObjectAlreadyExistsException("Object already exists on the resource: " + ex.getMessage(), ex);
            }

            // Execute entitlement modification on other objects (if needed)
            executeEntitlementChangesAdd(ctx, shadowClone, scripts, connOptions, result);

            LOGGER.trace("Added resource object {}", shadow);

            executeProvisioningScripts(ctx, ProvisioningOperationTypeType.ADD, BeforeAfterType.AFTER, scripts, result);

            computeResultStatus(result);

            AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncOpRet = AsynchronousOperationReturnValue.wrap(shadow, result);
            asyncOpRet.setOperationType(connectorAsyncOpRet.getOperationType());
            return asyncOpRet;
        } catch (Throwable t) {
            if (!specificExceptionRecorded) {
                result.recordFatalError(t);
            }
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Special case for multi-connectors (e.g. semi-manual connectors). There is a possibility that the object
     * which we want to add is already present in the backing store. In case of manual provisioning the resource
     * itself will not indicate "already exist" error. We have to explicitly check for that.
     */
    private void checkForAddConflicts(ProvisioningContext ctx, PrismObject<ShadowType> shadow, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, ObjectNotFoundException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Checking for add conflicts for {}", ShadowUtil.shortDumpShadow(shadow));
        }
        PrismObject<ShadowType> existingObject;
        ConnectorInstance readConnector = null;
        try {
            ConnectorInstance createConnector = ctx.getConnector(CreateCapabilityType.class, result);
            readConnector = ctx.getConnector(ReadCapabilityType.class, result);
            if (readConnector == createConnector) {
                // Same connector for reading and creating. We assume that the connector can check uniqueness itself.
                // No need to check explicitly. We will gladly skip the check, as the check may be additional overhead
                // that we normally do not need or want.
                return;
            }
            ResourceObjectIdentification identification = ResourceObjectIdentification.createFromShadow(ctx.getObjectClassDefinition(), shadow.asObjectable());

            existingObject = readConnector.fetchObject(identification, null, ctx, result);
        } catch (ObjectNotFoundException e) {
            // This is OK
            result.muteLastSubresultError();
            return;
        } catch (CommunicationException ex) {
            result.recordFatalError(
                    "Could not create object on the resource. Error communicating with the connector " + readConnector + ": " + ex.getMessage(), ex);
            throw new CommunicationException("Error communicating with the connector " + readConnector + ": "
                    + ex.getMessage(), ex);
        } catch (GenericFrameworkException ex) {
            result.recordFatalError("Could not create object on the resource. Generic error in connector: " + ex.getMessage(), ex);
            throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
        } catch (Throwable e){
            result.recordFatalError(e);
            throw e;
        }
        if (existingObject == null) {
            LOGGER.trace("No add conflicts for {}", shadow);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Detected add conflict for {}, conflicting shadow: {}", ShadowUtil.shortDumpShadow(shadow), ShadowUtil.shortDumpShadow(existingObject));
            }
            LOGGER.trace("Conflicting shadow:\n{}", existingObject.debugDumpLazily(1));
            ObjectAlreadyExistsException e = new ObjectAlreadyExistsException("Object " + ShadowUtil.shortDumpShadow(shadow) +
                    " already exists in the snapshot of " + ctx.getResource() + " as " + ShadowUtil.shortDumpShadow(existingObject));
            result.recordFatalError(e);
            throw e;
        }
    }

    public AsynchronousOperationResult deleteResourceObject(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            OperationProvisioningScriptsType scripts, ConnectorOperationOptions connOptions, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_RESOURCE_OBJECT);

        LOGGER.trace("Deleting resource object {}", shadow);

        checkForCapability(ctx, DeleteCapabilityType.class, result);

        Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil
                .getAllIdentifiers(shadow);

        if (ProvisioningUtil.isProtectedShadow(ctx.getProtectedAccountPatterns(expressionFactory, parentResult), shadow, matchingRuleRegistry, relationRegistry)) {
            LOGGER.error("Attempt to delete protected resource object " + ctx.getObjectClassDefinition() + ": "
                    + identifiers + "; ignoring the request");
            SecurityViolationException e = new SecurityViolationException("Cannot delete protected resource object "
                    + ctx.getObjectClassDefinition() + ": " + identifiers);
            result.recordFatalError(e);
            throw e;
        }

        executeProvisioningScripts(ctx, ProvisioningOperationTypeType.DELETE, BeforeAfterType.BEFORE, scripts, result);

        // Execute entitlement modification on other objects (if needed)
        executeEntitlementChangesDelete(ctx, shadow, scripts, connOptions, result);

        ConnectorInstance connector = ctx.getConnector(DeleteCapabilityType.class, result);
        AsynchronousOperationResult connectorAsyncOpRet;
        try {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "PROVISIONING DELETE operation on {}\n DELETE object, object class {}, identified by:\n{}",
                        ctx.getResource(), shadow.asObjectable().getObjectClass(),
                        SchemaDebugUtil.debugDump(identifiers));
            }

            if (!ResourceTypeUtil.isDeleteCapabilityEnabled(ctx.getResource())){
                UnsupportedOperationException e = new UnsupportedOperationException("Resource does not support 'delete' operation");
                result.recordFatalError(e);
                throw e;
            }

            connectorAsyncOpRet = connector.deleteObject(ctx.getObjectClassDefinition(), shadow, identifiers, ctx, result);

        } catch (ObjectNotFoundException ex) {
            result.recordFatalError("Can't delete object " + shadow
                    + ". Reason: " + ex.getMessage(), ex);
            throw new ObjectNotFoundException("An error occurred while deleting resource object " + shadow
                    + " with identifiers " + identifiers + ": " + ex.getMessage(), ex);
        } catch (CommunicationException ex) {
            result.recordFatalError(
                    "Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
            throw new CommunicationException("Error communicating with the connector " + connector + ": "
                    + ex.getMessage(), ex);
        } catch (ConfigurationException ex) {
            result.recordFatalError(
                    "Configuration error in connector " + connector + ": " + ex.getMessage(), ex);
            throw new ConfigurationException("Configuration error in connector " + connector + ": "
                    + ex.getMessage(), ex);
        } catch (ExpressionEvaluationException ex) {
            result.recordFatalError(
                    "Expression error while setting up the resource: " + ex.getMessage(), ex);
            throw new ExpressionEvaluationException("Expression error while setting up the resource: "
                    + ex.getMessage(), ex);
        } catch (GenericFrameworkException ex) {
            result.recordFatalError("Generic error in connector: " + ex.getMessage(), ex);
            throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
        } catch (SecurityViolationException | PolicyViolationException | RuntimeException | Error ex) {
            result.recordFatalError(ex);
            throw ex;
        }

        LOGGER.trace("Deleted resource object {}", shadow);

        executeProvisioningScripts(ctx, ProvisioningOperationTypeType.DELETE, BeforeAfterType.AFTER, scripts, result);

        computeResultStatus(result);
        LOGGER.debug("PROVISIONING DELETE result: {}", result.getStatus());

        AsynchronousOperationResult aResult = AsynchronousOperationResult.wrap(result);
        updateQuantum(ctx, connector, aResult, parentResult);
        if (connectorAsyncOpRet != null) {
            aResult.setOperationType(connectorAsyncOpRet.getOperationType());
        }
        return aResult;
    }

    private void updateQuantum(ProvisioningContext ctx, ConnectorInstance connectorUsedForOperation, AsynchronousOperationResult aResult, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ConnectorInstance readConnector = ctx.getConnector(ReadCapabilityType.class, parentResult);
        if (readConnector != connectorUsedForOperation) {
            // Writing by different connector that we are going to use for reading: danger of quantum effects
            aResult.setQuantumOperation(true);
        }
    }

    public AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> modifyResourceObject(
            ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            Collection<? extends ItemDelta<?, ?>> itemDeltas,
            XMLGregorianCalendar now,
            OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
                        SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_MODIFY_RESOURCE_OBJECT);

        try {
            LOGGER.trace("Modifying resource object {}, deltas:\n{}", repoShadow, DebugUtil.debugDumpLazily(itemDeltas, 1));

            RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
            Collection<Operation> operations = new ArrayList<>();

            Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil.getAllIdentifiers(repoShadow);

            if (ProvisioningUtil.isProtectedShadow(ctx.getProtectedAccountPatterns(expressionFactory, result), repoShadow, matchingRuleRegistry,
                    relationRegistry)) {
                if (hasChangesOnResource(itemDeltas)) {
                    LOGGER.error("Attempt to modify protected resource object {}: {}", objectClassDefinition, identifiers);
                    SecurityViolationException e = new SecurityViolationException("Cannot modify protected resource object "
                            + objectClassDefinition + ": " + identifiers);
                    result.recordFatalError(e);
                    throw e;
                } else {
                    // Return immediately. This structure of the code makes sure that we do not execute any
                    // resource operation for protected account even if there is a bug in the code below.
                    LOGGER.trace("No resource modifications for protected resource object {}: {}; skipping",
                            objectClassDefinition, identifiers);
                    result.recordNotApplicableIfUnknown();
                    return AsynchronousOperationReturnValue.wrap(null, result);
                }
            }

            boolean hasVolatilityTriggerModification = false;
            boolean hasResourceModification = false;
            for (ItemDelta<?, ?> modification: itemDeltas) {
                ItemPath path = modification.getPath();
                QName firstPathName = path.firstName();
                if (ProvisioningUtil.isAttributeModification(firstPathName)) {
                    hasResourceModification = true;
                    QName attrName = path.rest().firstNameOrFail();
                    RefinedAttributeDefinition<Object> attrDef = ctx.getObjectClassDefinition().findAttributeDefinition(attrName);
                    if (attrDef.isVolatilityTrigger()) {
                        LOGGER.trace("Will pre-read and re-read object because volatility trigger attribute {} has changed", attrName);
                        hasVolatilityTriggerModification = true;
                        break;
                    }
                } else if (ProvisioningUtil.isNonAttributeResourceModification(firstPathName)) {
                    hasResourceModification = true;
                }
            }

            if (!hasResourceModification) {
                // Quit early, so we avoid potential pre-read and other processing when there is no point of doing so.
                // Also the read may fail which may invoke consistency mechanism which will complicate the situation.
                LOGGER.trace("No resource modification found for {}, skipping", identifiers);
                result.recordNotApplicableIfUnknown();
                return AsynchronousOperationReturnValue.wrap(null, result);
            }

            /*
             *  State of the shadow before execution of the deltas - e.g. with original attributes, as it may be recorded in such a way in
             *  groups of which this account is a member of. (In case of object->subject associations.)
             *
             *  This is used when the resource does NOT provide referential integrity by itself. This is e.g. the case of OpenDJ with default
             *  settings.
             *
             *  On the contrary, AD and OpenDJ with referential integrity plugin do provide automatic referential integrity, so this feature is
             *  not needed.
             *
             *  We decide based on setting of explicitReferentialIntegrity in association definition.
             */
            collectAttributeAndEntitlementChanges(ctx, itemDeltas, operations, repoShadow, result);

            PrismObject<ShadowType> preReadShadow = null;
            Collection<PropertyModificationOperation> sideEffectOperations = null;
            AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyAsyncRet = null;

            if (hasVolatilityTriggerModification || ResourceTypeUtil.isAvoidDuplicateValues(ctx.getResource()) || isRename(ctx, operations)) {
                // We need to filter out the deltas that add duplicate values or remove values that are not there
                LOGGER.trace("Pre-reading resource shadow");
                preReadShadow = preReadShadow(ctx, identifiers, operations, true, repoShadow, result);  // yes, we need associations here
                LOGGER.trace("Pre-read object (straight from the resource):\n{}", DebugUtil.debugDumpLazily(preReadShadow, 1));
                // If there are pending changes in the shadow then we have to apply to pre-read object.
                // The pre-read object may be out of date (e.g. in case of semi-manual connectors).
                // In that case we may falsely remove some of the modifications. E.g. in case that
                // account is enabled, then disable and then enabled again. If backing store still
                // has the account as enabled, then the last enable operation would be ignored.
                // No case is created to re-enable the account. And the account stays disabled at the end.
                preReadShadow = shadowCaretaker.applyPendingOperations(ctx, repoShadow, preReadShadow, true, now);
                LOGGER.trace("Pre-read object (applied pending operations):\n{}", DebugUtil.debugDumpLazily(preReadShadow, 1));
            }

            if (!operations.isEmpty()) {

                if (InternalsConfig.isSanityChecks()) {
                    // MID-3964
                    if (MiscUtil.hasDuplicates(operations)) {
                        throw new SchemaException("Duplicated changes: "+operations);
                    }
                }

                // Execute primary ICF operation on this shadow
                modifyAsyncRet = executeModify(ctx, (preReadShadow == null ? repoShadow.clone() : preReadShadow), identifiers, operations, scripts, result, connOptions);
                if (modifyAsyncRet != null) {
                    sideEffectOperations = modifyAsyncRet.getReturnValue();
                }

            } else {
                // We have to check BEFORE we add script operations, otherwise the check would be pointless
                LOGGER.trace("No modifications for connector object specified. Skipping processing of subject executeModify.");
            }

            Collection<PropertyDelta<PrismPropertyValue>> sideEffectDeltas = convertToPropertyDelta(sideEffectOperations);

            /*
             *  State of the shadow after execution of the deltas - e.g. with new DN (if it was part of the delta), because this one should be recorded
             *  in groups of which this account is a member of. (In case of object->subject associations.)
             */
            PrismObject<ShadowType> shadowAfter = preReadShadow == null ? repoShadow.clone() : preReadShadow.clone();
            for (ItemDelta itemDelta : itemDeltas) {
                itemDelta.applyTo(shadowAfter);
            }

            PrismObject<ShadowType> postReadShadow = null;
            if (hasVolatilityTriggerModification) {
                // There may be other changes that were not detected by the connector. Re-read the object and compare.
                LOGGER.trace("Post-reading resource shadow");
                postReadShadow = preReadShadow(ctx, identifiers, operations, true, repoShadow, result);
                LOGGER.trace("Post-read object:\n{}", DebugUtil.debugDumpLazily(postReadShadow));
                ObjectDelta<ShadowType> resourceShadowDelta = preReadShadow.diff(postReadShadow);
                LOGGER.trace("Determined side-effect changes by old-new diff:\n{}", resourceShadowDelta.debugDumpLazily());
                for (ItemDelta modification: resourceShadowDelta.getModifications()) {
                    if (modification.getParentPath().startsWithName(ShadowType.F_ATTRIBUTES) && !ItemDeltaCollectionsUtil
                            .hasEquivalent(itemDeltas, modification)) {
                        ItemDeltaCollectionsUtil.merge(sideEffectDeltas, modification);
                    }
                }
                LOGGER.trace("Side-effect changes after merging with old-new diff:\n{}", DebugUtil.debugDumpLazily(sideEffectDeltas));
            }

            Collection<? extends ItemDelta> allDeltas = new ArrayList<>();
            ((Collection)allDeltas).addAll(itemDeltas);
            ((Collection)allDeltas).addAll(sideEffectDeltas);

            // Execute entitlement modification on other objects (if needed)
            executeEntitlementChangesModify(ctx,
                    preReadShadow == null ? repoShadow : preReadShadow,
                    postReadShadow == null ? shadowAfter : postReadShadow,
                    scripts, connOptions, allDeltas, result);

            if (!sideEffectDeltas.isEmpty()) {
                if (preReadShadow != null) {
                    PrismUtil.setDeltaOldValue(preReadShadow, sideEffectDeltas);
                } else {
                    PrismUtil.setDeltaOldValue(repoShadow, sideEffectDeltas);
                }
            }

            LOGGER.trace("Modification side-effect changes:\n{}", DebugUtil.debugDumpLazily(sideEffectDeltas));
            LOGGER.trace("Modified resource object {}", repoShadow);

            computeResultStatus(result);

            AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> aResult = AsynchronousOperationReturnValue.wrap(sideEffectDeltas, result);
            if (modifyAsyncRet != null) {
                aResult.setOperationType(modifyAsyncRet.getOperationType());
            }
            return aResult;
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        } finally {
            result.recordEnd();
        }
    }

    private Collection<PropertyDelta<PrismPropertyValue>> convertToPropertyDelta(
            Collection<PropertyModificationOperation> sideEffectOperations) {
        Collection<PropertyDelta<PrismPropertyValue>> sideEffectDeltas = new ArrayList<>();
        if (sideEffectOperations != null) {
            for (PropertyModificationOperation mod : sideEffectOperations){
                sideEffectDeltas.add(mod.getPropertyDelta());
            }
        }

        return sideEffectDeltas;
    }

    @SuppressWarnings("rawtypes")
    private AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> executeModify(ProvisioningContext ctx,
            PrismObject<ShadowType> currentShadow, Collection<? extends ResourceAttribute<?>> identifiers,
            Collection<Operation> operations, OperationProvisioningScriptsType scripts, OperationResult result,
            ConnectorOperationOptions connOptions)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            PolicyViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        Collection<PropertyModificationOperation> sideEffectChanges = new HashSet<>();

        RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
        if (operations.isEmpty()) {
            LOGGER.trace("No modifications for resource object. Skipping modification.");
            return null;
        } else {
            LOGGER.trace("Resource object modification operations: {}", operations);
        }

        checkForCapability(ctx, UpdateCapabilityType.class, result);

        if (!ShadowUtil.hasPrimaryIdentifier(identifiers, objectClassDefinition)) {
            Collection<? extends ResourceAttribute<?>> primaryIdentifiers = resourceObjectReferenceResolver.resolvePrimaryIdentifier(ctx, identifiers, "modification of resource object "+identifiers, result);
            if (primaryIdentifiers == null || primaryIdentifiers.isEmpty()) {
                throw new ObjectNotFoundException("Cannot find repository shadow for identifiers "+identifiers);
            }
            Collection allIdentifiers = new ArrayList();
            allIdentifiers.addAll(identifiers);
            allIdentifiers.addAll(primaryIdentifiers);
            identifiers = allIdentifiers;
        }

        executeProvisioningScripts(ctx, ProvisioningOperationTypeType.MODIFY, BeforeAfterType.BEFORE, scripts, result);

        // Invoke connector operation
        ConnectorInstance connector = ctx.getConnector(UpdateCapabilityType.class, result);
        AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> connectorAsyncOpRet = null;
        try {

            if (ResourceTypeUtil.isAvoidDuplicateValues(ctx.getResource())) {

                if (currentShadow == null) {
                    LOGGER.trace("Fetching shadow for duplicate filtering");
                    currentShadow = preReadShadow(ctx, identifiers, operations, false, currentShadow, result);
                }

                if (currentShadow == null) {

                    LOGGER.debug("We do not have pre-read shadow, skipping duplicate filtering");

                } else {

                    LOGGER.trace("Filtering out duplicate values");

                    Collection<Operation> filteredOperations = new ArrayList<>(operations.size());
                    for (Operation origOperation: operations) {
                        if (origOperation instanceof PropertyModificationOperation) {
                            PropertyModificationOperation modificationOperation = (PropertyModificationOperation)origOperation;
                            PropertyDelta<?> propertyDelta = modificationOperation.getPropertyDelta();
                            PropertyDelta<?> filteredDelta = ProvisioningUtil.narrowPropertyDelta(propertyDelta, currentShadow,
                                    modificationOperation.getMatchingRuleQName(), matchingRuleRegistry);
                            if (filteredDelta != null && !filteredDelta.isEmpty()) {
                                if (propertyDelta == filteredDelta) {
                                    filteredOperations.add(origOperation);
                                } else {
                                    PropertyModificationOperation newOp = new PropertyModificationOperation<>(filteredDelta);
                                    newOp.setMatchingRuleQName(modificationOperation.getMatchingRuleQName());
                                    filteredOperations.add(newOp);
                                }
                            } else {
                                LOGGER.trace("Filtering out modification {} because it has empty delta after narrow", propertyDelta);
                            }
                        }
                    }
                    if (filteredOperations.isEmpty()) {
                        LOGGER.debug("No modifications for connector object specified (after filtering). Skipping processing.");
                        result.recordSuccess();
                        return null;
                    }
                    operations = filteredOperations;
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "PROVISIONING MODIFY operation on {}\n MODIFY object, object class {}, identified by:\n{}\n changes:\n{}",
                        ctx.getResource(), objectClassDefinition.getHumanReadableName(),
                        SchemaDebugUtil.debugDump(identifiers, 1), SchemaDebugUtil.debugDump(operations, 1));
            }

            if (!ResourceTypeUtil.isUpdateCapabilityEnabled(ctx.getResource())) {
                if (operations == null || operations.isEmpty()){
                    LOGGER.debug("No modifications for connector object specified (after filtering). Skipping processing.");
                    result.recordSuccess();
                    return null;
                }
                UnsupportedOperationException e = new UnsupportedOperationException("Resource does not support 'update' operation");
                result.recordFatalError(e);
                throw e;
            }

            Collection<ResourceAttribute<?>> identifiersWorkingCopy = cloneIdentifiers(identifiers);            // because identifiers can be modified e.g. on rename operation
            List<Collection<Operation>> operationsWaves = sortOperationsIntoWaves(operations, objectClassDefinition);
            LOGGER.trace("Operation waves: {}", operationsWaves.size());
            boolean inProgress = false;
            String asynchronousOperationReference = null;
            for (Collection<Operation> operationsWave : operationsWaves) {
                operationsWave = convertToReplaceAsNeeded(ctx, operationsWave, identifiersWorkingCopy, objectClassDefinition, result);

                if (!operationsWave.isEmpty()) {
                    ResourceObjectIdentification identification = ResourceObjectIdentification.create(objectClassDefinition, identifiersWorkingCopy);
                    connectorAsyncOpRet = connector.modifyObject(identification, currentShadow, operationsWave, connOptions, ctx, result);
                    Collection<PropertyModificationOperation> sideEffects = connectorAsyncOpRet.getReturnValue();
                    if (sideEffects != null) {
                        sideEffectChanges.addAll(sideEffects);
                        // we accept that one attribute can be changed multiple times in sideEffectChanges; TODO: normalize
                    }
                    if (connectorAsyncOpRet.isInProgress()) {
                        inProgress = true;
                        asynchronousOperationReference = connectorAsyncOpRet.getOperationResult().getAsynchronousOperationReference();
                    }
                }
            }

            LOGGER.debug("PROVISIONING MODIFY successful, inProgress={}, side-effect changes {}", inProgress, DebugUtil.debugDumpLazily(sideEffectChanges));

            if (inProgress) {
                result.recordInProgress();
                result.setAsynchronousOperationReference(asynchronousOperationReference);
            }

        } catch (ObjectNotFoundException ex) {
            result.recordFatalError("Object to modify not found: " + ex.getMessage(), ex);
            throw new ObjectNotFoundException("Object to modify not found: " + ex.getMessage(), ex);
        } catch (CommunicationException ex) {
            result.recordFatalError(
                    "Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
            throw new CommunicationException("Error communicating with connector " + connector + ": "
                    + ex.getMessage(), ex);
        } catch (GenericFrameworkException ex) {
            result.recordFatalError(
                    "Generic error in the connector " + connector + ": " + ex.getMessage(), ex);
            throw new GenericConnectorException("Generic error in connector connector " + connector + ": "
                    + ex.getMessage(), ex);
        } catch (ObjectAlreadyExistsException ex) {
            result.recordFatalError("Conflict during modify: " + ex.getMessage(), ex);
            throw new ObjectAlreadyExistsException("Conflict during modify: " + ex.getMessage(), ex);
        } catch (SchemaException | ConfigurationException | ExpressionEvaluationException | SecurityViolationException | PolicyViolationException | RuntimeException | Error ex) {
            result.recordFatalError(ex.getMessage(), ex);
            throw ex;
        }

        executeProvisioningScripts(ctx, ProvisioningOperationTypeType.MODIFY, BeforeAfterType.AFTER, scripts, result);

        AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> asyncOpRet = AsynchronousOperationReturnValue.wrap(sideEffectChanges, result);
        if (connectorAsyncOpRet != null) {
            asyncOpRet.setOperationType(connectorAsyncOpRet.getOperationType());
        }
        return asyncOpRet;
    }

    private Collection<Operation> convertToReplaceAsNeeded(ProvisioningContext ctx, Collection<Operation> operationsWave, Collection<ResourceAttribute<?>> identifiers, RefinedObjectClassDefinition objectClassDefinition, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
        Collection<RefinedAttributeDefinition> readReplaceAttributes = determineReadReplace(ctx, operationsWave, objectClassDefinition);
        LOGGER.trace("Read+Replace attributes: {}", readReplaceAttributes);
        if (!readReplaceAttributes.isEmpty()) {
            AttributesToReturn attributesToReturn = new AttributesToReturn();
            attributesToReturn.setReturnDefaultAttributes(false);
            attributesToReturn.setAttributesToReturn(readReplaceAttributes);
            // TODO eliminate this fetch if this is first wave and there are no explicitly requested attributes
            // but make sure currentShadow contains all required attributes
            LOGGER.trace("Fetching object because of READ+REPLACE mode");
            PrismObject<ShadowType> currentShadow = fetchResourceObject(ctx, identifiers, attributesToReturn, false, result);
            operationsWave = convertToReplace(ctx, operationsWave, currentShadow, false);
        }
        UpdateCapabilityType updateCapability = ctx.getEffectiveCapability(UpdateCapabilityType.class);
        if (updateCapability != null) {
            AttributeContentRequirementType attributeContentRequirement = updateCapability.getAttributeContentRequirement();
            if (AttributeContentRequirementType.ALL.equals(attributeContentRequirement)) {
                LOGGER.trace("AttributeContentRequirement: {} for {}", attributeContentRequirement, ctx.getResource());
                PrismObject<ShadowType> currentShadow = fetchResourceObject(ctx, identifiers, null, false, result);
                if (currentShadow == null) {
                    throw new SystemException("Attribute content requirement set for resource "+ctx.toHumanReadableDescription()+", but read of shadow returned null, identifiers: "+identifiers);
                }
                operationsWave = convertToReplace(ctx, operationsWave, currentShadow, true);
            }
        }
        return operationsWave;
    }

    @SuppressWarnings("rawtypes")
    private PrismObject<ShadowType> preReadShadow(
            ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers,
            Collection<Operation> operations,
            boolean fetchEntitlements,
            PrismObject<ShadowType> repoShadow,
            OperationResult parentResult)
                    throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> currentShadow;
        List<RefinedAttributeDefinition> neededExtraAttributes = new ArrayList<>();
        for (Operation operation : operations) {
            RefinedAttributeDefinition rad = getRefinedAttributeDefinitionIfApplicable(operation, ctx.getObjectClassDefinition());
            if (rad != null && (!rad.isReturnedByDefault() || rad.getFetchStrategy() == AttributeFetchStrategyType.EXPLICIT)) {
                neededExtraAttributes.add(rad);
            }
        }

        AttributesToReturn attributesToReturn = new AttributesToReturn();
        attributesToReturn.setAttributesToReturn(neededExtraAttributes);
        try {
            currentShadow = fetchResourceObject(ctx, identifiers,
                attributesToReturn, fetchEntitlements, parentResult);
        } catch (ObjectNotFoundException e) {
            // This may happen for semi-manual connectors that are not yet up to date.
            // No big deal. We will have to work without it.
            LOGGER.warn("Cannot pre-read shadow {}, it is probably not present in the {}. Skipping pre-read.", identifiers, ctx.getResource());
            return null;
        }
        if (repoShadow != null) {
            currentShadow.setOid(repoShadow.getOid());
        }
        currentShadow.asObjectable().setName(new PolyStringType(ShadowUtil.determineShadowName(currentShadow)));
        return currentShadow;
    }

    private Collection<RefinedAttributeDefinition> determineReadReplace(ProvisioningContext ctx, Collection<Operation> operations, RefinedObjectClassDefinition objectClassDefinition) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Collection<RefinedAttributeDefinition> retval = new ArrayList<>();
        for (Operation operation : operations) {
            RefinedAttributeDefinition rad = getRefinedAttributeDefinitionIfApplicable(operation, objectClassDefinition);
            if (rad != null && isReadReplaceMode(ctx, rad, objectClassDefinition) && operation instanceof PropertyModificationOperation) {        // third condition is just to be sure
                PropertyDelta propertyDelta = ((PropertyModificationOperation) operation).getPropertyDelta();
                if (propertyDelta.isAdd() || propertyDelta.isDelete()) {
                    retval.add(rad);        // REPLACE operations are not needed to be converted to READ+REPLACE
                }
            }
        }
        return retval;
    }

    private boolean isReadReplaceMode(ProvisioningContext ctx, RefinedAttributeDefinition rad, RefinedObjectClassDefinition objectClassDefinition) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (rad.getReadReplaceMode() != null) {
            return rad.getReadReplaceMode();
        }
        // READ+REPLACE mode is if addRemoveAttributeCapability is NOT present. Try to determine from the capabilities. We may still need to force it.

        UpdateCapabilityType updateCapabilityType = objectClassDefinition.getEffectiveCapability(UpdateCapabilityType.class, ctx.getResource());
        if (updateCapabilityType == null) {
            // Strange. We are going to update, but we cannot update? Anyway, let it go, it should throw an error on a more appropriate place.
            return false;
        }
        if (BooleanUtils.isTrue(updateCapabilityType.isDelta())) {
            return false;
        }
        boolean readReplace;
        if (updateCapabilityType.isAddRemoveAttributeValues() == null) {
            // Deprecated. Legacy.
            readReplace = objectClassDefinition.getEffectiveCapability(AddRemoveAttributeValuesCapabilityType.class, ctx.getResource()) == null;
        } else {
            readReplace = !updateCapabilityType.isAddRemoveAttributeValues();
        }
        if (readReplace) {
            LOGGER.trace("Read+replace mode is forced because {} does not support addRemoveAttributeValues", ctx.getResource());
        }
        return readReplace;
    }

    private RefinedAttributeDefinition getRefinedAttributeDefinitionIfApplicable(Operation operation, RefinedObjectClassDefinition objectClassDefinition) {
        if (operation instanceof PropertyModificationOperation) {
            PropertyDelta propertyDelta = ((PropertyModificationOperation) operation).getPropertyDelta();
            if (isAttributeDelta(propertyDelta)) {
                QName attributeName = propertyDelta.getElementName();
                return objectClassDefinition.findAttributeDefinition(attributeName);
            }
        }
        return null;
    }

    /**
     *  Converts ADD/DELETE VALUE operations into REPLACE VALUE, if needed
     */
    private Collection<Operation> convertToReplace(ProvisioningContext ctx, Collection<Operation> operations, PrismObject<ShadowType> currentShadow, boolean requireAllAttributes) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        List<Operation> retval = new ArrayList<>(operations.size());
        for (Operation operation : operations) {
            if (operation instanceof PropertyModificationOperation) {
                PropertyDelta propertyDelta = ((PropertyModificationOperation) operation).getPropertyDelta();
                if (isAttributeDelta(propertyDelta)) {
                    QName attributeName = propertyDelta.getElementName();
                    RefinedAttributeDefinition rad = ctx.getObjectClassDefinition().findAttributeDefinition(attributeName);
                    if ((requireAllAttributes || isReadReplaceMode(ctx, rad, ctx.getObjectClassDefinition())) && (propertyDelta.isAdd() || propertyDelta.isDelete())) {
                        PropertyModificationOperation newOp = convertToReplace(propertyDelta, currentShadow, rad.getMatchingRuleQName());
                        newOp.setMatchingRuleQName(((PropertyModificationOperation) operation).getMatchingRuleQName());
                        retval.add(newOp);
                        continue;
                    }

                }
            }
            retval.add(operation);        // for yet-unprocessed operations
        }
        if (requireAllAttributes) {
            for (ResourceAttribute<?> currentAttribute : ShadowUtil.getAttributes(currentShadow)) {
                if (!containsDelta(operations, currentAttribute.getElementName())) {
                    RefinedAttributeDefinition rad = ctx.getObjectClassDefinition().findAttributeDefinition(currentAttribute.getElementName());
                    if (rad.canModify()) {
                        PropertyDelta resultingDelta = prismContext.deltaFactory().property().create(currentAttribute.getPath(), currentAttribute.getDefinition());
                        resultingDelta.setValuesToReplace(currentAttribute.getClonedValues());
                        retval.add(new PropertyModificationOperation(resultingDelta));
                    }
                }
            }
        }
        return retval;
    }

    private boolean containsDelta(Collection<Operation> operations, ItemName attributeName) {
        for (Operation operation : operations) {
            if (operation instanceof PropertyModificationOperation) {
                PropertyDelta propertyDelta = ((PropertyModificationOperation) operation).getPropertyDelta();
                if (isAttributeDelta(propertyDelta) && QNameUtil.match(attributeName, propertyDelta.getElementName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private PropertyModificationOperation convertToReplace(PropertyDelta<?> propertyDelta, PrismObject<ShadowType> currentShadow, QName matchingRuleQName) throws SchemaException {
        if (propertyDelta.isReplace()) {
            // this was probably checked before
            throw new IllegalStateException("PropertyDelta is both ADD/DELETE and REPLACE");
        }
        Collection<PrismPropertyValue> currentValues = new ArrayList<>();
        if (currentShadow != null) {
            // let's extract (parent-less) current values
            PrismProperty<?> currentProperty = currentShadow.findProperty(propertyDelta.getPath());
            if (currentProperty != null) {
                for (PrismPropertyValue currentValue : currentProperty.getValues()) {
                    currentValues.add(currentValue.clone());
                }
            }
        }
        final MatchingRule matchingRule;
        if (matchingRuleQName != null) {
            ItemDefinition def = propertyDelta.getDefinition();
            QName typeName;
            if (def != null) {
                typeName = def.getTypeName();
            } else {
                typeName = null;        // we'll skip testing rule fitness w.r.t type
            }
            matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, typeName);
        } else {
            matchingRule = null;
        }
        Comparator comparator = (Comparator<PrismPropertyValue<?>>) (o1, o2) -> {
            //noinspection unchecked
            if (o1.equals(o2, EquivalenceStrategy.REAL_VALUE, matchingRule)) {
                return 0;
            } else {
                return 1;
            }
        };
        // add values that have to be added
        if (propertyDelta.isAdd()) {
            for (PrismPropertyValue valueToAdd : propertyDelta.getValuesToAdd()) {
                if (!PrismValueCollectionsUtil.containsValue(currentValues, valueToAdd, comparator)) {
                    currentValues.add(valueToAdd.clone());
                } else {
                    LOGGER.warn("Attempting to add a value of {} that is already present in {}: {}",
                            valueToAdd, propertyDelta.getElementName(), currentValues);
                }
            }
        }
        // remove values that should not be there
        if (propertyDelta.isDelete()) {
            for (PrismPropertyValue valueToDelete : propertyDelta.getValuesToDelete()) {
                Iterator<PrismPropertyValue> iterator = currentValues.iterator();
                boolean found = false;
                while (iterator.hasNext()) {
                    PrismPropertyValue pValue = iterator.next();
                    LOGGER.trace("Comparing existing {} to about-to-be-deleted {}, matching rule: {}", pValue, valueToDelete, matchingRule);
                    if (comparator.compare(pValue, valueToDelete) == 0) {
                        LOGGER.trace("MATCH! compared existing {} to about-to-be-deleted {}", pValue, valueToDelete);
                        iterator.remove();
                        found = true;
                    }
                }
                if (!found) {
                    LOGGER.warn("Attempting to remove a value of {} that is not in {}: {}",
                            valueToDelete, propertyDelta.getElementName(), currentValues);
                }
            }
        }
        PropertyDelta resultingDelta = prismContext.deltaFactory().property().create(propertyDelta.getPath(), propertyDelta.getPropertyDefinition());
        resultingDelta.setValuesToReplace(currentValues);
        return new PropertyModificationOperation(resultingDelta);
    }

    private List<Collection<Operation>> sortOperationsIntoWaves(Collection<Operation> operations, RefinedObjectClassDefinition objectClassDefinition) {
        TreeMap<Integer,Collection<Operation>> waves = new TreeMap<>();    // operations indexed by priority
        List<Operation> others = new ArrayList<>();                    // operations executed at the end (either non-priority ones or non-attribute modifications)
        for (Operation operation : operations) {
            RefinedAttributeDefinition rad = getRefinedAttributeDefinitionIfApplicable(operation, objectClassDefinition);
            if (rad != null && rad.getModificationPriority() != null) {
                putIntoWaves(waves, rad.getModificationPriority(), operation);
                continue;
            }
            others.add(operation);
        }
        // computing the return value
        List<Collection<Operation>> retval = new ArrayList<>(waves.size()+1);
        Map.Entry<Integer,Collection<Operation>> entry = waves.firstEntry();
        while (entry != null) {
            retval.add(entry.getValue());
            entry = waves.higherEntry(entry.getKey());
        }
        retval.add(others);
        return retval;
    }

    private void putIntoWaves(Map<Integer, Collection<Operation>> waves, Integer key, Operation operation) {
        Collection<Operation> wave = waves.get(key);
        if (wave == null) {
            wave = new ArrayList<>();
            waves.put(key, wave);
        }
        wave.add(operation);
    }

    private Collection<ResourceAttribute<?>> cloneIdentifiers(Collection<? extends ResourceAttribute<?>> identifiers) {
        Collection<ResourceAttribute<?>> retval = new HashSet<>(identifiers.size());
        for (ResourceAttribute<?> identifier : identifiers) {
            retval.add(identifier.clone());
        }
        return retval;
    }

    private boolean isRename(ProvisioningContext ctx, Collection<Operation> modifications) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        for (Operation op : modifications){
            if (!(op instanceof PropertyModificationOperation)) {
                continue;
            }

            if (isIdentifierDelta(ctx, ((PropertyModificationOperation)op).getPropertyDelta())) {
                return true;
            }
        }
        return false;
    }

    private <T> boolean isIdentifierDelta(ProvisioningContext ctx, PropertyDelta<T> propertyDelta) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        return ctx.getObjectClassDefinition().isPrimaryIdentifier(propertyDelta.getElementName()) ||
                ctx.getObjectClassDefinition().isSecondaryIdentifier(propertyDelta.getElementName());
    }

    private PrismObject<ShadowType> executeEntitlementChangesAdd(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            OperationProvisioningScriptsType scripts, ConnectorOperationOptions connOptions,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap = new HashMap<>();

        shadow = entitlementConverter.collectEntitlementsAsObjectOperationInShadowAdd(ctx, roMap, shadow, parentResult);

        executeEntitlements(ctx, roMap, connOptions, parentResult);

        return shadow;
    }

    private PrismObject<ShadowType> executeEntitlementChangesModify(ProvisioningContext ctx, PrismObject<ShadowType> subjectShadowBefore,
            PrismObject<ShadowType> subjectShadowAfter,
            OperationProvisioningScriptsType scripts, ConnectorOperationOptions connOptions, Collection<? extends ItemDelta> subjectDeltas, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap = new HashMap<>();

        LOGGER.trace("executeEntitlementChangesModify, old shadow:\n{}", subjectShadowBefore.debugDumpLazily(1));

        for (ItemDelta subjectDelta : subjectDeltas) {
            ItemPath subjectItemPath = subjectDelta.getPath();

            if (ShadowType.F_ASSOCIATION.equivalent(subjectItemPath)) {
                ContainerDelta<ShadowAssociationType> containerDelta = (ContainerDelta<ShadowAssociationType>)subjectDelta;
                subjectShadowAfter = entitlementConverter.collectEntitlementsAsObjectOperation(ctx, roMap, containerDelta,
                        subjectShadowBefore, subjectShadowAfter, parentResult);

            } else {

                ContainerDelta<ShadowAssociationType> associationDelta = prismContext.deltaFactory().container().createDelta(ShadowType.F_ASSOCIATION, subjectShadowBefore.getDefinition());
                PrismContainer<ShadowAssociationType> associationContainer = subjectShadowBefore.findContainer(ShadowType.F_ASSOCIATION);
                if (associationContainer == null || associationContainer.isEmpty()) {
                    LOGGER.trace("No shadow association container in old shadow. Skipping processing entitlements change for {}.", subjectItemPath);
                    continue;
                }
                LOGGER.trace("Processing association container in old shadow for {}:\n{}", subjectItemPath, associationContainer.debugDumpLazily(1));

                // Delete + re-add association values that should ensure correct functioning in case of rename
                // This has to be done only for associations that require explicit referential integrity.
                // For these that do not, it is harmful, so it must be skipped.
                for (PrismContainerValue<ShadowAssociationType> associationValue : associationContainer.getValues()) {
                    QName associationName = associationValue.asContainerable().getName();
                    if (associationName == null) {
                        throw new IllegalStateException("No association name in " + associationValue);
                    }
                    RefinedAssociationDefinition associationDefinition = ctx.getObjectClassDefinition().findAssociationDefinition(associationName);
                    if (associationDefinition == null) {
                        throw new IllegalStateException("No association definition for " + associationValue);
                    }
                    if (!associationDefinition.requiresExplicitReferentialIntegrity()) {
                        continue;
                    }
                    QName valueAttributeName = associationDefinition.getResourceObjectAssociationType().getValueAttribute();
                    if (!ShadowUtil.matchesAttribute(subjectItemPath, valueAttributeName)) {
                        continue;
                    }
                    if (isRenameReal(subjectShadowBefore, subjectShadowAfter, subjectItemPath)) {
                        LOGGER.trace("Processing association {} on rename", associationName);
                        associationDelta.addValuesToDelete(associationValue.clone());
                        associationDelta.addValuesToAdd(associationValue.clone());
                    } else {
                        LOGGER.trace("NOT processing association {} because the rename is phantom", associationName);
                    }
                }
                LOGGER.trace("Resulting association delta for {}:\n{}", subjectItemPath, associationDelta.debugDumpLazily(1));
                if (!associationDelta.isEmpty()) {
                    entitlementConverter.collectEntitlementsAsObjectOperation(ctx, roMap, associationDelta, subjectShadowBefore, subjectShadowAfter, parentResult);
                }

            }
        }

        executeEntitlements(ctx, roMap, connOptions, parentResult);

        return subjectShadowAfter;
    }

    private <T> boolean isRenameReal(PrismObject<ShadowType> objectBefore, PrismObject<ShadowType> objectAfter, ItemPath itemPath) throws SchemaException {
        PrismProperty<T> propertyBefore = objectBefore.findProperty(itemPath);
        PrismProperty<T> propertyAfter = objectAfter.findProperty(itemPath);
        boolean beforeIsNull = propertyBefore == null || propertyBefore.isEmpty();
        boolean afterIsNull = propertyAfter == null || propertyAfter.isEmpty();
        if (beforeIsNull) {
            return !afterIsNull;
        } else if (afterIsNull) {
            return true;
        }
        MatchingRule<T> matchingRule = getMatchingRule(propertyAfter.getDefinition());
        return !MiscUtil.unorderedCollectionEquals(propertyBefore.getValues(), propertyAfter.getValues(),
                (v1, v2) -> {
                    try {
                        return matchingRule.match(getRealValue(v1), getRealValue(v2));
                    } catch (SchemaException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    private <T> MatchingRule<T> getMatchingRule(PrismPropertyDefinition<T> definition) throws SchemaException {
        QName matchingRuleName = defaultIfNull(
                definition != null ? definition.getMatchingRuleQName() : null,
                PrismConstants.DEFAULT_MATCHING_RULE_NAME);
        return matchingRuleRegistry.getMatchingRule(matchingRuleName, null);
    }

    private void executeEntitlementChangesDelete(ProvisioningContext ctx, PrismObject<ShadowType> subjectShadow,
            OperationProvisioningScriptsType scripts, ConnectorOperationOptions connOptions,
            OperationResult parentResult) throws SchemaException  {

        try {

            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap = new HashMap<>();

            entitlementConverter.collectEntitlementsAsObjectOperationDelete(ctx, roMap,
                    subjectShadow, parentResult);

            executeEntitlements(ctx, roMap, connOptions, parentResult);

        // TODO: now just log the errors, but not NOT re-throw the exception (except for some exceptions)
        // we want the original delete to take place, throwing an exception would spoil that
        } catch (SchemaException e) {
            throw e;
        } catch (CommunicationException | ObjectNotFoundException | SecurityViolationException | ConfigurationException | ObjectAlreadyExistsException | ExpressionEvaluationException | RuntimeException | Error e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void executeEntitlements(ProvisioningContext subjectCtx,
            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap, ConnectorOperationOptions connOptions, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Excuting entitlement chanes, roMap:\n{}", DebugUtil.debugDump(roMap, 1));
        }

        for (Entry<ResourceObjectDiscriminator,ResourceObjectOperations> entry: roMap.entrySet()) {
            ResourceObjectDiscriminator disc = entry.getKey();
            ProvisioningContext entitlementCtx = entry.getValue().getResourceObjectContext();
            Collection<? extends ResourceAttribute<?>> primaryIdentifiers = disc.getPrimaryIdentifiers();
            ResourceObjectOperations resourceObjectOperations = entry.getValue();
            Collection<? extends ResourceAttribute<?>> allIdentifiers = resourceObjectOperations.getAllIdentifiers();
            if (allIdentifiers == null || allIdentifiers.isEmpty()) {
                allIdentifiers = primaryIdentifiers;
            }
            Collection<Operation> operations = resourceObjectOperations.getOperations();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Excuting entitlement change identifiers={}:\n{}", allIdentifiers, DebugUtil.debugDump(operations, 1));
            }

            OperationResult result = parentResult.createMinorSubresult(OPERATION_MODIFY_ENTITLEMENT);
            try {

                executeModify(entitlementCtx, entry.getValue().getCurrentShadow(), allIdentifiers, operations, null, result, connOptions);

                result.recordSuccess();

            } catch (ObjectNotFoundException | CommunicationException | SchemaException | SecurityViolationException | PolicyViolationException | ConfigurationException | ObjectAlreadyExistsException | ExpressionEvaluationException e) {
                // We need to handle this specially.
                // E.g. ObjectNotFoundException means that the entitlement object was not found,
                // not that the subject was not found. It we throw ObjectNotFoundException here it may be
                // interpreted by the consistency code to mean that the subject is missing. Which is not
                // true. And that may cause really strange reactions. In fact we do not want to throw the
                // exception at all, because the primary operation was obviously successful. So just
                // properly record the operation in the result.
                LOGGER.error("Error while modifying entitlement {} of {}: {}", entitlementCtx, subjectCtx, e.getMessage(), e);
                result.recordFatalError(e);
            } catch (RuntimeException | Error e) {
                LOGGER.error("Error while modifying entitlement {} of {}: {}", entitlementCtx, subjectCtx, e.getMessage(), e);
                result.recordFatalError(e);
                throw e;
            }

        }
    }

    public SearchResultMetadata searchResourceObjects(ProvisioningContext ctx,
            ResourceObjectHandler resultHandler, ObjectQuery query, boolean fetchAssociations,
            FetchErrorReportingMethodType errorReportingMethod, OperationResult parentResult) throws SchemaException,
            CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        LOGGER.trace("Searching resource objects, query: {}", query);

        RefinedObjectClassDefinition objectClassDef = ctx.getObjectClassDefinition();
        AttributesToReturn attributesToReturn = ProvisioningUtil.createAttributesToReturn(ctx);
        SearchHierarchyConstraints searchHierarchyConstraints = entitlementConverter.determineSearchHierarchyConstraints(ctx, parentResult);

        if (InternalsConfig.consistencyChecks && query != null && query.getFilter() != null) {
            query.getFilter().checkConsistence(true);
        }

        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, parentResult);

        AtomicInteger objectCounter = new AtomicInteger(0);

        UcfFetchErrorReportingMethod ucfErrorReportingMethod;
        if (errorReportingMethod == FetchErrorReportingMethodType.FETCH_RESULT) {
            ucfErrorReportingMethod = UcfFetchErrorReportingMethod.UCF_OBJECT;
        } else {
            ucfErrorReportingMethod = UcfFetchErrorReportingMethod.EXCEPTION;
        }

        SearchResultMetadata metadata;
        try {

            metadata = connector.search(objectClassDef, query,
                    (ucfObject) -> {
                        ResourceObjectFound objectFound = new ResourceObjectFound(ucfObject, ResourceObjectConverter.this,
                                ctx, fetchAssociations);

                        // in order to utilize the cache right from the beginning...
                        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
                        try {

                            int objectNumber = objectCounter.getAndIncrement();

                            Task task = ctx.getTask();
                            boolean requestedTracingHere;
                            requestedTracingHere = task instanceof RunningTask &&
                                    ((RunningTask) task).requestTracingIfNeeded(
                                            (RunningTask) task, objectNumber,
                                            TracingRootType.RETRIEVED_RESOURCE_OBJECT_PROCESSING);
                            try {
                                OperationResultBuilder resultBuilder = parentResult
                                        .subresult(OperationConstants.OPERATION_SEARCH_RESULT)
                                        .setMinor()
                                        .addParam("number", objectNumber)
                                        .addArbitraryObjectAsParam("primaryIdentifierValue", ucfObject.getPrimaryIdentifierValue())
                                        .addArbitraryObjectAsParam("errorState", ucfObject.getErrorState());

                                // Here we request tracing if configured to do so. Note that this is only a partial solution: for multithreaded
                                // operations we currently do not trace the "worker" part of the processing.
                                boolean tracingRequested = setTracingInOperationResultIfRequested(resultBuilder,
                                        TracingRootType.RETRIEVED_RESOURCE_OBJECT_PROCESSING, task, parentResult);

                                OperationResult objResult = resultBuilder.build();
                                try {
                                    objectFound.initialize(task, objResult);
                                    return resultHandler.handle(objectFound, objResult);
                                } catch (Throwable t) {
                                    objResult.recordFatalError(t);
                                    throw t;
                                } finally {
                                    objResult.computeStatusIfUnknown();
                                    if (tracingRequested) {
                                        tracer.storeTrace(task, objResult, parentResult);
                                    }
                                    // FIXME: hack. Hardcoded ugly summarization of successes. something like
                                    //  AbstractSummarizingResultHandler [lazyman]
                                    if (objResult.isSuccess() && !tracingRequested && !objResult.isTraced()) {
                                        objResult.getSubresults().clear();
                                    }
                                    // TODO Reconsider this. It is quite dubious to touch parentResult from the inside.
                                    parentResult.summarize();
                                }
                            } finally {
                                RepositoryCache.exitLocalCaches();
                                if (requestedTracingHere && task instanceof RunningTask) {
                                    ((RunningTask) task).stopTracing();
                                }
                            }
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Throwable t) {
                            throw new TunnelException(t);
                        }
                    },
                    attributesToReturn, objectClassDef.getPagedSearches(ctx.getResource()), searchHierarchyConstraints,
                    ucfErrorReportingMethod, ctx, parentResult);

        } catch (GenericFrameworkException e) {
            parentResult.recordFatalError("Generic error in the connector: " + e.getMessage(), e);
            throw new SystemException("Generic error in the connector: " + e.getMessage(), e);
        } catch (CommunicationException ex) {
            parentResult.recordFatalError(
                    "Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
            throw new CommunicationException("Error communicating with the connector " + connector + ": "
                    + ex.getMessage(), ex);
        } catch (SecurityViolationException ex) {
            parentResult.recordFatalError(
                    "Security violation communicating with the connector " + connector + ": " + ex.getMessage(), ex);
            throw new SecurityViolationException("Security violation communicating with the connector " + connector + ": "
                    + ex.getMessage(), ex);
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            parentResult.recordFatalError("Problem while communicating with the connector " + connector + ": " +
                    cause.getMessage(), cause);
            if (cause instanceof SchemaException) {
                throw (SchemaException)cause;
            } else if (cause instanceof CommunicationException) {
                throw (CommunicationException)cause;
            } else if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException)cause;
            } else if (cause instanceof ConfigurationException) {
                throw (ConfigurationException)cause;
            } else if (cause instanceof SecurityViolationException) {
                throw (SecurityViolationException)cause;
            } else if (cause instanceof ExpressionEvaluationException) {
                throw (ExpressionEvaluationException)cause;
            } else if (cause instanceof GenericFrameworkException) {
                throw new GenericConnectorException(cause.getMessage(), cause);
            } else {
                throw new SystemException(cause.getMessage(), cause);
            }
        }

        computeResultStatus(parentResult);

        LOGGER.trace("Searching resource objects done: {}", parentResult.getStatus());

        return metadata;
    }

    private boolean setTracingInOperationResultIfRequested(OperationResultBuilder resultBuilder, TracingRootType tracingRoot,
            Task task, OperationResult parentResult) throws SchemaException {
        boolean tracingRequested;
        if (task != null && task.getTracingRequestedFor().contains(tracingRoot)) {
            tracingRequested = true;
            try {
                resultBuilder.tracingProfile(tracer.compileProfile(task.getTracingProfile(), parentResult));
            } catch (SchemaException | RuntimeException e) {
                parentResult.recordFatalError(e);
                throw e;
            }
        } else {
            tracingRequested = false;
        }
        return tracingRequested;
    }

    @SuppressWarnings("rawtypes")
    public PrismProperty fetchCurrentToken(ProvisioningContext ctx, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, ExpressionEvaluationException {
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.trace("Fetcing current sync token for {}", ctx);

        PrismProperty lastToken;
        ConnectorInstance connector = ctx.getConnector(LiveSyncCapabilityType.class, parentResult);
        try {
            lastToken = connector.fetchCurrentToken(ctx.getObjectClassDefinition(), ctx, parentResult);
        } catch (GenericFrameworkException e) {
            parentResult.recordFatalError("Generic error in the connector: " + e.getMessage(), e);
            throw new CommunicationException("Generic error in the connector: " + e.getMessage(), e);

        } catch (CommunicationException ex) {
            parentResult.recordFatalError(
                    "Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
            throw new CommunicationException("Error communicating with the connector " + connector + ": "
                    + ex.getMessage(), ex);
        }

        LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));

        computeResultStatus(parentResult);

        return lastToken;
    }


    public PrismObject<ShadowType> fetchResourceObject(ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers,
            AttributesToReturn attributesToReturn,
            boolean fetchAssociations,
            OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        PrismObject<ShadowType> resourceObject = resourceObjectReferenceResolver.fetchResourceObject(ctx, identifiers, attributesToReturn, parentResult);
        postProcessResourceObjectRead(ctx, resourceObject, fetchAssociations, parentResult);
        return resourceObject;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void applyAfterOperationAttributes(PrismObject<ShadowType> shadow,
            Collection<ResourceAttribute<?>> resourceAttributesAfterAdd) throws SchemaException {
        if (resourceAttributesAfterAdd == null) {
            return;
        }
        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
        for (ResourceAttribute attributeAfter : resourceAttributesAfterAdd) {
            ResourceAttribute attributeBefore = attributesContainer.findAttribute(attributeAfter.getElementName());
            if (attributeBefore != null) {
                attributesContainer.remove(attributeBefore);
            }
            if (!attributesContainer.contains(attributeAfter)) {
                attributesContainer.add(attributeAfter.clone());
            }
        }
    }

    private boolean hasChangesOnResource(
            Collection<? extends ItemDelta> itemDeltas) {
        for (ItemDelta itemDelta : itemDeltas) {
            if (isAttributeDelta(itemDelta) || SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
                return true;
            } else if (SchemaConstants.PATH_ACTIVATION.equivalent(itemDelta.getParentPath())){
                return true;
            } else if (ShadowType.F_ASSOCIATION.equivalent(itemDelta.getPath())) {
                return true;
            }
        }
        return false;
    }

    private void collectAttributeAndEntitlementChanges(ProvisioningContext ctx,
            Collection<? extends ItemDelta<?, ?>> objectChange, Collection<Operation> operations,
            PrismObject<ShadowType> shadow, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (operations == null) {
            operations = new ArrayList<>();
        }
        boolean activationProcessed = false;
        RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
        for (ItemDelta itemDelta : objectChange) {
            if (isAttributeDelta(itemDelta) || SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
                if (itemDelta instanceof PropertyDelta) {
                    PropertyModificationOperation attributeModification = new PropertyModificationOperation<>(
                            (PropertyDelta<?>) itemDelta);
                    RefinedAttributeDefinition<Object> attrDef = objectClassDefinition.findAttributeDefinition(itemDelta.getElementName());
                    if (attrDef != null) {
                        attributeModification.setMatchingRuleQName(attrDef.getMatchingRuleQName());
                        if (itemDelta.getDefinition() == null) {
                            itemDelta.setDefinition(attrDef);
                        }
                    }
                    operations.add(attributeModification);
                } else if (itemDelta instanceof ContainerDelta) {
                    // skip the container delta - most probably password change
                    // - it is processed earlier
                    continue;
                } else {
                    throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
                }
            } else if (SchemaConstants.PATH_ACTIVATION.equivalent(itemDelta.getParentPath())) {
                if (!activationProcessed) {
                    Collection<Operation> activationOperations = new ActivationConverter(ctx, commonBeans)
                            .createActivationChangeOperations(shadow.asObjectable(), objectChange, result);
                    operations.addAll(activationOperations);
                    activationProcessed = true;
                }
            } else if (ShadowType.F_ASSOCIATION.equivalent(itemDelta.getPath())) {
                if (itemDelta instanceof ContainerDelta) {
                    //noinspection unchecked
                    entitlementConverter.collectEntitlementChange(ctx, (ContainerDelta<ShadowAssociationType>)itemDelta, operations);
                } else {
                    throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
                }
            } else if (ShadowType.F_AUXILIARY_OBJECT_CLASS.equivalent(itemDelta.getPath())) {
                if (itemDelta instanceof PropertyDelta) {
                    PropertyModificationOperation attributeModification = new PropertyModificationOperation<>(
                            (PropertyDelta<?>) itemDelta);
                    operations.add(attributeModification);
                } else {
                    throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
                }
            } else {
                LOGGER.trace("Skip converting item delta: {}. It's not resource object change, but it is shadow change.", itemDelta);
            }
        }
    }

    private boolean isAttributeDelta(ItemDelta itemDelta) {
        return ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath());
    }

    public UcfFetchChangesResult fetchChanges(ProvisioningContext ctx, @NotNull PrismProperty<?> initialToken,
            ResourceObjectLiveSyncChangeListener outerListener,
            OperationResult gResult) throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, GenericFrameworkException, ObjectNotFoundException, ExpressionEvaluationException {

        LOGGER.trace("START fetch changes from {}, objectClass: {}", initialToken, ctx.getObjectClassDefinition());
        AttributesToReturn attrsToReturn;
        if (ctx.isWildcard()) {
            attrsToReturn = null;
        } else {
            attrsToReturn = ProvisioningUtil.createAttributesToReturn(ctx);
        }

        ConnectorInstance connector = ctx.getConnector(LiveSyncCapabilityType.class, gResult);
        Integer maxChanges = getMaxChanges(ctx);

        AtomicInteger processed = new AtomicInteger(0);
        UcfLiveSyncChangeListener localListener = (ucfChange, lParentResult) -> {
            int changeNumber = processed.getAndIncrement();

            Task task = ctx.getTask();
            boolean requestedTracingHere;
            requestedTracingHere = task instanceof RunningTask &&
                    ((RunningTask) task).requestTracingIfNeeded(
                            (RunningTask) task, changeNumber,
                            TracingRootType.LIVE_SYNC_CHANGE_PROCESSING);
            try {
                OperationResultBuilder resultBuilder = lParentResult.subresult(OPERATION_HANDLE_CHANGE)
                        .setMinor()
                        .addParam("number", changeNumber)
                        .addParam("localSequenceNumber", ucfChange.getLocalSequenceNumber())
                        .addArbitraryObjectAsParam("primaryIdentifier", ucfChange.getPrimaryIdentifierRealValue())
                        .addArbitraryObjectAsParam("token", ucfChange.getToken());

                // Here we request tracing if configured to do so. Note that this is only a partial solution: for multithreaded
                // livesync we currently do not trace the "worker" part of the processing.
                boolean tracingRequested;
                Exception preInitializationException = null;
                try {
                    tracingRequested = setTracingInOperationResultIfRequested(resultBuilder,
                            TracingRootType.LIVE_SYNC_CHANGE_PROCESSING, task, lParentResult);
                } catch (Exception e) {
                    lParentResult.recordFatalError(e);
                    preInitializationException = e;
                    tracingRequested = false;
                }

                OperationResult lResult = resultBuilder.build();
                try {
                    ResourceObjectLiveSyncChange change = new ResourceObjectLiveSyncChange(ucfChange,
                            preInitializationException, ResourceObjectConverter.this, ctx, attrsToReturn);
                    change.initialize(task, lResult);
                    return outerListener.onChange(change, lResult);
                } catch (Throwable t) {
                    lResult.recordFatalError(t);
                    throw t;
                } finally {
                    lResult.computeStatusIfUnknown();
                    if (tracingRequested) {
                        tracer.storeTrace(task, lResult, lParentResult);
                    }
                }
            } finally {
                if (requestedTracingHere && task instanceof RunningTask) {
                    ((RunningTask) task).stopTracing();
                }
            }
        };

        // get changes from the connector
        UcfFetchChangesResult fetchChangesResult = connector.fetchChanges(ctx.getObjectClassDefinition(), initialToken,
                attrsToReturn, maxChanges, ctx, localListener, gResult);

        computeResultStatus(gResult);

        LOGGER.trace("END fetch changes ({} changes); interrupted = {}; all fetched = {}, final token = {}", processed.get(),
                !ctx.canRun(), fetchChangesResult.isAllChangesFetched(), fetchChangesResult.getFinalToken());

        return fetchChangesResult;
    }

    @Nullable
    private Integer getMaxChanges(ProvisioningContext ctx) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Integer batchSizeInTask = ctx.getTask().getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_LIVE_SYNC_BATCH_SIZE);
        LiveSyncCapabilityType capability = ctx.getEffectiveCapability(LiveSyncCapabilityType.class);
        if (capability != null) {
            if (Boolean.TRUE.equals(capability.isPreciseTokenValue())) {
                return batchSizeInTask;
            } else {
                checkMaxChanges(batchSizeInTask, "LiveSync capability has preciseTokenValue not set to 'true'");
                return null;
            }
        } else {
            // Is this possible?
            checkMaxChanges(batchSizeInTask, "LiveSync capability is not found or disabled");
            return null;
        }
    }

    private void checkMaxChanges(Integer maxChangesFromTask, String reason) {
        if (maxChangesFromTask != null && maxChangesFromTask > 0) {
            throw new IllegalArgumentException("Cannot apply " + SchemaConstants.MODEL_EXTENSION_LIVE_SYNC_BATCH_SIZE.getLocalPart() +
                    " because " + reason);
        }
    }

    public void listenForAsynchronousUpdates(@NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectAsyncChangeListener outerListener, @NotNull OperationResult parentResult) throws SchemaException,
            CommunicationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {

        LOGGER.trace("Listening for async updates, objectClass: {}", ctx.getObjectClassDefinition());
        ConnectorInstance connector = ctx.getConnector(AsyncUpdateCapabilityType.class, parentResult);

        UcfAsyncUpdateChangeListener innerListener = (ucfChange, listenerTask, listenerResult) -> {
            ResourceObjectAsyncChange change = new ResourceObjectAsyncChange(ucfChange, ResourceObjectConverter.this, ctx);
            change.initialize(listenerTask, listenerResult);
            outerListener.onChange(change, listenerTask, listenerResult);
        };
        connector.listenForChanges(innerListener, ctx::canRun, parentResult);

        LOGGER.trace("Finished listening for async updates");
    }

    /**
     * Process simulated activation, credentials and other properties that are added to the object by midPoint.
     */
    void postProcessResourceObjectRead(ProvisioningContext ctx, PrismObject<ShadowType> resourceObject,
            boolean fetchAssociations, OperationResult result) throws SchemaException, CommunicationException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (resourceObject == null) {
            return;
        }
        ShadowType resourceObjectBean = resourceObject.asObjectable();

        ProvisioningUtil.setProtectedFlag(ctx, resourceObject, matchingRuleRegistry, relationRegistry, expressionFactory, result);

        if (resourceObjectBean.isExists() == null) {
            resourceObjectBean.setExists(true);
        }

        new ActivationConverter(ctx, commonBeans)
                .completeActivation(resourceObject, result);

        // Entitlements
        if (fetchAssociations) {
            entitlementConverter.postProcessEntitlementsRead(ctx, resourceObject, result);
        }
    }

    private void executeProvisioningScripts(ProvisioningContext ctx, ProvisioningOperationTypeType provisioningOperationType,
                                            BeforeAfterType beforeAfter, OperationProvisioningScriptsType scripts, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, GenericConnectorException {
        Collection<ExecuteProvisioningScriptOperation> operations = determineExecuteScriptOperations(provisioningOperationType, beforeAfter, scripts, ctx.getResource(), result);
        if (operations == null) {
            return;
        }
        ConnectorInstance connector = ctx.getConnector(ScriptCapabilityType.class, result);
        for (ExecuteProvisioningScriptOperation operation : operations) {
            StateReporter reporter = new StateReporter(ctx.getResource().getOid(), ctx.getTask());

            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("PROVISIONING SCRIPT EXECUTION {} {} operation on resource {}",
                            beforeAfter.value(), provisioningOperationType.value(),
                            ctx.getResource());
                }

                Object returnedValue = connector.executeScript(operation, reporter, result);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("PROVISIONING SCRIPT EXECUTION {} {} successful, returned value: {}",
                            beforeAfter.value(), provisioningOperationType.value(), returnedValue);
                }

            } catch (CommunicationException ex) {
                String message = "Could not execute provisioning script. Error communicating with the connector " + connector + ": " + ex.getMessage();
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, ex);
                    throw new CommunicationException(message, ex);
                } else {
                    LOGGER.warn("{}", message);
                }
            } catch (GenericFrameworkException ex) {
                String message = "Could not execute provisioning script. Generic error in connector: " + ex.getMessage();
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, ex);
                    throw new GenericConnectorException(message, ex);
                } else {
                    LOGGER.warn("{}", message);
                }
            } catch (Throwable t) {
                String message = "Could not execute provisioning script. Unexpected error in connector: " +t.getClass().getSimpleName() + ": " + t.getMessage();
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, t);
                    throw t;
                } else {
                    LOGGER.warn("{}", message);
                }
            }
        }
    }

    private Collection<ExecuteProvisioningScriptOperation> determineExecuteScriptOperations(ProvisioningOperationTypeType provisioningOperationType,
            BeforeAfterType beforeAfter, OperationProvisioningScriptsType scripts, ResourceType resource, OperationResult result) throws SchemaException {
        if (scripts == null) {
            // No warning needed, this is quite normal
            LOGGER.trace("Skipping creating script operation to execute. No scripts were defined.");
            return null;
        }

        Collection<ExecuteProvisioningScriptOperation> operations = new ArrayList<>();
        for (OperationProvisioningScriptType script : scripts.getScript()) {
            for (ProvisioningOperationTypeType operationType : script.getOperation()) {
                if (provisioningOperationType.equals(operationType) && beforeAfter.equals(script.getOrder())) {
                    ExecuteProvisioningScriptOperation scriptOperation = ProvisioningUtil.convertToScriptOperation(
                            script, "script value for " + operationType + " in " + resource, prismContext);
                    LOGGER.trace("Created script operation: {}", SchemaDebugUtil.prettyPrint(scriptOperation));
                    operations.add(scriptOperation);
                }
            }
        }
        return operations;
    }

    public AsynchronousOperationResult refreshOperationStatus(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow, String asyncRef, OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_REFRESH_OPERATION_STATUS);

        ResourceType resource;
        ConnectorInstance connector;
        try {
            resource = ctx.getResource();
            // TODO: not really correct. But good enough for now.
            connector = ctx.getConnector(UpdateCapabilityType.class, result);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException
                | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        OperationResultStatus status = null;
        if (connector instanceof AsynchronousOperationQueryable) {

            LOGGER.trace("PROVISIONING REFRESH operation ref={} on {}, object: {}",
                    asyncRef, resource, shadow);

            try {

                status = ((AsynchronousOperationQueryable)connector).queryOperationStatus(asyncRef, result);

            } catch (ObjectNotFoundException | SchemaException | ConfigurationException | CommunicationException e) {
                result.recordFatalError(e);
                throw e;
            }

            result.recordSuccess();

            LOGGER.debug("PROVISIONING REFRESH ref={} successful on {} {}, returned status: {}", asyncRef, resource, shadow, status);

        } else {
            LOGGER.trace("Ignoring refresh of shadow {}, because the connector is not async operation queryable", shadow.getOid());
            result.recordNotApplicableIfUnknown();
        }

        OperationResult refreshResult = new OperationResult(OPERATION_REFRESH_OPERATION_STATUS);
        refreshResult.setStatus(status);
        AsynchronousOperationResult asyncResult = AsynchronousOperationResult.wrap(refreshResult);
        updateQuantum(ctx, connector, asyncResult, result);
        return asyncResult;
    }

    private void computeResultStatus(OperationResult result) {
        if (result.isInProgress()) {
            return;
        }
        OperationResultStatus status = OperationResultStatus.SUCCESS;
        String asyncRef = null;
        for (OperationResult subresult: result.getSubresults()) {
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

    private <C extends CapabilityType> void checkForCapability(ProvisioningContext ctx, Class<C> capabilityClass, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        C capability = ctx.getEffectiveCapability(capabilityClass);
        if (capability == null || BooleanUtils.isFalse(capability.isEnabled())) {
            UnsupportedOperationException e = new UnsupportedOperationException("Operation not supported "+ctx.getDesc()+" as "+capabilityClass.getSimpleName()+" is missing");
            if (result != null) {
                result.recordFatalError(e);
            }
            throw e;
        }
    }

    public ShadowCaretaker getShadowCaretaker() {
        return shadowCaretaker;
    }

    public ResourceObjectsLocalBeans getLocalBeans() {
        return localBeans;
    }
}
