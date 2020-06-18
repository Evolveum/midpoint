/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Collection;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;

public interface AxiomItemFactory<V> {

    AxiomItem<V> create(AxiomItemDefinition def, Collection<? extends AxiomValue<?>> axiomItem);

}
