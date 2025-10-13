/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @author semancik
 */
@FunctionalInterface
public interface MappingPreExpression {

    void mappingPreExpression(ExpressionEvaluationContext context, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException;

}
