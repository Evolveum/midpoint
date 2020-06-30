/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.schema;

import java.util.Optional;

import com.evolveum.axiom.api.AxiomStructuredValue;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomInfraName;
import com.evolveum.axiom.api.AxiomItemName;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPath;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.api.AxiomPath.Component;
import com.evolveum.axiom.concepts.Navigable;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public interface AxiomItemDefinition extends AxiomNamedDefinition, Navigable<AxiomPath.Component<?>, AxiomItemDefinition> {

    AxiomName ROOT_SPACE = AxiomName.axiom("AxiomRootDefinition");
    AxiomName SPACE = AxiomName.axiom("AxiomItemDefinition");
    AxiomName NAME = AxiomName.axiom("name");
    AxiomName VALUE_SPACE = AxiomName.axiom("value");
    AxiomName DEFAULT = SPACE.localName("default");
    AxiomName CONSTANT = SPACE.localName("const");


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
                .add("type", def.typeDefinition())
                .toString();
    }

    static AxiomItemDefinition derived(AxiomName name , AxiomItemDefinition source) {
        return new DelegatedItemDefinition() {

            @Override
            protected AxiomItemDefinition delegate() {
                return source;
            }

            @Override
            public AxiomName name() {
                return name;
            }
        };
    }

    static AxiomValueIdentifier identifier(AxiomName name) {
        return AxiomValueIdentifier.from(ImmutableMap.of(NAME, name));
    }

    interface Inherited extends AxiomItemDefinition {

        AxiomItemDefinition original();

    }

    interface Extended extends AxiomItemDefinition {

        AxiomItemDefinition original();

    }

    default AxiomItemDefinition derived(AxiomName name) {
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

    Optional<AxiomIdentifierDefinition> identifierDefinition();

    Optional<AxiomName> substitutionOf();

    Optional<AxiomValue<?>> constantValue();

    Optional<AxiomValue<?>> defaultValue();

    default boolean isStructured() {
        return typeDefinition().isComplex();
    }

    @Override
    default Optional<? extends AxiomItemDefinition> resolve(Component<?> key) {
        if(key instanceof AxiomValueIdentifier) {
            return Optional.of(this);
        }
        if(key instanceof AxiomItemName) {
            return typeDefinition().itemDefinition((AxiomItemName) key);
        }
        return Optional.empty();
    }

}
