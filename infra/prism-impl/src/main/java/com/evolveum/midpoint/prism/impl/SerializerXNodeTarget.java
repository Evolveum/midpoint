/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SerializerXNodeTarget extends SerializerTarget<RootXNode> {

    public SerializerXNodeTarget(@NotNull PrismContextImpl prismContext) {
        super(prismContext);
    }

    @NotNull
    @Override
    public RootXNodeImpl write(@NotNull RootXNodeImpl xroot, SerializationContext context) throws SchemaException {
        return xroot;
    }

    @NotNull
    @Override
    public RootXNodeImpl write(@NotNull List<RootXNodeImpl> roots, SerializationContext context)
            throws SchemaException {
        throw new UnsupportedOperationException("Serialization of a collection of objects is not supported for XNode target.");
    }
}
