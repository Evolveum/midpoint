/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;
import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.computeResultStatusAndAsyncOpReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.util.ShadowReferenceAttributesCollection;

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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
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

    public static @NotNull ResourceObjectModifyReturnValue execute(
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

    private @NotNull ResourceObjectModifyReturnValue doExecute(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        LOGGER.trace("Modifying resource object {}, deltas:\n{}", repoShadow, DebugUtil.debugDumpLazily(requestedDeltas, 1));

        if (!ShadowUtil.hasResourceModifications(requestedDeltas)) {
            // Quit early, so we avoid potential pre-read and other processing when there is no point of doing so.
            // Also the induced read ops may fail which may invoke consistency mechanism which will complicate the situation.
            LOGGER.trace("No resource modification found for {}, skipping", identification);
            result.recordNotApplicableIfUnknown();
            return ResourceObjectModifyReturnValue.fromResult(result);
        }

        ctx.checkProtectedObjectModification(repoShadow);
        // Other checks (execution mode, capability) are executed inside ResourceObjectUcfModifyOperation

        Collection<Operation> ucfOperations = convertToUcfOperations(result);

        boolean hasVolatilityTriggerModification = hasVolatilityTriggerModification();
        ExistingResourceObjectShadow preReadObject = doPreReadIfNeeded(ucfOperations, hasVolatilityTriggerModification, result);

        UcfModifyReturnValue modifyResult;
        if (!ucfOperations.isEmpty()) {
            assertNoDuplicates(ucfOperations);
            // Execute primary UCF operation on this shadow
            modifyResult = ResourceObjectUcfModifyOperation.execute(
                    ctx, repoShadow, preReadObject, identification, ucfOperations, scripts, result, connOptions);
        } else {
            // We have to check BEFORE we add script operations, otherwise the check would be pointless
            LOGGER.trace("No modifications for connector object specified. Skipping processing of subject executeModify.");
            modifyResult = UcfModifyReturnValue.empty();
        }

        knownExecutedDeltas.addAll(
                modifyResult.getExecutedOperationsAsPropertyDeltas());

        ExistingResourceObjectShadow postReadObject;
        if (hasVolatilityTriggerModification && preReadObject != null) {
            // In rare cases, the object could not be pre-read even if tried to do so. Hence the nullity check.
            postReadObject = doPostReadIfNeeded(ucfOperations, knownExecutedDeltas, preReadObject, result);
        } else {
            postReadObject = null;
        }

        Collection<? extends ItemDelta<?, ?>> allDeltas = new ArrayList<>(requestedDeltas);
        ItemDeltaCollectionsUtil.addNotEquivalent(allDeltas, knownExecutedDeltas); // MID-6892

        // These are modification on related objects, e.g., groups (if needed)
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

        computeResultStatusAndAsyncOpReference(result);

        return ResourceObjectModifyReturnValue.fromResult(
                knownExecutedDeltas,
                result,
                modifyResult.getOperationType());
    }

    private @Nullable ExistingResourceObjectShadow doPreReadIfNeeded(
            Collection<Operation> ucfOperations, boolean hasVolatilityTriggerModification, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        if (!shouldDoPreRead(ucfOperations, hasVolatilityTriggerModification)) {
            return null;
        }

        LOGGER.trace("Pre-reading resource object");
        // yes, we need associations here (but why?)
        ExistingResourceObjectShadow resourceObject =
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
        ExistingResourceObjectShadow futurized = ResourceObjectFuturizer.futurizeResourceObject(
                ctx, repoShadow, resourceObject, true, now);
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

    private @Nullable ExistingResourceObjectShadow doPostReadIfNeeded(
            @NotNull Collection<Operation> ucfOperations,
            @NotNull Collection<PropertyDelta<?>> knownExecutedDeltas, // in-out parameter
            ExistingResourceObjectShadow preReadObject,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        assert preReadObject != null;

        // There may be other changes that were not detected by the connector. Re-read the object and compare.
        LOGGER.trace("Post-reading resource shadow");
        ExistingResourceObjectShadow postReadObject = preOrPostRead(
                ctx, identification, ucfOperations, true, repoShadow, result);
        LOGGER.trace("Post-read object:\n{}", DebugUtil.debugDumpLazily(postReadObject));
        if (postReadObject == null) {
            return null; // This may happen in rare cases (e.g. with semi-manual resources)
        }

        ObjectDelta<ShadowType> resourceShadowDelta = preReadObject.getPrismObject().diff(postReadObject.getPrismObject());
                LOGGER.trace("Determined side-effect changes by old-new diff:\n{}", resourceShadowDelta.debugDumpLazily());
        for (ItemDelta<?, ?> modification : resourceShadowDelta.getModifications()) {
            if (modification.getParentPath().startsWithName(ShadowType.F_ATTRIBUTES)
                    && !ItemDeltaCollectionsUtil.hasEquivalent(requestedDeltas, modification)
                    && !isSimulatedReferenceAttributeDelta(modification)) {
                ItemDeltaCollectionsUtil.merge(knownExecutedDeltas, modification);
            }
        }
        LOGGER.trace("Side-effect changes after merging with old-new diff:\n{}",
                DebugUtil.debugDumpLazily(knownExecutedDeltas));
        return postReadObject;
    }

    /**
     * There may be phantom "group membership delete" deltas when renaming account with simulated reference attributes that
     * have explicit (midPoint-provided) referential integrity. The reason is that the renamed account is not visible in
     * the original group/groups, as they still store the old name.
     *
     * Hence, we simply ignore simulated reference attribute deltas (regardless of referential integrity)
     * and do not put them into "executed deltas" collection.
     */
    private boolean isSimulatedReferenceAttributeDelta(ItemDelta<?, ?> modification) {
        return modification.getDefinition() instanceof ShadowReferenceAttributeDefinition refAttrDef
                && refAttrDef.isSimulated();
    }

    private boolean hasVolatilityTriggerModification() throws SchemaException {
        // Any attribute is a "volatility trigger", i.e., any modification can change anything.
        if (!ctx.getObjectDefinitionRequired().getAttributesVolatileOnModifyOperation().isEmpty()) {
            LOGGER.trace("Any attribute is a volatility trigger");
            return true;
        }

        // Only specific attributes are volatility triggers.
        for (ItemDelta<?, ?> itemDelta : requestedDeltas) {
            ItemPath path = itemDelta.getPath();
            QName firstPathName = path.firstName();
            if (ShadowUtil.isAttributeModification(firstPathName)) {
                QName attrName = path.rest().firstNameOrFail();
                if (ctx.findAttributeDefinitionRequired(attrName).isVolatilityTrigger()) {
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
     * . both `subjectBefore` and `subjectAfter` are known (this is the ideal case)
     * . only `subjectBefore` is known (there was no need to fetch the resource object after the operation)
     * . only the {@link #repoShadow} (before operation) is known
     *
     * In the second and the third case, we have to determine the expected subject state by applying the deltas.
     */
    private void determineAndExecuteEntitlementObjectsOperations(
            @Nullable ExistingResourceObjectShadow subjectBefore,
            @Nullable ExistingResourceObjectShadow subjectAfter,
            @NotNull Collection<? extends ItemDelta<?, ?>> subjectDeltas,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException {

        EntitlementObjectsOperations objectsOperations = new EntitlementObjectsOperations();
        EntitlementConverter entitlementConverter = new EntitlementConverter(ctx);

        ShadowType subjectShadowBefore;
        ShadowType subjectShadowAfter;
        if (subjectBefore != null) {
            subjectShadowBefore = subjectBefore.getBean();
            if (subjectAfter != null) {
                subjectShadowAfter = subjectAfter.getBean();
            } else {
                var expectedSubjectAfter = subjectBefore.clone();
                expectedSubjectAfter.updateWith(subjectDeltas);
                subjectShadowAfter = expectedSubjectAfter.bean;
            }
        } else {
            assert subjectAfter == null;
            subjectShadowBefore = repoShadow.getBean();
            var repoShadowAfter = repoShadow.clone();
            // We hope that all relevant attributes (regarding entitlement search) are in the shadow!
            // We accept that some deltas will not be applied correctly - the modifications that are not relevant for
            // the attributes in the shadow. But we are not interested in these. (We could filter them out, if really necessary.)
            repoShadowAfter.updateWith(subjectDeltas);
            subjectShadowAfter = repoShadowAfter.getBean();
        }

        var referenceAttributes = ShadowUtil.getReferenceAttributes(subjectShadowBefore);

        LOGGER.trace("determineAndExecuteEntitlementObjectsOperations, old subject state:\n{}",
                subjectShadowBefore.debugDumpLazily(1));

        for (ItemDelta<?, ?> subjectDelta : subjectDeltas) {

            ItemPath itemDeltaPath = subjectDelta.getPath();

            if (subjectDelta.getDefinition() instanceof ShadowReferenceAttributeDefinition) {

                // Directly manipulating the associations. We need to update the target objects, e.g. by adding/removing members.
                var attributesCollection = ShadowReferenceAttributesCollection.ofDelta(subjectDelta);
                entitlementConverter.transformToObjectOpsOnModify(
                        objectsOperations, attributesCollection, subjectShadowBefore, subjectShadowAfter, result);

            } else if (subjectDelta.getDefinition() instanceof ShadowSimpleAttributeDefinition) {

                // Changing any other attribute. This may affect simulated references: typically when the subject is renamed,
                // we (for resources without the referential integrity) have to adapt the reference targets, e.g. groups.
                // This is done by simulating DELETE/ADD of reference value.

                if (referenceAttributes.isEmpty()) {
                    LOGGER.trace("No references in old shadow. Skipping processing entitlements change for {}.", itemDeltaPath);
                    continue;
                }

                LOGGER.trace("Processing reference attributes in old shadow for {}:\n{}",
                        itemDeltaPath, DebugUtil.debugDumpLazily(referenceAttributes, 1));

                // Update explicit-ref-integrity associations if the subject binding attribute (e.g. a DN) is being changed.
                for (ShadowReferenceAttribute refAttr : referenceAttributes) {
                    var refAttrName = refAttr.getElementName();
                    var refAttrDef =
                            ctx.getObjectDefinitionRequired().findReferenceAttributeDefinitionRequired(
                                    refAttrName, () -> ctx.getExceptionDescription());
                    if (!EntitlementUtils.isSimulatedObjectToSubject(refAttrDef) // subject rename matters only for obj->sub ones
                            || !EntitlementUtils.isVisible(refAttrDef, ctx)
                            || !EntitlementUtils.requiresExplicitReferentialIntegrity(refAttrDef)) { // the resource takes care of this
                        continue;
                    }
                    var simulationDefinition = refAttrDef.getSimulationDefinitionRequired();
                    var subjectBindingAttrName = simulationDefinition.getPrimarySubjectBindingAttributeName();
                    if (!ShadowUtil.matchesAttribute(itemDeltaPath, subjectBindingAttrName)) {
                        continue; // this delta is not concerned with the binding attribute
                    }
                    for (var refAttrValue : refAttr.getValues()) {
                        if (!isChangeReal(subjectShadowBefore, subjectShadowAfter, itemDeltaPath)) {
                            // TODO WHY HERE?!
                            LOGGER.trace("NOT processing reference attribute {} because the related attribute ({}) change is phantom",
                                    refAttrName, subjectBindingAttrName);
                            continue;
                        }
                        LOGGER.trace("Processing reference attribute {} on reference-binding attribute ({}) change",
                                refAttrName, subjectBindingAttrName);
                        var refAttrDelta = refAttrDef.createEmptyDelta();
                        refAttrDelta.addValuesToDelete(refAttrValue.clone());
                        refAttrDelta.addValuesToAdd(refAttrValue.clone());
                        LOGGER.trace("Add-delete reference delta for {} and {}:\n{}",
                                itemDeltaPath, refAttrName, refAttrDelta.debugDumpLazily(1));
                        entitlementConverter.transformToObjectOpsOnModify(
                                objectsOperations, ShadowReferenceAttributesCollection.ofDelta(refAttrDelta),
                                subjectShadowBefore, subjectShadowAfter, result);
                    }
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

    /**
     * Converts requested deltas to UCF operations.
     *
     * Also fills-in definitions for attribute deltas, if not present; TODO why?
     */
    private List<Operation> convertToUcfOperations(OperationResult result) throws SchemaException {
        List<Operation> ucfOperations = new ArrayList<>();
        boolean activationProcessed = false;
        for (ItemDelta<?, ?> itemDelta : requestedDeltas) {
            if (isAttributeDelta(itemDelta)
                    || SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
                if (itemDelta instanceof PropertyDelta<?> propertyDelta) {
                    var attributeModification = new PropertyModificationOperation<>(propertyDelta);
                    // TODO will this work for passwords as well?
                    ShadowSimpleAttributeDefinition<?> attrDef = objectDefinition.findSimpleAttributeDefinition(itemDelta.getElementName());
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
                } else if (itemDelta instanceof ReferenceDelta) {
                    // Simulated subject-to-object reference deltas are converted to attribute deltas by the entitlement
                    // converter. Native ones are simply copied into ucfOperations without change.
                    var attributesCollection = ShadowReferenceAttributesCollection.ofDelta(itemDelta);
                    ucfOperations.addAll(
                            new EntitlementConverter(ctx)
                                    .transformToSubjectOpsOnModify(attributesCollection));
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
            } else if (ShadowType.F_AUXILIARY_OBJECT_CLASS.equivalent(itemDelta.getPath())) {
                if (itemDelta instanceof PropertyDelta<?> propertyDelta) {
                    ucfOperations.add(
                            new PropertyModificationOperation<>(propertyDelta));
                } else {
                    throw unsupported(itemDelta);
                }
            } else {
                LOGGER.trace("Skip converting item delta: {}. It's not a resource object change", itemDelta);
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
