/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Objects;

import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedActivityPolicyTriggerType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

public class EvaluatedActivityPolicyRuleTrigger<AC extends AbstractPolicyConstraintType> implements DebugDumpable {

    @NotNull
    private final AC constraint;

    private final LocalizableMessage message;

    private final LocalizableMessage shortMessage;

    public EvaluatedActivityPolicyRuleTrigger(@NotNull AC constraint, LocalizableMessage message, LocalizableMessage shortMessage) {
        this.constraint = constraint;
        this.message = message;
        this.shortMessage = shortMessage;
    }

    @NotNull
    public AC getConstraint() {
        return constraint;
    }

    public LocalizableMessage getMessage() {
        return message;
    }

    public LocalizableMessage getShortMessage() {
        return shortMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {return false;}
        EvaluatedActivityPolicyRuleTrigger<?> that = (EvaluatedActivityPolicyRuleTrigger<?>) o;
        return Objects.equals(constraint, that.constraint)
                && Objects.equals(message, that.message)
                && Objects.equals(shortMessage, that.shortMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraint, message, shortMessage);
    }

    @Override
    public String toString() {
        return "EvaluatedActivityPolicyRuleTrigger{" +
                "constraint=" + constraint +
                ", message=" + message +
                ", shortMessage=" + shortMessage +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        debugDumpCommon(sb, indent + 1);
        debugDumpSpecific(sb, indent + 1);
        return sb.toString();
    }

    private void debugDumpCommon(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelToStringLn(sb, "constraintName", constraint.getName(), indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "constraint", constraint, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "message", message, indent);
    }

    protected void debugDumpSpecific(StringBuilder sb, int indent) {
    }

    public EvaluatedActivityPolicyTriggerType toPolicyTriggerType() {
        EvaluatedActivityPolicyTriggerType state = new EvaluatedActivityPolicyTriggerType();
        state.setConstraintName(constraint.getName());
        state.setMessage(LocalizationUtil.createLocalizableMessageType(getMessage()));

        return state;
    }
}
