/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.util.DebugDumpable;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 *
 */
public interface XNode extends DebugDumpable, Visitable, Cloneable, Serializable {

    boolean isEmpty();

    QName getTypeQName();

    RootXNode toRootXNode();

    boolean isExplicitTypeDeclaration();

    @NotNull
    XNode clone();

    Integer getMaxOccurs();
}
