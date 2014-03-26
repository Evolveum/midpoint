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

package com.evolveum.midpoint.model.scripting.helpers;

import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ConstantExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionPipelineType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionSequenceType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.FilterExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ForeachExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.SearchExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.SelectExpressionType;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class JaxbHelper {

    private Map<Class<? extends ExpressionType>, QName> elements = new HashMap<>();

    private ObjectFactory objectFactory = new ObjectFactory();

    public JaxbHelper() {
        elements.put(ExpressionPipelineType.class, objectFactory.createPipeline(null).getName());
        elements.put(ExpressionSequenceType.class, objectFactory.createSequence(null).getName());
        elements.put(ForeachExpressionType.class, objectFactory.createForeach(null).getName());
        elements.put(SelectExpressionType.class, objectFactory.createSelect(null).getName());
        elements.put(FilterExpressionType.class, objectFactory.createFilter(null).getName());
        elements.put(SearchExpressionType.class, objectFactory.createSearch(null).getName());
        elements.put(ActionExpressionType.class, objectFactory.createAction(null).getName());
        elements.put(ConstantExpressionType.class, objectFactory.createConstant(null).getName());
    }

    /**
     * The scripting expression language is constructed in such a way that it's possible to derive
     * element QName from the expression type. This method does just that.
     *
     * @param expression
     * @param <T>
     * @return
     */
    public <T extends ExpressionType> JAXBElement<T> toJaxbElement(T expression) {
        QName name = elements.get(expression.getClass());
        if (name == null) {
            throw new IllegalStateException("Couldn't find element QName for " + expression.getClass());
        } else {
            return new JAXBElement<T>(name, (Class) expression.getClass(), expression);
        }
    }
}
