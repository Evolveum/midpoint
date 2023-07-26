/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 Resolves references in intelligent way: taking filters (and embedded expressions) into account.
 *
 * Different from ObjectResolver:
 * 1) more focused (resolving references only, no additional methods)
 * 2) advanced functionality (filters with expressions)
*/
@Experimental
public interface ReferenceResolver {

    enum Source {
        REPOSITORY, MODEL
    }

    @FunctionalInterface
    interface FilterExpressionEvaluator extends Serializable {
        ObjectFilter evaluate(ObjectFilter rawFilter, OperationResult result) throws SchemaException,
                ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException;
    }

    List<PrismObject<? extends ObjectType>> resolve(@NotNull ObjectReferenceType reference,
            Collection<SelectorOptions<GetOperationOptions>> options, @NotNull Source source,
            FilterExpressionEvaluator filterExpressionEvaluator, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;
}
