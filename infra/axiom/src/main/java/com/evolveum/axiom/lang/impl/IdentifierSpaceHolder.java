package com.evolveum.axiom.lang.impl;

import java.util.Map;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;


interface IdentifierSpaceHolder {

    void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key, ValueContext<?> context);

    public ValueContext<?> lookup(AxiomIdentifier space, IdentifierSpaceKey key);

    Map<IdentifierSpaceKey, ValueContext<?>> space(AxiomIdentifier space);
}
