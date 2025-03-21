/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.assignments;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.schema.config.AbstractAssignmentConfigItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Primary duty of this class is to be a part of assignment path. (This is what is visible through its interface,
 * AssignmentPathSegment.)
 *
 * However, it also stores some (although not complete) information about assignment evaluation.
 *
 * Note: we avoid getter methods for some final fields to simplify client code.
 *
 * @author semancik
 */
public class AssignmentPathSegmentImpl implements AssignmentPathSegment, Freezable {

    private boolean immutable;

    //region Description of the situation: source, assignment, target
    /**
     * Source object for this assignment path.
     * This is the object physically or logically holding the assignment or inducement.
     *
     * If the source is the focus, we use NEW state of the object.
     *
     * TODO consider if that's correct. But it is important e.g. when deriving the expression profile from the archetype.
     */
    final AssignmentHolderType source;

    /**
     * Human readable text describing the source (for error messages)
     */
    final String sourceDescription;

    /** The "physical" origin of the assignment. [EP:APSO] DONE 2/2 */
    @NotNull final ConfigurationItemOrigin assignmentOrigin;

    /**
     * Item-delta-item form of the assignment.
     * TODO which ODO (absolute/relative)? MID-6404
     */
    @NotNull private final ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi;

    /** Repo-generated ID for new assignment, if available. */
    @Nullable private final Long externalAssignmentId;

    /**
     * Are we evaluating the old or new state of the assignment?
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean evaluateOld;

    /**
     * Pure form of the assignment (new or old): taking {@link #evaluateOld} into account.
     */
    @NotNull final AssignmentType assignment;

    /** TODO describe, use */
    @Experimental
    @NotNull final AbstractAssignmentConfigItem assignmentConfigItem;

    /**
     * Relation of the assignment; normalized.
     */
    final QName relation;

    /**
     * True if we are referencing "real" assignment; false if this is an inducement.
     */
    private final boolean isAssignment;

    /**
     * Flag to make a difference between normal assignment/inducement and the one created based on the
     * archetype hierarchy. If true, assignment/inducement was created based on the superArchetypeRef
     * relation.
     */
    private final boolean archetypeHierarchy;

    /**
     * Resolved target object. (Can be null if there's no target or if the target was not resolved yet.)
     */
    final ObjectType target;

    /**
     * Is this a direct assignment (real or virtual) of the focus?
     */
    final boolean direct;

    //endregion

    //region Data related to segment evaluation
    /**
     * Is the whole path to SOURCE active?
     *
     * By "active" we mean that lifecycle state and activation (admin status / time validity)
     * indicate that the object(s) and respective assignment(s) are active. Conditions are not
     * considered here, at least not now.
     *
     * Also note that the activity of this assignment is NOT considered.
     *
     * Activity influences the evaluation process. But look for specific code to learn
     * precisely how.
     */
    final boolean pathToSourceActive;

    /**
     * Is assignment itself considered active (taking lifecycle, activation, and lifecycle state model
     * into account)? Note that we can evaluate parts of assignments even if they are not active
     * in this respect.
     *
     * The assignment can be considered active even if its source is not active!
     */
    private boolean assignmentActive;

    /**
     * Aggregated condition state for the path to assignment source (including the source itself).
     * Condition of the assignment is NOT considered.
     *
     * Condition state of the ultimate source (i.e. focus) is not considered.
     */
    final ConditionState pathToSourceConditionState;

    /**
     * Condition state of the assignment itself.
     */
    private ConditionState assignmentConditionState;

    /**
     * Combination of path-to-source and assignment condition state.
     */
    private ConditionState overallConditionState;

    /**
     * Evaluation order of this segment regarding the focus. If there were no inducements,
     * it would be a collection of relations of assignments on the path. For inducements
     * please see {@link TargetInducementEvaluation}.
     *
     * For evaluation orders please see discussion at the end of this class.
     */
    private final EvaluationOrder evaluationOrder;

    /**
     * Has this segment matching order regarding the focus? I.e. is its payload applied to the focus?
     * I.e. applies its content to the focus?
     *
     * Normally this value is computed when creating this object.
     * But in case of inducements it is set explicitly.
     *
     * For evaluation order matching please see discussion at the end of this class.
     */
    final boolean isMatchingOrder;

