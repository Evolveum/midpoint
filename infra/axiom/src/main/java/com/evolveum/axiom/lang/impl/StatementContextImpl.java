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
import com.evolveum.axiom.lang.impl.AbstractAxiomStatement.Factory;
import com.google.common.collect.Multimap;

public class StatementContextImpl<V> {
    public AxiomIdentifier keyword;
    public Factory<V, ? extends AxiomStatement<V>> factory;
    public V value;
    public List<AxiomStatement<?>> children;
    public Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap;

    public StatementContextImpl(List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap) {
        this.children = children;
        this.keywordMap = keywordMap;
    }
}