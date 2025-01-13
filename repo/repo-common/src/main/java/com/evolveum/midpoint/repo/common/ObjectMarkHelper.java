/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.MARK_PROTECTED_OID;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Computes effective object marks and their deltas.
 *
 * Does not deal with the policies attached to marks. See {@link ObjectOperationPolicyHelper} for that.
 *
 * Effective mark references have the following characteristics (see docs in the XSD):
 *
 * . They always have an OID.
 * . They have either a default relation, or `org:related` one. The latter means that the reference is inactive, and is present
 * only for the purpose of holding value metadata (usually that it was computed by a rule, but disabled by a statement).
 * . They are computed using the `production` configuration (regarding statements, and later e.g. marking rules).
 *
 * Note the difference between {@link #computeObjectMarks(ObjectType, ObjectMarksComputer, TaskExecutionMode, OperationResult)}
 * and {@link #computeObjectMarksWithoutComputer(ObjectType, TaskExecutionMode)}. The former is able to add extra marking, whereas
 * the latter simply applies policy statements to the existing marks.
 *
 * NOTE: Currently used for shadows only! Analogous functionality for focus objects is present
 * in {@link EvaluatedPolicyStatements} and the model code that uses it.
 *
 * @see ObjectOperationPolicyHelper
 */
@Component
public class ObjectMarkHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectMarkHelper.class);

    private static ObjectMarkHelper instance = null;

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;

    private Impl impl;

    @PostConstruct
    public void init() {
        impl = cacheRepositoryService.supportsMarks() ? new Modern() : new Legacy();
        instance = this;
    }

    @PreDestroy
    public void destroy() {
        instance = null;
    }

    public static ObjectMarkHelper get() {
        return instance;
    }

    /**
     * Computes effective marks for given object, taking into account:
     *
     * - Existing mark references (from the object); presumably, they were computed under the PRODUCTION mode.
     * - Policy statements (from the object); they are execution-mode-dependent, selected by `mode` parameter.
     * - Marks computer (optional); the operation result must be there only if the computer is present
     *
     * Notes:
     *
     * - Handles value metadata.
     * - No updates.
     * - Returns detached collection.
     *
     * @throws SchemaException but only if the computer is present
     */
    Collection<ObjectReferenceType> computeObjectMarks(
            @NotNull ObjectType object,
            @Nullable ObjectMarksComputer objectMarksComputer,
            @NotNull TaskExecutionMode mode,
            @Nullable OperationResult result) throws SchemaException {
        return impl.computeObjectMarks(object, objectMarksComputer, mode, result);
    }

    /**
     * A convenience no-computer variant of
     * {@link #computeObjectMarks(ObjectType, ObjectMarksComputer, TaskExecutionMode, OperationResult)}.
     */
    Collection<ObjectReferenceType> computeObjectMarksWithoutComputer(
            @NotNull ObjectType object, @NotNull TaskExecutionMode mode) {
        try {
            return computeObjectMarks(object, null, mode, null);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    /**
     * Computes effective mark delta for given object, given old state and the delta regarding policy statements.
     * Assuming that `effectiveMarkRef` is not changed in the same operation.
     */
    public ItemDelta<?, ?> computeEffectiveMarkDelta(ObjectType objectBefore, ItemDelta<?, ?> policyStatementDelta)
            throws SchemaException {
        return impl.computeEffectiveMarkDelta(objectBefore, policyStatementDelta);
    }

    /** Computes effective mark delta for given object, given current and desired state of marks. */
    public ItemDelta<?, ?> computeEffectiveMarkDelta(
            @NotNull Collection<ObjectReferenceType> currentMarks,
            @NotNull Collection<ObjectReferenceType> desiredMarks)
            throws SchemaException {
        return impl.computeEffectiveMarkDelta(currentMarks, desiredMarks);
    }

    private static abstract class Impl {

        abstract Collection<ObjectReferenceType> computeObjectMarks(
                @NotNull ObjectType object,
                @Nullable ObjectMarksComputer computer,
                @NotNull TaskExecutionMode mode,
                @Nullable OperationResult result) throws SchemaException;

        ItemDelta<?, ?> computeEffectiveMarkDelta(ObjectType objectBefore, ItemDelta<?, ?> policyStatementDelta)
                throws SchemaException {
            return null;
        }

        public ItemDelta<?, ?> computeEffectiveMarkDelta(
                @NotNull Collection<ObjectReferenceType> currentMarks,
                @NotNull Collection<ObjectReferenceType> desiredMarks)
                throws SchemaException {
            return null;
        }
    }

    private static class Legacy extends Impl {

        @Override
        public Collection<ObjectReferenceType> computeObjectMarks(
                @NotNull ObjectType object,
                @Nullable ObjectMarksComputer computer,
                @NotNull TaskExecutionMode mode,
                @Nullable OperationResult result) throws SchemaException {
            if (computer != null
                    && computer.getComputableMarksOids().contains(MARK_PROTECTED_OID)
                    && computer.computeObjectMarkPresence(MARK_PROTECTED_OID, Objects.requireNonNull(result)) != null) {
                return List.of(new ObjectReferenceType().oid(MARK_PROTECTED_OID).type(MarkType.COMPLEX_TYPE));
            } else {
                return List.of();
            }
        }
    }

    private static class Modern extends Impl {

        @Contract("_, !null, _, null -> fail")
        Collection<ObjectReferenceType> computeObjectMarks(
                @NotNull ObjectType object,
                @Nullable ObjectMarksComputer computer,
                @NotNull TaskExecutionMode mode,
                @Nullable OperationResult result) throws SchemaException {

            var computableMarksOids = computer != null ?
                    Set.copyOf(computer.getComputableMarksOids()) : Set.<String>of();

            var statements = object.getPolicyStatement().stream()
                    .filter(s -> SimulationUtil.isVisible(s.getLifecycleState(), mode))
                    .toList();

            // Ignoring the following yields:
            // - for non-computable marks: all statement-related yields
            // - for computable marks: all yields
            // The reason is that the policy rule based yields are currently not supported.

            var resultingRefs = new SmartMarkRefCollection(object);

            // 1. Currently computable marks
            for (var computableOid : computableMarksOids) {
                // We either add the mark anew (if computed as present), or ignore it completely (if computed as absent).
                // See the above comments about yields.
                var markPresence = computer.computeObjectMarkPresence(computableOid, Objects.requireNonNull(result));
                if (markPresence != null) {
                    resultingRefs.add(markPresence.toMarkRef());
                } else {
                    // will be treated from statements only
                }
            }

            // 2. All other existing (previously computed) marks
            for (ObjectReferenceType currentMarkRef : object.getEffectiveMarkRef()) {
                String oid = currentMarkRef.getOid();
                if (oid == null) {
                    continue; // illegal mark ref, just ignore it
                }
                if (!computableMarksOids.contains(oid)) {
                    // Not computable, just keep the mark
                    resultingRefs.add(currentMarkRef);
                }
            }

            // 3. Statements
            resultingRefs.addStatements(statements);

            return resultingRefs.asRefCollection();
        }

        public ItemDelta<?, ?> computeEffectiveMarkDelta(ObjectType objectBefore, ItemDelta<?, ?> policyStatementDelta)
                throws SchemaException {
            var objectAfter = objectBefore.clone();
            policyStatementDelta.applyTo(objectAfter.asPrismObject());

            var effectiveMarksBefore = objectBefore.getEffectiveMarkRef();
            var effectiveMarksAfter = get().computeObjectMarksWithoutComputer(objectAfter, TaskExecutionMode.PRODUCTION);

            return computeEffectiveMarkDelta(effectiveMarksBefore, effectiveMarksAfter);
        }

        public ItemDelta<?, ?> computeEffectiveMarkDelta(
                @NotNull Collection<ObjectReferenceType> currentMarks,
                @NotNull Collection<ObjectReferenceType> desiredMarks)
                throws SchemaException {

            // FIXME preliminary dirty implementation (TODO replace with ADD+DELETE)

            // We don't use QNameUtil.match here, because we want to ensure we store qualified values there
            // (and newValues are all qualified).
            // See also AssignmentProcessor#setReferences.
            EqualsChecker<ObjectReferenceType> comparator =
                    (a, b) -> Objects.equals(a.getOid(), b.getOid())
                            && Objects.equals(a.getRelation(), b.getRelation())
                            && a.asReferenceValue().getValueMetadata().equals(
                            b.asReferenceValue().getValueMetadata(), EquivalenceStrategy.REAL_VALUE);
            if (MiscUtil.unorderedCollectionEquals(currentMarks, desiredMarks, comparator)) {
                return null;
            } else {
                return PrismContext.get().deltaFor(ObjectType.class)
                        .item(ObjectType.F_EFFECTIVE_MARK_REF)
                        .replaceRealValues(desiredMarks)
                        .asItemDelta();
            }
        }
    }

    public interface ObjectMarksComputer {

        /**
         * Computes recomputable object mark presence for given object. Does not deal with policy statements in any way.
         * Returns {@code null} if the mark is not present.
         */
        @Nullable MarkPresence computeObjectMarkPresence(@NotNull String markOid, @NotNull OperationResult result)
                throws SchemaException;

        /** Returns OIDs of marks managed by this provider. */
        @NotNull Collection<String> getComputableMarksOids();
    }

    public static class MarkPresence {

        @NotNull private final String markOid;
        @NotNull private final MarkingRuleSpecificationType rule;

        private MarkPresence(@NotNull String markOid, @NotNull MarkingRuleSpecificationType rule) {
            this.markOid = markOid;
            this.rule = rule;
        }

        public static MarkPresence of(@NotNull String markOid, @NotNull MarkingRuleSpecificationType rule) {
            return new MarkPresence(markOid, rule);
        }

        @NotNull ObjectReferenceType toMarkRef() throws SchemaException {
            var ref = new ObjectReferenceType()
                    .oid(markOid)
                    .type(MarkType.COMPLEX_TYPE);
            ref.asReferenceValue()
                    .getValueMetadata()
                    .addMetadataValue(
                            new ValueMetadataType()
                                    .provenance(new ProvenanceMetadataType()
                                            .markingRule(rule))
                                    .asPrismContainerValue());
            return ref;
        }
    }

    /**
     * Encapsulates the logic of maintaining a collection of effective mark references.
     *
     * Currently it deals only with yields related to marking rules and policy statements.
     */
    private static class SmartMarkRefCollection {

        /** Linked hash map to preserve the order - not necessary but nice. */
        private final Map<String, ObjectReferenceType> markRefMap = new LinkedHashMap<>();

        /** For error reporting. */
        private final Object context;

        SmartMarkRefCollection(@NotNull Object context) {
            this.context = context;
        }

        /** Adds the marking rule based yields from markRef. */
        void add(@NotNull ObjectReferenceType markRef) throws SchemaException {
            var oid = Objects.requireNonNull(markRef.getOid());
            var metadataWithMarkingRuleProvenance = markRef.asReferenceValue().getValueMetadata().getValues().stream()
                    .map(v -> ((ValueMetadataType) v.asContainerable()))
                    .filter(v -> v.getProvenance() != null && v.getProvenance().getMarkingRule() != null)
                    .toList();
            if (metadataWithMarkingRuleProvenance.isEmpty()) {
                return; // ignoring markRef not related to marking rules
            }

            var storedRef = markRefMap.computeIfAbsent(
                    oid,
                    // We do not set the relation here, even if it was on the original markRef.
                    // We do that only if enforced by a (negative) statement.
                    k -> new ObjectReferenceType()
                            .oid(oid)
                            .type(MarkType.COMPLEX_TYPE));

            // Now transplant provenance metadata from markRef
            var storedRefMetadata = storedRef.asReferenceValue().getValueMetadata();
            for (var newMdValue : metadataWithMarkingRuleProvenance) {
                var newProvenance = Objects.requireNonNull(newMdValue.getProvenance());
                var matching = ValueMetadataTypeUtil.findMatchingValue(storedRefMetadata, newProvenance);
                if (matching != null) {
                    // TODO merge the metadata (later); now simply replacing the metadata
                    //noinspection unchecked
                    storedRefMetadata.remove((PrismContainerValue<Containerable>) matching);
                }
                storedRefMetadata.addMetadataValue(newMdValue.asPrismContainerValue().clone());
            }
        }

        /** Updates the information from marking rules with the statements. */
        void addStatements(List<PolicyStatementType> allStatements) throws SchemaException {
            var oids = allStatements.stream()
                    .map(s -> getOid(s.getMarkRef()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)); // to preserve the order (to be nice)
            for (var oid : oids) {
                var positiveStatements = allStatements.stream()
                        .filter(s -> oid.equals(getOid(s.getMarkRef())))
                        .filter(s -> s.getType() == PolicyStatementTypeType.APPLY)
                        .toList();
                var negativeStatements = allStatements.stream()
                        .filter(s -> oid.equals(getOid(s.getMarkRef())))
                        .filter(s -> s.getType() == PolicyStatementTypeType.EXCLUDE)
                        .toList();
                if (!negativeStatements.isEmpty()) {
                    if (!positiveStatements.isEmpty()) {
                        LOGGER.warn("Mark {} has both positive and negative statements, ignoring the positive ones; in {}",
                                oid, context);
                    }
                    var storedRef = markRefMap.get(oid);
                    if (storedRef != null) {
                        storedRef.setRelation(SchemaConstants.ORG_RELATED);
                        addStatementsValueMetadata(storedRef, negativeStatements);
                    } else {
                        // not defined by marking rules, so we don't need to add the negative information from statements
                    }
                } else if (!positiveStatements.isEmpty()) {
                    var storedRef = markRefMap.computeIfAbsent(
                            oid,
                            k -> new ObjectReferenceType()
                                    .oid(oid)
                                    .type(MarkType.COMPLEX_TYPE));
                    addStatementsValueMetadata(storedRef, positiveStatements);
                } else {
                    // could happen e.g. if the type of statement is not recognized
                }
            }
        }

        private void addStatementsValueMetadata(ObjectReferenceType markRef, List<PolicyStatementType> statements)
                throws SchemaException {
            var metadata = markRef.asReferenceValue().getValueMetadata();
            for (var statement : statements) {
                metadata.addMetadataValue(
                        new ValueMetadataType()
                                .provenance(new ProvenanceMetadataType()
                                        .policyStatement(new PolicyStatementSpecificationType()
                                                .statementId(statement.getId())))
                                .asPrismContainerValue());
            }
        }

        Collection<ObjectReferenceType> asRefCollection() {
            return markRefMap.values();
        }
    }
}
