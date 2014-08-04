/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.expressions;

import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SelectExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class SelectEvaluator extends BaseExpressionEvaluator {

    public Data evaluate(SelectExpressionType selectExpression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        Data output = Data.createEmpty();

        ItemPathType itemPathType = selectExpression.getPath();
        if (itemPathType == null) {
            return input;           // no path specified => select returns original data
        }
        ItemPath path = itemPathType.getItemPath();

        for (Item item : input.getData()) {
            Object o = item.find(path);
            if (o != null) {
                if (o instanceof Item) {
                    output.addItem((Item) o);
                } else {
                    throw new ScriptExecutionException("In 'select' commands, only property/container/reference selection is supported for now. Select on '" + path + "' returned this instead: " + o);
                }
            }
        }
        return output;
    }

}
