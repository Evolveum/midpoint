/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;

import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ErrorCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemStatePolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.springframework.stereotype.Component;

@Component
public class ItemStateConstraintEvaluator implements ActivityPolicyConstraintEvaluator<ItemStatePolicyConstraintType, EvaluatedItemStatePolicyTrigger> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemStateConstraintEvaluator.class);

    @Override
    public List<EvaluatedItemStatePolicyTrigger> evaluate(
            ItemStatePolicyConstraintType constraint,
            ActivityPolicyRuleEvaluationContext context,
            OperationResult result) {

        ItemProcessingResult processingResult = context.getProcessingResult();

        if (processingResult == null) {
            LOGGER.debug("No processing result available for evaluation of item state policy constraint '{}', skipping evaluation.", constraint);
            return List.of();
        }

        OperationResultStatus status = processingResult.operationResult().getStatus();
        OperationResultStatusType statusType = status.createStatusType();

        if (constraint.getStatus().contains(statusType)) {
            LOGGER.debug("Item state policy constraint '{}' matched for status '{}'.", constraint, status);

            LocalizableMessage message = new SingleLocalizableMessage(
                    "ItemStateConstraintEvaluator.resultStatusMatched",
                    new Object[] { constraint.getName(), status },
                    "Item state result status matched for constraint '" + constraint.getName() + "' with '" + status + "'");
            return List.of(new EvaluatedItemStatePolicyTrigger(constraint, message, message));
        }

        Throwable throwable = processingResult.exception();
        if (throwable == null) {
            return List.of();
        }

        ErrorCategoryType errorCategory = ExceptionUtil.getErrorCategory(throwable);
        if (constraint.getErrorCategory().contains(errorCategory)) {
            LOGGER.debug("Item state policy constraint '{}' matched for error category '{}'.", constraint, errorCategory);

            LocalizableMessage message = new SingleLocalizableMessage(
                    "ItemStateConstraintEvaluator.errorCategoryMatched",
                    new Object[] { constraint.getName(), errorCategory },
                    "Item state result error category matched for constraint '" + constraint.getName() + "' with '" + errorCategory + "'");

            return List.of(new EvaluatedItemStatePolicyTrigger(constraint, message, message));
        }

        return List.of();
    }
}
