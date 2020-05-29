package com.evolveum.axiom.lang.impl;

import java.util.Map;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;


interface IdentifierSpaceHolder {

    void register(AxiomName space, Scope scope, IdentifierSpaceKey key, ValueContext<?> context);

    public ValueContext<?> lookup(AxiomName space, IdentifierSpaceKey key);

    Map<IdentifierSpaceKey, ValueContext<?>> space(AxiomName space);
}
