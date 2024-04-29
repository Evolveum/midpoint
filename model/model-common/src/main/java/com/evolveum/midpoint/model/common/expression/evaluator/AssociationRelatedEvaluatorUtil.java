/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator;

import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;

import org.jetbrains.annotations.NotNull;

class AssociationRelatedEvaluatorUtil {

    static @NotNull ResourceAssociationDefinition getAssociationDefinition(ExpressionEvaluationContext context)
            throws ExpressionEvaluationException {
        var associationDefinitionTypedValue = context.getVariables().get(ExpressionConstants.VAR_ASSOCIATION_DEFINITION);
        var associationDefinition = associationDefinitionTypedValue != null ?
                (ResourceAssociationDefinition) associationDefinitionTypedValue.getValue() : null;
        if (associationDefinition == null) {
            throw new ExpressionEvaluationException(
                    ("No association definition variable in %s; the expression may be used in a wrong place. "
                            + "It is only supposed to create an association.").formatted(
                                    context.getContextDescription()));
        }
        return associationDefinition;
    }
}
