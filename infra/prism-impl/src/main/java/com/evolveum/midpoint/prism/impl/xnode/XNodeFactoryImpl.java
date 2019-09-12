/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.xnode;

import com.evolveum.midpoint.prism.xnode.*;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Temporary, experimental piece of code. Clients should not try to produce XNode objects themselves.
 */
public class XNodeFactoryImpl implements XNodeFactory {

	@Override
	public RootXNode root(QName rootElementName, XNode subnode) {
		return new RootXNodeImpl(rootElementName, subnode);
	}

	@Override
	public PrimitiveXNode<?> primitive() {
		return new PrimitiveXNodeImpl<>();
	}

	@Override
	public <T> PrimitiveXNode<T> primitive(T value, QName typeName) {
		PrimitiveXNodeImpl<T> rv = new PrimitiveXNodeImpl<>();
		rv.setValue(value, typeName);
		return rv;
	}

	@Override
	public <T> PrimitiveXNode<T> primitive(T value) {
		return new PrimitiveXNodeImpl<>(value);
	}

	@Override
	public <T> PrimitiveXNode<T> primitiveAttribute(T value) {
		PrimitiveXNodeImpl<T> rv = new PrimitiveXNodeImpl<>(value);
		rv.setAttribute(true);
		return rv;
	}

	@Override
	public <T> PrimitiveXNode<T> primitive(ValueParser<T> valueParser) {
		PrimitiveXNodeImpl<T> rv = new PrimitiveXNodeImpl<>();
		rv.setValueParser(valueParser);
		return rv;
	}

	@Override
	public <T> PrimitiveXNode<T> primitive(ValueParser<T> valueParser, QName typeName, boolean explicitTypeDeclaration) {
		PrimitiveXNodeImpl<T> rv = new PrimitiveXNodeImpl<>();
		rv.setValueParser(valueParser);
		rv.setTypeQName(typeName);
		rv.setExplicitTypeDeclaration(explicitTypeDeclaration);
		return rv;
	}

	@Override
	public MapXNode map() {
		return new MapXNodeImpl();
	}

	@Override
	public MapXNode map(Map<QName, XNode> source) {
		MapXNodeImpl map = new MapXNodeImpl();
		source.forEach((k, v) -> map.put(k, (XNodeImpl) v));
		return map;
	}

	@Override
	public MapXNode map(QName key, XNode value) {
		MapXNodeImpl map = new MapXNodeImpl();
		map.put(key, (XNodeImpl) value);
		return map;
	}

	@Override
	public ListXNode list(XNode... nodes) {
		ListXNodeImpl list = new ListXNodeImpl();
		for (XNode node : nodes) {
			list.add((XNodeImpl) node);
		}
		return list;
	}
}
