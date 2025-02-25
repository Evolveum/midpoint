/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

public class EvaluatedActivityPolicyRuleTrigger<AC extends AbstractPolicyConstraintType> {

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
}
