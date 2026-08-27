/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.xml.bind.JAXBElement;

import java.util.List;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.*;

/**
 * Utilities to dump policy rules, constraints, actions; typically for diagnostic purposes.
 */
public class PolicyRuleDumpUtil {

    public static String toShortString(PolicyRuleType rule) {
        if (rule != null) {
            return toShortString(rule.getPolicyConstraints()) + "→" + toShortString(rule.getPolicyActions());
        } else {
            return null;
        }
    }

    public static String toShortString(JAXBElement<? extends AbstractPolicyConstraintType> constraint) {
        StringBuilder sb = new StringBuilder();
        toShortString(sb, constraint, " ");
        return sb.toString();
    }

    public static String toShortString(PolicyConstraintsType constraints) {
        return toShortString(constraints, AND.getSymbol());
    }

    public static String toShortString(PolicyConstraintsType constraints, String join) {
        if (constraints == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        // we ignore refs to be able to dump even unresolved policy rules
        for (JAXBElement<AbstractPolicyConstraintType> constraint : PolicyRuleTypeUtil.toConstraintsList(constraints, true)) {
            toShortString(sb, constraint, join);
        }
        for (PolicyConstraintReferenceType ref : constraints.getRef()) {
            if (!sb.isEmpty()) {
                sb.append(join);
            }
            sb.append("ref:").append(ref.getName());
        }
        return sb.toString();
    }

    private static void toShortString(StringBuilder sb, JAXBElement<? extends AbstractPolicyConstraintType> constraint, String join) {
        PolicyConstraintKind c = PolicyConstraintKind.findByItemName(constraint.getName());
        if (!sb.isEmpty()) {
            sb.append(join);
        }
        if (c == AND) {
            sb.append('(');
            sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), c.getSymbol()));
            sb.append(')');
        } else if (c == OR) {
            sb.append('(');
            sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), c.getSymbol()));
            sb.append(')');
        } else if (c == NOT) {
            sb.append(NOT.getSymbol());
            sb.append("(");
            sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), AND.getSymbol()));
            sb.append(')');
        } else if (c == TRANSITION) {
            TransitionPolicyConstraintType trans = (TransitionPolicyConstraintType) constraint.getValue();
            sb.append(c.getSymbol());
            sb.append(toTransSymbol(trans.isStateBefore()));
            sb.append(toTransSymbol(trans.isStateAfter()));
            sb.append('(');
            sb.append(toShortString(trans.getConstraints(), AND.getSymbol()));
            sb.append(')');
        } else {
            sb.append(c.getSymbol());
        }
    }

    private static String toTransSymbol(Boolean state) {
        if (state != null) {
            return state ? "1" : "0";
        } else {
            return "x";
        }
    }

    public static String toShortString(PolicyActionsType actions) {
        return toShortString(actions, null);
    }

    /**
     * @param enabledActions if null we don't consider action enabled/disabled state
     */
    public static String toShortString(PolicyActionsType actions, List<PolicyActionConfigItem<?>> enabledActions) {
        if (actions == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (actions.getEnforcement() != null) {
            sb.append("enforce");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, EnforcementPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (!actions.getApproval().isEmpty()) {
            sb.append(" approve");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, ApprovalPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getRemediation() != null) {
            sb.append(" remedy");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, RemediationPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getPrune() != null) {
            sb.append(" prune");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, PrunePolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getCertification() != null) {
            sb.append(" certify");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, CertificationPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (!actions.getNotification().isEmpty()) {
            sb.append(" notify");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, NotificationPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getRecord() != null) {
            sb.append(" record");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, RecordPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (!actions.getScriptExecution().isEmpty()) {
            sb.append(" execute");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, ScriptExecutionPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getSuspendTask() != null) {
            sb.append(" suspend");
            if (enabledActions != null && PolicyRuleTypeUtil.filterActions(enabledActions, SuspendTaskPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        return sb.toString().trim();
    }

    public static String toDiagShortcut(PolicyConstraintKind constraintKind) {
        return constraintKind != null ? constraintKind.getSymbol() : "null";
    }
}
