package com.evolveum.axiom.lang.impl;

import java.util.Map;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;

interface IdentifierSpaceHolder {

    void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key, StatementContextImpl<?> context);

    public StatementContextImpl<?> lookup(AxiomIdentifier space, IdentifierSpaceKey key);

    Map<IdentifierSpaceKey, StatementContextImpl<?>> space(AxiomIdentifier space);
}
