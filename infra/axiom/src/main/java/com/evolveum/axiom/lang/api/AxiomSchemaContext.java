package com.evolveum.axiom.lang.api;

import java.util.Collection;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomSchemaContext {

    Collection<AxiomItemDefinition> roots();

    Optional<AxiomTypeDefinition> getType(AxiomIdentifier type);

    Collection<AxiomTypeDefinition> types();
}
