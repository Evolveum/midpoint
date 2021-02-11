/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import javax.xml.namespace.QName;

/**
 *
 */
public interface RootXNode extends XNode {
    QName getRootElementName();

    XNode getSubnode();

    MapXNode toMapXNode();

    @Override
    RootXNode copy();
}
