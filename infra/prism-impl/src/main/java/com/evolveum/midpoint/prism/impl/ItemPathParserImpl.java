/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.ItemPathParser;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathParserTemp;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ItemPathParserImpl implements ItemPathParser {

    @NotNull private final PrismContextImpl prismContext;

    ItemPathParserImpl(@NotNull PrismContextImpl prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public ItemPathType asItemPathType(String value) {
        return new ItemPathType(asItemPath(value));
    }

    @Override
    public UniformItemPath asItemPath(String value) {
        return ItemPathParserTemp.parseFromString(value);
    }
}
