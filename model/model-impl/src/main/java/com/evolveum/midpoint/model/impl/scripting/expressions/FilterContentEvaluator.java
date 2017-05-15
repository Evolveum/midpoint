/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.expressions;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.FilterContentExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
@Component
public class FilterContentEvaluator extends BaseExpressionEvaluator {

    public PipelineData evaluate(FilterContentExpressionType expression, PipelineData input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        List<ItemPath> keep = convert(expression.getKeep());
        List<ItemPath> remove = convert(expression.getRemove());
        if (keep.isEmpty() && remove.isEmpty()) {
            return input;       // nothing to do here
        }
        for (PipelineItem pipelineItem : input.getData()) {
            PrismValue value = pipelineItem.getValue();
            if (!(value instanceof PrismContainerValue)) {
                throw new ScriptExecutionException(
                        "In 'select' commands in keep/remove mode, we can act only on prism container values, not on " + value);
            }
            PrismContainerValue<?> pcv = (PrismContainerValue) value;
            if (!keep.isEmpty()) {
                pcv.keepPaths(keep);
            } else {
                pcv.removePaths(remove);
            }
        }
        return input;
    }

    private List<ItemPath> convert(List<ItemPathType> paths) {
        return paths.stream().map(p -> p.getItemPath()).collect(Collectors.toList());
    }

}
