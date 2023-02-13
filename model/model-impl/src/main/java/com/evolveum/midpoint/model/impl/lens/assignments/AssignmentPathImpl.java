/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.assignments;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.util.AssignmentPathUtil;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Path from focus object to a given assignment.
 * Contains also some (although not complete) information on evaluation of individual segments.
 *
 * @author semancik
 */
public class AssignmentPathImpl implements AssignmentPath {

    @NotNull private final List<AssignmentPathSegmentImpl> segments = new ArrayList<>();

    @NotNull
    @Override
    public List<AssignmentPathSegmentImpl> getSegments() {
        return segments;
    }

    @Override
    public AssignmentPathSegment getSegment(int index) {
        if (index >= 0) {
            return segments.get(index);
        } else {
            return segments.get(segments.size() + index);
        }
    }

    public void add(AssignmentPathSegmentImpl segment) {
        segments.add(segment);
    }

    void removeLast(AssignmentPathSegmentImpl segment) {
        AssignmentPathSegmentImpl last = last();
        if (last == null) {
            throw new IllegalStateException("Attempt to remove segment from empty path: " + this + "; segment=" + segment);
        } else {
            segments.remove(segments.size() - 1);
        }
    }

    private void replaceLast(AssignmentPathSegmentImpl newLastSegment) {
        segments.set(segments.size() - 1, newLastSegment);
    }

    @Override
    public AssignmentPathSegmentImpl first() {
        return segments.get(0);
    }

    @Override
    public boolean isEmpty() {
        return segments.isEmpty();
    }

    @Override
    public int size() {
        return segments.size();
    }

    @Override
    public AssignmentPathSegmentImpl last() {
        return beforeLast(0);
    }

    @Override
    public AssignmentPathSegmentImpl beforeLast(int n) {
        if (size() <= n) {
            return null;
        } else {
            return segments.get(segments.size() - 1 - n);
        }
    }

    @Override
    public int countTargetOccurrences(ObjectType target) {
        if (target == null) {
            return 0;
        }
        int count = 0;
        for (AssignmentPathSegment segment : segments) {
            ObjectType segmentTarget = segment.getTarget();
            if (segmentTarget != null) {
                if (segmentTarget.getOid() != null && target.getOid() != null && segmentTarget.getOid().equals(target.getOid())
                        || segmentTarget.getOid() == null && target.getOid() == null && segmentTarget.equals(target)) {
                    count++;
                }
            }
        }
        return count;
    }

    @NotNull
    @Override
    public List<ObjectType> getFirstOrderChain() {
        return segments.stream()
                .filter(seg -> seg.isMatchingOrder && seg.getTarget() != null)
                .map(seg -> seg.getTarget())
                .collect(Collectors.toList());
    }

    @Override
    public ObjectType getProtoRole() {
        ObjectType protoRole = null;
        for (AssignmentPathSegmentImpl segment : segments) {
            if (segment.isMatchingOrder && segment.getTarget() != null) {
                protoRole = segment.getTarget();
            }
        }
        return protoRole;
    }

