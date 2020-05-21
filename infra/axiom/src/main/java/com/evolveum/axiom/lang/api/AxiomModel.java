package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomModel {

    AxiomIdentifier NAMESPACE = AxiomIdentifier.axiom("namespace");
    AxiomIdentifier IMPORTED_NAMESPACE = AxiomIdentifier.axiom("ImportedNamespace");
    String BUILTIN_TYPES = "https://ns.evolveum.com/axiom/language";

    String name();
    String namespace();
}
