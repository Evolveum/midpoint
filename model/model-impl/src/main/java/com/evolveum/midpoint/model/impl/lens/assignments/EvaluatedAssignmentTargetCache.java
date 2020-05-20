/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.prism.delta.DeltaTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdempotenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 *
 */
class EvaluatedAssignmentTargetCache implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedAssignmentTargetCache.class);

    // Triple. Target processed for addition is not necessarily reusable for deletion
    // This is indexed by OID and relation only
    private final DeltaTriple<Map<Key, AssignmentTargetEvaluationInformation>> processedKeys;

    // Triple. Target processed for addition is not necessarily reusable for deletion
    // This is indexed by OID, relation and order
    private final DeltaTriple<Map<OrderKey, AssignmentTargetEvaluationInformation>> processedOrderKeys;

    EvaluatedAssignmentTargetCache() {
        processedOrderKeys = new DeltaTriple<>(HashMap::new);
        processedKeys = new DeltaTriple<>(HashMap::new);
    }

    void reset() {
        processedOrderKeys.foreach(Map::clear);
        processedKeys.foreach(Map::clear);
    }

    void resetForNextAssignment() {
        processedOrderKeys.foreach(map -> removeAssignmentScoped(map, "conservative assignment target evaluation cache"));
        processedKeys.foreach(map -> removeAssignmentScoped(map, "aggressive assignment target evaluation cache"));
    }

    private void removeAssignmentScoped(Map<? extends Key, AssignmentTargetEvaluationInformation> map, String desc) {
        int before = map.size();
        map.entrySet().removeIf(e -> e.getValue().isAssignmentScoped());
        int after = map.size();
        if (after < before) {
            LOGGER.trace("Removed {} entries from {}", before - after, desc);
        }
    }

    AssignmentTargetEvaluationInformation recordProcessing(AssignmentPathSegmentImpl segment, PlusMinusZero mode) {
        ObjectType targetType = segment.getTarget();
        if (!(targetType instanceof AbstractRoleType)) {
            return null;
        }
        if (!isCacheable((AbstractRoleType)targetType)) {
            return null;
        }
        AssignmentTargetEvaluationInformation targetEvaluationInformation = new AssignmentTargetEvaluationInformation();

        processedOrderKeys.get(mode).put(new OrderKey(segment), targetEvaluationInformation);
        if (targetType.getOid() != null) {
            processedKeys.get(mode).put(new Key(segment), targetEvaluationInformation);
        }

        return targetEvaluationInformation;
    }

    private boolean isCacheable(AbstractRoleType targetType) {
        IdempotenceType idempotence = targetType.getIdempotence();
        return idempotence != null && idempotence != IdempotenceType.NONE;
    }

    boolean canSkip(AssignmentPathSegmentImpl segment, PlusMinusZero mode) {
        ObjectType target = segment.getTarget();
        if (!(target instanceof AbstractRoleType)) {
//            LOGGER.trace("Non-skippable target: {}", target);
            return false;
        }
        IdempotenceType idempotence = ((AbstractRoleType)target).getIdempotence();
        if (idempotence == null || idempotence == IdempotenceType.NONE) {
//            LOGGER.trace("Not idempotent target: {}", target);
            return false;
        }
        if (idempotence == IdempotenceType.CONSERVATIVE && !segment.isMatchingOrder) {
            // this is quite important (and perhaps not too frequent) message, so let's keep it here
            LOGGER.trace("Conservative idempotent and order is not matching: {}", target);
            return false;
        }
        if (idempotence == IdempotenceType.AGGRESSIVE) {
            // Aggressive idempotence implies that this is not a meta-role
            // Therefore we want to skip all evaluation except for order=1
            // We skip these evaluations even if we have NOT seen this role
            // before. If we do not skip this evaluation then the role will
            // be remembered as evaluation and it will not be re-evaluated
            // in non-meta context.
            if (!segment.getEvaluationOrder().isOrderOne()) {
                // this is quite important (and perhaps not too frequent) message, so let's keep it here
                LOGGER.trace("Aggressive idempotent and non-one order: {}: {}", segment.getEvaluationOrder(), target);
                return true;
            }
            return processedKeys.get(mode).containsKey(new Key(segment));
        } else {
            return processedOrderKeys.get(mode).containsKey(new OrderKey(segment));
        }
    }

    private class Key {
        private final String targetOid;
        private final QName relation;

        Key(AssignmentPathSegmentImpl segment) {
            super();
            this.targetOid = segment.getTarget().getOid();
            this.relation = segment.relation;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((relation == null) ? 0 : relation.hashCode());
            result = prime * result + ((targetOid == null) ? 0 : targetOid.hashCode());
            return result;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (relation == null) {
                if (other.relation != null) {
                    return false;
                }
            } else if (!relation.equals(other.relation)) {
                return false;
            }
            if (targetOid == null) {
                if (other.targetOid != null) {
                    return false;
                }
            } else if (!targetOid.equals(other.targetOid)) {
                return false;
            }
            return true;
        }

        private EvaluatedAssignmentTargetCache getOuterType() {
            return EvaluatedAssignmentTargetCache.this;
        }

        @Override
        public String toString() {
            return "Key(" + content() + ")";
        }


        protected String content() {
            return targetOid + "[" + relation + "]";
        }

    }

    private class OrderKey extends Key {
        private final EvaluationOrder evaluationOrder;

        private OrderKey(AssignmentPathSegmentImpl segment) {
            super(segment);
            this.evaluationOrder = segment.getEvaluationOrder();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((evaluationOrder == null) ? 0 : evaluationOrder.hashCode());
            return result;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            OrderKey other = (OrderKey) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (evaluationOrder == null) {
                if (other.evaluationOrder != null) {
                    return false;
                }
            } else if (!evaluationOrder.equals(other.evaluationOrder)) {
                return false;
            }
            return true;
        }

        private EvaluatedAssignmentTargetCache getOuterType() {
            return EvaluatedAssignmentTargetCache.this;
        }

        @Override
        public String toString() {
            return "OrderKey("+ content() + ": order=" + evaluationOrder + ")";
        }

    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilder(EvaluatedAssignmentTargetCache.class, indent);
        sb.append("\n");
        DebugUtil.debugDumpLabelLn(sb, "processedKeys", indent + 1);
        processedKeys.debugDumpNoTitle(sb, indent + 2);
        sb.append("\n");
        DebugUtil.debugDumpLabelLn(sb, "processedOrderKeys", indent + 1);
        processedOrderKeys.debugDumpNoTitle(sb, indent + 2);
        return sb.toString();
    }

}
