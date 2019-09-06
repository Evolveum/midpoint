/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.helpers;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExpressionPipelineType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExpressionSequenceType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.FilterExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ForeachExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SearchExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SelectExpressionType;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */
public class ScriptingJaxbUtil {

    private static Map<Class<? extends ScriptingExpressionType>, QName> elements = new HashMap<>();

    private static ObjectFactory objectFactory = new ObjectFactory();

    static {
        elements.put(ExpressionPipelineType.class, objectFactory.createPipeline(null).getName());
        elements.put(ExpressionSequenceType.class, objectFactory.createSequence(null).getName());
        elements.put(ForeachExpressionType.class, objectFactory.createForeach(null).getName());
        elements.put(SelectExpressionType.class, objectFactory.createSelect(null).getName());
        elements.put(FilterExpressionType.class, objectFactory.createFilter(null).getName());
        elements.put(SearchExpressionType.class, objectFactory.createSearch(null).getName());
        elements.put(ActionExpressionType.class, objectFactory.createAction(null).getName());
    }

    /**
     * Ugly hack ... sometimes we have to convert "bare" ScriptingExpressionType instance to the JAXBElement version,
     * with the correct element name.
     */
    @SuppressWarnings({"raw", "unchecked"})
    public static JAXBElement<? extends ScriptingExpressionType> toJaxbElement(ScriptingExpressionType expressionType) {
        QName qname = elements.get(expressionType.getClass());
        if (qname == null) {
            throw new IllegalArgumentException("Unsupported expression type: " + expressionType.getClass());
        }
        return new JAXBElement(qname, expressionType.getClass(), expressionType);
    }

}
