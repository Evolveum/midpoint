package com.evolveum.axiom.api.schema;

import java.util.Collection;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomSchemaContext {

    Collection<AxiomItemDefinition> roots();

    Optional<AxiomItemDefinition> getRoot(AxiomIdentifier type);

    Optional<AxiomTypeDefinition> getType(AxiomIdentifier type);

    Collection<AxiomTypeDefinition> types();

    //AxiomValueFactory<?, ?> factoryFor(AxiomTypeDefinition type);
}
