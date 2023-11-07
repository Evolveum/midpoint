/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AttributeContentRequirementType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.*;
import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.objectAlreadyExistsException;
import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectModifyOperation.isAttributeDelta;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

/**
 * Responsibilities:
 *
 * . invoking UCF modify operation, including before/after scripts
 * . filtering out duplicate values (doing pre-read if needed)
 * . invoking operations in waves according to {@link ResourceAttributeDefinition#getModificationPriority()}
 * . treating READ+REPLACE mode
 *
 * Invoked either from {@link ResourceObjectModifyOperation} or when entitlement-side changes are executed.
 */
class ResourceObjectUcfModifyOperation extends ResourceObjectProvisioningOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectUcfModifyOperation.class);

    private final ProvisioningContext ctx;
    private ShadowType currentShadow;
    private final ResourceObjectIdentification identification;
    private Collection<Operation> operations;
    private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    private ResourceObjectUcfModifyOperation(
            ProvisioningContext ctx,
            ShadowType currentShadow,
            ResourceObjectIdentification identification,
            @NotNull Collection<Operation> operations,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions) {
        super(ctx, scripts, connOptions);
        this.ctx = ctx;
        this.currentShadow = currentShadow;
        this.identification = identification;
        this.operations = operations;
    }

    static AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> execute(
            ProvisioningContext ctx,
            ShadowType currentShadow,
            ResourceObjectIdentification identification,
            @NotNull Collection<Operation> operations,
            OperationProvisioningScriptsType scripts,
            OperationResult result,
            ConnectorOperationOptions connOptions)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            PolicyViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        return new ResourceObjectUcfModifyOperation(ctx, currentShadow, identification, operations, scripts, connOptions)
                .doExecuteModify(result);
    }

    private AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> doExecuteModify(
            OperationResult result)
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

        // What about protected object check here? It was done for the subject, but not for the entitlements.
        ctx.checkExecutionFullyPersistent();
        ctx.checkForCapability(UpdateCapabilityType.class);

        ResourceObjectIdentification.Primary primaryIdentification =
                b.resourceObjectReferenceResolver.resolvePrimaryIdentifiers(ctx, identification, result);

        executeProvisioningScripts(ProvisioningOperationTypeType.MODIFY, BeforeAfterType.BEFORE, result);

        // Invoke connector operation
        ConnectorInstance connector = ctx.getConnector(UpdateCapabilityType.class, result);
        AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> connectorAsyncOpRet = null;
        try {

            if (ResourceTypeUtil.isAvoidDuplicateValues(ctx.getResource())) {

                if (currentShadow == null) {
                    LOGGER.trace("Fetching shadow for duplicate filtering");
                    currentShadow =
                            preOrPostRead(ctx, primaryIdentification, operations, false, currentShadow, result);
                }

                if (currentShadow == null) {

                    LOGGER.debug("We do not have pre-read shadow, skipping duplicate filtering");

                } else {

                    LOGGER.trace("Filtering out duplicate values");

                    Collection<Operation> filteredOperations = new ArrayList<>(operations.size());
                    for (Operation origOperation : operations) {
                        if (origOperation instanceof PropertyModificationOperation<?> modificationOperation) {
                            PropertyDelta<?> propertyDelta = modificationOperation.getPropertyDelta();
                            PropertyDelta<?> filteredDelta =
                                    ProvisioningUtil.narrowPropertyDelta(
                                            propertyDelta,
                                            currentShadow,
                                            modificationOperation.getMatchingRuleQName(),
                                            b.matchingRuleRegistry);
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
                        primaryIdentification.debugDump(1), SchemaDebugUtil.debugDump(operations, 1));
            }

            // because identifiers can be modified e.g. on rename operation (TODO: is this really needed?)
            ResourceObjectIdentification identificationClone = primaryIdentification.clone();
            List<Collection<Operation>> operationsWaves = sortOperationsIntoWaves(operations, objectDefinition);
            LOGGER.trace("Operation waves: {}", operationsWaves.size());
            boolean inProgress = false;
            String asynchronousOperationReference = null;
            for (Collection<Operation> operationsWave : operationsWaves) {
                operationsWave = convertToReplaceAsNeeded(
                        ctx, currentShadow, operationsWave, identificationClone, objectDefinition, result);

                if (operationsWave.isEmpty()) {
                    continue;
                }

                try {
                    connectorAsyncOpRet = connector.modifyObject(
                            identificationClone, asPrismObject(currentShadow), operationsWave,
                            connOptions, ctx.getUcfExecutionContext(), result);
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
                    b.shadowAuditHelper.auditEvent(AuditEventType.MODIFY_OBJECT, currentShadow, operationsWave, ctx, result);
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

        executeProvisioningScripts(ProvisioningOperationTypeType.MODIFY, BeforeAfterType.AFTER, result);

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
            ResourceObjectIdentification identification,
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
            var fetchedResourceObject =
                    b.resourceObjectConverter.fetchResourceObject(
                            ctx, identification.ensurePrimary(), attributesToReturn, currentShadow, false, result);
            operationsWave = convertToReplace(ctx, operationsWave, fetchedResourceObject, false);
        }
        UpdateCapabilityType updateCapability = ctx.getCapability(UpdateCapabilityType.class); // TODO what if it's disabled?
        if (updateCapability != null) {
            AttributeContentRequirementType attributeContentRequirement = updateCapability.getAttributeContentRequirement();
            if (AttributeContentRequirementType.ALL.equals(attributeContentRequirement)) {
                LOGGER.trace("AttributeContentRequirement: {} for {}", attributeContentRequirement, ctx.getResource());
                var fetched =
                        b.resourceObjectConverter.fetchResourceObject(
                                ctx, identification.ensurePrimary(), null, currentShadow, false, result);
                if (fetched == null) {
                    throw new SystemException(
                            "Attribute content requirement set for resource %s, but read of shadow returned null, identifiers: %s"
                                    .formatted(ctx.toHumanReadableDescription(), identification));
                }
                operationsWave = convertToReplace(ctx, operationsWave, fetched, true);
            }
        }
        return operationsWave;
    }

    private Collection<ResourceAttributeDefinition<?>> determineReadReplace(
            ProvisioningContext ctx,
            Collection<Operation> operations,
            ResourceObjectDefinition objectDefinition) {
        Collection<ResourceAttributeDefinition<?>> retval = new ArrayList<>();
        for (Operation operation : operations) {
            ResourceAttributeDefinition<?> rad = operation.getAttributeDefinitionIfApplicable(objectDefinition);
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

    /**
     * Converts ADD/DELETE VALUE operations into REPLACE VALUE, if needed
     */
    private Collection<Operation> convertToReplace(
            ProvisioningContext ctx,
            Collection<Operation> operations,
            @Nullable CompleteResourceObject current,
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
                                convertToReplace(propertyDelta, current, rad.getMatchingRuleQName());
                        newOp.setMatchingRuleQName(((PropertyModificationOperation<?>) operation).getMatchingRuleQName());
                        retval.add(newOp);
                        continue;
                    }

                }
            }
            retval.add(operation);        // for yet-unprocessed operations
        }
        if (requireAllAttributes && current != null) {
            for (ResourceAttribute<?> currentAttribute : current.resourceObject().getAttributes()) {
                if (!containsDelta(operations, currentAttribute.getElementName())) {
                    ResourceAttributeDefinition<?> rad =
                            ctx.findAttributeDefinitionRequired(currentAttribute.getElementName());
                    if (rad.canModify()) {
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
            PropertyDelta<T> propertyDelta, @Nullable CompleteResourceObject resourceObject, QName matchingRuleQName)
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
            ResourceAttributeDefinition<?> rad = operation.getAttributeDefinitionIfApplicable(objectDefinition);
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
