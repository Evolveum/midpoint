/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
