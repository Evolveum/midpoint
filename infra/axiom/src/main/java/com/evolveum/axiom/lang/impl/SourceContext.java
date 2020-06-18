/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.antlr.AxiomModelStatementSource;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.api.stream.AxiomBuilderStreamTarget.ValueBuilder;
import com.evolveum.axiom.reactor.Dependency;
import com.google.common.base.Preconditions;

class SourceContext extends ValueContext<Void> implements AxiomRootContext, ValueBuilder {

    private static final AxiomName ROOT = AxiomName.from("root", "root");

    private final ModelReactorContext context;
    private final AxiomModelStatementSource source;
    private final Map<AxiomName, ItemContext> items = new HashMap();
    private final CompositeIdentifierSpace globalSpace;
    private Map<String, String> imports;

    public SourceContext(ModelReactorContext context, AxiomModelStatementSource source, Map<String, String> imports, CompositeIdentifierSpace space) {
        super(SourceLocation.runtime(), space);
        this.context = context;
        this.source = source;
        this.globalSpace = space;
        this.imports = imports;
    }

    @Override
    public AxiomName name() {
        return ROOT;
    }

    @Override
    public ItemContext<?> startItem(AxiomName identifier, SourceLocation loc) {
        return items.computeIfAbsent(identifier, id -> createItem(id, loc));
    }

    @Override
    public Optional<AxiomItemDefinition> childItemDef(AxiomName statement) {
        return context.rootDefinition(statement);
    }

    @Override
    protected SourceContext rootImpl() {
        return this;
    }

    @Override
    public void endValue(SourceLocation loc) {
        // NOOP
    }

    @Override
    public void register(AxiomName space, Scope scope, AxiomValueIdentifier key, ValueContext<?> context) {
        globalSpace.register(space, scope, key, context);
        this.context.register(space, scope, key, context);
    }

    @Override
    public ValueContext<?> lookup(AxiomName space, AxiomValueIdentifier key) {
        return globalSpace.lookup(space, key);
    }

    public <V> AxiomValueFactory<V> factoryFor(AxiomTypeDefinition type) {
        return context.typeFactory(type);
    }

    public void applyRuleDefinitions(ValueContext<?> valueContext) {
        context.applyRuleDefinitions(valueContext);
    }

    public Dependency<NamespaceContext> requireNamespace(AxiomName name, AxiomValueIdentifier namespaceId) {
        return Dependency.retriableDelegate(() -> {
            return context.namespace(name, namespaceId);
        });
    }

    @Override
    public void importIdentifierSpace(NamespaceContext namespaceContext) {
        Preconditions.checkArgument(namespaceContext instanceof IdentifierSpaceHolder);
        globalSpace.add((IdentifierSpaceHolder) namespaceContext);
    }

    @Override
    public void exportIdentifierSpace(AxiomValueIdentifier namespaceId) {
        context.exportIdentifierSpace(namespaceId, globalSpace.getExport());
    }

    @Override
    public AxiomNameResolver itemResolver() {
        return axiomAsConditionalDefault().or((prefix, localName) -> {
            String namespace = imports.get(prefix);
            if(namespace != null) {
                return AxiomName.from(namespace, localName);
            }
            return null;
        });
    }

    @Override
    public AxiomNameResolver valueResolver() {
        return AxiomNameResolver.BUILTIN_TYPES.or((prefix, localName) -> {
            String namespace = imports.get(prefix);
            if(namespace != null) {
                return AxiomName.from(namespace, localName);
            }
            return null;
        });
    }
}
