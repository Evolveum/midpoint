/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import javax.xml.namespace.QName;

/**
 *  Temporary interface to allow modifying XNode representation. Hopefully it will be removed soon. DO NOT USE.
 */
public interface XNodeMutator {

	<T> void setPrimitiveXNodeValue(PrimitiveXNode<T> node, T value, QName typeName);

	void putToMapXNode(MapXNode map, QName key, XNode value);

	void addToListXNode(ListXNode list, XNode... nodes);

	void setXNodeType(XNode node, QName explicitTypeName, boolean explicitTypeDeclaration);

}
