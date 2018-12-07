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
