/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.marshaller.BeanMarshaller;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 *
 */
public class PrismUtilInternal {
    public static ExpressionWrapper parseExpression(XNodeImpl node, PrismContext prismContext) throws SchemaException {
        if (!(node instanceof MapXNodeImpl)) {
            return null;
        }
        if (((MapXNodeImpl)node).isEmpty()) {
            return null;
        }
        for (Map.Entry<QName, XNodeImpl> entry: ((MapXNodeImpl)node).entrySet()) {
            if (PrismConstants.EXPRESSION_LOCAL_PART.equals(entry.getKey().getLocalPart())) {
                return parseExpression(entry, prismContext);
            }
        }
        return null;
    }

    public static ExpressionWrapper parseExpression(Map.Entry<QName, XNodeImpl> expressionEntry, PrismContext prismContext) throws SchemaException {
        if (expressionEntry == null) {
            return null;
        }
        RootXNodeImpl expressionRoot = new RootXNodeImpl(expressionEntry);
        PrismPropertyValue expressionPropertyValue = prismContext.parserFor(expressionRoot).parseItemValue();
        ExpressionWrapper expressionWrapper = new ExpressionWrapper(expressionEntry.getKey(), expressionPropertyValue.getValue());
        return expressionWrapper;
    }

    @NotNull
    public static MapXNodeImpl serializeExpression(@NotNull ExpressionWrapper expressionWrapper, BeanMarshaller beanMarshaller) throws SchemaException {
        MapXNodeImpl xmap = new MapXNodeImpl();
        Object expressionObject = expressionWrapper.getExpression();
        if (expressionObject == null) {
            return xmap;
        }
        XNodeImpl expressionXnode = beanMarshaller.marshall(expressionObject);
        if (expressionXnode == null) {
            return xmap;
        }
        xmap.put(expressionWrapper.getElementName(), expressionXnode);
        return xmap;
    }

    // TODO: Unify the two serializeExpression() methods
    public static MapXNodeImpl serializeExpression(ExpressionWrapper expressionWrapper, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
        MapXNodeImpl xmap = new MapXNodeImpl();
        Object expressionObject = expressionWrapper.getExpression();
        if (expressionObject == null) {
            return xmap;
        }
        RootXNode xroot = xnodeSerializer.serializeAnyData(expressionObject, expressionWrapper.getElementName());
        if (xroot == null || xroot.getSubnode() == null) {
            return xmap;
        }
        xmap.merge(expressionWrapper.getElementName(), xroot.getSubnode());
        return xmap;
    }
}
