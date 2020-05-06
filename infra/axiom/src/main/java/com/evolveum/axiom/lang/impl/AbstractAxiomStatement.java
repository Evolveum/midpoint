/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public abstract class AbstractAxiomStatement<V> implements AxiomStatement<V> {

    private final AxiomIdentifier keyword;
    private final V value;
    private final List<AxiomStatement<?>> children;
    private final Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap;

    public AbstractAxiomStatement(AxiomIdentifier keyword, V value, List<AxiomStatement<?>> children,
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

    interface Factory<V, I extends AxiomStatement<V>> {

        I create(AxiomIdentifier keyword, V value, List<AxiomStatement<?>> children,
            Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap);

        default boolean isChildAllowed(AxiomIdentifier child) {
            return true;
        }

    }

    public static class Context<V> {


        private final AxiomItemDefinition def;
        private final Factory<V, ? extends AxiomStatement<V>> factory;
        private V value;
        private final List<AxiomStatement<?>> children = new ArrayList<>();
        private final Multimap<AxiomIdentifier, AxiomStatement<?>> keywordMap = HashMultimap.create();

        public Context(AxiomItemDefinition itemDefinition, Factory<V, ? extends AxiomStatement<V>> factory) {
            this.def = itemDefinition;
            this.factory = factory;
        }

        public Context<V> setValue(V value) {
            this.value = value;
            return this;
        }

        boolean isChildAllowed(AxiomIdentifier child) {
            return def.type().item(child).isPresent();
        }

        Context<V> add(AxiomStatement<?> statement) {
            children.add(statement);
            keywordMap.put(statement.keyword(), statement);
            return this;
        }

        AxiomStatement<V> build() {
            return factory.create(def.identifier(), value, children, keywordMap);
        }

        @Override
        public String toString() {
            return "Builder(" + def.identifier() + ")";
        }

        public Optional<AxiomItemDefinition> childDef(AxiomIdentifier child) {
            return def.type().item(child);
        }

        public AxiomIdentifier identifier() {
            return def.identifier();
        }

        public Optional<AxiomItemDefinition> argumentDef() {
            return def.type().argument();
        }
    }

    static Context<?> builder(AxiomItemDefinition axiomItemDefinition, Factory<?,?> factory) {
        return new Context<>(axiomItemDefinition, factory);
    }



}