    /**
     * Evaluation order of this segment regarding the target. If there were no inducements,
     * it would be a collection of relations of assignments on the path. For inducements
     * please see {@link TargetInducementEvaluation}.
     *
     * For evaluation orders please see discussion at the end of this class.
     */
    private final EvaluationOrder evaluationOrderForTarget;

    /**
     * Has this segment matching order regarding the target? I.e. is its payload applied to the target?
     * (This is important e.g. for target policy rules.)
     *
     * Normally this value is computed when creating this object.
     * But in case of inducements it is set explicitly.
     *
     * For evaluation order matching please see discussion at the end of this class.
     */
    final boolean isMatchingOrderForTarget;

    /**
     * Last segment with the same evaluation order as this one. Used for higher-order
     * (summary-rewriting) inducements. See e.g. TestAssignmentProcessor2.test600.
     */
    private final Integer lastEqualOrderSegmentIndex;

    //endregion

    private AssignmentPathSegmentImpl(Builder builder) {
        source = builder.source;
        sourceDescription = builder.sourceDescription;
        assignmentIdi = builder.assignmentIdi;
        externalAssignmentId = builder.externalAssignmentId;
        // [EP:APSO] from builder DONE 1/1
        assignmentOrigin = Objects.requireNonNull(builder.assignmentOrigin, "no assignmentOrigin");
        evaluateOld = builder.evaluateOld;
        assignment = Util.getAssignment(assignmentIdi, evaluateOld);
        assignmentConfigItem = AbstractAssignmentConfigItem.of(assignment, assignmentOrigin);
        relation = getRelation(assignment, getRelationRegistry());
        isAssignment = builder.isAssignment;
        target = builder.target;
        direct = builder.direct;
        pathToSourceActive = builder.pathToSourceActive;
        // assignment activity is not present in builder (it's computed)
        pathToSourceConditionState = builder.pathToSourceConditionState;
        // other condition states not present in builder (it's computed)
        evaluationOrder = builder.evaluationOrder;
        isMatchingOrder = builder.isMatchingOrder != null ?
                builder.isMatchingOrder : computeMatchingOrder(evaluationOrder, assignment);
        evaluationOrderForTarget = builder.evaluationOrderForTarget;
        isMatchingOrderForTarget = builder.isMatchingOrderForTarget != null ?
                builder.isMatchingOrderForTarget : computeMatchingOrder(evaluationOrderForTarget, assignment);
        lastEqualOrderSegmentIndex = builder.lastEqualOrderSegmentIndex;
        archetypeHierarchy = builder.archetypeHierarchy;
    }

    private boolean computeMatchingOrder(EvaluationOrder evaluationOrder, AssignmentType assignment) {
        return evaluationOrder.matches(assignment.getOrder(), assignment.getOrderConstraint());
    }

    private AssignmentPathSegmentImpl(AssignmentPathSegmentImpl origSegment, ObjectType target) {
        this.source = origSegment.source;
        this.sourceDescription = origSegment.sourceDescription;
        this.assignmentIdi = origSegment.assignmentIdi;
        this.externalAssignmentId = origSegment.externalAssignmentId;
        this.assignmentOrigin = origSegment.assignmentOrigin; // [EP:APSO] from itself
        this.evaluateOld = origSegment.evaluateOld;
        this.assignment = origSegment.assignment;
        this.assignmentConfigItem = origSegment.assignmentConfigItem;
        this.relation = origSegment.relation;
        this.isAssignment = origSegment.isAssignment;
        this.archetypeHierarchy = origSegment.archetypeHierarchy;
        this.target = target;
        this.direct = origSegment.direct;
        this.pathToSourceActive = origSegment.pathToSourceActive;
        this.assignmentActive = origSegment.assignmentActive;
        this.pathToSourceConditionState = origSegment.pathToSourceConditionState;
        this.assignmentConditionState = origSegment.assignmentConditionState;
        this.overallConditionState = origSegment.overallConditionState;
        this.evaluationOrder = origSegment.evaluationOrder;
        this.isMatchingOrder = origSegment.isMatchingOrder;
        this.evaluationOrderForTarget = origSegment.evaluationOrderForTarget;
        this.isMatchingOrderForTarget = origSegment.isMatchingOrderForTarget;
        this.lastEqualOrderSegmentIndex = origSegment.lastEqualOrderSegmentIndex;
    }

    @Override
    public boolean isAssignment() {
        return isAssignment;
    }

    boolean isArchetypeHierarchy() {
        return archetypeHierarchy;
    }

    @NotNull
    public ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> getAssignmentIdi() {
        return assignmentIdi;
    }

