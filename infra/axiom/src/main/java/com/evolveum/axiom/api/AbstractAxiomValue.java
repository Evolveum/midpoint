/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public abstract class AbstractAxiomValue<V> implements AxiomValue<V> {

    private final AxiomTypeDefinition type;
    private final Map<AxiomName, AxiomItem<?>> infraItems;

    public AbstractAxiomValue(AxiomTypeDefinition type, Map<AxiomName, AxiomItem<?>> infraItems) {
        this.type = type;
        this.infraItems = infraItems;
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> infraItems() {
        return infraItems;
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return Optional.of(type);
    }

}
