package com.evolveum.axiom.lang.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.AxiomItemDefinition;
import com.evolveum.axiom.api.AxiomTypeDefinition;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.lang.antlr.AxiomModelStatementSource;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.impl.ItemStreamContextBuilder.ValueBuilder;
import com.evolveum.axiom.lang.spi.SourceLocation;
import com.evolveum.axiom.reactor.Dependency;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

class SourceContext extends ValueContext<Void> implements AxiomRootContext, ValueBuilder {

    private static final AxiomIdentifier ROOT = AxiomIdentifier.from("root", "root");

    private final ModelReactorContext context;
    private final AxiomModelStatementSource source;
    private final Map<AxiomIdentifier, ItemContext> items = new HashMap();
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
    public AxiomIdentifier name() {
        return ROOT;
    }

    @Override
    public ItemContext<?> startItem(AxiomIdentifier identifier, SourceLocation loc) {
        return items.computeIfAbsent(identifier, id -> createItem(id, loc));
    }

    @Override
    public Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement) {
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
    public void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key, ValueContext<?> context) {
        globalSpace.register(space, scope, key, context);
        this.context.register(space, scope, key, context);
    }

    @Override
    public ValueContext<?> lookup(AxiomIdentifier space, IdentifierSpaceKey key) {
        return globalSpace.lookup(space, key);
    }

    public <V> AxiomValueFactory<V, AxiomValue<V>> factoryFor(AxiomTypeDefinition type) {
        return context.typeFactory(type);
    }

    public void applyRuleDefinitions(ValueContext<?> valueContext) {
        context.applyRuleDefinitions(valueContext);
    }

    public Dependency<NamespaceContext> requireNamespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId) {
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
    public void exportIdentifierSpace(IdentifierSpaceKey namespaceId) {
        context.exportIdentifierSpace(namespaceId, globalSpace.getExport());
    }

    @Override
    public AxiomIdentifierResolver itemResolver() {
        return (prefix, localName) -> {
            if(Strings.isNullOrEmpty(prefix)) {
                AxiomIdentifier axiomNs = AxiomIdentifier.axiom(localName);
                if(childDef(axiomNs).isPresent()) {
                    return axiomNs;
                }
            }
            String namespace = imports.get(prefix);
            return AxiomIdentifier.from(namespace, localName);
        };
    }

    @Override
    public AxiomIdentifierResolver valueResolver() {
        return AxiomIdentifierResolver.BUILTIN_TYPES.or((prefix, localName) -> {
            String namespace = imports.get(prefix);
            return AxiomIdentifier.from(namespace, localName);
        });
    }
}
