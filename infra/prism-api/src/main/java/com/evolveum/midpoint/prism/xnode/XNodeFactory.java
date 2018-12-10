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

package com.evolveum.midpoint.prism.xnode;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 *  Temporary, experimental API. Clients should not try to produce XNode objects themselves.
 */
public interface XNodeFactory {

	RootXNode root(QName rootElementName, XNode subnode);

	<T> PrimitiveXNode<T> primitive();

	<T> PrimitiveXNode<T> primitive(T value, QName typeName);

	<T> PrimitiveXNode<T> primitive(T value);

	<T> PrimitiveXNode<T> primitive(ValueParser<T> valueParser);

	<T> PrimitiveXNode<T> primitive(ValueParser<T> valueParser, QName typeName, boolean explicitTypeDeclaration);

	MapXNode map();

	MapXNode map(Map<QName, XNode> source);

	MapXNode map(QName key, XNode value);

	ListXNode list(XNode... nodes);
}