    public @Nullable AssignmentType getAssignment(boolean evaluateOld) {
        return Util.getAssignment(assignmentIdi, evaluateOld);
    }

    public @Nullable PrismContainerValue<AssignmentType> getAssignmentPcv(boolean evaluateOld) {
        return Util.getAssignmentPcv(assignmentIdi, evaluateOld);
    }

    private static QName getRelation(AssignmentType assignment, RelationRegistry relationRegistry) {
        return assignment != null && assignment.getTargetRef() != null ?
                relationRegistry.normalizeRelation(assignment.getTargetRef().getRelation()) : null;
    }

    public @NotNull AssignmentType getAssignment() {
        return assignment;
    }

    public AssignmentType getAssignmentAny() {
        return assignmentIdi.getItemNew() != null ? getAssignment(false) : getAssignment(true);
    }

    @Override
    public AssignmentType getAssignmentNew() {
        var itemNew = assignmentIdi.getItemNew();
        if (itemNew == null || itemNew.isEmpty()) {
            return null;
        } else {
            return ((PrismContainer<AssignmentType>) itemNew).getRealValue();
        }
    }

    @Override
    public QName getRelation() {
        return relation;
    }

    @Override
    public ObjectType getTarget() {
        return target;
    }

    AssignmentPathSegmentImpl cloneWithTargetSet(ObjectType target) {
        return new AssignmentPathSegmentImpl(this, target);
    }

    @Override
    public AssignmentHolderType getSource() {
        return source; // can the source be null?
    }

    public String getSourceOid() {
        return source != null ? source.getOid() : null;
    }

    String getTargetDescription() {
        if (target != null) {
            return target + " in " + sourceDescription;
        } else {
            return "(target) in " + sourceDescription;
        }
    }

    boolean isAssignmentActive() {
        return assignmentActive;
    }

    void setAssignmentActive(boolean assignmentActive) {
        checkMutable();
        this.assignmentActive = assignmentActive;
    }

    boolean isFullPathActive() {
        return pathToSourceActive && assignmentActive;
    }

    PlusMinusZero getRelativeAssignmentRelativityMode() {
        return overallConditionState.getRelativeRelativityMode();
    }

    PlusMinusZero getAbsoluteAssignmentRelativityMode() {
        return overallConditionState.getAbsoluteRelativityMode();
    }

    public ConditionState getAssignmentConditionState() {
        return assignmentConditionState;
    }

    public void setAssignmentConditionState(ConditionState assignmentConditionState) {
        checkMutable();
        this.assignmentConditionState = assignmentConditionState;
    }

    public ConditionState getOverallConditionState() {
        return overallConditionState;
    }

    public void setOverallConditionState(ConditionState overallConditionState) {
        checkMutable();
        this.overallConditionState = overallConditionState;
    }

    boolean isNonNegativeRelativeRelativityMode() {
        return Util.isNonNegative(getRelativeAssignmentRelativityMode()); // TODO use condition to say this
    }

    public EvaluationOrder getEvaluationOrder() {
        return evaluationOrder;
    }

    EvaluationOrder getEvaluationOrderForTarget() {
        return evaluationOrderForTarget;
    }

    @Override
    public boolean isMatchingOrder() {
        return isMatchingOrder;
    }

    @Override
    public boolean isMatchingOrderForTarget() {
        return isMatchingOrderForTarget;
    }

