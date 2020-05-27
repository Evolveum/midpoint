/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import com.evolveum.axiom.lang.api.AxiomNamedDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public interface AxiomItemDefinition extends AxiomNamedDefinition, AxiomValue<AxiomItemDefinition> {

    AxiomIdentifier ROOT_SPACE = AxiomIdentifier.axiom("AxiomRootDefinition");
    AxiomIdentifier SPACE = AxiomIdentifier.axiom("AxiomItemDefinition");
    AxiomIdentifier NAME = AxiomIdentifier.axiom("name");

    @Override
    default AxiomItemDefinition get() {
        return this;
    }

    AxiomTypeDefinition typeDefinition();

    boolean operational();

    default boolean inherited() {
        return true;
    }

    default boolean required() {
        return minOccurs() > 0;
    }

    AxiomTypeDefinition definingType();

    int minOccurs();
    int maxOccurs();

    static String toString(AxiomItemDefinition def) {
        return MoreObjects.toStringHelper(AxiomItemDefinition.class)
                .add("name", def.name())
                .add("type", def.type())
                .toString();
    }

    static AxiomItemDefinition derived(AxiomIdentifier name , AxiomItemDefinition source) {
        return new DelegatedItemDefinition() {

            @Override
            protected AxiomItemDefinition delegate() {
                return source;
            }

            @Override
            public AxiomIdentifier name() {
                return name;
            }
        };
    }

    static IdentifierSpaceKey identifier(AxiomIdentifier name) {
        return IdentifierSpaceKey.from(ImmutableMap.of(NAME, name));
    }

    interface Inherited extends AxiomItemDefinition {

        AxiomItemDefinition original();

    }

    interface Extended extends AxiomItemDefinition {

        AxiomItemDefinition original();

    }

    default AxiomItemDefinition derived(AxiomIdentifier name) {
        return derived(name, this);
    }

    default AxiomItemDefinition notInherited() {
        return new DelegatedItemDefinition() {

            @Override
            public boolean operational() {
                return false;
            }

            @Override
            public boolean inherited() {
                return false;
            }

            @Override
            protected AxiomItemDefinition delegate() {
                return AxiomItemDefinition.this;
            }
        };
    }
}
