package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomPropertyDefinition  {

    AxiomIdentifier getIdentifier();
    AxiomTypeDefinition getType();
    boolean required();
}
