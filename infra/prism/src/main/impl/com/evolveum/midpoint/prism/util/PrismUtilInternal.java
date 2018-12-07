/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.marshaller.BeanMarshaller;
import com.evolveum.midpoint.prism.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.xnode.XNodeImpl;
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
		if (xroot == null) {
			return xmap;
		}
		xmap.merge(expressionWrapper.getElementName(), xroot.getSubnode());
		return xmap;
	}
}
