/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public interface NativeShadowReferenceAttributeDefinition
        extends NativeShadowAttributeDefinition {

    /** This is the reference class name. */
    @Override
    @NotNull QName getTypeName();

    /** This is more understandable for clients. */
    @NotNull default QName getReferenceTypeName() {
        return getTypeName();
    }
}
