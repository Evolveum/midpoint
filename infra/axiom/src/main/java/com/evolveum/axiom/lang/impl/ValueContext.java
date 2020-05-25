package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.evolveum.axiom.lang.api.AxiomItemValueBuilder;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.impl.AxiomStatementRule.ActionBuilder;
import com.evolveum.axiom.lang.impl.AxiomStatementRule.Lookup;
import com.evolveum.axiom.lang.spi.AxiomItemStreamTreeBuilder.ValueBuilder;
import com.evolveum.axiom.reactor.Dependency;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.SourceLocation;

public class ValueContext<V> extends AbstractContext<ItemContext<V>> implements AxiomValueContext<V>, ValueBuilder, Dependency<AxiomItemValue<V>> {

    private Dependency<AxiomItemValue<V>> result;
    private final LookupImpl lookup = new LookupImpl();
    private final V originalValue;
    private final Collection<Dependency<?>> dependencies = new HashSet<>();

    public ValueContext(SourceLocation loc, IdentifierSpaceHolder space) {
        super(null, loc, space);
        result = new Result(null,null);
        originalValue = null;
    }

    public ValueContext(ItemContext<V> itemContext, V value, SourceLocation loc) {
        super(itemContext, loc, AxiomIdentifierDefinition.Scope.LOCAL);
        originalValue = value;
        result = new Result(parent().type(), value);
    }

    @Override
    public AxiomIdentifier name() {
        return parent().name();
    }

    public LookupImpl getLookup() {
        return lookup;
    }

