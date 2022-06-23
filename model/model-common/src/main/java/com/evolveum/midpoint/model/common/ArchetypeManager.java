/*
 * Copyright (C) 2019-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.repo.common.SystemObjectCache;

import com.evolveum.midpoint.schema.RelationRegistry;

import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.evolveum.midpoint.util.MiscUtil;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.model.api.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ArchetypeTypeUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Component that can efficiently determine archetypes for objects.
 * It is backed by caches, therefore this is supposed to be a low-overhead service that can be
 * used in many places.
 *
 * [NOTE]
 * ====
 * When resolving archetype references (i.e. obtaining archetype objects from references in object assignments and
 * `archetypeRef` values, as well as when resolving super-archetypes), we currently handle dangling references
 * (non-existing objects) by ignoring them. We just log the exception, and keep the {@link OperationResultStatus#FATAL_ERROR}
 * in the result tree - where the lower-level code put it. (It may or may not be available to the ultimate caller; depending
 * on the overall operation result processing.)
 * ====
 *
 * @author Radovan Semancik
 */
@Component
public class ArchetypeManager implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypeManager.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(ArchetypeManager.class.getName() + ".content");

    /**
     * Cache invalidation is invoked when an object of any of these classes is modified.
     */
    private static final Collection<Class<?>> INVALIDATION_RELATED_CLASSES = Arrays.asList(
            ArchetypeType.class,
            SystemConfigurationType.class,
            ObjectTemplateType.class
    );

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private CacheRegistry cacheRegistry;
    @Autowired private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    private final Map<String, ArchetypePolicyType> archetypePolicyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void register() {
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }

    /**
     * Gets an archetype. Assumes that caching of {@link ArchetypeType} objects is enabled by global repository cache.
     * (By default, it is - see `default-caching-profile.xml` in `infra/schema` resources.)
     */
    public @NotNull ArchetypeType getArchetype(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return systemObjectCache
                .getArchetype(oid, result)
                .asObjectable();
    }

    /**
     * Determines all archetype OIDs (structural + auxiliary) for a given object.
     *
     * This is a simplified version of {@link #determineArchetypeOids(ObjectType, ObjectType)} method.
     */
    public @NotNull Set<String> determineArchetypeOids(@Nullable ObjectType object) {
        Set<String> oids;
        if (object instanceof AssignmentHolderType) {
            AssignmentHolderType assignmentHolder = (AssignmentHolderType) object;
            // To be safe, we look also at archetypeRef values. This may change in the future.
            oids = Sets.union(
                    getArchetypeOidsFromAssignments(assignmentHolder),
                    determineArchetypeOidsFromArchetypeRef(assignmentHolder));
        } else {
            oids = Set.of();
        }
        LOGGER.trace("Archetype OIDs determined (no-change case): {}", oids);
        return oids;
    }

    /**
     * Determines all archetype OIDs in the "dynamic" case where the object is changed during clockwork processing.
     *
     * See the code of this method and of {@link #determineArchetypeOids(AssignmentHolderType, AssignmentHolderType)}
     * for detailed explanation how the result is computed.
     *
     * @see #determineArchetypeOids(ObjectType)
     */
    public <O extends ObjectType> @NotNull Set<String> determineArchetypeOids(O before, O after) {

        // First, let us sort out static cases, where there is no change in the object internals.

        if (after == null) {
            // Object is being deleted. Let us simply take the OIDs from last known version (if there's any).
            return determineArchetypeOids(before);
        }

        if (before == null) {
            // Object is being added, and we have no "before" version. Reduces to the static case just as above.
            return determineArchetypeOids(after);
        }

        // Here we know we have (some) change. Let's sort things out.

        if (!(after instanceof AssignmentHolderType)) {
            assert !(before instanceof AssignmentHolderType); // We know the clockwork does not change the type of objects.
            return Set.of();
        }

        return determineArchetypeOids(
                (AssignmentHolderType) before,
                (AssignmentHolderType) after);
    }

    private @NotNull Set<String> determineArchetypeOids(
            @NotNull AssignmentHolderType before, @NotNull AssignmentHolderType after) {

        // Values assigned "at the end" (i.e. in `after` version). This is usually the authoritative information.
        Set<String> assignedOidsAfter = getArchetypeOidsFromAssignments(after);

        // We look at archetypeRef because there may be rare (theoretical?) cases when we have archetypeRefs
        // without corresponding assignments. But we have to be careful to select only relevant OIDs!
        Set<String> relevantOidsFromRef = getRelevantArchetypeOidsFromArchetypeRef(before, after, assignedOidsAfter);

        // Finally, we take a union of these sets. Note that they usually overlap.
        var oids = Sets.union(
                assignedOidsAfter,
                relevantOidsFromRef);

        LOGGER.trace("Archetype OIDs determined (dynamic case): {}", oids);
        return oids;
    }

    private @NotNull Set<String> getArchetypeOidsFromAssignments(AssignmentHolderType assignmentHolder) {
        var oids = assignmentHolder.getAssignment().stream()
                .map(AssignmentType::getTargetRef)
                .filter(Objects::nonNull)
                .filter(ref -> QNameUtil.match(ArchetypeType.COMPLEX_TYPE, ref.getType()))
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .map(AbstractReferencable::getOid)
                .collect(Collectors.toSet());
        stateCheck(!oids.contains(null),
                "OID-less (e.g. dynamic filter based) archetype assignments are not supported; in %s", assignmentHolder);
        LOGGER.trace("Assigned archetype OIDs: {}", oids);
        return oids;
    }

    /**
     * Selects relevant archetype OIDs from `archetypeRef`. We must take care here because this information may be out of date
     * in situations where we try to determine archetypes _before_ the assignment evaluator updates `archetypeRef` values.
     * (Even the values in `after` version of the object are not up-to-date in that case.)
     *
     * The best we can do is to eliminate any `archetypeRef` OIDs that were assigned "before" but are not assigned "after":
     * meaning they are unassigned by some (primary/secondary) delta.
     */
    private Set<String> getRelevantArchetypeOidsFromArchetypeRef(
            AssignmentHolderType before, AssignmentHolderType after, Set<String> assignedOidsAfter) {

        Set<String> assignedOidsBefore = getArchetypeOidsFromAssignments(before);

        // These are OIDs that were assigned at the beginning, but are no longer assigned. These are forbidden to use.
        Set<String> unassignedOids = Sets.difference(assignedOidsBefore, assignedOidsAfter);

        // These are the relevant OIDs: they are in (presumed ~ "after") archetypeRef and were not unassigned.
        var relevant = Sets.difference(
                determineArchetypeOidsFromArchetypeRef(after),
                unassignedOids);

        LOGGER.trace("Relevant archetype OIDs from archetypeRef: {}", relevant);
        return relevant;
    }

    private @NotNull Set<String> determineArchetypeOidsFromArchetypeRef(AssignmentHolderType assignmentHolder) {
        var oids = assignmentHolder.getArchetypeRef().stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation())) // should be always the case
                .map(AbstractReferencable::getOid)
                .filter(Objects::nonNull) // should be always the case
                .collect(Collectors.toSet());
        LOGGER.trace("'Effective' archetype OIDs (from archetypeRef): {}", oids);
        return oids;
    }

    /**
     * Determines all archetypes for "static" case (no change in the object).
     *
     * See the class-level note about resolving dangling references.
     */
    public @NotNull List<ArchetypeType> determineArchetypes(@Nullable ObjectType object, OperationResult result)
            throws SchemaException {
        return resolveArchetypeOids(
                determineArchetypeOids(object),
                object,
                result);
    }

    /**
     * Resolves archetype OIDs to full objects.
     *
     * See the class-level note about resolving dangling references.
     */
    public List<ArchetypeType> resolveArchetypeOids(Collection<String> oids, Object context, OperationResult result)
            throws SchemaException {
        List<ArchetypeType> archetypes = new ArrayList<>();
        for (String oid : oids) {
            try {
                archetypes.add(
                        systemObjectCache.getArchetype(oid, result)
                                .asObjectable());
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Archetype {} for {} cannot be found", oid, context);
            }
        }
        return archetypes;
    }

    /**
     * Determines the structural archetype for an object.
     *
     * (See the class-level note about resolving dangling references.)
     */
    public ArchetypeType determineStructuralArchetype(@Nullable AssignmentHolderType assignmentHolder, OperationResult result)
            throws SchemaException {
        return ArchetypeTypeUtil.getStructuralArchetype(
                determineArchetypes(assignmentHolder, result));
    }

    /**
     * Determines the archetype policy for an object. The policy is determined in the complex way, taking
     * auxiliary archetypes, super archetypes, and even legacy (subtype) configuration into account.
     *
     * (See the class-level note about resolving dangling archetype references.)
     */
    public ArchetypePolicyType determineArchetypePolicy(@Nullable ObjectType object, OperationResult result)
            throws SchemaException, ConfigurationException {
        Set<String> archetypeOids = determineArchetypeOids(object);
        List<ArchetypeType> archetypes = resolveArchetypeOids(archetypeOids, object, result);
        return determineArchetypePolicy(archetypes, object, result);
    }

    /**
     * A convenience variant of {@link #determineArchetypePolicy(ObjectType, OperationResult)}.
     */
    public ArchetypePolicyType determineArchetypePolicy(
            @Nullable PrismObject<? extends ObjectType> object, OperationResult result)
            throws SchemaException, ConfigurationException {
        return determineArchetypePolicy(asObjectable(object), result);
    }

    /**
     * Determines "complex" archetype policy; takes auxiliary archetypes, super archetypes,
     * and even legacy (subtype) configuration into account.
     */
    public ArchetypePolicyType determineArchetypePolicy(
            Collection<ArchetypeType> allArchetypes,
            ObjectType object,
            OperationResult result)
            throws SchemaException, ConfigurationException {
        return merge(
                determinePureArchetypePolicy(allArchetypes, result),
                determineObjectPolicyConfiguration(object, result));
    }

    /**
     * Determines "pure" archetype policy - just from archetypes, ignoring subtype configuration.
     */
    private ArchetypePolicyType determinePureArchetypePolicy(
            Collection<ArchetypeType> allArchetypes, OperationResult result)
            throws SchemaException, ConfigurationException {

        if (allArchetypes.isEmpty()) {
            return null;
        }

        ArchetypeType structuralArchetype = ArchetypeTypeUtil.getStructuralArchetype(allArchetypes);
        List<ArchetypeType> auxiliaryArchetypes = allArchetypes.stream()
                .filter(ArchetypeTypeUtil::isAuxiliary)
                .collect(Collectors.toList());
        if (structuralArchetype == null && !auxiliaryArchetypes.isEmpty()) {
            throw new SchemaException("Auxiliary archetype cannot be assigned without structural archetype");
        }

        // TODO collision detection
        // TODO why are we using getMergedPolicyForArchetype (i.e. without caching) instead of mergeArchetypePolicies
        //  (that does the caching)?

        ArchetypePolicyType mergedPolicy = new ArchetypePolicyType();

        for (ArchetypeType auxiliaryArchetype : auxiliaryArchetypes) {
            mergedPolicy = mergeArchetypePolicies(
                    mergedPolicy,
                    getMergedPolicyForArchetype(auxiliaryArchetype, result));
        }

        assert structuralArchetype != null;
        mergedPolicy = mergeArchetypePolicies(
                mergedPolicy,
                getMergedPolicyForArchetype(structuralArchetype, result));

        return mergedPolicy;
    }

    /**
     * Returns policy collected from this archetype and its super-archetypes. Uses the policy cache.
     *
     * TODO consider renaming to `getMergedPolicyForArchetype`
     */
    public ArchetypePolicyType mergeArchetypePolicies(PrismObject<ArchetypeType> archetype, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (archetype == null || archetype.getOid() == null) {
            return null;
        }
        ArchetypePolicyType cachedArchetypePolicy = archetypePolicyCache.get(archetype.getOid());
        if (cachedArchetypePolicy != null) {
            return cachedArchetypePolicy;
        }
        ArchetypePolicyType mergedArchetypePolicy = getMergedPolicyForArchetype(archetype.asObjectable(), result);
        if (mergedArchetypePolicy != null) {
            archetypePolicyCache.put(archetype.getOid(), mergedArchetypePolicy);
        }
        return mergedArchetypePolicy;
    }

    /**
     * Computes policy merged from this archetype and its super-archetypes.
     *
     * TODO consider renaming to `computeMergedPolicyForArchetype`
     */
    private ArchetypePolicyType getMergedPolicyForArchetype(ArchetypeType archetype, OperationResult result)
            throws SchemaException, ConfigurationException {
        ArchetypePolicyType currentPolicy = archetype.getArchetypePolicy();
        ArchetypeType superArchetype = getSuperArchetype(archetype, result);
        if (superArchetype == null) {
            return currentPolicy;
        } else {
            ArchetypePolicyType superPolicy = getMergedPolicyForArchetype(superArchetype, result);
            return mergeArchetypePolicies(superPolicy, currentPolicy);
        }
    }

    private @Nullable ArchetypeType getSuperArchetype(ArchetypeType archetype, OperationResult result)
            throws SchemaException, ConfigurationException {
        ObjectReferenceType superArchetypeRef = archetype.getSuperArchetypeRef();
        if (superArchetypeRef == null) {
            return null;
        }
        String oid = MiscUtil.configNonNull(
                superArchetypeRef.getOid(),
                () -> "Dynamic (non-OID) super-archetype references are not supported; in " + archetype);
        try {
            return systemObjectCache
                    .getArchetype(oid, result)
                    .asObjectable();
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Super archetype {} (of {}) couldn't be found", oid, archetype);
            return null;
        }
    }

    private ArchetypePolicyType mergeArchetypePolicies(ArchetypePolicyType superPolicy, ArchetypePolicyType currentPolicy) {
        if (currentPolicy == null) {
            if (superPolicy == null) {
                return null;
            }
            // FIXME: probably no need to clone, caller pass this to function, which also invokes clone
            return superPolicy.clone();
        }

        if (superPolicy == null) {
            // FIXME: probably no need to clone, caller pass this to function, which also invokes clone
            return currentPolicy.clone();
        }

        ArchetypePolicyType mergedPolicy = currentPolicy.clone();

        ArchetypeAdminGuiConfigurationType mergedAdminGuiConfig = mergeAdminGuiConfig(currentPolicy, superPolicy);
        mergedPolicy.setAdminGuiConfiguration(mergedAdminGuiConfig);

        ApplicablePoliciesType mergedApplicablePolicies = mergeApplicablePolicies(currentPolicy, superPolicy);
        mergedPolicy.setApplicablePolicies(mergedApplicablePolicies);

        AssignmentRelationApproachType mergedRelationApproach = mergeRelationApproach(currentPolicy, superPolicy);
        mergedPolicy.setAssignmentHolderRelationApproach(mergedRelationApproach);

        ConflictResolutionType mergedConflictResolutionType = mergeConflictResolution(currentPolicy, superPolicy);
        mergedPolicy.setConflictResolution(mergedConflictResolutionType);

        DisplayType mergedDisplayType = adminGuiConfigurationMergeManager.mergeDisplayType(currentPolicy.getDisplay(), superPolicy.getDisplay());
        mergedPolicy.setDisplay(mergedDisplayType);


        if (currentPolicy.getExpressionProfile() == null) {
            mergedPolicy.setExpressionProfile(superPolicy.getExpressionProfile());
        }

        List<ItemConstraintType> itemConstraints = mergeItemConstraints(currentPolicy.getItemConstraint(), superPolicy.getItemConstraint());
        mergedPolicy.getItemConstraint().clear();
        mergedPolicy.getItemConstraint().addAll(itemConstraints);

        LifecycleStateModelType mergedLifecycleStateModel = mergeLifecycleStateModel(currentPolicy.getLifecycleStateModel(), superPolicy.getLifecycleStateModel());
        mergedPolicy.setLifecycleStateModel(mergedLifecycleStateModel);


        //Experimental
        if (currentPolicy.getLinks() == null) {
            mergedPolicy.setLinks(superPolicy.getLinks());
        }

        if (currentPolicy.getObjectTemplateRef() == null) {
            mergedPolicy.setObjectTemplateRef(superPolicy.getObjectTemplateRef());
        }

        //DEPRECATED
        List<ItemConstraintType> propertyConstraints = mergeItemConstraints(currentPolicy.getPropertyConstraint(), superPolicy.getPropertyConstraint());
        mergedPolicy.getPropertyConstraint().clear();
        mergedPolicy.getPropertyConstraint().addAll(propertyConstraints);

        return mergedPolicy;
    }

    private ArchetypeAdminGuiConfigurationType mergeAdminGuiConfig(ArchetypePolicyType currentPolicy, ArchetypePolicyType superPolicy) {
        ArchetypeAdminGuiConfigurationType currentAdminGuiConfig = currentPolicy.getAdminGuiConfiguration();
        ArchetypeAdminGuiConfigurationType superAdminGuiConfig = superPolicy.getAdminGuiConfiguration();
        if (currentAdminGuiConfig == null) {
            return superAdminGuiConfig;
        }

        if (superAdminGuiConfig == null) {
            return currentAdminGuiConfig;
        }

        ArchetypeAdminGuiConfigurationType mergedAdminGuiConfig = new ArchetypeAdminGuiConfigurationType();
        GuiObjectDetailsPageType mergedObjectDetails = mergeObjectDetails(currentAdminGuiConfig, superAdminGuiConfig);
        mergedAdminGuiConfig.setObjectDetails(mergedObjectDetails);

        return mergedAdminGuiConfig;
    }

    private GuiObjectDetailsPageType mergeObjectDetails(ArchetypeAdminGuiConfigurationType currentAdminGuiConfig, ArchetypeAdminGuiConfigurationType superAdminGuiConfig) {
        GuiObjectDetailsPageType currentObjectDetails = currentAdminGuiConfig.getObjectDetails();
        GuiObjectDetailsPageType superObjectDetails = superAdminGuiConfig.getObjectDetails();
        if (currentObjectDetails == null) {
            if (superObjectDetails == null) {
                return null;
            }
            return superObjectDetails.clone();
        }

        if (superObjectDetails == null) {
            return currentObjectDetails.clone();
        }

        return adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(superObjectDetails, currentObjectDetails);
        //TODO save method, objectForm, relations
    }

    private ApplicablePoliciesType mergeApplicablePolicies(ArchetypePolicyType currentPolicy, ArchetypePolicyType superPolicy) {
        ApplicablePoliciesType currentApplicablePolicies = currentPolicy.getApplicablePolicies();
        ApplicablePoliciesType superApplicablePolicies = superPolicy.getApplicablePolicies();
        if (currentApplicablePolicies == null) {
            return superApplicablePolicies;
        }

        ApplicablePoliciesType mergedPolicies = currentApplicablePolicies.clone();
        if (superApplicablePolicies == null) {
            return mergedPolicies;
        }
        for (ObjectReferenceType policyGroupRef : superApplicablePolicies.getPolicyGroupRef()) {
            mergedPolicies.getPolicyGroupRef().add(policyGroupRef.clone());
        }

        return mergedPolicies;
    }

    private AssignmentRelationApproachType mergeRelationApproach(ArchetypePolicyType currentPolicy, ArchetypePolicyType superPolicy) {
        if (currentPolicy.getAssignmentHolderRelationApproach() != null) {
            return currentPolicy.getAssignmentHolderRelationApproach();
        }

        return superPolicy.getAssignmentHolderRelationApproach();

    }

    private ConflictResolutionType mergeConflictResolution(ArchetypePolicyType currentPolicy, ArchetypePolicyType superPolicy) {
        ConflictResolutionType currentConflictResolution = currentPolicy.getConflictResolution();
        ConflictResolutionType superConflictResolution = superPolicy.getConflictResolution();
        if (currentConflictResolution == null) {
            if (superConflictResolution == null) {
                return null;
            }
            return superConflictResolution.clone();
        }

        if (superConflictResolution == null) {
            return currentConflictResolution.clone();
        }

        ConflictResolutionType mergedConflictResolution = currentConflictResolution.clone();
        if (currentConflictResolution.getAction() == null) {
            mergedConflictResolution.setAction(superConflictResolution.getAction());
        }
        if (currentConflictResolution.getDelayUnit() == null) {
            mergedConflictResolution.setDelayUnit(superConflictResolution.getDelayUnit());
        }

        if (currentConflictResolution.getMaxAttempts() == null) {
            mergedConflictResolution.setMaxAttempts(superConflictResolution.getMaxAttempts());
        }

        return mergedConflictResolution;
    }

    private List<ItemConstraintType> mergeItemConstraints(List<ItemConstraintType> currentConstraints, List<ItemConstraintType> superConstraints) {
        return adminGuiConfigurationMergeManager.mergeContainers(currentConstraints, superConstraints,
                this::createItemConstraintPredicate,
                this::mergeItemConstraint);
    }

    private Predicate<ItemConstraintType> createItemConstraintPredicate(ItemConstraintType constraint) {
        return c -> pathsMatch(constraint.getPath(), c.getPath());
    }

    // we want to merge according to path, but there might exist more than 1 def without path, so rather do nothing.
    private boolean pathsMatch(ItemPathType supperPath, ItemPathType currentPath) {
        return supperPath != null && currentPath != null && supperPath.equivalent(currentPath);
    }

    private ItemConstraintType mergeItemConstraint(ItemConstraintType matchedConstraint, ItemConstraintType superConstraint) {
        ItemConstraintType mergedConstraint = cloneComplex(matchedConstraint);
        if (matchedConstraint.getVisibility() == null) {
            mergedConstraint.setVisibility(superConstraint.getVisibility());
        }
        return mergedConstraint;
    }

    private LifecycleStateModelType mergeLifecycleStateModel(LifecycleStateModelType currentLifecycleStateModel, LifecycleStateModelType superLifecycleStateModel) {
        if (currentLifecycleStateModel == null) {
            if (superLifecycleStateModel == null) {
                return null;
            }
            return superLifecycleStateModel.clone();
        }

        if (superLifecycleStateModel == null) {
            return currentLifecycleStateModel.clone();
        }

        LifecycleStateModelType mergedLifecycleModel = new LifecycleStateModelType();
        List<LifecycleStateType> mergedLifecycleState = mergeLifecycleState(currentLifecycleStateModel.getState(), superLifecycleStateModel.getState());
        mergedLifecycleModel.getState().addAll(mergedLifecycleState);

        return mergedLifecycleModel;
    }

    private List<LifecycleStateType> mergeLifecycleState(List<LifecycleStateType> currentState, List<LifecycleStateType> superLifecycleStates) {
        return adminGuiConfigurationMergeManager.mergeContainers(currentState, superLifecycleStates,
                this::createLifecycleStatePredicate,
                this::mergeLifecycleState);
    }

    private Predicate<LifecycleStateType> createLifecycleStatePredicate(LifecycleStateType currentState) {
        return s -> s.getName() != null && currentState.getName() != null && s.getName().equals(currentState.getName());
    }

    private LifecycleStateType mergeLifecycleState(LifecycleStateType currentLifecycleState, LifecycleStateType superLifecycleState) {
        LifecycleStateType mergedLifecycleState = new LifecycleStateType();
        if (currentLifecycleState.getName() == null) {
            mergedLifecycleState.setName(superLifecycleState.getName());
        }

        if (currentLifecycleState.getDescription() == null) {
            mergedLifecycleState.setDescription(superLifecycleState.getDescription());
        }

        if (currentLifecycleState.getDisplayName() == null) {
            mergedLifecycleState.setDisplayName(superLifecycleState.getDisplayName());
        }

        List<LifecycleStateActionType> mergedEntryActions = mergeEntryAction(currentLifecycleState.getEntryAction(), superLifecycleState.getEntryAction());
        if (mergedEntryActions != null) {
            mergedLifecycleState.getEntryAction().clear();
            mergedLifecycleState.getEntryAction().addAll(mergedEntryActions);
        }

        List<LifecycleStateActionType> mergedExitActions = mergeEntryAction(currentLifecycleState.getExitAction(), superLifecycleState.getExitAction());
        if (mergedExitActions != null) {
            mergedLifecycleState.getExitAction().clear();
            mergedLifecycleState.getExitAction().addAll(mergedExitActions);
        }

        if (currentLifecycleState.getForcedActivationStatus() == null) {
            mergedLifecycleState.setForcedActivationStatus(superLifecycleState.getForcedActivationStatus());
        }

        if (currentLifecycleState.isActiveAssignments() == null) {
            mergedLifecycleState.setActiveAssignments(superLifecycleState.isActiveAssignments());
        }

        VirtualAssignmentSpecificationType mergedAssignment = mergeForcedAssignment(currentLifecycleState.getForcedAssignment(), superLifecycleState.getForcedAssignment());
        mergedLifecycleState.setForcedAssignment(mergedAssignment);

        return mergedLifecycleState;
    }

    private VirtualAssignmentSpecificationType mergeForcedAssignment(VirtualAssignmentSpecificationType currentForcedAssignment, VirtualAssignmentSpecificationType superForcedAssignment) {
        if (currentForcedAssignment == null) {
            if (superForcedAssignment == null) {
                return null;
            }
            return superForcedAssignment.clone();
        }

        if (superForcedAssignment == null) {
            return currentForcedAssignment.clone();
        }

        VirtualAssignmentSpecificationType mergedAssignment = currentForcedAssignment.clone();
        if (currentForcedAssignment.getFilter() == null) {
            mergedAssignment.setFilter(superForcedAssignment.getFilter().clone());
        }

        if (currentForcedAssignment.getTargetType() == null) {
            mergedAssignment.setTargetType(superForcedAssignment.getTargetType());
        }

        return mergedAssignment;
    }

    private List<LifecycleStateActionType> mergeEntryAction(List<LifecycleStateActionType> currentActions, List<LifecycleStateActionType> superActions) {
        return adminGuiConfigurationMergeManager.mergeContainers(currentActions, superActions,
                this::createLifecycleStateActionPredicate,
                this::mergeAction);
    }

    private Predicate<LifecycleStateActionType> createLifecycleStateActionPredicate(LifecycleStateActionType action) {
        return a -> a.getName() != null && a.getName().equals(action.getName());
    }

    private LifecycleStateActionType mergeAction(LifecycleStateActionType currentAction, LifecycleStateActionType superAction) {
        LifecycleStateActionType mergedAction = currentAction.clone();

        LifecycleStateActionDataReductionType currentDataReduction = currentAction.getDataReduction();
        LifecycleStateActionDataReductionType superDataReduction = superAction.getDataReduction();
        if (currentDataReduction == null) {
            if (superDataReduction == null) {
                return mergedAction;
            }
            mergedAction.setDataReduction(superDataReduction.clone());
            return mergedAction;
        }

        if (superDataReduction == null) {
            return mergedAction;
        }

        LifecycleStateActionDataReductionType mergedDataReduction = mergeDataReduction(currentDataReduction, superDataReduction);
        if (mergedDataReduction != null) {
            mergedAction.setDataReduction(mergedDataReduction);
        }
        return mergedAction;
    }

    private LifecycleStateActionDataReductionType mergeDataReduction(LifecycleStateActionDataReductionType currentDataReduction, LifecycleStateActionDataReductionType superDataReduction) {
        List<ItemPathType> currentItems = currentDataReduction.getPurgeItem();
        List<ItemPathType> superItems = superDataReduction.getPurgeItem();

        LifecycleStateActionDataReductionType mergedDataReduction = currentDataReduction.clone();
        if (currentItems.isEmpty()) {
            if (superItems.isEmpty()) {
                return null;
            }
            superItems.forEach(i -> mergedDataReduction.getPurgeItem().add(i.clone()));
            return mergedDataReduction;
        }

        for (ItemPathType superItem : superItems) {
            if (!currentItems.contains(superItem)) {
                mergedDataReduction.getPurgeItem().add(superItem.clone());
            }
        }

        return mergedDataReduction;
    }

    private ArchetypePolicyType merge(ArchetypePolicyType archetypePolicy, ObjectPolicyConfigurationType objectPolicy) {
        if (archetypePolicy == null && objectPolicy == null) {
            return null;
        }
        if (archetypePolicy == null) {
            return objectPolicy.clone();
        }
        if (objectPolicy == null) {
            return archetypePolicy.clone();
        }
        ArchetypePolicyType resultPolicy = archetypePolicy.clone();

        if (archetypePolicy.getApplicablePolicies() == null && objectPolicy.getApplicablePolicies() != null) {
            resultPolicy.setApplicablePolicies(objectPolicy.getApplicablePolicies().clone());
        }
        if (archetypePolicy.getConflictResolution() == null && objectPolicy.getConflictResolution() != null) {
            resultPolicy.setConflictResolution(objectPolicy.getConflictResolution().clone());
        }
        if (archetypePolicy.getDisplay() == null && objectPolicy.getDisplay() != null) {
            resultPolicy.setDisplay(objectPolicy.getDisplay().clone());
        }
        if (archetypePolicy.getExpressionProfile() == null && objectPolicy.getExpressionProfile() != null) {
            resultPolicy.setExpressionProfile(objectPolicy.getExpressionProfile());
        }
        if (archetypePolicy.getLifecycleStateModel() == null && objectPolicy.getLifecycleStateModel() != null) {
            resultPolicy.setLifecycleStateModel(objectPolicy.getLifecycleStateModel().clone());
        }
        if (archetypePolicy.getObjectTemplateRef() == null && objectPolicy.getObjectTemplateRef() != null) {
            resultPolicy.setObjectTemplateRef(objectPolicy.getObjectTemplateRef().clone());
        }
        if (archetypePolicy.getItemConstraint().isEmpty()) {
            for (ItemConstraintType objItemConstraint : objectPolicy.getItemConstraint()) {
                resultPolicy.getItemConstraint().add(objItemConstraint.clone());
            }
        }
        // Deprecated
        if (archetypePolicy.getPropertyConstraint().isEmpty()) {
            for (ItemConstraintType objPropertyConstraint : objectPolicy.getPropertyConstraint()) {
                resultPolicy.getPropertyConstraint().add(objPropertyConstraint.clone());
            }
        }
        return resultPolicy;
    }

    private ObjectPolicyConfigurationType determineObjectPolicyConfiguration(
            ObjectType object, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (object == null) {
            return null;
        }
        SystemConfigurationType systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
        if (systemConfiguration == null) {
            return null;
        }
        return determineObjectPolicyConfiguration(object, systemConfiguration);
    }

    public <O extends ObjectType> ExpressionProfile determineExpressionProfile(PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        ArchetypePolicyType archetypePolicy = determineArchetypePolicy(object, result);
        if (archetypePolicy == null) {
            return null;
        }
        String expressionProfileId = archetypePolicy.getExpressionProfile();
        return systemObjectCache.getExpressionProfile(expressionProfileId, result);
    }

    private static ObjectPolicyConfigurationType determineObjectPolicyConfiguration(
            ObjectType object, SystemConfigurationType systemConfiguration) throws ConfigurationException {
        List<String> subTypes = FocusTypeUtil.determineSubTypes(object);
        return determineObjectPolicyConfiguration(object.getClass(), subTypes, systemConfiguration);
    }

    public static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(Class<O> objectClass, List<String> objectSubtypes, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
        ObjectPolicyConfigurationType applicablePolicyConfigurationType = null;
        for (ObjectPolicyConfigurationType aPolicyConfigurationType: systemConfigurationType.getDefaultObjectPolicyConfiguration()) {
            QName typeQName = aPolicyConfigurationType.getType();
            if (typeQName == null) {
                continue;       // TODO implement correctly (using 'applicable policies' perhaps)
            }
            ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
            if (objectType == null) {
                throw new ConfigurationException("Unknown type "+typeQName+" in default object policy definition in system configuration");
            }
            if (objectType.getClassDefinition() == objectClass) {
                String aSubType = aPolicyConfigurationType.getSubtype();
                if (aSubType == null) {
                    if (applicablePolicyConfigurationType == null) {
                        applicablePolicyConfigurationType = aPolicyConfigurationType;
                    }
                } else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
                    applicablePolicyConfigurationType = aPolicyConfigurationType;
                }
            }
        }
        return applicablePolicyConfigurationType;
    }

    // TODO take object's archetype into account
    public static <O extends ObjectType> LifecycleStateModelType determineLifecycleModel(PrismObject<O> object, PrismObject<SystemConfigurationType> systemConfiguration) throws ConfigurationException {
        if (systemConfiguration == null) {
            return null;
        }
        return determineLifecycleModel(object, systemConfiguration.asObjectable());
    }

    public static <O extends ObjectType> LifecycleStateModelType determineLifecycleModel(PrismObject<O> object, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
        ObjectPolicyConfigurationType objectPolicyConfiguration =
                determineObjectPolicyConfiguration(asObjectable(object), systemConfigurationType);
        if (objectPolicyConfiguration == null) {
            return null;
        }
        return objectPolicyConfiguration.getLifecycleStateModel();
    }

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        // This seems to be harsh (and probably is), but a policy can really depend on a mix of objects. So we have to play
        // it safely and invalidate eagerly. Hopefully objects of these types do not change often.
        if (type == null || INVALIDATION_RELATED_CLASSES.contains(type)) {
            archetypePolicyCache.clear();
        }
    }

    @Override
    public @NotNull Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType()
                .name(ArchetypeManager.class.getName())
                .size(archetypePolicyCache.size()));
    }

    @Override
    public void dumpContent() {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            archetypePolicyCache.forEach((k, v) -> LOGGER_CONTENT.info("Cached archetype policy: {}: {}", k, v));
        }
    }

    private <C extends Containerable> C cloneComplex(C containerable) {
        return containerable.cloneWithoutId();
    }
}
