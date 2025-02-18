/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;
import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.isAddShadowEnabled;
import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.isModifyShadowEnabled;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventType;
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
import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.impl.*;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.async.UcfAsyncUpdateChangeListener;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.*;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import static com.evolveum.midpoint.schema.result.OperationResult.HANDLE_OBJECT_FOUND;

/**
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
    @VisibleForTesting
    public static final String OP_SEARCH_RESOURCE_OBJECTS = DOT_CLASS + "searchResourceObjects";
    @VisibleForTesting
    public static final String OP_HANDLE_OBJECT_FOUND = DOT_CLASS + HANDLE_OBJECT_FOUND;
    static final String OP_COUNT_RESOURCE_OBJECTS = DOT_CLASS + "countResourceObjects";

    @Autowired private EntitlementConverter entitlementConverter;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private ResourceObjectReferenceResolver resourceObjectReferenceResolver;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private CommonBeans commonBeans;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private ResourceObjectsBeans beans;
    @Autowired private ShadowAuditHelper shadowAuditHelper;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectConverter.class);

    public static final String FULL_SHADOW_KEY = ResourceObjectConverter.class.getName() + ".fullShadow";

    /**
     * @param repoShadow Used when read capability is "caching only"
     */
    public @NotNull ShadowType getResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull Collection<? extends ResourceAttribute<?>> identifiers,
            @Nullable ShadowType repoShadow,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException {

        LOGGER.trace("Getting resource object {}", identifiers);
        PrismObject<ShadowType> resourceObject = fetchResourceObject(
                ctx,
                identifiers,
                ctx.createAttributesToReturn(),
                asPrismObject(repoShadow),
                fetchAssociations,
                result);
        LOGGER.trace("Got resource object\n{}", resourceObject.debugDumpLazily());
        return resourceObject.asObjectable();
    }

    /**
     * Tries to get the object directly if primary identifiers are present. Tries to search for the object if they are not.
     */
    public PrismObject<ShadowType> locateResourceObject(
            ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException {

        LOGGER.trace("Locating resource object {}", identifiers);

        if (hasAllPrimaryIdentifiers(identifiers, ctx.getObjectDefinitionRequired())) {
            return fetchResourceObject(
                    ctx,
                    identifiers,
                    ctx.createAttributesToReturn(),
                    null,
                    true, // todo consider whether it is always necessary to fetch the entitlements
                    result);
        } else {
            return searchBySecondaryIdentifiers(ctx, identifiers, result);
        }
    }

    private PrismObject<ShadowType> searchBySecondaryIdentifiers(
            ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers,
            OperationResult result)
            throws SchemaException, CommunicationException, SecurityViolationException, ObjectNotFoundException,
            ConfigurationException, ExpressionEvaluationException {

        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();
        Collection<? extends ResourceAttributeDefinition<?>> secondaryIdDefs = objectDefinition.getSecondaryIdentifiers();
        LOGGER.trace("Searching by secondary identifier(s) {}, using values of: {}", secondaryIdDefs, identifiers);
        if (secondaryIdDefs.isEmpty()) {
            throw new SchemaException( // Shouldn't be ConfigurationException?
                    String.format("No secondary identifier(s) defined, cannot search for secondary identifiers among %s (%s)",
                            identifiers, ctx.getExceptionDescription()));
        }

        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);

        for (ResourceAttributeDefinition<?> secondaryIdDef : secondaryIdDefs) {
            ResourceAttribute<?> secondaryIdentifier = getSecondaryIdentifier(identifiers, secondaryIdDef);
            if (secondaryIdentifier == null) {
                LOGGER.trace("Secondary identifier {} is not provided, will try another one (if available)", secondaryIdDef);
                continue;
            }
            PrismPropertyValue<?> secondaryIdentifierValue = getSecondaryIdentifierValue(secondaryIdentifier);
            if (secondaryIdentifierValue == null) {
                LOGGER.trace("Secondary identifier {} has no value, will try another one (if available)", secondaryIdentifier);
                continue;
            }

            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .itemWithDef(secondaryIdDef, ShadowType.F_ATTRIBUTES, secondaryIdDef.getItemName())
                    .eq(secondaryIdentifierValue)
                    .build();
            Holder<PrismObject<ShadowType>> shadowHolder = new Holder<>();
            UcfObjectHandler handler = (ucfObject, lResult) -> {
                if (!shadowHolder.isEmpty()) {
                    throw new IllegalStateException(
                            String.format("More than one object found for secondary identifier %s (%s)",
                                    secondaryIdentifier, ctx.getExceptionDescription()));
                }
                shadowHolder.setValue(ucfObject.getResourceObject());
                return true;
            };
            try {
                // TODO constraints? scope?
                connector.search(
                        objectDefinition,
                        query,
                        handler,
                        ctx.createAttributesToReturn(),
                        null,
                        null,
                        UcfFetchErrorReportingMethod.EXCEPTION,
                        ctx.getUcfExecutionContext(),
                        result);
                if (shadowHolder.isEmpty()) {
                    // We could consider continuing with another secondary identifier, but let us keep the original behavior.
                    throw new ObjectNotFoundException(
                            String.format("No object found for secondary identifier %s (%s)",
                                    secondaryIdentifier, ctx.getExceptionDescription(connector)),
                            ShadowType.class,
                            null,
                            ctx.isAllowNotFound());
                }
                PrismObject<ShadowType> shadow = shadowHolder.getValue();
                // todo consider whether it is always necessary to fetch the entitlements
                postProcessResourceObjectRead(ctx, shadow, true, result);
                LOGGER.trace("Located resource object {}", shadow);
                return shadow;
            } catch (GenericFrameworkException e) {
                throw new GenericConnectorException(
                        String.format("Generic exception in connector while searching for object (%s): %s",
                                ctx.getExceptionDescription(connector), e.getMessage()),
                        e);
            }
        }
        throw new SchemaException( // Shouldn't be other kind of exception?
                String.format("No suitable secondary identifier(s) defined, cannot search for secondary identifiers among %s (%s)",
                        identifiers, ctx.getExceptionDescription()));
    }

    private ResourceAttribute<?> getSecondaryIdentifier(
            Collection<? extends ResourceAttribute<?>> identifiers, ResourceAttributeDefinition<?> secondaryIdentifierDef) {
        for (ResourceAttribute<?> identifier : identifiers) {
            if (identifier.getElementName().equals(secondaryIdentifierDef.getItemName())) {
                return identifier;
            }
        }
        return null;
    }

    private PrismPropertyValue<?> getSecondaryIdentifierValue(ResourceAttribute<?> secondaryIdentifier)
            throws SchemaException {
        //noinspection unchecked,rawtypes
        List<PrismPropertyValue<?>> secondaryIdentifierValues = (List) secondaryIdentifier.getValues();
        if (secondaryIdentifierValues.size() > 1) {
            // TODO context
            throw new SchemaException("Secondary identifier has more than one value: " + secondaryIdentifier.getValues());
        } else if (secondaryIdentifierValues.size() == 1) {
            return secondaryIdentifierValues.get(0).clone();
        } else {
            return null;
        }
    }

    // TODO: there should be only one primary identifier (clarify the method)
    private boolean hasAllPrimaryIdentifiers(
            Collection<? extends ResourceAttribute<?>> attributes, ResourceObjectDefinition objectDefinition) {
        Collection<? extends ResourceAttributeDefinition<?>> identifierDefs = objectDefinition.getPrimaryIdentifiers();
        for (ResourceAttributeDefinition<?> identifierDef : identifierDefs) {
            boolean found = false;
            for (ResourceAttribute<?> attribute : attributes) {
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

    public AsynchronousOperationReturnValue<ShadowType> addResourceObject(
            ProvisioningContext ctx,
            ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            boolean skipExplicitUniquenessCheck,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_ADD_RESOURCE_OBJECT);
        try {
            ResourceType resource = ctx.getResource();

            LOGGER.trace("Adding resource object {}", shadow);

            ctx.checkExecutionFullyPersistent();

            // We might be modifying the shadow (e.g. for simulated capabilities). But we do not want the changes
            // to propagate back to the calling code. Hence the clone.
            ShadowType shadowClone = shadow.clone();

            Collection<ResourceAttribute<?>> resourceAttributesAfterAdd;

            if (!isAddShadowEnabled(ctx.getProtectedAccountPatterns(expressionFactory, result), shadowClone, result)) {
                throw new SecurityViolationException(
                        String.format("Cannot add protected shadow %s (%s)", shadowClone, ctx.getExceptionDescription()));
            }

            if (!skipExplicitUniquenessCheck) {
                checkForAddConflicts(ctx, shadowClone, result);
            }

            checkForCapability(ctx, CreateCapabilityType.class);

            executeProvisioningScripts(ctx, ProvisioningOperationTypeType.ADD, BeforeAfterType.BEFORE, scripts, result);

            entitlementConverter.processEntitlementsAdd(shadowClone.asPrismObject(), ctx);

            ConnectorInstance connector = ctx.getConnector(CreateCapabilityType.class, result);
            AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> connectorAsyncOpRet;
            try {
                LOGGER.debug("PROVISIONING ADD operation on resource {}\n ADD object:\n{}\n",
                        resource, shadowClone.debugDumpLazily());

                new ActivationConverter(ctx, commonBeans)
                        .transformActivationOnAdd(shadowClone, result);

                connectorAsyncOpRet = connector.addObject(shadowClone.asPrismObject(), ctx.getUcfExecutionContext(), result);
                resourceAttributesAfterAdd = connectorAsyncOpRet.getReturnValue();

                LOGGER.debug("PROVISIONING ADD successful, returned attributes:\n{}",
                        SchemaDebugUtil.prettyPrintLazily(resourceAttributesAfterAdd));

                // Be careful NOT to apply this to the cloned shadow. This needs to be propagated outside this method.
                applyAfterOperationAttributes(shadow, resourceAttributesAfterAdd);
            } catch (CommunicationException ex) {
                throw communicationException(ctx, connector, ex);
            } catch (GenericFrameworkException ex) {
                throw genericConnectorException(ctx, connector, ex);
            } catch (ObjectAlreadyExistsException ex) {
                throw objectAlreadyExistsException("", ctx, connector, ex);
            } finally {
                shadowAuditHelper.auditEvent(AuditEventType.ADD_OBJECT, shadow, ctx, result);
            }

            // Execute entitlement modification on other objects (if needed)
            executeEntitlementChangesAdd(ctx, shadowClone, scripts, connOptions, result);

            LOGGER.trace("Added resource object {}", shadowClone);

            executeProvisioningScripts(ctx, ProvisioningOperationTypeType.ADD, BeforeAfterType.AFTER, scripts, result);

            computeResultStatus(result);

            // This should NOT be the cloned shadow: we need the original in order to retry the operation.
            AsynchronousOperationReturnValue<ShadowType> asyncOpRet = AsynchronousOperationReturnValue.wrap(shadow, result);
            asyncOpRet.setOperationType(connectorAsyncOpRet.getOperationType());
            return asyncOpRet;
        } catch (Throwable t) {
            result.recordException("Could not create object on the resource: " + t.getMessage(), t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Special case for multi-connectors (e.g. semi-manual connectors). There is a possibility that the object
     * which we want to add is already present in the backing store. In case of manual provisioning the resource
     * itself will not indicate "already exist" error. We have to explicitly check for that.
     */
    private void checkForAddConflicts(ProvisioningContext ctx, ShadowType shadow, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        LOGGER.trace("Checking for add conflicts for {}", ShadowUtil.shortDumpShadowLazily(shadow));
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
            ResourceObjectIdentification identification =
                    ResourceObjectIdentification.createFromShadow(ctx.getObjectDefinitionRequired(), shadow);

            existingObject =
                    readConnector.fetchObject(identification, null, ctx.getUcfExecutionContext(), result);
        } catch (ObjectNotFoundException e) {
            // This is OK
            result.muteLastSubresultError();
            return;
        } catch (CommunicationException ex) {
            throw communicationException(ctx, readConnector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, readConnector, ex);
        }
        if (existingObject == null) {
            LOGGER.trace("No add conflicts for {}", shadow);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Detected add conflict for {}, conflicting shadow: {}",
                        ShadowUtil.shortDumpShadow(shadow), ShadowUtil.shortDumpShadow(existingObject));
            }
            LOGGER.trace("Conflicting shadow:\n{}", existingObject.debugDumpLazily(1));
            throw new ObjectAlreadyExistsException(
                    String.format("Object %s already exists in the snapshot of %s as %s",
                            ShadowUtil.shortDumpShadow(shadow),
                            ctx.getResource(),
                            ShadowUtil.shortDumpShadow(existingObject)));
        }
    }

    public AsynchronousOperationResult deleteResourceObject(
            ProvisioningContext ctx,
            ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_RESOURCE_OBJECT);
        try {

            LOGGER.trace("Deleting resource object {}", shadow);

            ctx.checkExecutionFullyPersistent();

            checkForCapability(ctx, DeleteCapabilityType.class);

            Collection<? extends ResourceAttribute<?>> identifiers = getIdentifiers(ctx, shadow);

            checkIfProtected(ctx, shadow, identifiers, result);

            executeProvisioningScripts(ctx, ProvisioningOperationTypeType.DELETE, BeforeAfterType.BEFORE, scripts, result);

            // Execute entitlement modification on other objects (if needed)
            executeEntitlementChangesDelete(ctx, shadow, scripts, connOptions, result);

            ConnectorInstance connector = ctx.getConnector(DeleteCapabilityType.class, result);
            AsynchronousOperationResult connectorAsyncOpRet;
            try {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "PROVISIONING DELETE operation on {}\n DELETE object, object class {}, identified by:\n{}",
                            ctx.getResource(), shadow.getObjectClass(),
                            SchemaDebugUtil.debugDump(identifiers));
                }

                if (!ResourceTypeUtil.isDeleteCapabilityEnabled(ctx.getResource())) {
                    throw new UnsupportedOperationException(
                            String.format("Resource does not support 'delete' operation (%s)", ctx.getExceptionDescription()));
                }

                connectorAsyncOpRet =
                        connector.deleteObject(
                                ctx.getObjectDefinitionRequired(),
                                shadow.asPrismObject(),
                                identifiers,
                                ctx.getUcfExecutionContext(),
                                result);
            } catch (ObjectNotFoundException ex) {
                throw ex.wrap(String.format(
                        "An error occurred while deleting resource object %s with identifiers %s (%s)",
                        shadow, identifiers, ctx.getExceptionDescription(connector)));
            } catch (CommunicationException ex) {
                throw communicationException(ctx, connector, ex);
            } catch (GenericFrameworkException ex) {
                throw genericConnectorException(ctx, connector, ex);
            } catch (ConfigurationException ex) {
                throw configurationException(ctx, connector, ex);
            } finally {
                shadowAuditHelper.auditEvent(AuditEventType.DELETE_OBJECT, shadow, ctx, result);
            }

            LOGGER.trace("Deleted resource object {}", shadow);

            executeProvisioningScripts(ctx, ProvisioningOperationTypeType.DELETE, BeforeAfterType.AFTER, scripts, result);

            computeResultStatus(result);
            LOGGER.debug("PROVISIONING DELETE result: {}", result.getStatus());

            AsynchronousOperationResult aResult = AsynchronousOperationResult.wrap(result);
            updateQuantum(ctx, connector, aResult, result); // The result is not closed, even if its status is set. So we use it.
            if (connectorAsyncOpRet != null) {
                aResult.setOperationType(connectorAsyncOpRet.getOperationType());
            }
            return aResult;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void checkIfProtected(
            ProvisioningContext ctx,
            ShadowType shadow,
            Collection<? extends ResourceAttribute<?>> identifiers,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        Collection<ResourceObjectPattern> protectedPatterns = ctx.getProtectedAccountPatterns(expressionFactory, result);
        if (!ProvisioningUtil.isDeleteShadowEnabled(protectedPatterns, shadow, result)) {
            throw new SecurityViolationException(
                    String.format("Cannot delete protected resource object (%s): %s",
                            ctx.getExceptionDescription(), identifiers));
        }
    }

    @Nullable
    private Collection<? extends ResourceAttribute<?>> getIdentifiers(ProvisioningContext ctx, ShadowType shadow)
            throws SchemaException, ConfigurationException {
        if (ShadowUtil.isAttributesContainerRaw(shadow)) {
            // This could occur if shadow was re-read during op state processing
            ctx.applyAttributesDefinition(shadow);
        }
        return ShadowUtil.getAllIdentifiers(shadow);
    }

    private void updateQuantum(
            ProvisioningContext ctx,
            ConnectorInstance connectorUsedForOperation,
            AsynchronousOperationResult aResult,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ConnectorInstance readConnector = ctx.getConnector(ReadCapabilityType.class, parentResult);
        if (readConnector != connectorUsedForOperation) {
            // Writing by different connector that we are going to use for reading: danger of quantum effects
            aResult.setQuantumOperation(true);
        }
    }

    /**
     * Returns known executed deltas as reported by {@link ConnectorInstance#modifyObject(ResourceObjectIdentification,
     * PrismObject, Collection, ConnectorOperationOptions, UcfExecutionContext, OperationResult)}.
     */
    public AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue<?>>>> modifyResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            Collection<? extends ItemDelta<?, ?>> itemDeltas,
            XMLGregorianCalendar now,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OPERATION_MODIFY_RESOURCE_OBJECT)
                .addParam("repoShadow", repoShadow)
                .addArbitraryObjectAsParam("connOptions", connOptions)
                .addArbitraryObjectCollectionAsParam("itemDeltas", itemDeltas)
                .addArbitraryObjectAsContext("ctx", ctx)
                .build();

        try {
            LOGGER.trace("Modifying resource object {}, deltas:\n{}", repoShadow, DebugUtil.debugDumpLazily(itemDeltas, 1));

            ResourceObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();

            Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil.getAllIdentifiers(repoShadow);

            if (!isModifyShadowEnabled(ctx.getProtectedAccountPatterns(expressionFactory, result), repoShadow, result)) {
                if (hasChangesOnResource(itemDeltas)) {
                    throw new SecurityViolationException(
                            String.format("Cannot modify protected resource object (%s): %s",
                                    ctx.getExceptionDescription(), identifiers));
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
            for (ItemDelta<?, ?> modification : itemDeltas) {
                ItemPath path = modification.getPath();
                QName firstPathName = path.firstName();
                if (ShadowUtil.isAttributeModification(firstPathName)) {
                    hasResourceModification = true;
                    QName attrName = path.rest().firstNameOrFail();
                    ResourceAttributeDefinition<?> attrDef =
                            ctx.getObjectDefinitionRequired().findAttributeDefinitionRequired(attrName);
                    if (attrDef.isVolatilityTrigger()) {
                        LOGGER.trace("Will pre-read and re-read object because volatility trigger attribute {} has changed", attrName);
                        hasVolatilityTriggerModification = true;
                        break;
                    }
                } else if (ShadowUtil.isNonAttributeResourceModification(firstPathName)) {
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
            Collection<Operation> operations = new ArrayList<>();
            collectAttributeAndEntitlementChanges(ctx, itemDeltas, operations, repoShadow, result);

            boolean shouldDoPreRead;
            if (hasVolatilityTriggerModification) {
                LOGGER.trace("-> Doing resource object pre-read because of volatility trigger modification");
                shouldDoPreRead = true;
            } else if (ResourceTypeUtil.isAvoidDuplicateValues(ctx.getResource())) {
                LOGGER.trace("Doing resource object pre-read because 'avoidDuplicateValues' is set");
                shouldDoPreRead = true;
            } else if (isRename(ctx, operations)) {
                LOGGER.trace("Doing resource object pre-read because of rename operation");
                shouldDoPreRead = true;
            } else {
                LOGGER.trace("Will not do resource object pre-read because there's no explicit reason to do so");
                shouldDoPreRead = false;
            }

            ShadowType preReadShadow = null;
            if (shouldDoPreRead) {
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

            AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> modifyAsyncRet;
            if (!operations.isEmpty()) {
                assertNoDuplicates(operations);
                // Execute primary ICF operation on this shadow
                modifyAsyncRet = executeModify(
                        ctx,
                        (preReadShadow == null ? repoShadow.clone() : preReadShadow),
                        identifiers,
                        operations,
                        scripts,
                        result,
                        connOptions);
            } else {
                // We have to check BEFORE we add script operations, otherwise the check would be pointless
                LOGGER.trace("No modifications for connector object specified. Skipping processing of subject executeModify.");
                modifyAsyncRet = null;
            }

            // Should contain side-effects. May contain explicitly requested and executed operations.
            Collection<PropertyDelta<PrismPropertyValue<?>>> knownExecutedDeltas;
            if (modifyAsyncRet != null) {
                Collection<PropertyModificationOperation<?>> knownExecutedOperations = modifyAsyncRet.getReturnValue();
                knownExecutedDeltas = convertToPropertyDeltas(knownExecutedOperations);
            } else {
                knownExecutedDeltas = emptyList();
            }

            /*
             *  State of the shadow after execution of the deltas - e.g. with new DN (if it was part of the delta), because this one should be recorded
             *  in groups of which this account is a member of. (In case of object->subject associations.)
             */
            ShadowType shadowAfter = preReadShadow == null ? repoShadow.clone() : preReadShadow.clone();
            for (ItemDelta<?, ?> itemDelta : itemDeltas) {
                itemDelta.applyTo(asPrismObject(shadowAfter));
            }

            ShadowType postReadShadow = null;
            if (hasVolatilityTriggerModification) {
                // There may be other changes that were not detected by the connector. Re-read the object and compare.
                LOGGER.trace("Post-reading resource shadow");
                postReadShadow = preReadShadow(ctx, identifiers, operations, true, repoShadow, result);
                LOGGER.trace("Post-read object:\n{}", DebugUtil.debugDumpLazily(postReadShadow));
                ObjectDelta<ShadowType> resourceShadowDelta = preReadShadow.asPrismObject().diff(asPrismObject(postReadShadow));
                LOGGER.trace("Determined side-effect changes by old-new diff:\n{}", resourceShadowDelta.debugDumpLazily());
                for (ItemDelta<?, ?> modification : resourceShadowDelta.getModifications()) {
                    if (modification.getParentPath().startsWithName(ShadowType.F_ATTRIBUTES) &&
                            !ItemDeltaCollectionsUtil.hasEquivalent(itemDeltas, modification)) {
                        ItemDeltaCollectionsUtil.merge(knownExecutedDeltas, modification);
                    }
                }
                LOGGER.trace("Side-effect changes after merging with old-new diff:\n{}",
                        DebugUtil.debugDumpLazily(knownExecutedDeltas));
            }

            Collection<? extends ItemDelta<?, ?>> allDeltas = new ArrayList<>(itemDeltas);
            ItemDeltaCollectionsUtil.addNotEquivalent(allDeltas, knownExecutedDeltas); // MID-6892

            // Execute entitlement modification on other objects (if needed)
            executeEntitlementChangesModify(ctx,
                    preReadShadow == null ? repoShadow : preReadShadow,
                    postReadShadow == null ? shadowAfter : postReadShadow,
                    scripts, connOptions, allDeltas, result);

            if (!knownExecutedDeltas.isEmpty()) {
                if (preReadShadow != null) {
                    PrismUtil.setDeltaOldValue(
                            preReadShadow.asPrismObject(), knownExecutedDeltas);
                } else {
                    PrismUtil.setDeltaOldValue(
                            asPrismObject(repoShadow), knownExecutedDeltas);
                }
            }

            LOGGER.trace("Modification side-effect changes:\n{}", DebugUtil.debugDumpLazily(knownExecutedDeltas));
            LOGGER.trace("Modified resource object {}", repoShadow);

            computeResultStatus(result);

            AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue<?>>>>
                    aResult = AsynchronousOperationReturnValue.wrap(knownExecutedDeltas, result);
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

    private void assertNoDuplicates(Collection<Operation> operations) throws SchemaException {
        if (InternalsConfig.isSanityChecks()) {
            // MID-3964
            if (MiscUtil.hasDuplicates(operations)) {
                throw new SchemaException("Duplicated changes: " + operations); // TODO context
            }
        }
    }

    private Collection<PropertyDelta<PrismPropertyValue<?>>> convertToPropertyDeltas(
            @NotNull Collection<PropertyModificationOperation<?>> operations) {
        Collection<PropertyDelta<PrismPropertyValue<?>>> deltas = new ArrayList<>();
        for (PropertyModificationOperation<?> mod : operations) {
            //noinspection unchecked
            deltas.add((PropertyDelta<PrismPropertyValue<?>>) mod.getPropertyDelta());
        }
        return deltas;
    }

    private AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> executeModify(
            ProvisioningContext ctx,
            ShadowType currentShadow,
            Collection<? extends ResourceAttribute<?>> identifiers,
            @NotNull Collection<Operation> operations,
            OperationProvisioningScriptsType scripts,
            OperationResult result,
            ConnectorOperationOptions connOptions)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            PolicyViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        // Should include known side effects. May include also executed requested changes. See ConnectorInstance.modifyObject.
        Collection<PropertyModificationOperation<?>> knownExecutedChanges = new HashSet<>();

        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();
        if (operations.isEmpty()) {
            LOGGER.trace("No modifications for resource object. Skipping modification.");
            return null;
        } else {
            LOGGER.trace("Resource object modification operations: {}", operations);
        }

        ctx.checkExecutionFullyPersistent();

        checkForCapability(ctx, UpdateCapabilityType.class);

        if (!ShadowUtil.hasPrimaryIdentifier(identifiers, objectDefinition)) {
            Collection<? extends ResourceAttribute<?>> primaryIdentifiers =
                    resourceObjectReferenceResolver.resolvePrimaryIdentifier(ctx, identifiers,
                            "modification of resource object " + identifiers, result);
            if (primaryIdentifiers == null || primaryIdentifiers.isEmpty()) {
                throw new ObjectNotFoundException(
                        String.format("Cannot find repository shadow for identifiers %s (%s)",
                                identifiers, ctx.getExceptionDescription()),
                        ShadowType.class,
                        null,
                        ctx.isAllowNotFound());
            }
            Collection<ResourceAttribute<?>> allIdentifiers = new ArrayList<>();
            allIdentifiers.addAll(identifiers);
            allIdentifiers.addAll(primaryIdentifiers);
            identifiers = allIdentifiers;
        }

        executeProvisioningScripts(ctx, ProvisioningOperationTypeType.MODIFY, BeforeAfterType.BEFORE, scripts, result);

        // Invoke connector operation
        ConnectorInstance connector = ctx.getConnector(UpdateCapabilityType.class, result);
        AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> connectorAsyncOpRet = null;
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
                    for (Operation origOperation : operations) {
                        if (origOperation instanceof PropertyModificationOperation) {
                            PropertyModificationOperation<?> modificationOperation =
                                    (PropertyModificationOperation<?>) origOperation;
                            PropertyDelta<?> propertyDelta = modificationOperation.getPropertyDelta();
                            PropertyDelta<?> filteredDelta =
                                    ProvisioningUtil.narrowPropertyDelta(
                                            propertyDelta,
                                            currentShadow,
                                            modificationOperation.getMatchingRuleQName(),
                                            matchingRuleRegistry);
                            if (filteredDelta != null && !filteredDelta.isEmpty()) {
                                if (propertyDelta == filteredDelta) {
                                    filteredOperations.add(origOperation);
                                } else {
                                    PropertyModificationOperation<?> newOp = new PropertyModificationOperation<>(filteredDelta);
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
                        ctx.getResource(), objectDefinition.getHumanReadableName(),
                        SchemaDebugUtil.debugDump(identifiers, 1), SchemaDebugUtil.debugDump(operations, 1));
            }

            if (!ResourceTypeUtil.isUpdateCapabilityEnabled(ctx.getResource())) {
                if (operations.isEmpty()) {
                    LOGGER.debug("No modifications for connector object specified (after filtering). Skipping processing.");
                    result.recordSuccess();
                    return null;
                }
                UnsupportedOperationException e = new UnsupportedOperationException("Resource does not support 'update' operation");
                result.recordFatalError(e);
                throw e;
            }

            Collection<ResourceAttribute<?>> identifiersWorkingCopy = cloneIdentifiers(identifiers);            // because identifiers can be modified e.g. on rename operation
            List<Collection<Operation>> operationsWaves = sortOperationsIntoWaves(operations, objectDefinition);
            LOGGER.trace("Operation waves: {}", operationsWaves.size());
            boolean inProgress = false;
            String asynchronousOperationReference = null;
            for (Collection<Operation> operationsWave : operationsWaves) {
                operationsWave = convertToReplaceAsNeeded(
                        ctx, currentShadow, operationsWave, identifiersWorkingCopy, objectDefinition, result);

                if (operationsWave.isEmpty()) {
                    continue;
                }

                try {
                    ResourceObjectIdentification identification = ResourceObjectIdentification.create(objectDefinition, identifiersWorkingCopy);
                    connectorAsyncOpRet = connector.modifyObject(
                            identification, asPrismObject(currentShadow), operationsWave, connOptions, ctx.getUcfExecutionContext(), result);
                    Collection<PropertyModificationOperation<?>> currentKnownExecutedChanges = connectorAsyncOpRet.getReturnValue();
                    if (currentKnownExecutedChanges != null) {
                        knownExecutedChanges.addAll(currentKnownExecutedChanges);
                        // we accept that one attribute can be changed multiple times in sideEffectChanges; TODO: normalize
                    }
                    if (connectorAsyncOpRet.isInProgress()) {
                        inProgress = true;
                        asynchronousOperationReference = connectorAsyncOpRet.getOperationResult().getAsynchronousOperationReference();
                    }
                } finally {
                    shadowAuditHelper.auditEvent(AuditEventType.MODIFY_OBJECT, currentShadow, operationsWave, ctx, result);
                }
            }

            LOGGER.debug("PROVISIONING MODIFY successful, inProgress={}, known executed changes (potentially including "
                    + "side-effects):\n{}", inProgress, DebugUtil.debugDumpLazily(knownExecutedChanges));

            if (inProgress) {
                result.setInProgress();
                result.setAsynchronousOperationReference(asynchronousOperationReference);
            }
        } catch (ObjectNotFoundException ex) {
            throw ex.wrap(String.format("Object to modify was not found (%s)", ctx.getExceptionDescription(connector)));
        } catch (CommunicationException ex) {
            throw communicationException(ctx, connector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, connector, ex);
        } catch (ObjectAlreadyExistsException ex) {
            throw objectAlreadyExistsException("Conflict during 'modify' operation: ", ctx, connector, ex);
        }

        executeProvisioningScripts(ctx, ProvisioningOperationTypeType.MODIFY, BeforeAfterType.AFTER, scripts, result);

        AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> asyncOpRet =
                AsynchronousOperationReturnValue.wrap(knownExecutedChanges, result);
        if (connectorAsyncOpRet != null) {
            asyncOpRet.setOperationType(connectorAsyncOpRet.getOperationType());
        }
        return asyncOpRet;
    }

    private Collection<Operation> convertToReplaceAsNeeded(
            ProvisioningContext ctx,
            ShadowType currentShadow,
            Collection<Operation> operationsWave,
            Collection<ResourceAttribute<?>> identifiers,
            ResourceObjectDefinition objectDefinition,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        Collection<ResourceAttributeDefinition<?>> readReplaceAttributes = determineReadReplace(ctx, operationsWave, objectDefinition);
        LOGGER.trace("Read+Replace attributes: {}", readReplaceAttributes);
        if (!readReplaceAttributes.isEmpty()) {
            AttributesToReturn attributesToReturn = new AttributesToReturn();
            attributesToReturn.setReturnDefaultAttributes(false);
            attributesToReturn.setAttributesToReturn(readReplaceAttributes);
            // TODO eliminate this fetch if this is first wave and there are no explicitly requested attributes
            // but make sure currentShadow contains all required attributes
            LOGGER.trace("Fetching object because of READ+REPLACE mode");
            PrismObject<ShadowType> fetchedShadow =
                    fetchResourceObject(
                            ctx, identifiers, attributesToReturn, asPrismObject(currentShadow), false, result);
            operationsWave = convertToReplace(ctx, operationsWave, fetchedShadow, false);
        }
        UpdateCapabilityType updateCapability = ctx.getCapability(UpdateCapabilityType.class); // TODO what if it's disabled?
        if (updateCapability != null) {
            AttributeContentRequirementType attributeContentRequirement = updateCapability.getAttributeContentRequirement();
            if (AttributeContentRequirementType.ALL.equals(attributeContentRequirement)) {
                LOGGER.trace("AttributeContentRequirement: {} for {}", attributeContentRequirement, ctx.getResource());
                PrismObject<ShadowType> fetchedShadow =
                        fetchResourceObject(
                                ctx, identifiers, null, asPrismObject(currentShadow), false, result);
                if (fetchedShadow == null) {
                    throw new SystemException("Attribute content requirement set for resource " + ctx.toHumanReadableDescription() + ", but read of shadow returned null, identifiers: " + identifiers);
                }
                operationsWave = convertToReplace(ctx, operationsWave, fetchedShadow, true);
            }
        }
        return operationsWave;
    }

    private ShadowType preReadShadow(
            ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers,
            Collection<Operation> operations,
            boolean fetchEntitlements,
            ShadowType repoShadow,
            OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        List<ResourceAttributeDefinition<?>> neededExtraAttributes = new ArrayList<>();
        for (Operation operation : operations) {
            ResourceAttributeDefinition<?> rad = getAttributeDefinitionIfApplicable(operation, ctx.getObjectDefinition());
            if (rad != null && (!rad.isReturnedByDefault() || rad.getFetchStrategy() == AttributeFetchStrategyType.EXPLICIT)) {
                neededExtraAttributes.add(rad);
            }
        }

        AttributesToReturn attributesToReturn = new AttributesToReturn();
        attributesToReturn.setAttributesToReturn(neededExtraAttributes);
        ShadowType currentShadow;
        try {
            currentShadow =
                    fetchResourceObject(
                            ctx, identifiers, attributesToReturn, asPrismObject(repoShadow), fetchEntitlements, parentResult)
                            .asObjectable();
        } catch (ObjectNotFoundException e) {
            // This may happen for semi-manual connectors that are not yet up to date.
            // No big deal. We will have to work without it.
            LOGGER.warn("Cannot pre-read shadow {}, it is probably not present in the {}. Skipping pre-read.", identifiers, ctx.getResource());
            return null;
        }
        if (repoShadow != null) {
            currentShadow.setOid(repoShadow.getOid());
        }
        currentShadow.setName(
                ShadowUtil.determineShadowNameRequired(currentShadow));
        return currentShadow;
    }

    private Collection<ResourceAttributeDefinition<?>> determineReadReplace(
            ProvisioningContext ctx,
            Collection<Operation> operations,
            ResourceObjectDefinition objectDefinition) {
        Collection<ResourceAttributeDefinition<?>> retval = new ArrayList<>();
        for (Operation operation : operations) {
            ResourceAttributeDefinition<?> rad = getAttributeDefinitionIfApplicable(operation, objectDefinition);
            if (rad != null && isReadReplaceMode(ctx, rad, objectDefinition) && operation instanceof PropertyModificationOperation) {        // third condition is just to be sure
                PropertyDelta<?> propertyDelta = ((PropertyModificationOperation<?>) operation).getPropertyDelta();
                if (propertyDelta.isAdd() || propertyDelta.isDelete()) {
                    retval.add(rad);        // REPLACE operations are not needed to be converted to READ+REPLACE
                }
            }
        }
        return retval;
    }

    private boolean isReadReplaceMode(
            ProvisioningContext ctx, ResourceAttributeDefinition<?> rad, ResourceObjectDefinition objectDefinition) {
        if (rad.getReadReplaceMode() != null) {
            return rad.getReadReplaceMode();
        }
        // READ+REPLACE mode is if addRemoveAttributeCapability is NOT present.
        // Try to determine from the capabilities. We may still need to force it.

        UpdateCapabilityType updateCapability =
                objectDefinition.getEnabledCapability(UpdateCapabilityType.class, ctx.getResource());
        if (updateCapability == null) {
            // Strange. We are going to update, but we cannot update? Anyway, let it go, it should throw an error on a more appropriate place.
            return false;
        }
        if (BooleanUtils.isTrue(updateCapability.isDelta())
                || BooleanUtils.isTrue(updateCapability.isAddRemoveAttributeValues())) {
            return false;
        }
        LOGGER.trace("Read+replace mode is forced because {} does not support deltas nor addRemoveAttributeValues",
                ctx.getResource());
        return true;
    }

    private ResourceAttributeDefinition<?> getAttributeDefinitionIfApplicable(
            Operation operation, ResourceObjectDefinition objectDefinition) {
        if (operation instanceof PropertyModificationOperation) {
            PropertyDelta<?> propertyDelta = ((PropertyModificationOperation<?>) operation).getPropertyDelta();
            if (isAttributeDelta(propertyDelta)) {
                QName attributeName = propertyDelta.getElementName();
                return objectDefinition.findAttributeDefinition(attributeName);
            }
        }
        return null;
    }

    /**
     * Converts ADD/DELETE VALUE operations into REPLACE VALUE, if needed
     */
    private Collection<Operation> convertToReplace(
            ProvisioningContext ctx,
            Collection<Operation> operations,
            PrismObject<ShadowType> currentShadow,
            boolean requireAllAttributes) throws SchemaException {
        List<Operation> retval = new ArrayList<>(operations.size());
        for (Operation operation : operations) {
            if (operation instanceof PropertyModificationOperation) {
                PropertyDelta<?> propertyDelta = ((PropertyModificationOperation<?>) operation).getPropertyDelta();
                if (isAttributeDelta(propertyDelta)) {
                    QName attributeName = propertyDelta.getElementName();
                    ResourceAttributeDefinition<?> rad =
                            ctx.getObjectDefinitionRequired().findAttributeDefinitionRequired(attributeName);
                    if ((requireAllAttributes || isReadReplaceMode(ctx, rad, ctx.getObjectDefinition()))
                            && (propertyDelta.isAdd() || propertyDelta.isDelete())) {
                        PropertyModificationOperation<?> newOp =
                                convertToReplace(propertyDelta, currentShadow, rad.getMatchingRuleQName());
                        newOp.setMatchingRuleQName(((PropertyModificationOperation<?>) operation).getMatchingRuleQName());
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
                    ResourceAttributeDefinition<?> rad =
                            ctx.findAttributeDefinitionRequired(currentAttribute.getElementName());
                    if (rad.canModify()) {
                        //noinspection rawtypes
                        PropertyDelta resultingDelta =
                                prismContext.deltaFactory().property()
                                        .create(currentAttribute.getPath(), currentAttribute.getDefinition());
                        //noinspection unchecked
                        resultingDelta.setValuesToReplace(currentAttribute.getClonedValues());
                        //noinspection unchecked,rawtypes
                        retval.add(new PropertyModificationOperation<>(resultingDelta));
                    }
                }
            }
        }
        return retval;
    }

    private boolean containsDelta(Collection<Operation> operations, ItemName attributeName) {
        for (Operation operation : operations) {
            if (operation instanceof PropertyModificationOperation) {
                PropertyDelta<?> propertyDelta = ((PropertyModificationOperation<?>) operation).getPropertyDelta();
                if (isAttributeDelta(propertyDelta) && QNameUtil.match(attributeName, propertyDelta.getElementName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private <T> PropertyModificationOperation<T> convertToReplace(
            PropertyDelta<T> propertyDelta, PrismObject<ShadowType> currentShadow, QName matchingRuleQName)
            throws SchemaException {
        if (propertyDelta.isReplace()) {
            // this was probably checked before
            throw new IllegalStateException("PropertyDelta is both ADD/DELETE and REPLACE");
        }
        Collection<PrismPropertyValue<T>> currentValues = new ArrayList<>();
        if (currentShadow != null) {
            // let's extract (parent-less) current values
            PrismProperty<T> currentProperty = currentShadow.findProperty(propertyDelta.getPath());
            if (currentProperty != null) {
                for (PrismPropertyValue<T> currentValue : currentProperty.getValues()) {
                    currentValues.add(currentValue.clone());
                }
            }
        }
        final MatchingRule<T> matchingRule;
        if (matchingRuleQName != null) {
            PrismPropertyDefinition<T> def = propertyDelta.getDefinition();
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
        EqualsChecker<PrismPropertyValue<T>> equalsChecker =
                (o1, o2) -> o1.equals(o2, EquivalenceStrategy.REAL_VALUE, matchingRule);
        // add values that have to be added
        if (propertyDelta.isAdd()) {
            for (PrismPropertyValue<T> valueToAdd : propertyDelta.getValuesToAdd()) {
                if (!PrismValueCollectionsUtil.containsValue(currentValues, valueToAdd, equalsChecker)) {
                    currentValues.add(valueToAdd.clone());
                } else {
                    LOGGER.warn("Attempting to add a value of {} that is already present in {}: {}",
                            valueToAdd, propertyDelta.getElementName(), currentValues);
                }
            }
        }
        // remove values that should not be there
        if (propertyDelta.isDelete()) {
            for (PrismPropertyValue<T> valueToDelete : propertyDelta.getValuesToDelete()) {
                Iterator<PrismPropertyValue<T>> iterator = currentValues.iterator();
                boolean found = false;
                while (iterator.hasNext()) {
                    PrismPropertyValue<T> pValue = iterator.next();
                    LOGGER.trace("Comparing existing {} to about-to-be-deleted {}, matching rule: {}",
                            pValue, valueToDelete, matchingRule);
                    if (equalsChecker.test(pValue, valueToDelete)) {
                        LOGGER.trace("MATCH! compared existing {} to about-to-be-deleted {}", pValue, valueToDelete);
                        iterator.remove();
                        found = true;
                    }
                }
                if (!found) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.warn("Attempting to remove a value of {} that is not in {}: {}",
                                valueToDelete, propertyDelta.getElementName(), currentValues);
                    } else {
                        LOGGER.warn("Attempting to remove a value of {} that is not in {}",
                                valueToDelete, propertyDelta.getElementName());
                    }
                }
            }
        }
        PropertyDelta<T> resultingDelta = prismContext.deltaFactory().property().create(
                propertyDelta.getPath(), propertyDelta.getPropertyDefinition());
        resultingDelta.setValuesToReplace(currentValues);
        return new PropertyModificationOperation<>(resultingDelta);
    }

    private List<Collection<Operation>> sortOperationsIntoWaves(
            Collection<Operation> operations, ResourceObjectDefinition objectDefinition) {
        TreeMap<Integer, Collection<Operation>> waves = new TreeMap<>(); // operations indexed by priority
        List<Operation> others = new ArrayList<>(); // operations executed at the end (either non-priority ones or non-attribute modifications)
        for (Operation operation : operations) {
            ResourceAttributeDefinition<?> rad = getAttributeDefinitionIfApplicable(operation, objectDefinition);
            if (rad != null && rad.getModificationPriority() != null) {
                putIntoWaves(waves, rad.getModificationPriority(), operation);
                continue;
            }
            others.add(operation);
        }
        // computing the return value
        List<Collection<Operation>> retval = new ArrayList<>(waves.size() + 1);
        Map.Entry<Integer, Collection<Operation>> entry = waves.firstEntry();
        while (entry != null) {
            retval.add(entry.getValue());
            entry = waves.higherEntry(entry.getKey());
        }
        retval.add(others);
        return retval;
    }

    private void putIntoWaves(Map<Integer, Collection<Operation>> waves, Integer key, Operation operation) {
        waves.computeIfAbsent(key, k -> new ArrayList<>())
                .add(operation);
    }

    private Collection<ResourceAttribute<?>> cloneIdentifiers(Collection<? extends ResourceAttribute<?>> identifiers) {
        Collection<ResourceAttribute<?>> retval = new HashSet<>(identifiers.size());
        for (ResourceAttribute<?> identifier : identifiers) {
            retval.add(identifier.clone());
        }
        return retval;
    }

    private boolean isRename(ProvisioningContext ctx, Collection<Operation> modifications) {
        for (Operation op : modifications) {
            if (op instanceof PropertyModificationOperation
                    && isIdentifierDelta(ctx, ((PropertyModificationOperation<?>) op).getPropertyDelta())) {
                return true;
            }
        }
        return false;
    }

    private <T> boolean isIdentifierDelta(ProvisioningContext ctx, PropertyDelta<T> propertyDelta) {
        return ctx.getObjectDefinitionRequired().isPrimaryIdentifier(propertyDelta.getElementName()) ||
                ctx.getObjectDefinitionRequired().isSecondaryIdentifier(propertyDelta.getElementName());
    }

    private void executeEntitlementChangesAdd(
            ProvisioningContext ctx,
            ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap = new HashMap<>();

        entitlementConverter.collectEntitlementsAsObjectOperationInShadowAdd(roMap, shadow, ctx, parentResult);

        executeEntitlements(ctx, roMap, connOptions, parentResult);
    }

    private void executeEntitlementChangesModify(ProvisioningContext ctx,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            Collection<? extends ItemDelta<?, ?>> subjectDeltas,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap = new HashMap<>();

        LOGGER.trace("executeEntitlementChangesModify, old shadow:\n{}", subjectShadowBefore.debugDumpLazily(1));

        for (ItemDelta<?, ?> subjectDelta : subjectDeltas) {
            ItemPath subjectItemPath = subjectDelta.getPath();

            if (ShadowType.F_ASSOCIATION.equivalent(subjectItemPath)) {
                //noinspection unchecked
                ContainerDelta<ShadowAssociationType> containerDelta = (ContainerDelta<ShadowAssociationType>) subjectDelta;
                subjectShadowAfter = entitlementConverter.collectEntitlementsAsObjectOperation(
                        roMap, containerDelta, subjectShadowBefore, subjectShadowAfter, ctx, parentResult);

            } else {

                ContainerDelta<ShadowAssociationType> associationDelta =
                        prismContext.deltaFactory().container().createDelta(
                                ShadowType.F_ASSOCIATION, subjectShadowBefore.asPrismObject().getDefinition());
                PrismContainer<ShadowAssociationType> associationContainer =
                        subjectShadowBefore.asPrismObject().findContainer(ShadowType.F_ASSOCIATION);
                if (associationContainer == null || associationContainer.isEmpty()) {
                    LOGGER.trace("No shadow association container in old shadow. Skipping processing entitlements change for {}.",
                            subjectItemPath);
                    continue;
                }
                LOGGER.trace("Processing association container in old shadow for {}:\n{}",
                        subjectItemPath, associationContainer.debugDumpLazily(1));

                // Delete + re-add association values that should ensure correct functioning in case of rename
                // This has to be done only for associations that require explicit referential integrity.
                // For these that do not, it is harmful, so it must be skipped.
                for (PrismContainerValue<ShadowAssociationType> associationValue : associationContainer.getValues()) {
                    QName associationName = associationValue.asContainerable().getName();
                    if (associationName == null) {
                        throw new IllegalStateException(String.format("No association name in %s (%s)",
                                associationValue, ctx.getExceptionDescription()));
                    }
                    ResourceAssociationDefinition associationDefinition =
                            ctx.getObjectDefinitionRequired().findAssociationDefinition(associationName);
                    if (associationDefinition == null) {
                        throw new IllegalStateException(String.format("No association definition for %s (%s)",
                                associationValue, ctx.getExceptionDescription()));
                    }
                    if (!associationDefinition.requiresExplicitReferentialIntegrity()) {
                        continue;
                    }
                    QName valueAttributeName = associationDefinition.getDefinitionBean().getValueAttribute();
                    if (!ShadowUtil.matchesAttribute(subjectItemPath, valueAttributeName)) {
                        continue;
                    }
                    if (isRenameReal(subjectShadowBefore, subjectShadowAfter, subjectItemPath)) {
                        LOGGER.trace("Processing association {} on rename", associationName);
                        //noinspection unchecked
                        associationDelta.addValuesToDelete(associationValue.clone());
                        //noinspection unchecked
                        associationDelta.addValuesToAdd(associationValue.clone());
                    } else {
                        LOGGER.trace("NOT processing association {} because the rename is phantom", associationName);
                    }
                }
                LOGGER.trace("Resulting association delta for {}:\n{}", subjectItemPath, associationDelta.debugDumpLazily(1));
                if (!associationDelta.isEmpty()) {
                    entitlementConverter.collectEntitlementsAsObjectOperation(roMap, associationDelta, subjectShadowBefore, subjectShadowAfter, ctx, parentResult);
                }

            }
        }

        executeEntitlements(ctx, roMap, connOptions, parentResult);
    }

    private <T> boolean isRenameReal(ShadowType objectBefore, ShadowType objectAfter, ItemPath itemPath) throws SchemaException {
        PrismProperty<T> propertyBefore = objectBefore.asPrismObject().findProperty(itemPath);
        PrismProperty<T> propertyAfter = objectAfter.asPrismObject().findProperty(itemPath);
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

    private void executeEntitlementChangesDelete(
            ProvisioningContext ctx,
            ShadowType subjectShadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            OperationResult result) throws SchemaException {

        try {

            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap = new HashMap<>();

            entitlementConverter.collectEntitlementsAsObjectOperationDelete(roMap, subjectShadow, ctx, result);

            executeEntitlements(ctx, roMap, connOptions, result);

            // TODO: now just log the errors, but do NOT re-throw the exception (except for some exceptions)
            //  we want the original delete to take place, throwing an exception would spoil that
        } catch (SchemaException | Error e) {
            // These we want to propagate.
            throw e;
        } catch (CommunicationException | ObjectNotFoundException | SecurityViolationException | ConfigurationException |
                ObjectAlreadyExistsException | ExpressionEvaluationException | RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void executeEntitlements(ProvisioningContext subjectCtx,
            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap, ConnectorOperationOptions connOptions, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Excuting entitlement chanes, roMap:\n{}", DebugUtil.debugDump(roMap, 1));
        }

        for (Entry<ResourceObjectDiscriminator, ResourceObjectOperations> entry : roMap.entrySet()) {
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

            } catch (ObjectNotFoundException | CommunicationException | SchemaException | SecurityViolationException |
                    PolicyViolationException | ConfigurationException | ObjectAlreadyExistsException |
                    ExpressionEvaluationException e) {
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
            } finally {
                result.computeStatusIfUnknown();
            }
        }
    }

    public SearchResultMetadata searchResourceObjects(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectHandler resultHandler,
            @Nullable ObjectQuery query,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod,
            @NotNull OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return new ResourceObjectSearchOperation(ctx, resultHandler, query, fetchAssociations, errorReportingMethod, beans)
                .execute(parentResult);
    }

    public Integer countResourceObjects(
            @NotNull ProvisioningContext ctx,
            @Nullable ObjectQuery query,
            @NotNull OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return new ResourceObjectCountOperation(ctx, query, beans)
                .execute(parentResult);
    }

    public LiveSyncToken fetchCurrentToken(ProvisioningContext ctx, OperationResult parentResult)
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

        computeResultStatus(parentResult);

        return TokenUtil.fromUcf(lastToken);
    }

    /**
     * @param repoShadow Used when read capability is "caching only"
     */
    PrismObject<ShadowType> fetchResourceObject(
            ProvisioningContext ctx,
            @NotNull Collection<? extends ResourceAttribute<?>> identifiers,
            AttributesToReturn attributesToReturn,
            @Nullable PrismObject<ShadowType> repoShadow,
            boolean fetchAssociations,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        PrismObject<ShadowType> resourceObject =
                resourceObjectReferenceResolver.fetchResourceObject(
                        ctx, identifiers, attributesToReturn, repoShadow, result);
        postProcessResourceObjectRead(ctx, resourceObject, fetchAssociations, result);
        return resourceObject;
    }

    /**
     * Attributes returned by the connector update the original shadow: they are either added (if not present before),
     * or they replace their previous versions.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void applyAfterOperationAttributes(ShadowType shadow,
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

    // TODO consider using ShadowUtil.hasResourceModifications
    private boolean hasChangesOnResource(
            Collection<? extends ItemDelta<?, ?>> itemDeltas) {
        for (ItemDelta<?, ?> itemDelta : itemDeltas) {
            if (isAttributeDelta(itemDelta) || SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
                return true;
            } else if (SchemaConstants.PATH_ACTIVATION.equivalent(itemDelta.getParentPath())) {
                return true;
            } else if (ShadowType.F_ASSOCIATION.equivalent(itemDelta.getPath())) {
                return true;
            }
        }
        return false;
    }

    private void collectAttributeAndEntitlementChanges(ProvisioningContext ctx,
            Collection<? extends ItemDelta<?, ?>> objectChange, Collection<Operation> operations,
            ShadowType shadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (operations == null) {
            operations = new ArrayList<>();
        }
        boolean activationProcessed = false;
        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();
        for (ItemDelta<?, ?> itemDelta : objectChange) {
            if (isAttributeDelta(itemDelta) || SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
                if (itemDelta instanceof PropertyDelta) {
                    PropertyModificationOperation<?> attributeModification =
                            new PropertyModificationOperation<>((PropertyDelta<?>) itemDelta);
                    ResourceAttributeDefinition<?> attrDef = objectDefinition.findAttributeDefinition(itemDelta.getElementName());
                    if (attrDef != null) {
                        attributeModification.setMatchingRuleQName(attrDef.getMatchingRuleQName());
                        if (itemDelta.getDefinition() == null) {
                            //noinspection unchecked,rawtypes
                            ((ItemDelta) itemDelta).setDefinition(attrDef);
                        }
                    }
                    operations.add(attributeModification);
                } else if (itemDelta instanceof ContainerDelta) {
                    // skip the container delta - most probably password change
                    // - it is processed earlier
                } else {
                    throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
                }
            } else if (SchemaConstants.PATH_ACTIVATION.equivalent(itemDelta.getParentPath())) {
                if (!activationProcessed) {
                    Collection<Operation> activationOperations = new ActivationConverter(ctx, commonBeans)
                            .createActivationChangeOperations(shadow, objectChange, result);
                    operations.addAll(activationOperations);
                    activationProcessed = true;
                }
            } else if (ShadowType.F_ASSOCIATION.equivalent(itemDelta.getPath())) {
                if (itemDelta instanceof ContainerDelta) {
                    //noinspection unchecked
                    entitlementConverter.collectEntitlementChange(operations, (ContainerDelta<ShadowAssociationType>) itemDelta, ctx);
                } else {
                    throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
                }
            } else if (ShadowType.F_AUXILIARY_OBJECT_CLASS.equivalent(itemDelta.getPath())) {
                if (itemDelta instanceof PropertyDelta) {
                    PropertyModificationOperation<?> attributeModification =
                            new PropertyModificationOperation<>((PropertyDelta<?>) itemDelta);
                    operations.add(attributeModification);
                } else {
                    throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
                }
            } else {
                LOGGER.trace("Skip converting item delta: {}. It's not resource object change, but it is shadow change.", itemDelta);
            }
        }
    }

    private boolean isAttributeDelta(ItemDelta<?, ?> itemDelta) {
        return ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath());
    }

    public UcfFetchChangesResult fetchChanges(ProvisioningContext ctx, @NotNull LiveSyncToken initialToken,
            @Nullable Integer maxChangesConfigured, ResourceObjectLiveSyncChangeListener outerListener,
            OperationResult gResult) throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, GenericFrameworkException, ObjectNotFoundException, ExpressionEvaluationException {

        LOGGER.trace("START fetch changes from {}, objectClass: {}", initialToken, ctx.getObjectClassDefinition());
        AttributesToReturn attrsToReturn;
        if (ctx.isWildcard()) {
            attrsToReturn = null;
        } else {
            attrsToReturn = ctx.createAttributesToReturn();
        }

        ConnectorInstance connector = ctx.getConnector(LiveSyncCapabilityType.class, gResult);
        Integer maxChanges = getMaxChanges(maxChangesConfigured, ctx);

        AtomicInteger processed = new AtomicInteger(0);
        UcfLiveSyncChangeListener localListener = (ucfChange, lParentResult) -> {
            int changeNumber = processed.getAndIncrement();

            Task task = ctx.getTask();
            OperationResult lResult = lParentResult.subresult(OPERATION_HANDLE_CHANGE)
                    .setMinor()
                    .addParam("number", changeNumber)
                    .addParam("localSequenceNumber", ucfChange.getLocalSequenceNumber())
                    .addArbitraryObjectAsParam("primaryIdentifier", ucfChange.getPrimaryIdentifierRealValue())
                    .addArbitraryObjectAsParam("token", ucfChange.getToken()).build();

            try {
                ResourceObjectLiveSyncChange change = new ResourceObjectLiveSyncChange(ucfChange,
                        null, ResourceObjectConverter.this, ctx, attrsToReturn);
                change.initialize(task, lResult);
                return outerListener.onChange(change, lResult);
            } catch (Throwable t) {
                lResult.recordFatalError(t);
                throw t;
            } finally {
                lResult.computeStatusIfUnknown();
            }
        };

        // get changes from the connector
        UcfFetchChangesResult fetchChangesResult = connector.fetchChanges(ctx.getObjectDefinition(),
                TokenUtil.toUcf(initialToken), attrsToReturn, maxChanges, ctx.getUcfExecutionContext(), localListener, gResult);

        computeResultStatus(gResult);

        LOGGER.trace("END fetch changes ({} changes); interrupted = {}; all fetched = {}, final token = {}", processed.get(),
                !ctx.canRun(), fetchChangesResult.isAllChangesFetched(), fetchChangesResult.getFinalToken());

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

    public void listenForAsynchronousUpdates(@NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectAsyncChangeListener outerListener, @NotNull OperationResult parentResult) throws SchemaException,
            CommunicationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {

        LOGGER.trace("Listening for async updates in {}", ctx);
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

        ProvisioningUtil.setEffectiveProvisioningPolicy(ctx, resourceObjectBean, expressionFactory, result);

        if (resourceObjectBean.isExists() == null) {
            resourceObjectBean.setExists(true);
        }

        new ActivationConverter(ctx, commonBeans)
                .completeActivation(resourceObject, result);

        // Entitlements
        if (fetchAssociations) {
            entitlementConverter.postProcessEntitlementsRead(resourceObject, ctx, result);
        }
    }

    private void executeProvisioningScripts(
            ProvisioningContext ctx,
            ProvisioningOperationTypeType provisioningOperationType,
            BeforeAfterType beforeAfter,
            OperationProvisioningScriptsType scripts,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, GenericConnectorException {
        Collection<ExecuteProvisioningScriptOperation> operations =
                determineExecuteScriptOperations(provisioningOperationType, beforeAfter, scripts, ctx.getResource());
        if (operations == null || operations.isEmpty()) {
            return;
        }
        ctx.checkExecutionFullyPersistent();
        ConnectorInstance connector = ctx.getConnector(ScriptCapabilityType.class, result);
        for (ExecuteProvisioningScriptOperation operation : operations) {
            UcfExecutionContext ucfCtx = new UcfExecutionContext(
                    lightweightIdentifierGenerator, ctx.getResource(), ctx.getTask());

            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("PROVISIONING SCRIPT EXECUTION {} {} operation on resource {}",
                            beforeAfter.value(), provisioningOperationType.value(),
                            ctx.getResource());
                }

                Object returnedValue = connector.executeScript(operation, ucfCtx, result);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("PROVISIONING SCRIPT EXECUTION {} {} successful, returned value: {}",
                            beforeAfter.value(), provisioningOperationType.value(), returnedValue);
                }

            } catch (CommunicationException ex) {
                String message = String.format(
                        "Could not execute provisioning script. Error communicating with the connector (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage());
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, ex);
                    result.markExceptionRecorded();
                    throw new CommunicationException(message, ex);
                } else {
                    LOGGER.warn("{}", message);
                }
            } catch (GenericFrameworkException ex) {
                String message = String.format(
                        "Could not execute provisioning script. Generic error in connector (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage());
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, ex);
                    result.markExceptionRecorded();
                    throw new GenericConnectorException(message, ex);
                } else {
                    LOGGER.warn("{}", message);
                }
            } catch (Throwable t) {
                String message = String.format(
                        "Could not execute provisioning script. Unexpected error in connector (%s): %s: %s",
                        ctx.getExceptionDescription(connector), t.getClass().getSimpleName(), t.getMessage());
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, t);
                    result.markExceptionRecorded();
                    throw t;
                } else {
                    LOGGER.warn("{}", message);
                }
            }
        }
    }

    private Collection<ExecuteProvisioningScriptOperation> determineExecuteScriptOperations(
            ProvisioningOperationTypeType provisioningOperationType,
            BeforeAfterType beforeAfter,
            OperationProvisioningScriptsType scripts,
            ResourceType resource) throws SchemaException {
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

    public AsynchronousOperationResult refreshOperationStatus(
            ProvisioningContext ctx, ShadowType shadow, String asyncRef, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

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

                status = ((AsynchronousOperationQueryable) connector).queryOperationStatus(asyncRef, result);

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
        updateQuantum(ctx, connector, asyncResult, parentResult); // We have to use parent result here because the result is closed.
        return asyncResult;
    }

    /**
     * Does _not_ close the operation result, just sets its status (and async operation reference).
     */
    static void computeResultStatus(OperationResult result) {
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

    private <C extends CapabilityType> void checkForCapability(ProvisioningContext ctx, Class<C> capabilityClass) {
        if (!ctx.hasCapability(capabilityClass)) {
            throw new UnsupportedOperationException(
                    String.format("Operation not supported %s as %s is missing", ctx.getDesc(), capabilityClass.getSimpleName()));
        }
    }

    public ShadowCaretaker getShadowCaretaker() {
        return shadowCaretaker;
    }

    public ResourceObjectsBeans getBeans() {
        return beans;
    }

    private static ObjectAlreadyExistsException objectAlreadyExistsException(
            String message, ProvisioningContext ctx, ConnectorInstance connector, ObjectAlreadyExistsException ex) {
        return new ObjectAlreadyExistsException(
                String.format("%sObject already exists on the resource (%s): %s",
                        message, ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    private static GenericConnectorException genericConnectorException(
            ProvisioningContext ctx, ConnectorInstance connector, GenericFrameworkException ex) {
        return new GenericConnectorException(
                String.format("Generic error in connector (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    private static CommunicationException communicationException(
            ProvisioningContext ctx, ConnectorInstance connector, CommunicationException ex) {
        return new CommunicationException(
                String.format("Error communicating with the resource (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    private static ConfigurationException configurationException(
            ProvisioningContext ctx, ConnectorInstance connector, ConfigurationException ex) {
        return new ConfigurationException(
                String.format("Configuration error (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }
}
