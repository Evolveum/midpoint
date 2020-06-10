/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Expression evaluator that is provided to lower-level components in UCF layer.
 */
@Experimental
public interface UcfExpressionEvaluator {

    /**
     * Evaluates given expression.
     */
    <O> List<O> evaluate(ExpressionType expressionBean, VariablesMap variables, QName outputPropertyName,
            String contextDescription, Task task, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException;
}
