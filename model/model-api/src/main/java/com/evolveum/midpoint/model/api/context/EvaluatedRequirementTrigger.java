/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents triggered requirement constraint.
 */
public class EvaluatedRequirementTrigger extends EvaluatedExclusionRequirementTrigger {

    @NotNull private final ObjectReferenceType requiredTargetRef;

    // we keep thisTarget and thisPath here because in the future they might be useful
    public EvaluatedRequirementTrigger(
            @NotNull ExclusionPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage,
            @NotNull EvaluatedAssignment thisAssignment,
            @NotNull ObjectType thisTarget,
            @NotNull ObjectReferenceType requiredTargetRef,
            @NotNull AssignmentPath thisPath,
            boolean enforcementOverride) {
        super(PolicyConstraintKindType.REQUIREMENT, constraint, message, shortMessage, thisAssignment, thisTarget, thisPath, enforcementOverride);
        this.requiredTargetRef = requiredTargetRef;
    }

    public @NotNull ObjectReferenceType getRequiredTargetRef() {
        return requiredTargetRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluatedRequirementTrigger)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EvaluatedRequirementTrigger that = (EvaluatedRequirementTrigger) o;
        return Objects.equals(requiredTargetRef, that.requiredTargetRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), requiredTargetRef);
    }

    @Override
    protected void debugDumpSpecific(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelToStringLn(sb, "requiredTargetRef", requiredTargetRef, indent);
    }

    @Override
    public EvaluatedRequirementTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options, @Nullable EvaluatedAssignment newOwner) {
        EvaluatedRequirementTriggerType rv = new EvaluatedRequirementTriggerType();
        fillCommonContent(rv);
        if (options.isFullStorageStrategy()) {
            rv.setRequirementObjectRef(ObjectTypeUtil.createObjectRef(requiredTargetRef));
        }
        return rv;
    }

}
