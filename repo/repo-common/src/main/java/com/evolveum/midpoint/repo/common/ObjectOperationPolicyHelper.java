package com.evolveum.midpoint.repo.common;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.MARK_PROTECTED_OID;
import static com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationPolicyViolationSeverityType.ERROR;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.MiscUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectMarkHelper.ObjectMarksComputer;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Deals with {@code effectiveOperationPolicy} in objects (currently shadows).
 *
 * [NOTE]
 * ====
 * *BEWARE OF THE EXECUTION MODES*
 *
 * Most methods take `mode` as a parameter, as both policy statements and default policy for shadow object type can be
 * mode-dependent. The caller is responsible for providing the correct mode, especially regarding the existing (cached)
 * effective policy in the provided object.
 *
 * Rule: The repository will contain mark refs computed only under the PRODUCTION mode.
 * I.e., the development-mode statements will not be present there.
 * ====
 *
 * @see ObjectMarkHelper
 */
@Component
public class ObjectOperationPolicyHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectOperationPolicyHelper.class);

    private static final String OP_COMPUTE_EFFECTIVE_POLICY =
            ObjectOperationPolicyHelper.class.getName() + ".computeEffectivePolicy";

    private static ObjectOperationPolicyHelper instance = null;

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private ObjectMarkHelper objectMarkHelper;

    private Impl behaviour;

    @PostConstruct
    public void init() {
        behaviour = cacheRepositoryService.supportsMarks() ? new MarkSupport() : new Legacy();
        instance = this;
    }

    @PreDestroy
    public void destroy() {
        instance = null;
    }

    public static ObjectOperationPolicyHelper get() {
        return instance;
    }

    /**
     * Returns present effective policy, or computes it (from stored mark refs and statements) if not present.
     *
     * For shadows, it assumes the shadow has the up-to-date definition present (because of the default policy).
     *
     * Execution mode: It influences the policy statements and the default policy. If there is a pre-computed policy
     * in the object, the caller is responsible to provide a mode that matches the one under which the pre-computed policy
     * was computed.
     */
    public @NotNull ObjectOperationPolicyType getEffectivePolicy(
            @NotNull ObjectType object, @NotNull TaskExecutionMode mode, @NotNull OperationResult result)
            throws ConfigurationException {
        var policy = object.getEffectiveOperationPolicy();
        if (policy != null) {
            return policy;
        }
        return computeEffectivePolicy(object, mode, result);
    }

    /**
     * Computes effective operation policy from stored mark refs and policy statements. Uses marks from the native repository.
     *
     * BEWARE: If we compute policy for a shadow, it must have the correct definition present!
     *
     * Execution mode: It influences the policy statements and the default policy. We assume that the stored mark refs were
     * computed under the PRODUCTION mode, i.e., that no development-mode policy statements were taken into account.
     */
    public @NotNull ObjectOperationPolicyType computeEffectivePolicy(
            @NotNull ObjectType object,
            @NotNull TaskExecutionMode mode,
            @NotNull OperationResult parentResult)
            throws ConfigurationException {
        var result = parentResult.createMinorSubresult(OP_COMPUTE_EFFECTIVE_POLICY);
        try {
            var effectiveMarkRefs = objectMarkHelper.computeObjectMarksWithoutComputer(object, mode);
            var defaultPolicy = behaviour.getDefaultPolicyForObject(object, mode, result);
            return behaviour.computeEffectiveOperationPolicy(effectiveMarkRefs, defaultPolicy, object, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Computes effective marks and policies; depending on the support in repo (see below).
     *
     * For native repository:
     *
     * 1. Computes effective marks (using policy statements, the computer, and the currently effective ones) - for both
     * production and current (client-provided) mode.
     * 2. Computes effective policy based on current-mode marks.
     * 3. Updates both in the shadow, plus the legacy protected object flag.
     *
     * For legacy repository:
     *
     * 1. Just computes and updates the legacy protected object flag plus corresponding operation policy.
     *
     * BEWARE: If we compute policy for a shadow, it must have the correct definition present!
     *
     * Just computes. DOES NOT UPDATE THE OBJECT.
     */
    public EffectiveMarksAndPolicies computeEffectiveMarksAndPolicies(
            @NotNull ObjectType object,
            @NotNull ObjectMarksComputer objectMarksComputer,
            @NotNull TaskExecutionMode mode,
            @NotNull OperationResult result) throws SchemaException, ConfigurationException {

        var productionModeEffectiveMarkRefs =
                objectMarkHelper.computeObjectMarks(
                        object, objectMarksComputer, TaskExecutionMode.PRODUCTION, result);
        var currentModeEffectiveMarkRefs =
                TaskExecutionMode.PRODUCTION.equals(mode) ?
                        List.copyOf(productionModeEffectiveMarkRefs) :
                        objectMarkHelper.computeObjectMarks(object, objectMarksComputer, mode, result);
        var defaultPolicy = behaviour.getDefaultPolicyForObject(object, mode, result);
        var effectiveOperationPolicy =
                behaviour.computeEffectiveOperationPolicy(currentModeEffectiveMarkRefs, defaultPolicy, object, result);

        return new EffectiveMarksAndPolicies(
                behaviour.supportsMarks() ? List.copyOf(productionModeEffectiveMarkRefs) : List.of(),
                behaviour.supportsMarks() ? List.copyOf(currentModeEffectiveMarkRefs) : List.of(),
                effectiveOperationPolicy,
                object instanceof ShadowType && isProtected(effectiveOperationPolicy));
    }

    private boolean isProtected(ObjectOperationPolicyType effectiveOperationPolicy) {
        return isAddDisabled(effectiveOperationPolicy)
                && isModifyDisabled(effectiveOperationPolicy)
                && isDeleteDisabled(effectiveOperationPolicy)
                && isSyncInboundDisabled(effectiveOperationPolicy)
                && isSyncOutboundDisabled(effectiveOperationPolicy)
                && isMembershipSyncInboundDisabled(effectiveOperationPolicy)
                && isMembershipSyncOutboundDisabled(effectiveOperationPolicy)
                && Boolean.TRUE.equals(getToleranceOverride(effectiveOperationPolicy));
    }

    /** There is a full implementation for native repo, and limited one for generic repo (that does not support marks). */
    private abstract static class Impl {

        /**
         * Converts marks into effective policy.
         *
         * - Input: currently effective mark refs + default policy
         * - Output: the policy (from the marks)
         *
         * The trivial implementation has no `MarkType` objects, so it's limited
         * to deal with "protected" mark in a legacy way (everything is forbidden).
         */
        abstract @NotNull ObjectOperationPolicyType computeEffectiveOperationPolicy(
                @NotNull Collection<ObjectReferenceType> effectiveMarkRefs,
                @Nullable ObjectOperationPolicyType defaultPolicy,
                @NotNull Object context,
                @NotNull OperationResult result);

        @Nullable ObjectOperationPolicyType getDefaultPolicyForObject(
                @NotNull ObjectType object,
                @NotNull TaskExecutionMode mode,
                @NotNull OperationResult result) throws ConfigurationException {
            return null;
        }

        @Nullable ObjectOperationPolicyType getDefaultPolicyForResourceObjectType(
                @NotNull ResourceObjectDefinition objectDefinition,
                @NotNull TaskExecutionMode mode,
                @NotNull OperationResult result) throws ConfigurationException {
            return null;
        }

        boolean supportsMarks() {
            return false;
        }
    }

    private class MarkSupport extends Impl {

        /** We derive policies from actual object marks. */
        @Override
        @NotNull ObjectOperationPolicyType computeEffectiveOperationPolicy(
                @NotNull Collection<ObjectReferenceType> effectiveMarkRefs,
                @Nullable ObjectOperationPolicyType defaultPolicy,
                @NotNull Object context,
                @NotNull OperationResult result) {
            var ret = new ObjectOperationPolicyType();
            var effectiveMarkOids = extractEffectiveMarkOids(effectiveMarkRefs);
            var marks = resolveMarkOids(effectiveMarkOids, context, result);

            try {
                computeEffectiveStatus(ret, PATH_ADD, marks, defaultPolicy);
                computeEffectiveStatus(ret, PATH_MODIFY, marks, defaultPolicy);
                computeEffectiveStatus(ret, PATH_DELETE, marks, defaultPolicy);
                computeEffectiveStatus(ret, PATH_SYNC_INBOUND, marks, defaultPolicy);
                computeEffectiveStatus(ret, PATH_SYNC_OUTBOUND, marks, defaultPolicy);
                computeEffectiveStatus(ret, PATH_MEMBERSHIP_SYNC_INBOUND, marks, defaultPolicy);
                computeEffectiveStatus(ret, PATH_MEMBERSHIP_SYNC_OUTBOUND, marks, defaultPolicy);

                var toleranceOverride = computeToleranceOverride(marks, defaultPolicy, context);
                if (toleranceOverride != null) {
                    //noinspection unchecked
                    ret.asPrismContainerValue()
                            .findOrCreateProperty(PATH_MEMBERSHIP_TOLERANCE)
                            .setRealValue(toleranceOverride);
                }
            } catch (SchemaException e) {
                throw SystemException.unexpected(e); // schema should be OK here
            }
            return ret;
        }

        /**
         * Collects restrictions for specific operation (represented by `path`) from a set of marks.
         * Selects the most restrictive one with the highest severity and applies it onto current policy.
         */
        private void computeEffectiveStatus(
                @NotNull ObjectOperationPolicyType resultingPolicy,
                @NotNull ItemPath path,
                @NotNull Collection<MarkType> marks,
                @Nullable ObjectOperationPolicyType defaultPolicy) throws SchemaException {

            AbstractOperationPolicyConfigurationType mostRestricted = null;
            AbstractOperationPolicyConfigurationType fullyEnabled = null;
            for (MarkType mark : marks) {
                var markPolicyItemValue = getPolicyItemValue(mark.getObjectOperationPolicy(), path);
                if (isEmpty(markPolicyItemValue)) {
                    // ignoring this one
                } else if (isFullyEnabled(markPolicyItemValue)) {
                    if (fullyEnabled == null) {
                        // Any "no restriction" item is OK for us
                        fullyEnabled = markPolicyItemValue.clone();
                    }
                } else if (isMoreRestricted(markPolicyItemValue, mostRestricted)) {
                    mostRestricted = markPolicyItemValue.clone();
                }
            }

            if (mostRestricted != null) {
                // Restrictions take precedence
                setPolicyItemValue(resultingPolicy, path, mostRestricted);
            } else if (fullyEnabled != null) {
                // "No restriction" takes precedence over default
                setPolicyItemValue(resultingPolicy, path, fullyEnabled);
            } else {
                // Now applying the default, if there's any
                var defaultPolicyItemValue = getPolicyItemValue(defaultPolicy, path);
                if (defaultPolicyItemValue != null) {
                    setPolicyItemValue(resultingPolicy, path, defaultPolicyItemValue.clone());
                }
            }
        }

        private boolean isEmpty(AbstractOperationPolicyConfigurationType value) {
            if (value instanceof OperationPolicyConfigurationType booleanStyle) {
                return booleanStyle.getEnabled() == null;
            } else if (value instanceof SyncInboundOperationPolicyConfigurationType syncInboundStyle) {
                return syncInboundStyle.getEnabled() == null;
            } else if (value == null) {
                return true;
            } else {
                throw unsupported(value);
            }
        }

        private static @NotNull AssertionError unsupported(AbstractOperationPolicyConfigurationType value) {
            return new AssertionError("Unsupported policy configuration type: " + MiscUtil.getValueWithClass(value));
        }

        private boolean isMoreRestricted(
                @NotNull AbstractOperationPolicyConfigurationType value1,
                @Nullable AbstractOperationPolicyConfigurationType value2) {
            if (value2 == null) {
                return true;
            }
            // Both value1 and value2 are "not empty" and "restricting something" here (due to caller's logic)
            // We have to compare non-boolean restriction levels here
            if (value1 instanceof SyncInboundOperationPolicyConfigurationType syncInboundStyle1) {
                var level1 = getRestrictionValue(syncInboundStyle1.getEnabled());
                if (value2 instanceof SyncInboundOperationPolicyConfigurationType syncInboundStyle2) {
                    var level2 = getRestrictionValue(syncInboundStyle2.getEnabled());
                    if (level1 > level2) {
                        return true;
                    } else if (level1 < level2) {
                        return false;
                    }
                } else {
                    throw unsupported(value2);
                }
            } else {
                // there are no restriction levels for OperationPolicyConfigurationType (yet)
            }
            // Restrictions levels are the same, let's compare severities
            var severity1 = Objects.requireNonNullElse(value1.getSeverity(), ERROR);
            var severity2 = Objects.requireNonNullElse(value2.getSeverity(), ERROR);
            return isHigher(severity1, severity2);
        }

        private boolean isFullyEnabled(@NotNull AbstractOperationPolicyConfigurationType value) {
            if (value instanceof OperationPolicyConfigurationType booleanStyle) {
                return Boolean.TRUE.equals(booleanStyle.isEnabled());
            } else if (value instanceof SyncInboundOperationPolicyConfigurationType syncInboundStyle) {
                return syncInboundStyle.getEnabled() == SyncInboundOperationPolicyEnabledType.TRUE;
            } else {
                throw unsupported(value);
            }
        }

        private static @Nullable AbstractOperationPolicyConfigurationType getPolicyItemValue(
                @Nullable ObjectOperationPolicyType policy, @NotNull ItemPath path) {
            if (policy == null) {
                return null;
            }
            //noinspection unchecked
            var policyItem = (PrismContainer<? extends AbstractOperationPolicyConfigurationType>)
                    policy.asPrismContainerValue().findItem(path);
            if (policyItem == null || policyItem.hasNoValues()) {
                return null;
            }
            return policyItem.getRealValue(AbstractOperationPolicyConfigurationType.class);
        }

        private static void setPolicyItemValue(
                @NotNull ObjectOperationPolicyType policy,
                @NotNull ItemPath path,
                @NotNull AbstractOperationPolicyConfigurationType value) throws SchemaException {
            //noinspection unchecked
            policy.asPrismContainerValue().findOrCreateItem(path, PrismContainer.class, null)
                    .add(value.asPrismContainerValue());
        }

        /** Returns {@code true} if {@code s1} is higher than {@code s2}. */
        private boolean isHigher(
                @NotNull OperationPolicyViolationSeverityType s1, @NotNull OperationPolicyViolationSeverityType s2) {
            return getValue(s1) > getValue(s2);
        }

        private int getValue(@NotNull OperationPolicyViolationSeverityType s) {
            return switch (s) {
                case INFO -> 1;
                case ERROR -> 2;
            };
        }

        private int getRestrictionValue(@NotNull SyncInboundOperationPolicyEnabledType s) {
            return switch (s) {
                case TRUE -> 0;
                case EXCEPT_FOR_MAPPINGS -> 1;
                case FALSE -> 2;
            };
        }

        private Boolean computeToleranceOverride(
                @NotNull Collection<MarkType> marks,
                @Nullable ObjectOperationPolicyType defaultPolicy,
                @NotNull Object context) {
            var givingTrue = new ArrayList<>();
            var givingFalse = new ArrayList<>();
            marks.forEach(
                    m -> {
                        var toleranceOverride = getToleranceOverride(m.getObjectOperationPolicy());
                        if (Boolean.TRUE.equals(toleranceOverride)) {
                            givingTrue.add(m);
                        } else if (Boolean.FALSE.equals(toleranceOverride)) {
                            givingFalse.add(m);
                        }
                    }
            );
            boolean shouldBeTrue = !givingTrue.isEmpty();
            boolean shouldBeFalse = !givingFalse.isEmpty();
            if (shouldBeTrue && shouldBeFalse) {
                // We may consider throwing an exception here.
                LOGGER.warn("Conflicting tolerance override setting in {}: marks giving TRUE: {}, FALSE: {} - "
                                + "continuing as tolerant (because it's safer)",
                        context, givingTrue, givingFalse);
                return true;
            } else if (shouldBeTrue) {
                LOGGER.trace("Tolerance override = true because of {} (for {})", givingTrue, context);
                return true;
            } else if (shouldBeFalse) {
                LOGGER.trace("Tolerance override = false because of {} (for {})", givingFalse, context);
                return false;
            } else {
                return getToleranceOverride(defaultPolicy);
            }
        }

        private static Boolean getToleranceOverride(ObjectOperationPolicyType policy) {
            if (policy == null) {
                return null;
            }
            var synchronize = policy.getSynchronize();
            if (synchronize == null) {
                return null;
            }
            var membership = synchronize.getMembership();
            if (membership == null) {
                return null;
            }
            return membership.getTolerant();
        }

        // TODO move MarkManager downwards (at least those part that can be moved) and provide this functionality there
        private @NotNull Collection<MarkType> resolveMarkOids(
                @NotNull Set<String> oidsToResolve,
                Object context,
                @NotNull OperationResult result) {
            if (!cacheRepositoryService.supportsMarks() || oidsToResolve.isEmpty()) {
                return List.of();
            }
            try {
                // This should be resolved from the cache; and the list should be quite short (for now)
                var allMarks = cacheRepositoryService.searchObjects(MarkType.class, null, null, result);

                // We don't need to filter by archetype, as the references should be OK in this regard
                // Moreover, archetypes present slight problems in lower level tests, as they are not computed correctly
                // in provisioning-impl tests.
                var resolved = allMarks.stream()
                        .filter(mark -> oidsToResolve.contains(mark.getOid()))
                        .map(o -> o.asObjectable())
                        .toList();

                if (resolved.size() < oidsToResolve.size()) {
                    // Look! Something is missing. Let's find out what it is.
                    var missing = new HashSet<>(oidsToResolve);
                    resolved.forEach(r -> missing.remove(r.getOid()));
                    LOGGER.warn("The following marks could not be resolved: {}; in {}", missing, context);
                }

                return resolved;
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        }

        @Override
        @Nullable ObjectOperationPolicyType getDefaultPolicyForObject(
                @NotNull ObjectType object, @NotNull TaskExecutionMode mode, @NotNull OperationResult result)
                throws ConfigurationException {
            if (!(object instanceof ShadowType shadow)) {
                return null;
            }
            // We assume only well-formed shadows (with the definition) here.
            return getDefaultPolicyForResourceObjectType(
                    ShadowUtil.getResourceObjectDefinition(shadow),
                    mode, result);
        }

        @Override
        @Nullable ObjectOperationPolicyType getDefaultPolicyForResourceObjectType(
                @NotNull ResourceObjectDefinition objectDef,
                @NotNull TaskExecutionMode mode,
                @NotNull OperationResult result) throws ConfigurationException {
            var markOid = objectDef.getDefaultOperationPolicyOid(mode);
            if (markOid == null) {
                return null;
            }
            var marks = resolveMarkOids(Set.of(markOid), "determining default policy for " + objectDef, result);
            if (!marks.isEmpty()) {
                return marks.iterator().next().getObjectOperationPolicy();
            } else {
                throw new ConfigurationException(
                        "Mark %s for the default policy for %s does not exist".formatted(markOid, objectDef),
                        new ObjectNotFoundException(MarkType.class, markOid));
            }
        }

        @Override
        boolean supportsMarks() {
            return true;
        }
    }

    /** Ignores invalid or informational (`org:related`) references. */
    private static @NotNull Set<String> extractEffectiveMarkOids(@NotNull Collection<ObjectReferenceType> markRefs) {
        var relationRegistry = SchemaService.get().relationRegistry();
        return markRefs.stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .map(ref -> ref.getOid())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static class Legacy extends Impl {

        /** We are limited to simple "protected or not" decision. */
        @Override
        @NotNull ObjectOperationPolicyType computeEffectiveOperationPolicy(
                @NotNull Collection<ObjectReferenceType> effectiveMarkRefs,
                @Nullable ObjectOperationPolicyType defaultPolicy,
                @NotNull Object context,
                @NotNull OperationResult result) {
            var effectiveOids = extractEffectiveMarkOids(effectiveMarkRefs);
            if (effectiveOids.contains(MARK_PROTECTED_OID)) {
                return new ObjectOperationPolicyType()
                        .synchronize(new SynchronizeOperationPolicyConfigurationType()
                                .inbound(syncDisabled(OperationPolicyViolationSeverityType.INFO))
                                .outbound(disabled(OperationPolicyViolationSeverityType.INFO))
                                .membership(new SynchronizeMembershipOperationPolicyConfigurationType()
                                        .inbound(disabled(OperationPolicyViolationSeverityType.INFO))
                                        .outbound(disabled(OperationPolicyViolationSeverityType.INFO))
                                        .tolerant(true)))
                        .add(disabled(ERROR))
                        .modify(disabled(ERROR))
                        .delete(disabled(ERROR));
            } else {
                // We can safely ignore the default policy here, as it always comes from the marks (which are not supported here)
                return new ObjectOperationPolicyType();
            }
        }

        private OperationPolicyConfigurationType disabled(OperationPolicyViolationSeverityType severity) {
            return new OperationPolicyConfigurationType()
                    .enabled(false)
                    .severity(severity);
        }

        @SuppressWarnings("SameParameterValue")
        private SyncInboundOperationPolicyConfigurationType syncDisabled(OperationPolicyViolationSeverityType severity) {
            return new SyncInboundOperationPolicyConfigurationType()
                    .enabled(SyncInboundOperationPolicyEnabledType.FALSE)
                    .severity(severity);
        }
    }

    public @Nullable ObjectOperationPolicyType getDefaultPolicyForResourceObjectType(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull TaskExecutionMode mode,
            @NotNull OperationResult result) throws ConfigurationException {
        return behaviour.getDefaultPolicyForResourceObjectType(objectDefinition, mode, result);
    }

    /**
     * Contains effective mark refs (both production-mode and current-mode), and computed effective operation policy
     * (for the current mode).
     *
     * Note that mark ref collections are always empty for generic repository.
     *
     * The client must make sure that the current-mode information on mark refs will not get into the repo!
     */
    public record EffectiveMarksAndPolicies(
            @NotNull Collection<ObjectReferenceType> productionModeEffectiveMarkRefs,
            @NotNull Collection<ObjectReferenceType> currentModeEffectiveMarkRefs,
            @NotNull ObjectOperationPolicyType effectiveOperationPolicy,
            boolean isProtected) {

        /** Applies current-mode effective marks and policies. BEWARE: make sure this information will not get into the repo! */
        public void applyTo(@NotNull ObjectType object) {
            object.getEffectiveMarkRef().clear();
            object.getEffectiveMarkRef().addAll(currentModeEffectiveMarkRefs);
            object.setEffectiveOperationPolicy(effectiveOperationPolicy);
            if (object instanceof ShadowType shadow && isProtected) {
                shadow.setProtectedObject(true);
            }
        }
    }
}
