/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.List;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.collect.Multimap;

public class AnyAxiomStatement<V> extends AbstractAxiomStatement<V> {

    public static final AxiomStatementFactoryContext FACTORY = AxiomStatementFactoryContext.defaultFactory(AnyAxiomStatement::new);

    public AnyAxiomStatement(AxiomIdentifier keyword, V value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap) {
        super(keyword, value, children, keywordMap);
    }



}