    @Override
    public boolean isDelegation() {
        return getRelationRegistry().isDelegation(relation);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + assignmentIdi.hashCode();
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        return result;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    private boolean equalsExceptForTarget(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AssignmentPathSegmentImpl other = (AssignmentPathSegmentImpl) obj;
        if (!assignmentIdi.equals(other.assignmentIdi)) {
            return false;
        }
        if (source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!source.equals(other.source)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings({ "RedundantIfStatement", "EqualsWhichDoesntCheckParameterClass" })
    @Override
    public boolean equals(Object obj) {
        if (!equalsExceptForTarget(obj)) {
            return false;
        }
        AssignmentPathSegmentImpl other = (AssignmentPathSegmentImpl) obj;
        if (target == null) {
            if (other.target != null) {
                return false;
            }
        } else if (!target.equals(other.target)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AssignmentPathSegment(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        evaluationOrder.shortDump(sb);
        if (isMatchingOrder) {
            sb.append("(match)");
        }
        if (isMatchingOrderForTarget) {
            sb.append("(match-target)");
        }
        sb.append(": ");
        sb.append(source).append(" ");
        if (!isAssignment) {
            sb.append("inducement ");
        }
        PrismContainer<AssignmentType> assignment = (PrismContainer<AssignmentType>) assignmentIdi.getAnyItem();
        AssignmentType assignmentType = assignment != null ? assignment.getRealValue() : null;
        if (assignmentType != null) {
            sb.append("id:").append(assignmentType.getId()).append(" ");
            if (assignmentType.getConstruction() != null) {
                sb.append("Constr '").append(assignmentType.getConstruction().getDescription()).append("' ");
            }
            if (assignmentType.getFocusMappings() != null) {
                sb.append("FMappings (").append(assignmentType.getFocusMappings().getMapping().size()).append(") ");
            }
            if (assignmentType.getPolicyRule() != null) {
                sb.append("Rule '").append(assignmentType.getPolicyRule().getName()).append("' ");
            }
        }
        ObjectReferenceType targetRef = assignmentType != null ? assignmentType.getTargetRef() : null;
        if (target != null || targetRef != null) {
            sb.append("-[");
            if (relation != null) {
                sb.append(relation.getLocalPart());
            }
            sb.append("]-> ");
            if (target != null) {
                sb.append(target);
            } else {
                sb.append(ObjectTypeUtil.toShortString(targetRef, true));
            }
        }
        // TODO eventually remove this
        if (lastEqualOrderSegmentIndex != null) {
            sb.append(", lastEqualOrder: ").append(lastEqualOrderSegmentIndex);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, "AssignmentPathSegment", indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "source", source == null ? "null" : source.toString(), indent + 1);
        String assignmentOrInducement = isAssignment ? "assignment" : "inducement";
        DebugUtil.debugDumpWithLabelLn(sb, assignmentOrInducement + " old", String.valueOf(assignmentIdi.getItemOld()), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, assignmentOrInducement + " delta", String.valueOf(assignmentIdi.getDelta()), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, assignmentOrInducement + " new", String.valueOf(assignmentIdi.getItemNew()), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "target", target == null ? "null" : target.toString(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "evaluationOrder", evaluationOrder, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "isMatchingOrder", isMatchingOrder, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "isMatchingOrderForTarget", isMatchingOrderForTarget, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "relation", relation, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "pathToSourceActive", pathToSourceActive, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "assignmentActive", assignmentActive, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "pathToSourceConditionState", String.valueOf(pathToSourceConditionState), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "assignmentConditionState", String.valueOf(assignmentConditionState), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "overallConditionState", String.valueOf(overallConditionState), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "lastEqualOrderSegmentIndex", lastEqualOrderSegmentIndex, indent + 1);
        return sb.toString();
    }

    @NotNull
    @Override
    public AssignmentPathSegmentType toAssignmentPathSegmentBean(boolean includeAssignmentsContent) {
        AssignmentPathSegmentType rv = new AssignmentPathSegmentType();
        if (includeAssignmentsContent) {
            rv.setAssignment(assignment.clone());
        }
        rv.setAssignmentId(getAssignmentId());
        if (source != null) {
            rv.setSourceRef(ObjectTypeUtil.createObjectRef(source));
            rv.setSourceDisplayName(ObjectTypeUtil.getDisplayName(source));
        }
        if (target != null) {
            rv.setTargetRef(ObjectTypeUtil.createObjectRef(target, relation));
            rv.setTargetDisplayName(ObjectTypeUtil.getDisplayName(target));
        }
        rv.setMatchingOrder(isMatchingOrder);
        rv.setIsAssignment(isAssignment);
        return rv;
    }

    Integer getLastEqualOrderSegmentIndex() {
        return lastEqualOrderSegmentIndex;
    }

    @Override
    public boolean matches(@NotNull List<OrderConstraintsType> orderConstraints) {
        return evaluationOrder.matches(null, orderConstraints);
    }

    // preliminary implementation; use only to compare segments in paths (pointing to the same target OID)
    // that are to be checked for equivalency
    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equivalent(AssignmentPathSegment otherSegment) {
        if (!getPrismContext().relationsEquivalent(relation, otherSegment.getRelation())) {
            return false;
        }
        if (target == null && otherSegment.getTarget() == null) {
            return true;            // TODO reconsider this in general case
        }
        if (target == null || otherSegment.getTarget() == null) {
            return false;
        }
        return Objects.equals(target.getOid(), otherSegment.getTarget().getOid());
    }

    @Override
    public boolean isImmutable() {
        return immutable;
    }

    @Override
    public void freeze() {
        immutable = true;
    }

    public @Nullable Long getAssignmentId() {
        return MiscUtil.getFirstNonNull(assignment.getId(), externalAssignmentId);
    }

    @NotNull
    private RelationRegistry getRelationRegistry() {
        return SchemaService.get().relationRegistry();
    }

    @NotNull
    private PrismContext getPrismContext() {
        return PrismService.get().prismContext();
    }

    public static final class Builder {
        private AssignmentHolderType source;
        private String sourceDescription;
        private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi;
        private Long externalAssignmentId;
        private ConfigurationItemOrigin assignmentOrigin; // [EP:APSO] DONE 1/1
        private boolean evaluateOld;
        private boolean isAssignment;
        private boolean archetypeHierarchy;
        private ObjectType target;
        private boolean direct;
        private boolean pathToSourceActive;
        private ConditionState pathToSourceConditionState;
        private Integer lastEqualOrderSegmentIndex;
        private Boolean isMatchingOrder;
        private EvaluationOrder evaluationOrder;
        private Boolean isMatchingOrderForTarget;
        private EvaluationOrder evaluationOrderForTarget;

        public Builder() {
        }

        public Builder source(AssignmentHolderType val) {
            source = val;
            return this;
        }

        public Builder sourceDescription(String val) {
            sourceDescription = val;
            return this;
        }

        Builder assignmentIdi(ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> val) {
            assignmentIdi = val;
            return this;
        }

        Builder externalAssignmentId(Long val) {
            externalAssignmentId = val;
            return this;
        }

        public Builder assignment(AssignmentType assignment) {
            try {
                //noinspection unchecked,rawtypes
                assignmentIdi = new ItemDeltaItem<>(
                        LensUtil.createAssignmentSingleValueContainer(assignment),
                        assignment.asPrismContainerValue().getDefinition());
            } catch (SchemaException e) {
                // should not really occur!
                throw new SystemException("Couldn't create assignment IDI: " + e.getMessage(), e);
            }

            return this;
        }

        public Builder assignmentOrigin(ConfigurationItemOrigin val) {
            assignmentOrigin = val; // [EP:APSO] DONE 4/4
            return this;
        }

        public Builder evaluateOld(boolean val) {
            evaluateOld = val;
            return this;
        }

        /** Declares the segment as an assignment, not inducement. */
        public Builder isAssignment() {
            isAssignment = true;
            return this;
        }

        /** Declares the segment as an inducement. */
        public Builder isInducement() {
            isAssignment = false;
            return this;
        }

        public Builder isHierarchy(boolean val) {
            archetypeHierarchy = val;
            return this;
        }

        public Builder target(ObjectType val) {
            target = val;
            return this;
        }

        public Builder direct(boolean val) {
            direct = val;
            return this;
        }

        public Builder pathToSourceValid(boolean val) {
            pathToSourceActive = val;
            return this;
        }

        public Builder pathToSourceConditionState(ConditionState val) {
            pathToSourceConditionState = val;
            return this;
        }

        Builder lastEqualOrderSegmentIndex(Integer val) {
            lastEqualOrderSegmentIndex = val;
            return this;
        }

        public Builder isMatchingOrder(Boolean val) {
            isMatchingOrder = val;
            return this;
        }

        public Builder evaluationOrder(EvaluationOrder val) {
            evaluationOrder = val;
            return this;
        }

        Builder isMatchingOrderForTarget(Boolean val) {
            isMatchingOrderForTarget = val;
            return this;
        }

        public Builder evaluationOrderForTarget(EvaluationOrder val) {
            evaluationOrderForTarget = val;
            return this;
        }

        public AssignmentPathSegmentImpl build() {
            return new AssignmentPathSegmentImpl(this);
        }
    }
}

/*
 *  Assignments and inducements can carry constructions, focus mappings, and policy rules.
 *  We can call these "assignment/inducement payload", or "payload" for short.
 *
 *  When looking at assignments/inducements in assignment path, payload of some assignments/inducements will be collected
 *  to focus, while payload from others will be not. How we know what to collect?
 *
 *  For assignments/inducements belonging directly to the focus, we take payload from all the assignments. Not from inducements.
 *  For assignments/inducements belonging to roles (assigned to focus), we take payload from all the inducements of order 1.
 *  For assignments/inducements belonging to meta-roles (assigned to roles), we take payload from all the inducements of order 2.
 *  And so on. (It is in fact a bit more complicated, as described below when discussing relations. But OK for the moment.)
 *
 *  To know whether to collect payload from assignment/inducement, i.e. from assignment path segment, we
 *  define "isMatchingOrder" attribute - and collect only if value of this attribute is true.
 *
 *  How we compute this attribute?
 *
 *  Each assignment path segment has an evaluation order. First assignment has an evaluation order of 1, second
 *  assignment has an order of 2, etc. Order of a segment can be seen as the number of assignments segments in the path
 *  (including itself). And, for "real" assignments (i.e. not inducements), we collect payload from assignment segments
 *  of order 1.
 *
 *  But what about inducements? There are two - somewhat related - questions:
 *
 *  1. How we compute isMatchingOrder for inducement segments?
 *  2. How we compute evaluation order for inducement segments?
 *
 *  As for #1: To compute isMatchingOrder, we must take evaluation order of the _previous_ segment, and compare
 *  it with the order (or, more generally, order constraints) of the inducement. If they match, we say that inducement
 *  has matching order.
 *
 *  As for #2: It is not usual that inducements have targets (roles) with another assignments, i.e. that evaluation continues
 *  after an inducement segment. But it definitely could happen. We can look at it this way: inducement is something like
 *  a "shortcut" that creates an assignment where no assignment was before. E.g. if we have R1 -A-> MR1 -I-> MR2,
 *  the "newly created" assignment is R1 -A-> MR2. I.e. as if the " -A-> MR1 -I-> " part was just replaced by " -A-> ".
 *  If the inducement is of higher order, even more assignments are "cut out". From R1 -A-> MR1 -A-> MMR1 -(I2)-> MR2
 *  we have R1 -A-> MR2, i.e. we cut out " -A-> MR1 -A-> MMR1 -(I2)-> " and replaced it by " -A-> ".
 *  So it looks like that when computing new evaluation order of an inducement, we have to "go back" few steps
 *  through the assignment path.
 *
 *  Such situation can also easily occur when org structures are used. As depicted in TestAssignmentProcessor2.test500
 *  and later, imagine this:
 *
 *           Org1 -----I----+                                              Org2 -----I----+
 *             ^            | (orderConstraints 1..N)                        ^            | (orderConstraints: manager: 1)
 *             |            |                                                |            |
 *             |            V                                                |            V
 *           Org11        Admin                                            Org21        Admin
 *             ^                                                             ^
 *             |                                                         (manager)
 *             |                                                             |
 *            jack                                                          jack
 *
 *  In order to resolve such cases, we created the "resetOrder" attribute. It can be applied either to
 *  summary order, or to one or more specific relations (please, don't specify it for both summary order
 *  and specific relations!)
 *
 *  In the above examples, we could specify e.g. resetOrder=1 for summary order (for both left and right situation).
 *  For the right one, we could instead specify resetOrder=0 for org:manager relation; although the former solution
 *  is preferable.
 *
 *  By simply specifying inducement order greater than 1 (e.g. 5) without any specific order constraints,
 *  we implicitly provide resetOrder instruction for summary order that points order-1 levels back (i.e. 4 levels back
 *  for order=5).
 *
 *  Generally, it is preferred to use resetOrder for summary order. It works well with both normal and target
 *  evaluation order. When resetting individual components, target evaluation order can have problems, as shown
 *  in TestEvaluationProcessor2.test520.
 *
 *  ----
 *
 *  Because evaluation order can "increase" and "decrease", it is possible that it goes to zero or below, and then
 *  increase back to positive numbers. Is that OK? Imagine this:
 *
 *  (Quite an ugly example, but such things might exist.)
 *
 *  Metarole:CrewMember ----I----+  Metarole:Sailors
 *           A                   |  A
 *           |                   |  |
 *           |                   V  |
 *         Pirate               Sailor
 *           A
 *           |
 *           |
 *          jack
 *
 *  When evaluating jack->Pirate assignment, it is OK to collect from everything (Pirate, CrewMember, Sailor, Sailors).
 *
 *  But when evaluating Pirate as a focal object (forget about jack for the moment), we have Pirate->CrewMember assignment.
 *  For this assignment we should ignore payload from Sailor (obviously, because the order is not matching), but from
 *  Metarole:Sailors as well. Payload from Sailors is not connected to Pirate in any meaningful way. (For example, if
 *  Sailors prescribes an account construction for Sailor, it is of no use to collect this construction when evaluating
 *  Pirate as focal object!)
 *
 *  Evaluating relations
 *  ====================
 *
 *  With the arrival of various kinds of relations (deputy, manager, approver, owner) things got a bit complicated.
 *  For instance, the deputy relation cannot be used to determine evaluation order in a usual way, because if
 *  we have this situation:
 *
 *  Pirate -----I-----> Sailor
 *    A
 *    | (default)
 *    |
 *   jack
 *    A
 *    | (deputy)
 *    |
 *  barbossa
 *
 *  we obviously want to have barbossa to obtain all payload from roles Pirate and Sailor: exactly as jack does.
 *  So, the evaluation order of " barbossa -A-> jack -A-> Pirate " should be 1, not two. So deputy is a very special
 *  kind of relation, that does _not_ increase the traditional evaluation order. But we really want to record
 *  the fact that the deputy is on the assignment path; therefore, besides traditional "scalar" evaluation order
 *  (called "summaryOrder") we maintain evaluation orders for each relation separately. In the above example,
 *  the evaluation orders would be:
 *     barbossa--->jack     summary: 0, deputy: 1, default: 0
 *     jack--->Pirate       summary: 1, deputy: 1, default: 1
 *     Pirate-I->Sailor     summary: 1, deputy: 1, default: 1 (because the inducement has a default order of 1)
 *
 *  When we determine matchingOrder for an inducement (question #1 above), we can ask about summary order,
 *  as well as about individual components (deputy and default, in this case).
 *
 *  Actually, we have three categories of relations (see MID-3581):
 *
 *  - membership relations: apply membership references, payload, authorizations, gui config
 *  - delegation relations: similar to membership, bud different handling of order
 *  - other relations: apply membership references but in limited way; payload, authorizations and gui config
 *    are - by default - not applied.
 *
 *  Currently, membership relations are: default (i.e. member), manager, and meta.
 *  Delegation: deputy.
 *  Other: approver, owner, and custom ones.
 *
 *  As for the "other" relations: they bring membership information, but limited to the target that is directly
 *  assigned. So if jack is an approver for role Landluber, his roleMembershipRef will contain ref to Landluber
 *  but not e.g. to role Earthworm that is induced by Landluber. In a similar way, payload from targets assigned
 *  by "other" relations is not collected.
 *
 *  Both of this can be overridden by using specific orderConstraints on particular inducement.
 *  Set of order constraint is considered to match evaluation order with "other" relations, if for each such "other"
 *  relation it contains related constraint. So, if one explicitly wants an inducement to be applied when
 *  "approver" relation is encountered, he may do so.
 *
 *  Note: authorizations and gui config information are not considered to be a payload of assignment,
 *  because they are not part of an assignment/inducement - they are payload of a role. In the future we
 *  might move them into assignments/inducements.
 *
 *  Collecting target policy rules
 *  ==============================
 *
 *  Special consideration must be given when collecting target policy rules, i.e. rules that are attached to
 *  assignment targets. Such rules are typically attached to roles that are being assigned. So let's consider this:
 *
 *  rule1 (e.g. assignment approval policy rule)
 *    A
 *    |
 *    |
 *  Pirate
 *    A
 *    |
 *    |
 *   jack
 *
 *  When evaluating jack->Pirate assignment, rule1 would not be normally taken into account, because its assignment
 *  (Pirate->rule1) has an order of 2. However, we want to collect it - but not as an item related to focus, but
 *  as an item related to evaluated assignment's target. Therefore besides isMatchingOrder we maintain isMatchingOrderForTarget
 *  that marks all segments (assignments/inducements) that contain policy rules relevant to the evaluated assignment's target.
 *
 *  The computation is done by maintaining two evaluationOrders:
 *   - plain evaluationOrder that is related to the focus object [starts from ZERO.advance(first assignment relation)]
 *   - special evaluationOrderForTarget that is related to the target of the assignment being evaluated [starts from ZERO]
 *
 *  Finally, how we distinguish between "this target" and "other targets" policy rules? Current approach
 *  is like this: count the number of times the evaluation order for target reached ZERO level. First encounter with
 *  that level is on "this target". And we assume that each subsequent marks a target that is among "others".
 *
 *  See PayloadEvaluation.appliesDirectly.
 */
