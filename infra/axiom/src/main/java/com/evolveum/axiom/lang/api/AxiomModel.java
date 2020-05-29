package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomName;

public interface AxiomModel {

    AxiomName NAMESPACE = AxiomName.axiom("namespace");
    AxiomName IMPORTED_NAMESPACE = AxiomName.axiom("ImportedNamespace");
    String BUILTIN_TYPES = "https://ns.evolveum.com/axiom/language";

    String name();
    String namespace();
}
