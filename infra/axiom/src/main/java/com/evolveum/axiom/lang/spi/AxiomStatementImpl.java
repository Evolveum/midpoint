/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.spi;

import java.util.Collection;
import java.util.List;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMap.Builder;

public class AxiomStatementImpl<V> implements AxiomStatement<V> {

    private final AxiomIdentifier keyword;
    private final V value;
    private final List<AxiomStatement<?>> children;
    private final Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap;

    public AxiomStatementImpl(AxiomIdentifier keyword, V value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap) {
        super();
        this.keyword = keyword;
        this.value = value;
        this.children = ImmutableList.copyOf(children);
        this.keywordMap = ImmutableMultimap.copyOf(keywordMap);
    }

    @Override
    public Collection<AxiomStatement<?>> children(AxiomIdentifier type) {
        return keywordMap.get(type);
    }

    @Override
    public Collection<AxiomStatement<?>> children() {
        return children;
    }

    @Override
    public AxiomIdentifier keyword() {
        return keyword;
    }

    @Override
    public V value() {
        return value;
    }

    @Override
    public String toString() {
        return keyword + "{value=" + value + "}";
    }

    public interface Factory<V, I extends AxiomStatement<V>> {

        I create(AxiomIdentifier type, V value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap);

    }

    protected void putAll(Builder<AxiomIdentifier, AxiomItemDefinition> builder,
            Collection<AxiomItemDefinition> children) {
        for (AxiomItemDefinition definition : children) {
            builder.put(definition.name(), definition);
        }
    }

    public static <V, T extends AxiomStatement<V>> Factory<V, T> factory() {
        return (Factory) AxiomStatementImpl::new;
    }

}
