/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.impl.AbstractAxiomStatement.Factory;

public interface AxiomStatementFactoryContext {

    AbstractAxiomStatement.Factory<?, ?> factoryFor(AxiomTypeDefinition identifier);

    static AxiomStatementFactoryContext defaultFactory(AbstractAxiomStatement.Factory factory) {
        return (identifier) -> factory;
    }

}
