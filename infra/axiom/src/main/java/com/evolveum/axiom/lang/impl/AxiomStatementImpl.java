package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomStatement;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

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

    public static class Builder<V> {

        private final AxiomIdentifier keyword;
        private V value;
        private List<AxiomStatement<?>> children = new ArrayList<>();
        private Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap = ArrayListMultimap.create();

        public Builder(AxiomIdentifier keyword) {
            this.keyword = keyword;
        }

        public Builder<V> setValue(V value) {
            this.value = value;
            return this;
        }

        Builder<V> add(AxiomStatement<?> statement) {
            children.add(statement);
            keywordMap.put(statement.keyword(), statement);
            return this;
        }

        AxiomStatement<V> build() {
            return new AxiomStatementImpl<>(keyword, value, children, keywordMap);
        }

        @Override
        public String toString() {
            return "Builder(" + keyword + ")";
        }
    }



}
