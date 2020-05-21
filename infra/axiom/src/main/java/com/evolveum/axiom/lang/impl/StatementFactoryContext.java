/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.spi.AxiomStatementImpl;

interface StatementFactoryContext {

    AxiomStatementImpl.Factory<?, ?> factoryFor(AxiomTypeDefinition identifier);

    static StatementFactoryContext defaultFactory(AxiomStatementImpl.Factory<?,?> factory) {
        return (identifier) -> factory;
    }

}
