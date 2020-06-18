/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Map;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public class SimpleValue<T> extends AbstractAxiomValue<T> implements AxiomSimpleValue<T> {

    private final T value;

    SimpleValue(AxiomTypeDefinition type, T value, Map<AxiomName, AxiomItem<?>> infraItems) {
        super(type, infraItems);
        this.value = value;
    }

    public static final <V> AxiomSimpleValue<V> create(AxiomTypeDefinition def, V value, Map<AxiomName, AxiomItem<?>> infraItems) {
        return new SimpleValue<V>(def, value, infraItems);
    }

    @Override
    public T value() {
        return value;
    }

}
