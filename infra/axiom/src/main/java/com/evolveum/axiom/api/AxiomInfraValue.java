/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Map;
import java.util.Optional;

public interface AxiomInfraValue {

    Map<AxiomName, AxiomItem<?>> infraItems();

    default Optional<AxiomItem<?>> infraItem(AxiomName name) {
        return Optional.ofNullable(infraItems().get(name));
    }

    interface Factory<V extends AxiomInfraValue> {
        V create(Map<AxiomName,AxiomItem<?>> infraItems);
    }
}
