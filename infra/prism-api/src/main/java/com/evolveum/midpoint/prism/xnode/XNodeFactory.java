/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    <T> PrimitiveXNode<T> primitiveAttribute(T value);

    <T> PrimitiveXNode<T> primitive(ValueParser<T> valueParser);

    <T> PrimitiveXNode<T> primitive(ValueParser<T> valueParser, QName typeName, boolean explicitTypeDeclaration);

    MapXNode map();

    MapXNode map(Map<QName, XNode> source);

    MapXNode map(QName key, XNode value);

    ListXNode list(XNode... nodes);
}
