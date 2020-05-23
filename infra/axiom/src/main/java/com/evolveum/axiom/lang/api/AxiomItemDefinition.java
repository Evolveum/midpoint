/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public interface AxiomItemDefinition extends AxiomNamedDefinition, AxiomItemValue<AxiomItemDefinition> {

    AxiomIdentifier ROOT_SPACE = AxiomIdentifier.axiom("AxiomRootDefinition");
    AxiomIdentifier SPACE = AxiomIdentifier.axiom("AxiomItemDefinition");
    AxiomIdentifier NAME = AxiomIdentifier.axiom("name");

    @Override
    default AxiomItemDefinition get() {
        return this;
    }

    AxiomTypeDefinition typeDefinition();

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

    static IdentifierSpaceKey identifier(AxiomIdentifier name) {
        return IdentifierSpaceKey.from(ImmutableMap.of(NAME, name));
    }

}
