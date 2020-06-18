/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.schema;

import java.util.Collection;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;

public interface AxiomSchemaContext {

    Collection<AxiomItemDefinition> roots();

    Optional<AxiomItemDefinition> getRoot(AxiomName type);

    Optional<AxiomTypeDefinition> getType(AxiomName type);

    Collection<AxiomTypeDefinition> types();

}
