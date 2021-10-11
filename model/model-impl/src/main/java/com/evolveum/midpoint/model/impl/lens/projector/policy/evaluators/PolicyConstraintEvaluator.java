/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBElement;

/**
 * Evaluates given policy constraint within specific context.
 */
public interface PolicyConstraintEvaluator<T extends AbstractPolicyConstraintType> {

    <AH extends AssignmentHolderType> EvaluatedPolicyRuleTrigger<?> evaluate(@NotNull JAXBElement<T> constraint,
            @NotNull PolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException;
}
