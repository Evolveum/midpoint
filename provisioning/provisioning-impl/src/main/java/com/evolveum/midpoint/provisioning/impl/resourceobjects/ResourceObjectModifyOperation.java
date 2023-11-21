/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;
import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.computeResultStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectFuturizer;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementConverter.EntitlementObjectsOperations;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.UcfModifyReturnValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Responsibilities:
 *
 * . check for protected objects (other checks are postponed to {@link ResourceObjectUcfModifyOperation})
 * . pre-reading objects because of volatile attributes, renames, and avoidDuplicateValues option
 * . post-reading objects to determine exact changes (because of volatile attributes)
 * . determining known executed deltas (i.e. side-effect changes), including estimated old values
 */
public class ResourceObjectModifyOperation extends ResourceObjectProvisioningOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectModifyOperation.class);

    @NotNull private final ProvisioningContext ctx;
    @NotNull private final ResourceObjectDefinition objectDefinition;
    @NotNull private final ResourceObjectIdentification.WithPrimary identification;
    @NotNull private final RepoShadow repoShadow;
    private final Collection<? extends ItemDelta<?, ?>> requestedDeltas;
    private final XMLGregorianCalendar now;

    /** Should contain side-effects. May contain explicitly requested and executed operations. */
    private final Collection<PropertyDelta<?>> knownExecutedDeltas = new ArrayList<>();

    private ResourceObjectModifyOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            Collection<? extends ItemDelta<?, ?>> requestedDeltas,
            XMLGregorianCalendar now) throws SchemaException {
        super(ctx, scripts, connOptions);
        this.ctx = ctx;
        this.objectDefinition = ctx.getObjectDefinitionRequired();
        this.identification = repoShadow.getIdentificationRequired().ensurePrimary();
        this.repoShadow = repoShadow;
        this.requestedDeltas = requestedDeltas;
        this.now = now;
    }

    public static ResourceObjectModifyReturnValue execute(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            Collection<? extends ItemDelta<?, ?>> requestedDeltas,
            XMLGregorianCalendar now,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        return new ResourceObjectModifyOperation(ctx, repoShadow, scripts, connOptions, requestedDeltas, now)
                .doExecute(result);
    }

    private ResourceObjectModifyReturnValue doExecute(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        LOGGER.trace("Modifying resource object {}, deltas:\n{}", repoShadow, DebugUtil.debugDumpLazily(requestedDeltas, 1));

        if (!ShadowUtil.hasResourceModifications(requestedDeltas)) {
            // Quit early, so we avoid potential pre-read and other processing when there is no point of doing so.
            // Also the induced read ops may fail which may invoke consistency mechanism which will complicate the situation.
            LOGGER.trace("No resource modification found for {}, skipping", identification);
            result.recordNotApplicableIfUnknown();
            return ResourceObjectModifyReturnValue.of(result);
        }

        ctx.checkProtectedObjectModification(repoShadow, result);
        // Other checks (execution mode, capability) are executed inside ResourceObjectUcfModifyOperation

        Collection<Operation> ucfOperations = convertToUcfOperations(result);

        boolean hasVolatileAttributeModification = hasVolatileAttributeModification();
        ExistingResourceObject preReadObject = doPreReadIfNeeded(ucfOperations, hasVolatileAttributeModification, result);

        UcfModifyReturnValue modifyResult;
        if (!ucfOperations.isEmpty()) {
            assertNoDuplicates(ucfOperations);
            // Execute primary UCF operation on this shadow
            modifyResult = ResourceObjectUcfModifyOperation.execute(
                    ctx, repoShadow, preReadObject, identification, ucfOperations, scripts, result, connOptions);
        } else {
            // We have to check BEFORE we add script operations, otherwise the check would be pointless
            LOGGER.trace("No modifications for connector object specified. Skipping processing of subject executeModify.");
            modifyResult = null;
        }

        if (modifyResult != null) {
            knownExecutedDeltas.addAll(
                    modifyResult.getExecutedOperationsAsPropertyDeltas());
        }

        ExistingResourceObject postReadObject;
        if (hasVolatileAttributeModification && preReadObject != null) {
            // In rare cases, the object could not be pre-read even if tried to do so. Hence the nullity check.
            postReadObject = doPostReadIfNeeded(ucfOperations, knownExecutedDeltas, preReadObject, result);
        } else {
            postReadObject = null;
        }

        Collection<? extends ItemDelta<?, ?>> allDeltas = new ArrayList<>(requestedDeltas);
        ItemDeltaCollectionsUtil.addNotEquivalent(allDeltas, knownExecutedDeltas); // MID-6892

        // Execute entitlement modification on other objects (if needed)
        determineAndExecuteEntitlementObjectsOperations(
                preReadObject, postReadObject, allDeltas, result);

        if (!knownExecutedDeltas.isEmpty()) {
            PrismObject<ShadowType> source =
                    preReadObject != null
                            ? preReadObject.getPrismObject()
                            : repoShadow.getPrismObject();
            PrismUtil.setDeltaOldValue(source, knownExecutedDeltas);
        }

        LOGGER.trace("Modification side-effect changes:\n{}", DebugUtil.debugDumpLazily(knownExecutedDeltas));
        LOGGER.trace("Modified resource object {}", repoShadow);

        computeResultStatus(result);

        return ResourceObjectModifyReturnValue.of(
                knownExecutedDeltas,
                result,
                modifyResult != null ? modifyResult.getOperationType() : null);
    }

    private @Nullable ExistingResourceObject doPreReadIfNeeded(
            Collection<Operation> ucfOperations, boolean hasVolatileAttributeModification, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        if (!shouldDoPreRead(ucfOperations, hasVolatileAttributeModification)) {
            return null;
        }

        LOGGER.trace("Pre-reading resource object");
        // yes, we need associations here (but why?)
        ExistingResourceObject resourceObject =
                preOrPostRead(ctx, identification, ucfOperations, true, repoShadow, result);
        if (resourceObject == null) {
            return null;
        }
        LOGGER.trace("Pre-read object (straight from the resource):\n{}", DebugUtil.debugDumpLazily(resourceObject, 1));
        // If there are pending changes in the shadow then we have to apply to pre-read object.
        // The pre-read object may be out of date (e.g. in case of semi-manual connectors).
        // In that case we may falsely remove some of the modifications. E.g. in case that
        // account is enabled, then disabled and then enabled again. If backing store still
        // has the account as enabled, then the last enable operation would be ignored.
        // No case is created to re-enable the account. And the account stays disabled at the end.
        ExistingResourceObject futurized = ResourceObjectFuturizer.futurizeResourceObject(
                ctx, repoShadow, resourceObject, true, null, now);
        LOGGER.trace("Pre-read object (applied pending operations):\n{}", DebugUtil.debugDumpLazily(futurized, 1));
        return futurized;
    }

    private boolean shouldDoPreRead(Collection<Operation> operations, boolean hasVolatilityTriggerModification) {
        if (hasVolatilityTriggerModification) {
            LOGGER.trace("-> Doing resource object pre-read because of volatility trigger modification");
            return true;
        } else if (ctx.isAvoidDuplicateValues()) {
            LOGGER.trace("Doing resource object pre-read because 'avoidDuplicateValues' is set");
            return true;
        } else if (Operation.isRename(operations, objectDefinition)) {
            LOGGER.trace("Doing resource object pre-read because of rename operation");
            return true;
        } else {
            LOGGER.trace("Will not do resource object pre-read because there's no explicit reason to do so");
            return false;
        }
    }

    private @Nullable ExistingResourceObject doPostReadIfNeeded(
            @NotNull Collection<Operation> ucfOperations,
            @NotNull Collection<PropertyDelta<?>> knownExecutedDeltas, // in-out parameter
            ExistingResourceObject preReadObject,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        assert preReadObject != null;

        // There may be other changes that were not detected by the connector. Re-read the object and compare.
        LOGGER.trace("Post-reading resource shadow");
        ExistingResourceObject postReadObject = preOrPostRead(
                ctx, identification, ucfOperations, true, repoShadow, result);
        LOGGER.trace("Post-read object:\n{}", DebugUtil.debugDumpLazily(postReadObject));
        if (postReadObject == null) {
            return null; // This may happen in rare cases (e.g. with semi-manual resources)
        }
        ObjectDelta<ShadowType> resourceShadowDelta = preReadObject.getPrismObject().diff(postReadObject.getPrismObject());
        LOGGER.trace("Determined side-effect changes by old-new diff:\n{}", resourceShadowDelta.debugDumpLazily());
        for (ItemDelta<?, ?> modification : resourceShadowDelta.getModifications()) {
            if (modification.getParentPath().startsWithName(ShadowType.F_ATTRIBUTES)
                    && !ItemDeltaCollectionsUtil.hasEquivalent(requestedDeltas, modification)) {
                ItemDeltaCollectionsUtil.merge(knownExecutedDeltas, modification);
            }
        }
        LOGGER.trace("Side-effect changes after merging with old-new diff:\n{}",
                DebugUtil.debugDumpLazily(knownExecutedDeltas));
        return postReadObject;
    }

    private boolean hasVolatileAttributeModification() throws SchemaException {
        for (ItemDelta<?, ?> itemDelta : requestedDeltas) {
            ItemPath path = itemDelta.getPath();
            QName firstPathName = path.firstName();
            if (ShadowUtil.isAttributeModification(firstPathName)) {
                QName attrName = path.rest().firstNameOrFail();
                ResourceAttributeDefinition<?> attrDef =
                        ctx.getObjectDefinitionRequired().findAttributeDefinitionRequired(attrName);
                if (attrDef.isVolatilityTrigger()) {
                    LOGGER.trace("Volatility trigger attribute {} is being changed", attrName);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines and executes the entitlement-related operations on *other objects*, i.e. the entitlements themselves.
     * There are various situations regarding the information available:
     *
     * . both `objectBefore` and `objectAfter` are known (this is the ideal case)
     * . only `objectBefore` is known (there was no need to fetch the object after the operation)
     * . only the {@link #repoShadow} (before operation) is known
     *
     * In the second and the third case, we have to determine the expected object state by applying the deltas.
     */
    private void determineAndExecuteEntitlementObjectsOperations(
            @Nullable ExistingResourceObject objectBefore,
            @Nullable ExistingResourceObject objectAfter,
            @NotNull Collection<? extends ItemDelta<?, ?>> subjectDeltas,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        EntitlementObjectsOperations objectsOperations = new EntitlementObjectsOperations();
        EntitlementConverter entitlementConverter = new EntitlementConverter(ctx);

        ShadowType subjectShadowBefore;
        ShadowType subjectShadowAfter;
        if (objectBefore != null) {
            subjectShadowBefore = objectBefore.getBean();
            if (objectAfter != null) {
                subjectShadowAfter = objectAfter.getBean();
            } else {
                var expectedObjectAfter = objectBefore.clone();
                expectedObjectAfter.updateWith(subjectDeltas);
                subjectShadowAfter = expectedObjectAfter.bean;
            }
        } else {
            assert objectAfter == null;
            subjectShadowBefore = repoShadow.getBean();
            var repoShadowAfter = repoShadow.clone();
            // We hope that all relevant attributes (regarding entitlement search) are in the shadow!
            // We accept that some deltas will not be applied correctly - the modifications that are not relevant for
            // the attributes in the shadow. But we are not interested in these. (We could filter them out, if really necessary.)
            repoShadowAfter.updateWith(subjectDeltas);
            subjectShadowAfter = repoShadowAfter.getBean();
        }

        LOGGER.trace("determineAndExecuteEntitlementObjectsOperations, old subject state:\n{}",
                subjectShadowBefore.debugDumpLazily(1));

        for (ItemDelta<?, ?> subjectDelta : subjectDeltas) {

            ItemPath subjectItemPath = subjectDelta.getPath();

            if (ShadowType.F_ASSOCIATION.equivalent(subjectItemPath)) {

                // Adding or removing an association value.

                //noinspection unchecked
                ContainerDelta<ShadowAssociationType> assocContainerDelta = (ContainerDelta<ShadowAssociationType>) subjectDelta;
                subjectShadowAfter = entitlementConverter.transformToObjectOpsOnModify(
                        objectsOperations, assocContainerDelta, subjectShadowBefore, subjectShadowAfter, result);

            } else {

                // Changing any other attribute. We check if the attribute matches any of the current associations of the subject.

                ContainerDelta<ShadowAssociationType> associationDelta =
                        PrismContext.get().deltaFactory().container().createDelta(
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
                    if (isChangeReal(subjectShadowBefore, subjectShadowAfter, subjectItemPath)) {
                        LOGGER.trace("Processing association {} on association-binding attribute ({}) change",
                                associationName, valueAttributeName);
                        //noinspection unchecked
                        associationDelta.addValuesToDelete(associationValue.clone());
                        //noinspection unchecked
                        associationDelta.addValuesToAdd(associationValue.clone());
                    } else {
                        LOGGER.trace("NOT processing association {} because the related attribute ({}) change is phantom",
                                associationName, valueAttributeName);
                    }
                }
                LOGGER.trace("Resulting association delta for {}:\n{}", subjectItemPath, associationDelta.debugDumpLazily(1));
                if (!associationDelta.isEmpty()) {
                    entitlementConverter.transformToObjectOpsOnModify(
                            objectsOperations, associationDelta, subjectShadowBefore, subjectShadowAfter, result);
                }
            }
        }

        executeEntitlementObjectsOperations(objectsOperations, result);
    }

    private <T> boolean isChangeReal(ShadowType objectBefore, ShadowType objectAfter, ItemPath itemPath) throws SchemaException {
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
        return b.matchingRuleRegistry.getMatchingRule(matchingRuleName, null);
    }

    /** Also fills-in definitions for attribute deltas, if not present. */
    private List<Operation> convertToUcfOperations(OperationResult result) throws SchemaException {
        List<Operation> ucfOperations = new ArrayList<>();
        boolean activationProcessed = false;
        for (ItemDelta<?, ?> itemDelta : requestedDeltas) {
            if (isAttributeDelta(itemDelta)
                    || SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
                if (itemDelta instanceof PropertyDelta<?> propertyDelta) {
                    PropertyModificationOperation<?> attributeModification =
                            new PropertyModificationOperation<>(propertyDelta);
                    ResourceAttributeDefinition<?> attrDef = objectDefinition.findAttributeDefinition(itemDelta.getElementName());
                    if (attrDef != null) {
                        attributeModification.setMatchingRuleQName(attrDef.getMatchingRuleQName());
                        if (itemDelta.getDefinition() == null) {
                            //noinspection unchecked,rawtypes
                            ((ItemDelta) itemDelta).setDefinition(attrDef);
                        }
                    }
                    ucfOperations.add(attributeModification);
                } else if (itemDelta instanceof ContainerDelta) {
                    // skip the container delta - most probably password change - it is processed earlier (??)
                } else {
                    throw unsupported(itemDelta);
                }
            } else if (SchemaConstants.PATH_ACTIVATION.equivalent(itemDelta.getParentPath())) {
                if (!activationProcessed) {
                    // We process all activation deltas at once. (Why?)
                    ucfOperations.addAll(new ActivationConverter(ctx)
                            .transformOnModify(repoShadow, requestedDeltas, result));
                    activationProcessed = true;
                }
            } else if (ShadowType.F_ASSOCIATION.equivalent(itemDelta.getPath())) {
                if (itemDelta instanceof ContainerDelta) {
                    //noinspection unchecked
                    ucfOperations.addAll(
                            new EntitlementConverter(ctx)
                                    .transformToSubjectOpsOnModify((ContainerDelta<ShadowAssociationType>) itemDelta)
                                    .getOperations());
                } else {
                    throw unsupported(itemDelta);
                }
            } else if (ShadowType.F_AUXILIARY_OBJECT_CLASS.equivalent(itemDelta.getPath())) {
                if (itemDelta instanceof PropertyDelta<?> propertyDelta) {
                    ucfOperations.add(
                            new PropertyModificationOperation<>(propertyDelta));
                } else {
                    throw unsupported(itemDelta);
                }
            } else {
                LOGGER.trace(
                        "Skip converting item delta: {}. It's not resource object change, but it is shadow change.", itemDelta);
            }
        }
        LOGGER.trace("Converted to UCF operations:\n{}", DebugUtil.debugDumpLazily(ucfOperations, 1));
        return ucfOperations;
    }

    private static UnsupportedOperationException unsupported(ItemDelta<?, ?> itemDelta) {
        return new UnsupportedOperationException("Unsupported delta: " + itemDelta);
    }

    private void assertNoDuplicates(Collection<Operation> operations) throws SchemaException {
        if (InternalsConfig.isSanityChecks()) {
            // MID-3964
            if (MiscUtil.hasDuplicates(operations)) {
                throw new SchemaException("Duplicated changes: " + operations); // TODO context
            }
        }
    }

    static boolean isAttributeDelta(ItemDelta<?, ?> itemDelta) {
        return ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath());
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}

/*
 * TODO what to do with this comment?
 *
 * State of the shadow before execution of the deltas - e.g. with original attributes, as it may be recorded in such a way in
 * groups of which this account is a member of. (In case of object->subject associations.)
 *
 * This is used when the resource does NOT provide referential integrity by itself. This is e.g. the case of OpenDJ with default
 * settings.
 *
 * On the contrary, AD and OpenDJ with referential integrity plugin do provide automatic referential integrity, so this feature is
 * not needed.
 *
 * We decide based on setting of explicitReferentialIntegrity in association definition.
 */
