package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.concepts.Optionals;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomStatement;
import com.evolveum.axiom.lang.spi.AxiomStatementBuilder;
import com.evolveum.axiom.lang.spi.AxiomStreamTreeBuilder;
import com.evolveum.axiom.lang.spi.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomStatementImpl.Factory;
import com.evolveum.axiom.reactor.Depedency;

public abstract class StatementContextImpl<V> implements StatementContext<V>, AxiomStreamTreeBuilder.NodeBuilder, IdentifierSpaceHolder {

    private final AxiomItemDefinition definition;

    private final IdentifierSpaceHolder localSpace;

    private SourceLocation startLocation;
    private SourceLocation endLocation;
    private SourceLocation valueLocation;
    private Depedency<AxiomStatement<V>> result;


    StatementContextImpl(AxiomItemDefinition definition, SourceLocation loc, Scope scope, Scope... rest) {
        this.definition = definition;
        this.startLocation = loc;
        this.localSpace = new IdentifierSpaceHolderImpl(scope, rest);
    }


    protected void initResult() {
        AxiomStatementBuilder<V> builder = new AxiomStatementBuilder<>(definition.name(), typeFactory(definition.type()));
        this.result = new StatementContextResult<>(definition, builder);
    }

    protected <T> Factory<T, ? extends AxiomStatement<T>> typeFactory(AxiomTypeDefinition type) {
        return parent().typeFactory(type);
    }


    public void setValue(Object value) {
        mutableResult().setValue((V) value);
    }

    private StatementContextResult<V> mutableResult() {
        if(result instanceof StatementContextResult) {
            return (StatementContextResult) result;
        }
        return null;
    }

    boolean isChildAllowed(AxiomIdentifier child) {
        return definition.type().item(child).isPresent();
    }


    @SuppressWarnings("rawtypes")
    @Override
    public StatementContextImpl startChildNode(AxiomIdentifier identifier, SourceLocation loc) {
        StatementContextImpl<V> childCtx = new StatementContextImpl.Child<V>(this, childDef(identifier).get(), loc);
        mutableResult().add(childCtx);
        return childCtx;
    }


    public IdentifierSpaceHolder localSpace() {
        return localSpace;
    }

    @Override
    public String toString() {
        return "Context(" + definition.name() + ")";
    }

    @Override
    public Optional<AxiomItemDefinition> childDef(AxiomIdentifier child) {
        return definition.type().item(child);
    }

    @Override
    public AxiomIdentifier identifier() {
        return definition.name();
    }

    @Override
    public V requireValue() throws AxiomSemanticException {
        return AxiomSemanticException.checkNotNull(mutableResult().value(), definition, "must have argument specified.");
    }

    @Override
    public AxiomItemDefinition definition() {
        return definition;
    }

    @Override
    public <V> StatementContext<V> createEffectiveChild(AxiomIdentifier axiomIdentifier, V value) {
        StatementContextImpl<V> child = startChildNode(axiomIdentifier, null);
        child.setValue(value, null);
        return child;
    }


    public void registerRule(StatementRuleContextImpl<V> rule) {
        mutableResult().addRule(rule);
        //addOutstanding(rule);
    }

    protected void addOutstanding(StatementRuleContextImpl<?> rule) {
        parent().addOutstanding(rule);
    }

    public SourceLocation startLocation() {
        return startLocation;
    }

    public <V> Optional<StatementContextImpl<V>> firstChild(AxiomItemDefinition child) {
        return Optionals.first(children(child.name()));
    }

    private <V> Collection<StatementContextImpl<V>> children(AxiomIdentifier identifier) {
        return (Collection) mutableResult().get(identifier);
    }

    public StatementRuleContextImpl<V> addRule(StatementRule<V> statementRule) throws AxiomSemanticException {
        StatementRuleContextImpl<V> action = new StatementRuleContextImpl<V>(this,statementRule);
        statementRule.apply(action);
        return action;
    }

    @Override
    public Optional<V> optionalValue() {
        return Optional.ofNullable(mutableResult().value());
    }

    @Override
    public void replace(Depedency<AxiomStatement<?>> supplier) {
        this.result = (Depedency) supplier;
    }

    @Override
    abstract public StatementContextImpl<?> parent();

    @Override
    public void setValue(Object value, SourceLocation loc) {
        setValue(value);
        this.valueLocation = loc;
    }

    public Depedency<AxiomStatement<V>> asRequirement() {
        if (result instanceof StatementContextResult) {
            return Depedency.deffered(result);
        }
        return result;
    }

    public Supplier<? extends AxiomStatement<?>> asLazy() {
        return Lazy.from(() -> result.get());
    }

    public boolean isSatisfied() {
        return result.isSatisfied();
    }


