package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomName;

public interface AxiomModel {

    AxiomName NAMESPACE = AxiomName.axiom("namespace");
    AxiomName IMPORTED_NAMESPACE = AxiomName.axiom("ImportedNamespace");
    String BUILTIN_TYPES = "https://schema.evolveum.com/ns/axiom/types";

    String name();
    String namespace();
}