    public boolean hasOnlyOrgs() {
        for (AssignmentPathSegmentImpl segment : segments) {
            if (segment.getTarget() == null) {
                return false;
            }
            if (!(segment.getTarget() instanceof OrgType)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public AssignmentPathImpl clone() {
        return cloneFirst(size());
    }

    @Override
    public AssignmentPathImpl cloneFirst(int n) {
        AssignmentPathImpl clone = new AssignmentPathImpl();
        clone.segments.addAll(this.segments.subList(0, n));
        return clone;
    }

    @Override
    public String toString() {
        return "AssignmentPath(" + segments + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, "AssignmentPath", indent);
        if (segments.isEmpty()) {
            sb.append(" (empty)");
        } else {
            sb.append(" (").append(segments.size()).append(")");
            if (DebugUtil.isDetailedDebugDump()) {
                sb.append("\n");
                DebugUtil.debugDump(sb, segments, indent + 1, false);
            } else {
                for (AssignmentPathSegmentImpl segment : segments) {
                    sb.append("\n");
                    DebugUtil.indentDebugDump(sb, indent + 1);
                    segment.shortDump(sb);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        ObjectType previousTarget = null;
        for (AssignmentPathSegmentImpl segment : segments) {
            if (previousTarget == null) {
                sb.append(segment.getSource()).append(" ");
            }
            ObjectType target = segment.getTarget();
            QName relation = segment.relation;
            if (target != null) {
                sb.append("--");
                if (segment.isAssignment()) {
                    sb.append("a");
                } else {
                    sb.append("i");
                }
                sb.append("[");
                if (relation != null) {
                    sb.append(relation.getLocalPart());
                }
                sb.append("]--> ");
                sb.append(target);
            } else {
                if (segment.isAssignment()) {
                    sb.append("a");
                } else {
                    sb.append("i");
                }
                sb.append("(no target)");
            }
            previousTarget = target;
            sb.append(" ");
        }
    }

    @Override
    public AssignmentPathType toAssignmentPathType(boolean includeAssignmentsContent) {
        AssignmentPathType rv = new AssignmentPathType();
        AtomicInteger segmentOrder = new AtomicInteger(1);
        segments.forEach(seg -> rv.getSegment().add(
                seg.toAssignmentPathSegmentType(includeAssignmentsContent)
                        .segmentOrder(segmentOrder.getAndIncrement())));
        return rv;
    }

    @NotNull
    public PrismContext getPrismContext() {
        return PrismService.get().prismContext();
    }

    @Override
    public ExtensionType collectExtensions(int startAt) throws SchemaException {
        return AssignmentPathUtil.collectExtensions(this, startAt, getPrismContext());
    }

    @Override
    public boolean matches(@NotNull List<OrderConstraintsType> orderConstraints) {
        if (isEmpty()) {
            throw new UnsupportedOperationException("Checking order constraints on empty assignment path is not currently supported.");
        } else {
            return last().matches(orderConstraints);
        }
    }

    @Override
    public boolean equivalent(AssignmentPath other) {
        if (size() != other.size()) {
            return false;
        }
        for (int i = 0; i < segments.size(); i++) {
            AssignmentPathSegment segment = segments.get(i);
            AssignmentPathSegment otherSegment = other.getSegments().get(i);
            if (!segment.equivalent(otherSegment)) {
                return false;
            }
        }
        return true;
    }

    boolean containsDelegation(boolean evaluateOld, RelationRegistry relationRegistry) {
        return segments.stream()
                .anyMatch(aps -> DeputyUtils.isDelegationAssignment(aps.getAssignment(evaluateOld), relationRegistry));
    }

    void replaceLastSegmentWithTargetedOne(@NotNull AssignmentHolderType target) {
        AssignmentPathSegmentImpl last = last();
        assert last != null;
        AssignmentPathSegmentImpl newLast = last.cloneWithTargetSet(target);
        newLast.freeze();
        replaceLast(newLast);
    }

    /**
     * Used as a source for thisObject variable. This variable is officially deprecated, but it is used as a legacy
     * role pointer in {@link com.evolveum.midpoint.model.common.expression.evaluator.AssociationFromLinkExpressionEvaluator}.
     *
     * This is the specification from common-3 documentation:
     *
     * "The legacy algorithm is guaranteed to work up to meta-role level.
     * For plain roles (order-one inducement) the role itself is selected.
     * For meta-roles (order-two inducement) the first (plain) role is selected.
     * At the meta-meta role level (order-three inducement) and above the
     * behavior is formally undefined and it may change in any future versions.
     * However, current behaviour roughly corresponds to assignment path index -2."
     *
     * Should be removed or adapted on appropriate occasion.
     */
    public ObjectType getConstructionThisObject() {
        AssignmentPathSegmentImpl constructionSource = beforeLast(1);
        if (constructionSource == null) {
            return null;
        } else if (constructionSource.getEvaluationOrder().getSummaryOrder() == 1) {
            return constructionSource.getTarget();
        } else {
            return constructionSource.getSource(); // first role (for order=2) a.k.a. index -2 (generally)
        }
    }

    public boolean appliesDirectly() {
        assert !isEmpty();
        // TODO what about deputy relation which does not increase summaryOrder?
        long zeroOrderCount = segments.stream()
                .filter(seg -> seg.getEvaluationOrderForTarget().getSummaryOrder() == 0)
                .count();
        return zeroOrderCount == 1;
    }
}
