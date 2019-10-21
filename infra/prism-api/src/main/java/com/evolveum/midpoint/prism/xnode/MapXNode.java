/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 *  Note we cannot use "extends Map" here, because in that case we would have to declare XNodeImpl as map value parameter.
 */
public interface MapXNode extends XNode, Serializable, DebugDumpable {

    boolean containsKey(Object key);
    boolean containsValue(Object value);
    XNode get(Object key);

    boolean isEmpty();

    @NotNull
    MapXNode clone();

    int size();

    Set<QName> keySet();

    RootXNode getEntryAsRoot(@NotNull QName key);

    Map.Entry<QName, ? extends XNode> getSingleSubEntry(String errorContext) throws SchemaException;

    RootXNode getSingleSubEntryAsRoot(String errorContext) throws SchemaException;

    // EXPERIMENTAL
    Map<QName, ? extends XNode> asMap();
}
