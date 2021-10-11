/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.util.AssignmentPathUtil;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class AssignmentPathImpl implements AssignmentPath {

    @NotNull private final List<AssignmentPathSegmentImpl> segments = new ArrayList<>();
    @NotNull private final PrismContext prismContext;

    public AssignmentPathImpl(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

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

    public void removeLast(AssignmentPathSegmentImpl segment) {
        AssignmentPathSegmentImpl last = last();
        if (last == null) {
            throw new IllegalStateException("Attempt to remove segment from empty path: " + this + "; segment=" + segment);
        } else if (!last.equals(segment)) {
            throw new IllegalStateException("Attempt to remove wrong segment from the end of path: " + this + "; segment=" + segment);
        } else {
            segments.remove(segments.size() - 1);
        }
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
    public int size() { return segments.size(); }

//    @Override
//    public EvaluationOrder getEvaluationOrder() {
//        if (isEmpty()) {
//            return EvaluationOrderImpl.ZERO;
//        } else {
//            return last().getEvaluationOrder();
//        }
//    }

    @Override
    public AssignmentPathSegmentImpl last() {
        return beforeLast(0);
    }

    @Override
    public AssignmentPathSegmentImpl beforeLast(int n) {
        if (size() <= n) {
            return null;
        } else {
            return segments.get(segments.size()-1-n);
        }
    }

    @Override
    public int countTargetOccurrences(ObjectType target) {
        if (target == null) {
            return 0;
        }
        int count = 0;
        for (AssignmentPathSegment segment: segments) {
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
                .filter(seg -> seg.isMatchingOrder() && seg.getTarget() != null)
                .map(seg -> seg.getTarget())
                .collect(Collectors.toList());
    }

    @Override
    public ObjectType getProtoRole() {
        ObjectType protoRole = null;
        for (AssignmentPathSegmentImpl segment: segments) {
            if (segment.isMatchingOrder() && segment.getTarget() != null) {
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
        AssignmentPathImpl clone = new AssignmentPathImpl(prismContext);
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
                for (AssignmentPathSegmentImpl segment: segments) {
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
        for (AssignmentPathSegmentImpl segment: segments) {
            if (previousTarget == null) {
                sb.append(segment.getSource()).append(" ");
            }
//            sb.append("(");
//            segment.getEvaluationOrder().shortDump(sb);
//            sb.append("): ");
            ObjectType target = segment.getTarget();
            QName relation = segment.getRelation();
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
                if (target != null) {
                    sb.append(target);
                }
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
        segments.forEach(seg -> rv.getSegment().add(seg.toAssignmentPathSegmentType(includeAssignmentsContent)));
        return rv;
    }

    @NotNull
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public ExtensionType collectExtensions(int startAt) throws SchemaException {
        return AssignmentPathUtil.collectExtensions(this, startAt, prismContext);
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
}
