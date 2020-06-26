/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 *  Note we cannot use "extends Map" here, because in that case we would have to declare XNodeImpl as map value parameter.
 */
public interface MapXNode extends XNode, MetadataAware {

    boolean containsKey(QName key);

    XNode get(QName key);

    boolean isEmpty();

    @NotNull
    MapXNode clone();

    int size();

    /**
     * @return Immutable set of keys.
     */
    Set<QName> keySet();

    RootXNode getEntryAsRoot(@NotNull QName key);

    Map.Entry<QName, ? extends XNode> getSingleSubEntry(String errorContext) throws SchemaException;

    RootXNode getSingleSubEntryAsRoot(String errorContext) throws SchemaException;

    /**
     * @return Shallow clone of the node in the form of a map.
     */
    Map<QName, ? extends XNode> toMap();
}
