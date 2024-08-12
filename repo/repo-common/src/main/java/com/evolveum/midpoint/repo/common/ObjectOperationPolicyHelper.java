package com.evolveum.midpoint.repo.common;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.MARK_PROTECTED_OID;
import static com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectables;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementTypeType.APPLY;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementTypeType.EXCLUDE;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Manages {@code effectiveMarkRef} and {@code effectiveOperationPolicy} in objects (currently shadows).
 *
 * [NOTE]
 * ====
 * *WATCH THE "COMPUTE" METHOD SIGNATURES!*
 *
 * Some methods compute the policy and effective mark refs just from the stored refs/statements; while others use
 * the actual {@link ObjectMarksComputer} to do the computations.
 * ====
 */
@Component
public class ObjectOperationPolicyHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectOperationPolicyHelper.class);

    private static final String OP_COMPUTE_EFFECTIVE_POLICY =
            ObjectOperationPolicyHelper.class.getName() + ".computeEffectivePolicy";

    private static ObjectOperationPolicyHelper instance = null;

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private PrismContext prismContext;

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

    /** Returns present effective policy, or computes it (from stored mark refs and statements) if not present. */
    public @NotNull ObjectOperationPolicyType getEffectivePolicy(ObjectType object, OperationResult result) {
        var policy = object.getEffectiveOperationPolicy();
        if (policy != null) {
            return policy;
        }
        return computeEffectivePolicy(object, result);
    }

    /** Computes effective operation policy from stored mark refs and policy statements. Uses marks from the native repository. */
    public @NotNull ObjectOperationPolicyType computeEffectivePolicy(ObjectType object, OperationResult parentResult) {
        var result = parentResult.createMinorSubresult(OP_COMPUTE_EFFECTIVE_POLICY);
        try {
            var effectiveMarkRefs = behaviour.getEffectiveMarkRefsWithStatements(object);
            return behaviour.computeEffectiveOperationPolicy(effectiveMarkRefs, object, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Computes effective marks and policy and updates the shadow; depending on the support in repo (see below).
     *
     * For native repository:
     *
     * 1. Computes effective marks (using policy statements, the computer, and the currently effective ones).
     * 2. Computes effective policy based on these marks.
     * 3. Updates both in the shadow, plus the legacy protected object flag.
     *
     * For legacy repository:
     *
     * 1. Just computes and updates the legacy protected object flag plus corresponding operation policy.
     */
    public EffectiveMarksAndPolicies computeEffectiveMarksAndPolicies(
            @NotNull ObjectType object,
            @NotNull ObjectMarksComputer objectMarksComputer,
            @NotNull OperationResult result) throws SchemaException {

        var effectiveMarkRefs = behaviour.computeEffectiveMarks(object, objectMarksComputer, result);
        var effectiveOperationPolicy = behaviour.computeEffectiveOperationPolicy(effectiveMarkRefs, object, result);

        return new EffectiveMarksAndPolicies(
                behaviour.supportsMarks() ? List.copyOf(effectiveMarkRefs) : List.of(),
                effectiveOperationPolicy,
                object instanceof ShadowType && isProtected(effectiveOperationPolicy));
    }

    private boolean isProtected(ObjectOperationPolicyType effectiveOperationPolicy) {
        return isAddDisabled(effectiveOperationPolicy)
                && isModifyDisabled(effectiveOperationPolicy)
                && isDeleteDisabled(effectiveOperationPolicy)
                && isSyncInboundDisabled(effectiveOperationPolicy)
                && isSyncOutboundDisabled(effectiveOperationPolicy);
    }

    /**
     * Computes effective mark delta for given object, given current and desired state.
     */
    public ItemDelta<?, ?> computeEffectiveMarkDelta(
            @NotNull Collection<ObjectReferenceType> currentMarks,
            @NotNull Collection<ObjectReferenceType> desiredMarks) throws SchemaException {
        return behaviour.computeEffectiveMarkDelta(currentMarks, desiredMarks);
    }

    /**
     * Converts policy statement delta into effective mark delta.
     *
     * APPROXIMATE ONLY; limitations are:
     *
     * 1. Assumes that all effective mark refs were added through policy statements (which is currently not true).
     * 2. Assumes that there is no concurrently computed/executed effective mark refs delta.
     * (Otherwise the computed delta may conflict with it.)
     *
     * TODO consider replacing this method
     */
    public @Nullable ItemDelta<?, ?> computeEffectiveMarkDelta(
            @NotNull ObjectType object, @NotNull ItemDelta<?, ?> policyStatementDelta)
            throws SchemaException {
        return behaviour.computeEffectiveMarkDelta(object, policyStatementDelta);
    }

    public EvaluatedPolicyStatements computeEffectiveMarkDelta(
            ObjectType object,
            Map<PlusMinusZero, Collection<PolicyStatementType>> policyStatements) throws SchemaException {
        return behaviour.computeEffectiveMarkDelta(policyStatements);
    }

    /** There is a full implementation for native repo, and limited one for generic repo (that does not support marks). */
    private abstract static class Impl {

        /** Computes effective marks for given object, taking into account the current state and the provided computer. */
        abstract Collection<ObjectReferenceType> computeEffectiveMarks(
                @NotNull ObjectType object,
                @NotNull ObjectMarksComputer objectMarksComputer,
                @NotNull OperationResult result) throws SchemaException;

        /**
         * Combines effective mark refs + policy statements for given object.
         * No other computations.
         *
         * Returns detached collection.
         */
        Collection<ObjectReferenceType> getEffectiveMarkRefsWithStatements(@NotNull ObjectType object) {
            return new ArrayList<>();
        }

        /**
         * Converts marks into effective policy. The trivial implementation has no MarkType objects, so it's limited
         * to deal with "protected" mark in a legacy way (everything is forbidden).
         */
        abstract @NotNull ObjectOperationPolicyType computeEffectiveOperationPolicy(
                Collection<ObjectReferenceType> effectiveMarkRefs, Object context, OperationResult result);

        /** Sets the effective marks references into the object bean. */
        void setEffectiveMarks(ObjectType object, Collection<ObjectReferenceType> effectiveMarkRefs) {
            // NOOP by default, since marks are not supported by the generic repository
        }

        /**
         * Converts policy statement delta into effective mark delta.
         *
         * FIXME BEWARE! Imprecise! If a mark was added by a statement and assigned automatically at the same time, removing the
         *  statement will remove the mark from `effectiveMarkRef`.
         */
        @Nullable ItemDelta<?, ?> computeEffectiveMarkDelta(ObjectType object, ItemDelta<?, ?> policyStatementDelta)
                throws SchemaException {
            return null;
        }

        EvaluatedPolicyStatements computeEffectiveMarkDelta(Map<PlusMinusZero, Collection<PolicyStatementType>> policyStatements) {
            return null;
        }

        public ItemDelta<?, ?> computeEffectiveMarkDelta(
                @NotNull Collection<ObjectReferenceType> currentMarks,
                @NotNull Collection<ObjectReferenceType> desiredMarks) throws SchemaException {
            return null;
        }

        boolean supportsMarks() {
            return false;
        }
    }

    private class MarkSupport extends Impl {

        @Override
        boolean supportsMarks() {
            return true;
        }

        @Override
        void setEffectiveMarks(ObjectType object, Collection<ObjectReferenceType> effectiveMarkRefs) {
            object.getEffectiveMarkRef().clear();
            object.getEffectiveMarkRef().addAll(effectiveMarkRefs);
        }

        @Override
        Collection<ObjectReferenceType> getEffectiveMarkRefsWithStatements(@NotNull ObjectType object) {
            return applyStatementsToMarkRefs(object.getEffectiveMarkRef(), object.getPolicyStatement());
        }

        /**
         * Applies statements to mark references. The "proposed" refs may or may not have some statements already applied.
         * Returns detached collection. The original collection is not touched.
         */
        private Collection<ObjectReferenceType> applyStatementsToMarkRefs(
                List<ObjectReferenceType> proposedMarkRefs, List<PolicyStatementType> statements) {

            // 1. Take proposed marks which were not excluded
            List<ObjectReferenceType> effectiveMarkRefs =
                    proposedMarkRefs.stream()
                            .filter(m -> m.getOid() != null && !statementsContain(statements, m.getOid(), EXCLUDE))
                            .collect(Collectors.toList());

            // 2. Add marks which were explicitly applied (and are not already there)
            for (var statement : statements) {
                var statementMarkOid = getOid(statement.getMarkRef());
                if (statement.getType() == APPLY && statementMarkOid != null) {
                    addMarkIfNotPresent(effectiveMarkRefs, statementMarkOid);
                }
            }

            return effectiveMarkRefs;
        }

        /**
         * Computes or recomputes shadow marks for given shadow: We compute all computable marks
         * either from policy statements, or by calling the computer.
         *
         * Nothing is updated in the shadow; only the new collection of effective marks is returned.
         */
        @NotNull Collection<ObjectReferenceType> computeEffectiveMarks(
                @NotNull ObjectType object,
                @NotNull ObjectMarksComputer objectMarksComputer,
                @NotNull OperationResult result) throws SchemaException {

            var computableMarksOids = Set.copyOf(objectMarksComputer.getComputableMarksOids());

            var statements = object.getPolicyStatement();
            var enforcedByStatements = getMarksOidsFromStatements(statements, APPLY);
            var forbiddenByStatements = getMarksOidsFromStatements(statements, EXCLUDE);
            var drivenByStatements = Sets.union(enforcedByStatements, forbiddenByStatements);
            var conflictingInStatements = Sets.intersection(enforcedByStatements, forbiddenByStatements);

            stateCheck(
                    conflictingInStatements.isEmpty(),
                    "Marks %s are both enforced and forbidden by policy statements in %s",
                    conflictingInStatements, object);

            // Note that some marks can be both computable and driven by statements
            var allMarkOids = Sets.union(computableMarksOids, drivenByStatements);

            // Now let's compute the effective marks. We start with the information in the object.
            var effectiveMarkRefs = CloneUtil.cloneCollectionMembers(object.getEffectiveMarkRef());

            // And we process presence of all marks individually
            for (var markOid : allMarkOids) {
                Boolean shouldBeThere;
                if (enforcedByStatements.contains(markOid)) {
                    shouldBeThere = true;
                } else if (forbiddenByStatements.contains(markOid)) {
                    shouldBeThere = false;
                } else if (computableMarksOids.contains(markOid)) {
                    shouldBeThere = objectMarksComputer.computeObjectMarkPresence(markOid, result);
                } else {
                    shouldBeThere = null;
                }

                if (Boolean.TRUE.equals(shouldBeThere)) {
                    addMarkIfNotPresent(effectiveMarkRefs, markOid);
                } else if (Boolean.FALSE.equals(shouldBeThere)) {
                    removeMarkIfPresent(effectiveMarkRefs, markOid);
                } else {
                    // no decision, let's keep it untouched
                }
            }

            return effectiveMarkRefs;
        }

        private static @NotNull Set<String> getMarksOidsFromStatements(
                @NotNull Collection<PolicyStatementType> statements, @NotNull PolicyStatementTypeType type) {
            return statements.stream()
                    .filter(s -> s.getType() == type)
                    .map(s -> getOid(s.getMarkRef()))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        }

        private void addMarkIfNotPresent(@NotNull Collection<ObjectReferenceType> markRefs, @NotNull String markOid) {
            if (!containsOid(markRefs, markOid)) {
                markRefs.add(
                        new ObjectReferenceType().oid(markOid).type(MarkType.COMPLEX_TYPE));
            }
        }

        private void removeMarkIfPresent(@NotNull Collection<ObjectReferenceType> markRefs, @NotNull String markOid) {
            markRefs.removeIf(ref -> markOid.equals(getOid(ref)));
        }

        /** We derive policies from actual object marks. */
        @Override
        @NotNull ObjectOperationPolicyType computeEffectiveOperationPolicy(
                Collection<ObjectReferenceType> effectiveMarkRefs,
                Object context,
                OperationResult result) {
            var ret = new ObjectOperationPolicyType();
            Collection<MarkType> marks = getShadowMarks(effectiveMarkRefs, result);

            try {
                disableOperationIfRequiredByMarks(ret, PATH_ADD, marks);
                disableOperationIfRequiredByMarks(ret, PATH_MODIFY, marks);
                disableOperationIfRequiredByMarks(ret, PATH_DELETE, marks);
                disableOperationIfRequiredByMarks(ret, PATH_SYNC_INBOUND, marks);
                disableOperationIfRequiredByMarks(ret, PATH_SYNC_OUTBOUND, marks);

                var toleranceOverride = computeToleranceOverride(marks, context);
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
         * Collects "disable" instructions for specific operation (represented by `path`) from a set of marks.
         * Selects the one with the highest severity and applies it onto current policy.
         */
        private void disableOperationIfRequiredByMarks(
                @NotNull ObjectOperationPolicyType resultingPolicy,
                @NotNull ItemPath path,
                @NotNull Collection<MarkType> marks) throws SchemaException {
            OperationPolicyConfigurationType highestSeverityBlocker = null;
            for (MarkType mark : marks) {
                var markPolicy = mark.getObjectOperationPolicy();
                if (markPolicy == null) {
                    continue;
                }
                //noinspection unchecked
                var markPolicyItem =
                        (PrismContainer<OperationPolicyConfigurationType>)
                                markPolicy.asPrismContainerValue().findItem(path);
                if (markPolicyItem == null || markPolicyItem.hasNoValues()) {
                    continue;
                }
                var markPolicyItemValue = markPolicyItem.getRealValue(OperationPolicyConfigurationType.class);
                var severity = Objects.requireNonNullElse(markPolicyItemValue.getSeverity(), OperationPolicyViolationSeverityType.ERROR);
                if (Boolean.FALSE.equals(markPolicyItemValue.getEnabled())) {
                    if (highestSeverityBlocker == null || isHigher(severity, highestSeverityBlocker.getSeverity())) {
                        highestSeverityBlocker = markPolicyItemValue.clone();
                        highestSeverityBlocker.setSeverity(severity); // to treat null values
                    }
                }
            }
            if (highestSeverityBlocker != null) {
                //noinspection unchecked
                resultingPolicy.asPrismContainerValue().findOrCreateItem(path, PrismContainer.class, null)
                        .add(highestSeverityBlocker.asPrismContainerValue());
            }
        }

        /** Returns {code true} if {code s1} is higher than {code s2}. */
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

        private Boolean computeToleranceOverride(Collection<MarkType> marks, Object context) {
            var givingTrue = new ArrayList<>();
            var givingFalse = new ArrayList<>();
            marks.forEach(
                    m -> {
                        var policy = m.getObjectOperationPolicy();
                        if (policy == null) {
                            return;
                        }
                        var synchronize = policy.getSynchronize();
                        if (synchronize == null) {
                            return;
                        }
                        var membership = synchronize.getMembership();
                        if (membership == null) {
                            return;
                        }
                        var toleranceOverride = membership.getTolerant();
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
                // TODO what should we do here? What is the default? Or should we throw an exception?
                LOGGER.warn("Conflicting tolerance override setting in {}: marks giving TRUE: {}, FALSE: {} - "
                                + "continuing as non-tolerant",
                        context, givingTrue, givingFalse);
                return false;
            } else if (shouldBeTrue) {
                LOGGER.trace("Tolerance override = true because of {} (for {})", givingTrue, context);
                return true;
            } else if (shouldBeFalse) {
                LOGGER.trace("Tolerance override = false because of {} (for {})", givingFalse, context);
                return false;
            } else {
                return null;
            }
        }

        private Collection<MarkType> getShadowMarks(Collection<ObjectReferenceType> tagRefs, @NotNull OperationResult result) {
            // FIXME: Consider caching of all shadow marks and doing post-filter only
            if (!cacheRepositoryService.supportsMarks() || tagRefs.isEmpty()) {
                return List.of();
            }
            String[] tagRefIds = tagRefs.stream().map(t -> t.getOid()).toArray(String[]::new);
            ObjectQuery query = prismContext.queryFor(MarkType.class)
                    //.item(TagType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_SHADOW_MARK.value())
                    // Tag is Shadow Marks
                    .item(MarkType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value())
                    .and()
                    // Tag is assigned to shadow
                    .id(tagRefIds)
                    .build();
            try {
                return asObjectables(cacheRepositoryService.searchObjects(MarkType.class, query, null, result));
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        }

        @Override
        ItemDelta<?, ?> computeEffectiveMarkDelta(ObjectType objectBefore, ItemDelta<?, ?> policyStatementDelta)
                throws SchemaException {
            ObjectType objectAfter = objectBefore.clone();

            var effectiveMarksBefore = objectBefore.getEffectiveMarkRef();
            List<ObjectReferenceType> markRefsToDelete = new ArrayList<>();
            List<ObjectReferenceType> markRefsToAdd = new ArrayList<>();

            //noinspection unchecked
            var statementsToDelete = (Collection<PolicyStatementType>) policyStatementDelta.getRealValuesToDelete();
            for (var statementToDelete : emptyIfNull(statementsToDelete)) {
                if (statementToDelete.getType() == APPLY && statementToDelete.getMarkRef() != null) {
                    var referencedMarkOid = getOid(statementToDelete.getMarkRef());
                    if (referencedMarkOid != null) {
                        var markRefImpliedByStatement =
                                findEffectiveMarkRefImpliedByStatement(effectiveMarksBefore, referencedMarkOid);
                        if (markRefImpliedByStatement != null) {
                            markRefsToDelete.add(markRefImpliedByStatement.clone());
                        }
                    }
                }
            }

            policyStatementDelta.applyTo(objectAfter.asPrismObject());
            var statementsAfter = objectAfter.getPolicyStatement();
            for (var markAfter : applyStatementsToMarkRefs(effectiveMarksBefore, statementsAfter)) {
                if (!containsRef(effectiveMarksBefore, markAfter)) {
                    markRefsToAdd.add(markAfter.clone());
                }
            }

            if (!markRefsToDelete.isEmpty() || !markRefsToAdd.isEmpty()) {
                return PrismContext.get().deltaFor(ObjectType.class)
                        .item(ObjectType.F_EFFECTIVE_MARK_REF)
                        .deleteRealValues(markRefsToDelete)
                        .addRealValues(markRefsToAdd)
                        .asItemDelta();
            } else {
                return null; // Nothing to add or remove.
            }
        }

        @Override
        public ItemDelta<?, ?> computeEffectiveMarkDelta(
                @NotNull Collection<ObjectReferenceType> currentMarks,
                @NotNull Collection<ObjectReferenceType> desiredMarks)
                throws SchemaException {

            var refsToDelete = difference(currentMarks, desiredMarks);
            var refsToAdd = difference(desiredMarks, currentMarks);

            if (!refsToDelete.isEmpty() || !refsToAdd.isEmpty()) {
                return PrismContext.get().deltaFor(ObjectType.class)
                        .item(ObjectType.F_EFFECTIVE_MARK_REF)
                        .deleteRealValues(refsToDelete)
                        .addRealValues(refsToAdd)
                        .asItemDelta();
            } else {
                // Nothing to add or remove.
                return null;
            }
        }

        /** Returns "whole - part", looking only at OIDs. Ignores null OIDs. Returns detached collection. */
        Collection<ObjectReferenceType> difference(
                @NotNull Collection<ObjectReferenceType> whole, @NotNull Collection<ObjectReferenceType> part) {
            var difference = new ArrayList<ObjectReferenceType>();
            for (var refFromWhole : whole) {
                if (refFromWhole.getOid() != null && !containsOid(part, refFromWhole.getOid())) {
                    difference.add(refFromWhole.clone());
                }
            }
            return difference;
        }

        private ObjectReferenceType findEffectiveMarkRefImpliedByStatement(List<ObjectReferenceType> effectiveMarks, String oid) {
            for (ObjectReferenceType mark : effectiveMarks) {
                if (oid.equals(mark.getOid()) && isImpliedByStatement(mark)) {
                    return mark;
                }
            }
            return null;
        }

        private boolean isImpliedByStatement(ObjectReferenceType mark) {
            // This is the limitation: we assume that each effective mark was implied by a statement, which is not true.
            return true;
        }

        private boolean containsRef(Collection<ObjectReferenceType> refs, ObjectReferenceType ref) {
            return containsOid(refs, ref.getOid());
        }
    }

    private static class Legacy extends Impl {

        /** We have no state. We have no storage for marks. So all we are interested in is the Protected mark. */
        @Override
        Collection<ObjectReferenceType> computeEffectiveMarks(
                @NotNull ObjectType object, @NotNull ObjectMarksComputer objectMarksComputer,
                @NotNull OperationResult result) throws SchemaException {
            if (objectMarksComputer.getComputableMarksOids().contains(MARK_PROTECTED_OID)
                    && objectMarksComputer.computeObjectMarkPresence(MARK_PROTECTED_OID, result)) {
                return List.of(new ObjectReferenceType().oid(MARK_PROTECTED_OID).type(MarkType.COMPLEX_TYPE));
            } else {
                return List.of();
            }
        }

        /** We are limited to simple "protected or not" decision. */
        @Override
        @NotNull ObjectOperationPolicyType computeEffectiveOperationPolicy(
                Collection<ObjectReferenceType> effectiveMarkRefs, Object context, OperationResult result) {
            if (containsOid(effectiveMarkRefs, MARK_PROTECTED_OID)) {
                return new ObjectOperationPolicyType()
                        .synchronize(new SynchronizeOperationPolicyConfigurationType()
                                .inbound(disabled(OperationPolicyViolationSeverityType.INFO))
                                .outbound(disabled(OperationPolicyViolationSeverityType.INFO)))
                        .add(disabled(OperationPolicyViolationSeverityType.ERROR))
                        .modify(disabled(OperationPolicyViolationSeverityType.ERROR))
                        .delete(disabled(OperationPolicyViolationSeverityType.ERROR));
            } else {
                return new ObjectOperationPolicyType();
            }
        }

        private OperationPolicyConfigurationType disabled(OperationPolicyViolationSeverityType severity) {
            return new OperationPolicyConfigurationType()
                    .enabled(false)
                    .severity(severity);
        }
    }

    private static boolean containsOid(@NotNull Collection<ObjectReferenceType> refs, @NotNull String oid) {
        return refs.stream()
                .anyMatch(ref -> oid.equals(ref.getOid()));
    }

    private boolean statementsContain(
            @NotNull List<PolicyStatementType> statements,
            @NotNull String markOid,
            @NotNull PolicyStatementTypeType policyType) {
        return statements.stream().anyMatch(
                s -> s.getType() == policyType && markOid.equals(getOid(s.getMarkRef())));
    }

    public interface ObjectMarksComputer {

        /** Computes recomputable object mark presence for given object. Does not deal with policy statements in any way. */
        boolean computeObjectMarkPresence(String markOid, OperationResult result) throws SchemaException;

        /** Returns OIDs of marks managed by this provider. */
        @NotNull Collection<String> getComputableMarksOids();
    }

    public record EffectiveMarksAndPolicies(
            @NotNull Collection<ObjectReferenceType> effectiveMarkRefs,
            @NotNull ObjectOperationPolicyType effectiveOperationPolicy,
            boolean isProtected) {

        public void applyTo(@NotNull ObjectType object) {
            object.getEffectiveMarkRef().clear();
            object.getEffectiveMarkRef().addAll(effectiveMarkRefs);
            object.setEffectiveOperationPolicy(effectiveOperationPolicy);
            if (object instanceof ShadowType shadow && isProtected) {
                shadow.setProtectedObject(true);
            }
        }
    }
}
