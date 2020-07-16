package com.evolveum.axiom.api.stream;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public interface StreamContext {

    AxiomTypeDefinition currentInfra();

    AxiomTypeDefinition valueType();

    AxiomItemDefinition currentItem();
}
