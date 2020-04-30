package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;

class AxiomBuiltInProperty implements AxiomPropertyDefinition {

    private final AxiomIdentifier argument;
    private final AxiomTypeDefinition type;

    public AxiomBuiltInProperty(AxiomIdentifier argument, AxiomTypeDefinition type) {
        this.argument = argument;
        this.type = type;
    }

    @Override
    public AxiomIdentifier getIdentifier() {
        return argument;
    }

    @Override
    public AxiomTypeDefinition getType() {
        return type;
    }

    @Override
    public boolean required() {
        return true;
    }
}
