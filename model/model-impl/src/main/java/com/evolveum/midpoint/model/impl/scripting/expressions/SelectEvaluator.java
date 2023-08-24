/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.expressions;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SelectExpressionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TODO
 */
@Component
public class SelectEvaluator extends BaseExpressionEvaluator {

    public PipelineData evaluate(SelectExpressionType selectExpression, PipelineData input, ExecutionContext context, OperationResult result) {
        if (selectExpression.getPath() == null) {
            return input;
        }
        ItemPath path = selectExpression.getPath().getItemPath();
        PipelineData output = PipelineData.createEmpty();
        for (PipelineItem item : input.getData()) {
            Object o = item.getValue().find(path);
            if (o != null) {
                if (o instanceof Item) {
                    List<? extends PrismValue> values = ((Item<? extends PrismValue, ?>) o).getValues();
                    values.forEach((v) ->
                            output.addValue(v, item.getResult().clone(), item.getVariables()));        // clone to avoid aggregating subresults into unrelated results
                } else {
                    throw new UnsupportedOperationException(
                            "In 'select' commands, only property/container/reference selection is supported for now. Select on '"
                                    + path + "' returned this instead: " + o);
                }
            }
        }
        return output;
    }
}
