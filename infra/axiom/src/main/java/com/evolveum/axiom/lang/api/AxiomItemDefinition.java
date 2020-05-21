/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.base.MoreObjects;

public interface AxiomItemDefinition extends AxiomNamedDefinition {

    AxiomIdentifier ROOT_SPACE = AxiomIdentifier.axiom("AxiomRootDefinition");

    AxiomTypeDefinition type();

    default boolean required() {
        return minOccurs() > 0;
    }

    int minOccurs();
    int maxOccurs();

    static String toString(AxiomItemDefinition def) {
        return MoreObjects.toStringHelper(AxiomItemDefinition.class)
                .add("name", def.name())
                .add("type", def.type())
                .toString();
    }

}
