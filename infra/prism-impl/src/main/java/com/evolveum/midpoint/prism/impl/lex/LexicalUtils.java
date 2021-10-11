/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex;

import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class LexicalUtils {

    @NotNull
    public static RootXNodeImpl createRootXNode(XNodeImpl xnode, QName rootElementName) {
        if (xnode instanceof RootXNodeImpl) {
            return (RootXNodeImpl) xnode;
        } else {
            RootXNodeImpl xroot = new RootXNodeImpl(rootElementName);
            xroot.setSubnode(xnode);
            return xroot;
        }
    }
}
