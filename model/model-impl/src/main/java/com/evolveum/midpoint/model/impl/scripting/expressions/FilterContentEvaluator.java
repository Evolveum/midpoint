/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.expressions;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.FilterContentExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Component
public class FilterContentEvaluator extends BaseExpressionEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(FilterContentEvaluator.class);

    public PipelineData evaluate(
            FilterContentExpressionType expression, PipelineData input, ExecutionContext context)
            throws SchemaException {
        List<ItemPath> keep = convert(expression.getKeep());
        List<ItemPath> remove = convert(expression.getRemove());
        if (keep.isEmpty() && remove.isEmpty()) {
            return input; // nothing to do here
        }
        for (PipelineItem pipelineItem : input.getData()) {
            PrismValue value = pipelineItem.getValue();
            if (!(value instanceof PrismContainerValue<?> pcv)) {
                String message =
                        "In 'select' commands in keep/remove mode, we can act only on prism container values, not on " + value;
                if (context.isContinueOnAnyError()) {
                    LOGGER.error(message);
                } else {
                    throw new SchemaException(message);
                }
            } else {
                if (!keep.isEmpty()) {
                    pcv.keepPaths(keep);
                } else {
                    pcv.removePaths(remove);
                }
            }
        }
        return input;
    }

    private List<ItemPath> convert(List<ItemPathType> paths) {
        return paths.stream().map(p -> prismContext.toPath(p)).collect(Collectors.toList());
    }

}
