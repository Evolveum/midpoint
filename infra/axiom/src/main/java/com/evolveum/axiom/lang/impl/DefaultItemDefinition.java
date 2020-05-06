/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;

class DefaultItemDefinition implements AxiomItemDefinition {

    private AxiomIdentifier identifier;
    private AxiomTypeDefinition type;

    public DefaultItemDefinition(AxiomIdentifier identifier, AxiomTypeDefinition type) {
        super();
        this.identifier = identifier;
        this.type = type;
    }

    @Override
    public AxiomIdentifier identifier() {
        return identifier;
    }

    @Override
    public String documentation() {
        return null;
    }

    @Override
    public AxiomTypeDefinition type() {
        return type;
    }

    @Override
    public boolean required() {
        // TODO Auto-generated method stub
        return false;
    }


}
