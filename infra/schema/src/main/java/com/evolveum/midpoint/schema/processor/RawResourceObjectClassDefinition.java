/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary data structure used to hold raw (fetched) object class definitions.
 *
 * Intentionally _not_ a prism {@link ComplexTypeDefinition} to keep things simple.
 */
public class RawResourceObjectClassDefinition {

    @NotNull private final QName name;

    @NotNull private final List<RawResourceAttributeDefinition<?>> attributeDefinitions = new ArrayList<>();

    private boolean defaultInAKind;

    public RawResourceObjectClassDefinition(@NotNull QName name) {
        this.name = name;
    }

    public @NotNull QName getName() {
        return name;
    }

    public boolean isDefaultInAKind() {
        return defaultInAKind;
    }

    public void setDefaultInAKind(boolean defaultInAKind) {
        this.defaultInAKind = defaultInAKind;
    }

    public @NotNull List<RawResourceAttributeDefinition<?>> getAttributeDefinitions() {
        return attributeDefinitions;
    }


}
