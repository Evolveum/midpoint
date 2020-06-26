/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import java.util.List;

public class SerializerDomTarget extends SerializerTarget<Element> {

    SerializerDomTarget(@NotNull PrismContextImpl prismContext) {
        super(prismContext);
    }

    @Override
    @NotNull
    public Element write(@NotNull RootXNodeImpl xroot, SerializationContext context) throws SchemaException {
        return prismContext.getLexicalProcessorRegistry().domProcessor().writeXRootToElement(xroot);
    }

    @NotNull
    @Override
    public Element write(@NotNull List<RootXNodeImpl> roots, @Nullable SerializationContext context)
            throws SchemaException {
        return prismContext.getLexicalProcessorRegistry().domProcessor().writeXRootListToElement(roots);
    }
}
