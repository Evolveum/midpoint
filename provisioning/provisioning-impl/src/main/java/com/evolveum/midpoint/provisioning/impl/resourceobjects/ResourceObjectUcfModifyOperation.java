/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.*;
import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectModifyOperation.isAttributeDelta;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.util.*;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AttributeContentRequirementType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

/**
 * Responsibilities:
 *
 * . invoking UCF modify operation, including before/after scripts
 * . filtering out duplicate values (doing pre-read if needed)
 * . invoking operations in waves according to {@link ShadowSimpleAttributeDefinition#getModificationPriority()}
 * . treating READ+REPLACE mode
 *
 * Invoked either from {@link ResourceObjectModifyOperation} or when entitlement-side changes are executed.
 */
class ResourceObjectUcfModifyOperation extends ResourceObjectProvisioningOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectUcfModifyOperation.class);

    private final ProvisioningContext ctx;

    /**
     * The repository shadow. The underlying in-memory bean must not be modified during the operation!
     *
     * TODO review its purpose here!
     */
    private final RepoShadow repoShadow;

    private ExistingResourceObjectShadow currentObject;

    @NotNull private final ResourceObjectIdentification.WithPrimary identification;
    private Collection<Operation> operations;
    private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    private ResourceObjectUcfModifyOperation(
            ProvisioningContext ctx,
            RepoShadow repoShadow,
            ExistingResourceObjectShadow currentObject,
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            @NotNull Collection<Operation> operations,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions) {
        super(ctx, scripts, connOptions);
        this.ctx = ctx;
        this.repoShadow = repoShadow;
        this.currentObject = currentObject;
        this.identification = identification;
        this.operations = operations;
    }

    /**
     * The identification may or may not be primary. In the latter case, it is resolved using
     * {@link ResourceObjectReferenceResolver} (currently, through the repo search).
     */
    static @NotNull UcfModifyReturnValue execute(
            ProvisioningContext ctx,
            RepoShadow repoShadow,
            ExistingResourceObjectShadow preReadObject,
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            @NotNull Collection<Operation> operations,
            OperationProvisioningScriptsType scripts,
            OperationResult result,
            ConnectorOperationOptions connOptions)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            PolicyViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        return new ResourceObjectUcfModifyOperation(ctx, repoShadow, preReadObject, identification, operations, scripts, connOptions)
                .doExecuteModify(result);
    }

    private @NotNull UcfModifyReturnValue doExecuteModify(OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            PolicyViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        // Should include known side effects. May include also executed requested changes. See ConnectorInstance.modifyObject.
        Collection<PropertyModificationOperation<?>> knownExecutedChanges = new HashSet<>();

        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();
        if (operations.isEmpty()) {
            LOGGER.trace("No modifications for resource object. Skipping modification.");
            return UcfModifyReturnValue.empty();
        } else {
            LOGGER.trace("Resource object modification operations: {}", operations);
        }

        // What about protected object check here? It was done for the subject, but not for the entitlements.
        ctx.checkExecutionFullyPersistent();
        ctx.checkForCapability(UpdateCapabilityType.class);

        executeProvisioningScripts(ProvisioningOperationTypeType.MODIFY, BeforeAfterType.BEFORE, result);

        // Invoke connector operation
        ConnectorInstance connector = ctx.getConnector(UpdateCapabilityType.class, result);
        UcfModifyReturnValue connectorRetVal = UcfModifyReturnValue.empty();
        try {

            if (ResourceTypeUtil.isAvoidDuplicateValues(ctx.getResource())) {

                if (currentObject == null) {
                    LOGGER.trace("Fetching shadow for duplicate filtering");
                    currentObject =
                            preOrPostRead(ctx, identification, operations, false, repoShadow, result);
                }

                if (currentObject == null) {

                    LOGGER.debug("We do not have pre-read shadow, skipping duplicate filtering");

                } else {

                    LOGGER.trace("Filtering out duplicate values");

                    Collection<Operation> filteredOperations = new ArrayList<>(operations.size());
                    for (Operation origOperation : operations) {
                        if (origOperation instanceof PropertyModificationOperation<?> modificationOperation) {
                            var propertyDelta = modificationOperation.getPropertyDelta();
                            var filteredDelta =
                                    ProvisioningUtil.narrowPropertyDelta(
                                            propertyDelta,
                                            currentObject,
                                            modificationOperation.getMatchingRuleQName(),
                                            b.matchingRuleRegistry);
                            if (filteredDelta != null && !filteredDelta.isEmpty()) {
                                if (propertyDelta == filteredDelta) {
                                    filteredOperations.add(origOperation);
                                } else {
                                    var newOp = new PropertyModificationOperation<>(filteredDelta);
                                    newOp.setMatchingRuleQName(modificationOperation.getMatchingRuleQName());
                                    filteredOperations.add(newOp);
                                }
                            } else {
                                LOGGER.trace("Filtering out modification {} because it has empty delta after narrow", propertyDelta);
                            }
                        } else {
                            filteredOperations.add(origOperation); // Reference attributes are here (not deduplicating them now)
                        }
                    }
                    if (filteredOperations.isEmpty()) {
                        LOGGER.debug("No modifications for connector object specified (after filtering). Skipping processing.");
                        result.recordSuccess();
                        return UcfModifyReturnValue.empty();
                    }
                    operations = filteredOperations;
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "PROVISIONING MODIFY operation on {}\nMODIFY object, object type {}, identified by:\n{}\n changes:\n{}",
                        ctx.getResource(), objectDefinition.getHumanReadableName(),
                        identification.debugDump(1), SchemaDebugUtil.debugDump(operations, 1));
            }

            // because identifiers can be modified e.g. on rename operation (TODO: is this really needed?)
            var identificationClone = identification.clone();
            var operationsWaves = sortOperationsIntoWaves(operations, objectDefinition);
            LOGGER.trace("Operation waves: {}", operationsWaves.size());
            var inProgress = false;
            String asyncOpReference = null;
            for (var operationsInCurrentWave : operationsWaves) {
                operationsInCurrentWave =
                        convertToReplaceAsNeeded(operationsInCurrentWave, identificationClone, objectDefinition, result);
                if (operationsInCurrentWave.isEmpty()) {
                    continue;
                }

                ShadowType magicShadow; // TODO explain or eliminate this
                if (currentObject != null) {
                    magicShadow = currentObject.getBean();
                } else if (repoShadow != null) {
                    magicShadow = repoShadow.getBean();
                } else {
                    magicShadow = null; // some connectors (e.g. manual) will fail on this
                }
                try {
                    connectorRetVal = connector.modifyObject(
                            identificationClone, asPrismObject(magicShadow), operationsInCurrentWave,
                            connOptions, ctx.getUcfExecutionContext(), result);
                    var currentKnownExecutedChanges = connectorRetVal.getExecutedOperations();
                    knownExecutedChanges.addAll(currentKnownExecutedChanges);
                    // we accept that one attribute can be changed multiple times in sideEffectChanges; TODO: normalize
                    if (connectorRetVal.isInProgress()) {
                        inProgress = true;
                        asyncOpReference = connectorRetVal.getAsynchronousOperationReference();
                    }
                } finally {
                    b.shadowAuditHelper.auditEvent(
                            AuditEventType.MODIFY_OBJECT,
                            magicShadow,
                            operationsInCurrentWave,
                            ctx,
                            result);
                }
            }

            LOGGER.debug("PROVISIONING MODIFY successful, inProgress={}, known executed changes (potentially including "
                    + "side-effects):\n{}", inProgress, DebugUtil.debugDumpLazily(knownExecutedChanges));

            if (inProgress) {
                result.setInProgress();
                result.setAsynchronousOperationReference(asyncOpReference);
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

        executeProvisioningScripts(ProvisioningOperationTypeType.MODIFY, BeforeAfterType.AFTER, result);

        // This is a brutal approximation. For example, if there are multiple asynchronous operation references,
        // only the last one is preserved. But we shouldn't support multiple waves with asynchronous resources anyway.
        return UcfModifyReturnValue.fromResult(
                knownExecutedChanges,
                result,
                connectorRetVal.getOperationType());
    }

    private Collection<Operation> convertToReplaceAsNeeded(
            Collection<Operation> operationsWave,
            ResourceObjectIdentification.WithPrimary identification,
            ResourceObjectDefinition objectDefinition,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        var readReplaceAttributes = determineReadReplaceAttributes(ctx, operationsWave, objectDefinition);
        LOGGER.trace("Read+Replace attributes: {}", readReplaceAttributes);
        if (!readReplaceAttributes.isEmpty()) {
            ShadowItemsToReturn shadowItemsToReturn = new ShadowItemsToReturn();
            shadowItemsToReturn.setReturnDefaultAttributes(false);
            shadowItemsToReturn.setItemsToReturn(readReplaceAttributes);
            // TODO eliminate this fetch if this is first wave and there are no explicitly requested attributes
            //  but make sure currentShadow contains all required attributes
            LOGGER.trace("Fetching object because of READ+REPLACE mode");
            ResourceObjectShadow fetchedResourceObject;
            if (ctx.isReadingCachingOnly()) {
                if (currentObject != null) {
                    fetchedResourceObject =
                            b.resourceObjectConverter.completeResourceObject( // TODO is this ever used/needed?
                                            ctx, currentObject, false, result)
                                    .resourceObject();
                } else if (repoShadow != null) {
                    fetchedResourceObject = repoShadow.asResourceObject();
                } else {
                    return null; // TODO what to do now?
                }
            } else {
                fetchedResourceObject =
                        b.resourceObjectConverter.fetchResourceObject(
                                        ctx, identification, shadowItemsToReturn, false, result)
                                .resourceObject();
            }
            operationsWave = convertToReplace(ctx, operationsWave, fetchedResourceObject, false);
        }
        UpdateCapabilityType updateCapability = ctx.getCapability(UpdateCapabilityType.class); // TODO what if it's disabled?
        if (updateCapability != null) {
            AttributeContentRequirementType attributeContentRequirement = updateCapability.getAttributeContentRequirement();
            if (attributeContentRequirement == AttributeContentRequirementType.ALL) {
                LOGGER.trace("AttributeContentRequirement: {} for {}", attributeContentRequirement, ctx.getResource());
                ResourceObjectShadow fetched;
                if (ctx.isReadingCachingOnly()) {
                    if (currentObject != null) {
                        fetched =
                                b.resourceObjectConverter.completeResourceObject( // TODO is this ever needed?
                                                ctx, currentObject, false, result)
                                        .resourceObject();
                    } else if (repoShadow != null) {
                        fetched = repoShadow.asResourceObject();
                    } else {
                        throw new IllegalStateException(
                                "Attribute content requirement set for resource %s, but there's no shadow, identifiers: %s"
                                        .formatted(ctx.toHumanReadableDescription(), identification));
                    }
                } else {
                    fetched =
                            b.resourceObjectConverter.fetchResourceObject(
                                            ctx, identification, null, false, result)
                                    .resourceObject();
                }
                operationsWave = convertToReplace(ctx, operationsWave, fetched, true);
            }
        }
        return operationsWave;
    }

    private Collection<ShadowSimpleAttributeDefinition<?>> determineReadReplaceAttributes(
            ProvisioningContext ctx,
            Collection<Operation> operations,
            ResourceObjectDefinition objectDefinition) {
        Collection<ShadowSimpleAttributeDefinition<?>> retval = new ArrayList<>();
        for (var operation : operations) {
            var rad = operation.getAttributeDefinitionIfApplicable(objectDefinition);
            if (rad != null
                    && isReadReplaceMode(ctx, rad, objectDefinition)
                    && operation instanceof PropertyModificationOperation<?> propertyModificationOperation) {
                var propertyDelta = propertyModificationOperation.getPropertyDelta();
                if (propertyDelta.isAdd() || propertyDelta.isDelete()) {
                    retval.add(rad);
                } else {
                    // REPLACE operations are not needed to be converted to READ+REPLACE
                }
            }
        }
        return retval;
    }

    private boolean isReadReplaceMode(
            ProvisioningContext ctx, ShadowSimpleAttributeDefinition<?> rad, ResourceObjectDefinition objectDefinition) {
        if (rad.getReadReplaceMode() != null) {
            return rad.getReadReplaceMode();
        }
        // READ+REPLACE mode is if addRemoveAttributeCapability is NOT present.
        // Try to determine from the capabilities. We may still need to force it.

        var updateCapability = objectDefinition.getEnabledCapability(UpdateCapabilityType.class, ctx.getResource());
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

    /**
     * Converts ADD/DELETE VALUE operations into REPLACE VALUE, if needed
     */
    private Collection<Operation> convertToReplace(
            ProvisioningContext ctx,
            Collection<Operation> operations,
            @Nullable ResourceObjectShadow current,
            boolean requireAllAttributes) throws SchemaException {
        List<Operation> retval = new ArrayList<>(operations.size());
        for (Operation operation : operations) {
            if (operation instanceof PropertyModificationOperation) {
                PropertyDelta<?> propertyDelta = ((PropertyModificationOperation<?>) operation).getPropertyDelta();
                if (isAttributeDelta(propertyDelta)) {
                    QName attributeName = propertyDelta.getElementName();
                    ShadowSimpleAttributeDefinition<?> rad =
                            ctx.getObjectDefinitionRequired().findSimpleAttributeDefinitionRequired(attributeName);
                    if ((requireAllAttributes || isReadReplaceMode(ctx, rad, ctx.getObjectDefinition()))
                            && (propertyDelta.isAdd() || propertyDelta.isDelete())) {
                        PropertyModificationOperation<?> newOp =
                                convertToReplace(propertyDelta, current, rad.getMatchingRuleQName());
                        newOp.setMatchingRuleQName(((PropertyModificationOperation<?>) operation).getMatchingRuleQName());
                        retval.add(newOp);
                        continue;
                    }
                }
            }
            retval.add(operation); // for yet-unprocessed operations
        }
        if (requireAllAttributes && current != null) {
            for (var currentAttribute : current.getSimpleAttributes()) { // TODO what about reference attribute?
                if (!containsDelta(operations, currentAttribute.getElementName())) {
                    if (ctx.findAttributeDefinitionRequired(currentAttribute.getElementName()).canModify()) {
                        //noinspection rawtypes
                        PropertyDelta resultingDelta =
                                PrismContext.get().deltaFactory().property()
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
            PropertyDelta<T> propertyDelta, @Nullable ResourceObjectShadow resourceObject, QName matchingRuleQName)
            throws SchemaException {
        if (propertyDelta.isReplace()) {
            // this was probably checked before
            throw new IllegalStateException("PropertyDelta is both ADD/DELETE and REPLACE");
        }
        Collection<PrismPropertyValue<T>> currentValues = new ArrayList<>();
        if (resourceObject != null) {
            // let's extract (parent-less) current values
            PrismProperty<T> currentProperty = resourceObject.getPrismObject().findProperty(propertyDelta.getPath());
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
            matchingRule = b.matchingRuleRegistry.getMatchingRule(matchingRuleQName, typeName);
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
        PropertyDelta<T> resultingDelta = PrismContext.get().deltaFactory().property().create(
                propertyDelta.getPath(), propertyDelta.getPropertyDefinition());
        resultingDelta.setValuesToReplace(currentValues);
        return new PropertyModificationOperation<>(resultingDelta);
    }

    private List<Collection<Operation>> sortOperationsIntoWaves(
            Collection<Operation> operations, ResourceObjectDefinition objectDefinition) {
        TreeMap<Integer, Collection<Operation>> waves = new TreeMap<>(); // operations indexed by priority
        List<Operation> others = new ArrayList<>(); // operations executed at the end (either non-priority ones or non-attribute modifications)
        for (Operation operation : operations) {
            ShadowSimpleAttributeDefinition<?> rad = operation.getAttributeDefinitionIfApplicable(objectDefinition);
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

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
