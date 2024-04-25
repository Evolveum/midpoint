/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public interface NativeShadowAssociationDefinition
        extends NativeShadowItemDefinition {

    /** This is the association class name. */
    @Override
    @NotNull QName getTypeName();

    /** This is more understandable for clients. */
    @NotNull default QName getAssociationClassName() {
        return getTypeName();
    }
}
