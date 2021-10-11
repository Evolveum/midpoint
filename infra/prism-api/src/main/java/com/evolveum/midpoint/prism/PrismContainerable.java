/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

/**
 * @author semancik
 *
 */
public interface PrismContainerable<T extends Containerable> extends Itemable, ParentVisitable {

    @Override
    PrismContainerDefinition<T> getDefinition();

    Class<T> getCompileTimeClass();

    default ComplexTypeDefinition getComplexTypeDefinition() {
        PrismContainerDefinition def = getDefinition();
        return def != null ? def.getComplexTypeDefinition() : null;
    }
}
