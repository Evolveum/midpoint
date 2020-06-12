package com.evolveum.axiom.api.schema;

import java.util.Collection;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;

public interface AxiomSchemaContext {

    Collection<AxiomItemDefinition> roots();

    Optional<AxiomItemDefinition> getRoot(AxiomName type);

    Optional<AxiomTypeDefinition> getType(AxiomName type);

    Collection<AxiomTypeDefinition> types();

    //AxiomValueFactory<?, ?> factoryFor(AxiomTypeDefinition type);
}