    @Override
    public Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement) {
        return parent().type().itemDefinition(statement);
    }

    @Override
    public ItemContext<?> startItem(AxiomIdentifier identifier, SourceLocation loc) {
        return mutable().getOrCreateItem(identifier, loc);
    }

    @Override
    public void endValue(SourceLocation loc) {
        rootImpl().applyRuleDefinitions(this);
    }

    protected Result mutable() {
        Preconditions.checkState(result instanceof ValueContext.Result);
        return (Result) result;
    }

    @Override
    public boolean isSatisfied() {
        return result.isSatisfied();
    }

    @Override
    public AxiomItemValue<V> get() {
        return result.get();
    }

    @Override
    public Exception errorMessage() {
        return null;
    }

    private ItemContext<?> mutableItem(Supplier<? extends AxiomItem<?>> supplier) {
        Preconditions.checkState(supplier instanceof ItemContext);
        return (ItemContext<?>) supplier;
    }

    public AxiomItemDefinition itemDefinition() {
        return parent().definition();
    }

    public ValueActionImpl<V> addAction(String name) {
        return new ValueActionImpl<>(this, name);
    }

    protected ItemContext<?> createItem(AxiomIdentifier id, SourceLocation loc) {
        return new ItemContext<>(this, id ,childDef(id).get(), loc);
    }

    private class Result implements Dependency<AxiomItemValue<V>> {

        AxiomTypeDefinition type;
        AxiomItemValueBuilder<V, AxiomItemValue<V>> builder;
        private V value;

        public Result(AxiomTypeDefinition type, V value) {
            this.type = type;
            this.value = value;
            builder = AxiomItemValueBuilder.create(type, null);
        }

        ItemContext<?> getOrCreateItem(AxiomIdentifier identifier, SourceLocation loc) {
            return mutableItem(builder.get(identifier, id -> {
                ItemContext<?> item = createItem(id, loc);
                addDependency(item);
                return item;
            }));
        }

        <T> Dependency<AxiomItem<T>> getItem(AxiomIdentifier item) {
            Supplier<? extends AxiomItem<?>> maybeItem = builder.get(item);
            if(maybeItem == null) {
                return null;
            }
            if(maybeItem instanceof Dependency<?>) {
                return (Dependency) maybeItem;
            }
            return Dependency.immediate((AxiomItem<T>) maybeItem.get());
        }



        @Override
        public boolean isSatisfied() {
            return Dependency.allSatisfied(dependencies);
        }

        @Override
        public AxiomItemValue<V> get() {
            builder.setValue(value);
            builder.setFactory(rootImpl().factoryFor(type));
            return builder.get();
        }

        @Override
        public Exception errorMessage() {
            return null;
        }

    }

    void addDependency(Dependency<?> action) {
        dependencies.add(action);
    }

    @Override
    public void replace(AxiomItemValue<?> axiomItemValue) {
        this.result = Dependency.immediate((AxiomItemValue<V>) axiomItemValue);
    }

    @Override
    public <T> AxiomItemContext<T> childItem(AxiomIdentifier name) {
        return (AxiomItemContext<T>) mutable().getOrCreateItem(name, SourceLocation.runtime());
    }

    @Override
    public V currentValue() {
        if(result instanceof ValueContext.Result) {
            return ((ValueContext<V>.Result) result).value;
        }
        return get().get();
    }

    @Override
    public void mergeItem(AxiomItem<?> axiomItem) {
        ItemContext<?> item = startItem(axiomItem.name(), SourceLocation.runtime());
        for(AxiomItemValue<?> value : axiomItem.values()) {
            ValueContext<?> valueCtx = item.startValue(value.get(),SourceLocation.runtime());
            valueCtx.replace(value);
            valueCtx.endValue(SourceLocation.runtime());
        }
        item.endNode(SourceLocation.runtime());
    }

    @Override
    public void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key) {
        register(space, scope, key, this);
    }



    @Override
    public ActionBuilder<?> newAction(String name) {
        return new ValueActionImpl(this, name);
    }

    @Override
    public AxiomRootContext root() {
        return parent().rootImpl();
    }

    public void dependsOnAction(ValueActionImpl<V> action) {
        addDependency(action);
    }

    public <T> Dependency<AxiomItem<T>> requireChild(AxiomIdentifier item) {
        return Dependency.retriableDelegate(() -> {
            if(result instanceof ValueContext.Result) {
                return ((ValueContext.Result) result).getItem(item);
            }
            return Dependency.from(result.get().item(item));

        });
    }

    @Override
    public void replaceValue(V object) {
        mutable().value = object;
    }

    public boolean isMutable() {
        return result instanceof ValueContext.Result;
    }

    @Override
    public String toString() {
        return new StringBuffer().append(parent().definition().name().localName())
                .append(" ")
                .append(originalValue != null ? originalValue : "")
                .toString();
    }

    private class LookupImpl implements Lookup<V> {

        @Override
        public AxiomItemDefinition itemDefinition() {
            return parent().definition();
        }

        @Override
        public Dependency<NamespaceContext> namespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId) {
            return rootImpl().requireNamespace(name, namespaceId);
        }

        @Override
        public <T> Dependency<AxiomItem<T>> child(AxiomItemDefinition name, Class<T> valueType) {
            return requireChild(name.name());
        }

        @Override
        public Dependency<AxiomValueContext<?>> modify(AxiomIdentifier space, IdentifierSpaceKey key) {
            return (Dependency.retriableDelegate(() -> {
                ValueContext<?> maybe = lookup(space, key);
                if(maybe != null) {
                    //maybe.addDependency(this);
                    return Dependency.immediate(maybe);
                }
                return null;
            }));
        }

        @Override
        public Dependency.Search<AxiomItemValue<?>> global(AxiomIdentifier space,
                IdentifierSpaceKey key) {
            return Dependency.retriableDelegate(() -> {
                ValueContext<?> maybe = lookup(space, key);
                if(maybe != null) {
                    return (Dependency) maybe;
                }
                return null;
            });
        }


        @Override
        public Dependency.Search<AxiomItemValue<?>> namespaceValue(AxiomIdentifier space,
                IdentifierSpaceKey key) {
            return Dependency.retriableDelegate(() -> {
                ValueContext<?> maybe = lookup(space, key);
                if(maybe != null) {
                    return (Dependency) maybe;
                }
                return null;
            });
        }

        @Override
        public Dependency<V> finalValue() {
            return map(v -> v.get());
        }

        @Override
        public V currentValue() {
            return ValueContext.this.currentValue();
        }

        @Override
        public V originalValue() {
            return originalValue;
        }

        @Override
        public boolean isMutable() {
            return ValueContext.this.isMutable();
        }

        @Override
        public Lookup<?> parentValue() {
            return parent().parent().getLookup();
        }

        @Override
        public AxiomSemanticException error(String message, Object... arguments) {
            return new AxiomSemanticException(startLocation() + " " + Strings.lenientFormat(message, arguments));
        }
    }

}
