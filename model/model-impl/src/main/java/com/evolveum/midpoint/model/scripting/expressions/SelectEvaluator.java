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

package com.evolveum.midpoint.model.scripting.expressions;

import com.evolveum.midpoint.model.scripting.Data;
import com.evolveum.midpoint.model.scripting.ExecutionContext;
import com.evolveum.midpoint.model.scripting.ScriptExecutionException;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.SearchExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.SelectExpressionType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

/**
 * @author mederly
 */
@Component
public class SelectEvaluator extends BaseExpressionEvaluator {

    public Data evaluate(SelectExpressionType selectExpression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        if (input == null) {
            return null;
        }

        Data output = Data.createEmpty();

        Element pathElement = selectExpression.getPath();
        if (pathElement == null) {
            return input;           // no path specified => select returns original data
        }
        ItemPath path = new XPathHolder(pathElement).toItemPath();

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