    @Override
    public void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key) {
        register(space, scope, key, this);
    }

    @Override
    public void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key, StatementContextImpl<?> context) {
        switch (scope) {
            case GLOBAL:
                parentNs().register(space, scope, key, context);
                break;
            case LOCAL:
                localSpace.register(space, scope, key, context);
                break;
        default:
            throw new IllegalStateException("Unsupported scope");
        }
    }

    abstract IdentifierSpaceHolder parentNs();

    @Override
    public StatementContextImpl<?> lookup(AxiomIdentifier space, IdentifierSpaceKey key) {
        StatementContextImpl<?> maybe = localSpace.lookup(space, key);
        if(maybe != null) {
            return maybe;
        }
        return parentNs().lookup(space, key);

    }

    @Override
    public Map<IdentifierSpaceKey, StatementContextImpl<?>> space(AxiomIdentifier space) {
        return localSpace.space(space);
    }

    private void registerLocalParent(AxiomIdentifier space, IdentifierSpaceKey key) {
        // TODO Auto-generated method stub

    }

    @Override
    public V requireValue(Class<V> type) {
        if(result instanceof StatementContextResult) {
            return type.cast(((StatementContextResult) result).value());
        }
        return null;
    }

    @Override
    public void endNode(SourceLocation loc) {
        this.endLocation = loc;
        root().reactor.endStatement(this, loc);
    }

    public Depedency<AxiomStatement<?>> requireChild(AxiomItemDefinition required) {
        return Depedency.retriableDelegate(() -> {
            if(mutableResult() != null) {
                return (Depedency) firstChild(required).map(StatementContextImpl::asRequirement).orElse(null);
            }
            Optional<AxiomStatement<?>> maybe = result.get().first(required);

            return Depedency.from(maybe);
        });
    }

    protected IdentifierSpaceHolder exports() {
        throw new IllegalStateException("Statement does not provide exports");
    }

    static class Child<V> extends StatementContextImpl<V> {

        private StatementContextImpl<?> parent;

        public Child(StatementContextImpl<?> parent, AxiomItemDefinition definition, SourceLocation loc) {
            super(definition, loc, Scope.LOCAL);
            this.parent = parent;
            initResult();
        }

        @Override
        public StatementContextImpl<?> parent() {
            return parent;
        }

        @Override
        IdentifierSpaceHolder parentNs() {
            return parent;
        }

        @Override
        public void exportIdentifierSpace(IdentifierSpaceKey namespace) {
            throw new UnsupportedOperationException("Child can not export identifier space");
        }

        @Override
        public void importIdentifierSpace(NamespaceContext namespaceContext) {
            throw new UnsupportedOperationException("Child can not import identifier space");
        }

    }

    static class Root<V> extends StatementContextImpl<V> {

        private static final AxiomIdentifier IMPORTED_MODEL = AxiomIdentifier.axiom("ImportedModel");
        private final ModelReactorContext reactor;
        private Set<IdentifierSpaceHolder> imports;

        public Root(ModelReactorContext reactor, AxiomItemDefinition definition,
                SourceLocation loc) {
            super(definition, loc, Scope.GLOBAL, Scope.LOCAL);
            this.reactor = reactor;
            this.imports = new HashSet<>();
            initResult();
        }

        @Override
        protected IdentifierSpaceHolder parentNs() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void addOutstanding(StatementRuleContextImpl<?> rule) {
            reactor.addOutstanding(rule);
        }

        @Override
        public void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key,
                StatementContextImpl<?> context) {
            if (Scope.GLOBAL.equals(scope)) {
                    reactor.space().register(space, scope, key, context);
            }
            localSpace().register(space, scope, key, context);
        }

        @Override
        public StatementContextImpl<?> lookup(AxiomIdentifier space, IdentifierSpaceKey key) {
            StatementContextImpl<?> maybe = localSpace().lookup(space, key);
            if(maybe != null) {
                return maybe;
            }
            for(IdentifierSpaceHolder model : imports) {
                maybe = model.lookup(space, key);
                if(maybe != null) {
                    return maybe;
                }
            }
            return null;
        }

        @Override
        protected <T> Factory<T, ? extends AxiomStatement<T>> typeFactory(AxiomTypeDefinition type) {
            return reactor.typeFactory(type);
        }

        @Override
        public StatementContextImpl<?> parent() {
            return this;
        }

        @Override
        protected IdentifierSpaceHolder exports() {
            return localSpace();
        }

        @Override
        public void exportIdentifierSpace(IdentifierSpaceKey namespace) {
            reactor.exportIdentifierSpace(namespace, localSpace());
        }

        @Override
        public void importIdentifierSpace(NamespaceContext namespaceContext) {
            if(namespaceContext instanceof IdentifierSpaceHolder) {
                imports.add((IdentifierSpaceHolder) namespaceContext);
            }
        }

        @Override
        protected Root<?> root() {
            return this;
        }

        public Depedency<NamespaceContext> requireNamespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId) {
            return Depedency.retriableDelegate(() -> {
                return reactor.namespace(name, namespaceId);
            });
        }
    }

    protected Root<?> root() {
        return parent().root();
    }

}
