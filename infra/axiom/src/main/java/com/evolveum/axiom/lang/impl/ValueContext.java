/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.lang.impl.AxiomStatementRule.ActionBuilder;
import com.evolveum.axiom.lang.impl.AxiomStatementRule.Lookup;
import com.evolveum.axiom.api.stream.AxiomBuilderStreamTarget.ItemBuilder;
import com.evolveum.axiom.api.stream.AxiomBuilderStreamTarget.ValueBuilder;
import com.evolveum.axiom.reactor.Dependency;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueBuilder;
import com.evolveum.axiom.api.meta.Inheritance;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;

public class ValueContext<V> extends AbstractContext<ItemContext<V>> implements AxiomValueContext<V>, ValueBuilder, Dependency<AxiomValue<V>> {

    private Dependency<AxiomValue<V>> result;
    private final LookupImpl lookup = new LookupImpl();
    private final V originalValue;
    private final Collection<Dependency<?>> dependencies = new HashSet<>();
    private AxiomValue<V> lazyValue;
    public ReferenceDependency referenceDependency = new ReferenceDependency();
    public Reference reference = new Reference();

    public ValueContext(SourceLocation loc, IdentifierSpaceHolder space) {
        super(null, loc, space);
        result = new Result(null,null);
        originalValue = null;
    }

    public ValueContext(ItemContext<V> itemContext, V value, SourceLocation loc) {
        super(itemContext, loc, AxiomIdentifierDefinition.Scope.LOCAL);
        originalValue = value;
        result = new Result(parent().type(), value);
        lazyValue = new LazyValue<>(itemDefinition().typeDefinition(),() -> get());
    }

    @Override
    public AxiomName name() {
        return parent().name();
    }

    public LookupImpl getLookup() {
        return lookup;
    }

    @Override
    public Optional<AxiomItemDefinition> childItemDef(AxiomName statement) {
        return parent().type().itemDefinition(statement);
    }

    @Override
    public ItemContext<?> startItem(AxiomName identifier, SourceLocation loc) {
        return mutable().getOrCreateItem(Inheritance.adapt(parent().name(), identifier), loc);
    }

    @Override
    public void endValue(SourceLocation loc) {
        rootImpl().applyRuleDefinitions(this);
    }

    protected Result mutable() {
        Preconditions.checkState(result instanceof ValueContext.Result, "Result is not mutable.");
        return (Result) result;
    }

    @Override
    public boolean isSatisfied() {
        return result.isSatisfied();
    }

    @Override
    public AxiomValue<V> get() {
        if(isMutable()) {
            result = Dependency.immediate(result.get());
        }
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

    protected ItemContext<?> createItem(AxiomName id, SourceLocation loc) {
        AxiomItemDefinition childDef = childItemDef(id).get();
        if(childDef.identifierDefinition().isPresent()) {
            return new MapItemContext<>(this, id, childDef, loc);
        }
        return new ItemContext<>(this, id ,childDef, loc);
    }

    private class Result implements Dependency<AxiomValue<V>> {

        AxiomTypeDefinition type;
        AxiomValueBuilder<V> builder;
        private V value;

        public Result(AxiomTypeDefinition type, V value) {
            this.type = type;
            this.value = value;
            builder = AxiomValueBuilder.create(type, null);
        }

        ItemContext<?> getOrCreateItem(AxiomName identifier, SourceLocation loc) {
            return mutableItem(builder.get(identifier, id -> {
                ItemContext<?> item = createItem(id, loc);
                addDependency(item);
                return item;
            }));
        }

        <T> Dependency<AxiomItem<T>> getItem(AxiomName item) {
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
        public AxiomValue<V> get() {
            builder.setValue(value);
            builder.setFactory(rootImpl().factoryFor(type));
            return builder.get();
        }

        @Override
        public Exception errorMessage() {
            return null;
        }

        public boolean hasItem(AxiomName name) {
            Supplier<? extends AxiomItem<?>> maybeItem = builder.get(name);
            return maybeItem != null;
        }

    }

    void addDependency(Dependency<?> action) {
        dependencies.add(action);
    }

    @Override
    public void replace(AxiomValue<?> axiomItemValue) {
        this.result = Dependency.immediate((AxiomValue<V>) axiomItemValue);
    }

    @Override
    public <T> ItemContext<T> childItem(AxiomName name) {
        return (ItemContext<T>) mutable().getOrCreateItem(Inheritance.adapt(parent().name(), name), SourceLocation.runtime());
    }

    @Override
    public V currentValue() {
        if(result instanceof ValueContext.Result) {
            return ((ValueContext<V>.Result) result).value;
        }
        return get().value();
    }

    @Override
    public void mergeItem(AxiomItem<?> axiomItem) {
        ItemContext<?> item = startItem(axiomItem.name(), SourceLocation.runtime());
        item.merge(axiomItem.values());
        item.endNode(SourceLocation.runtime());
    }

    @Override
    public void register(AxiomName space, Scope scope, AxiomValueIdentifier key) {
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

    public <T> Dependency.Search<AxiomItem<T>> requireChild(AxiomName item) {
        return Dependency.retriableDelegate(() -> createItemDependency(item));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    <T> Dependency<AxiomItem<T>> createItemDependency(AxiomName item) {
        if(result instanceof ValueContext.Result) {
            return ((ValueContext.Result) result).getItem(item);
        }
        return Dependency.fromNullable((AxiomItem<T>) result.get().asComplex().get().item(item).orElse(null));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <V> AxiomValueReference<V> asReference() {
        return (AxiomValueReference) reference;
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
        public Dependency<NamespaceContext> namespace(AxiomName name, AxiomValueIdentifier namespaceId) {
            return rootImpl().requireNamespace(name, namespaceId);
        }

        Optional<AxiomItemDefinition> resolveChildDef(AxiomItemDefinition name) {
            Optional<AxiomItemDefinition> exactDef = childItemDef(name.name());
            if(exactDef.isPresent()) {
                Optional<AxiomItemDefinition> localDef = childItemDef(parent().name().localName(name.name().localName()));
                if(localDef.isPresent() && localDef.get() instanceof AxiomItemDefinition.Inherited) {
                    return localDef;
                }
            }
            return exactDef;
        }

        @Override
        public <T> Dependency<AxiomItem<T>> child(AxiomItemDefinition definition, Class<T> valueType) {
            return requireChild(Inheritance.adapt(parent().name(), definition));
        }

        @Override
        public <T> Dependency<AxiomItem<T>> child(AxiomName definition, Class<T> valueType) {
            return requireChild(Inheritance.adapt(parent().name(), definition));
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public <T> Dependency<AxiomValue<T>> onlyItemValue(AxiomItemDefinition definition, Class<T> valueType) {
            return (Dependency) Dependency.retriableDelegate(() -> {
                Dependency<AxiomItem<?>> item;
                if(result instanceof ValueContext.Result) {
                    item = ((ValueContext.Result) result).getItem(definition.name());
                } else {
                    item = result.flatMap(v -> Dependency.from(v.asComplex().get().item(definition)));
                }
                if(item instanceof ItemContext) {
                    return ((ItemContext) item).onlyValue0();
                } else if (item == null) {
                    return null;
                }
                return item.map(i -> i.onlyValue());
            });
        }

        @Override
        public Dependency<AxiomValueContext<?>> modify(AxiomName space, AxiomValueIdentifier key) {
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
        public Dependency<AxiomValueContext<?>> modify() {
            return Dependency.immediate(ValueContext.this);
        }

        @Override
        public Dependency.Search<AxiomValue<?>> global(AxiomName space,
                AxiomValueIdentifier key) {
            return Dependency.retriableDelegate(() -> {
                ValueContext<?> maybe = lookup(space, key);
                if(maybe != null) {
                    return (Dependency) maybe;
                }
                return null;
            });
        }

        @Override
        public Dependency.Search<AxiomValueReference<?>> reference(AxiomName space,
                AxiomValueIdentifier key) {
            return Dependency.retriableDelegate(() -> {
                ValueContext<?> maybe = lookup(space, key);
                if(maybe != null) {
                    return Dependency.immediate(maybe.reference);
                }
                return null;
            });
        }

        @Override
        public Dependency.Search<AxiomValue<?>> namespaceValue(AxiomName space,
                AxiomValueIdentifier key) {
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
            return map(v -> v.value());
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
            return AxiomSemanticException.create(startLocation(), message, arguments);
        }
    }

    @Override
    public AxiomNameResolver itemResolver() {
        return (prefix, localName) -> {
            if(Strings.isNullOrEmpty(prefix)) {
                AxiomName localNs = AxiomName.local(localName);
                Optional<AxiomItemDefinition> childDef = childItemDef(localNs);
                if(childDef.isPresent()) {
                    return Inheritance.adapt(parent().name(), childDef.get());
                }
                ItemContext<?> parent = parent();
                while(parent != null) {
                    AxiomName parentNs = AxiomName.from(parent.name().namespace(), localName);
                    if(childItemDef(parentNs).isPresent()) {
                        return parentNs;
                    }
                    parent = parent.parent().parent();
                }
            }
            return rootImpl().itemResolver().resolveIdentifier(prefix, localName);
        };
    }

    public AxiomValue<?> lazyValue() {
        return lazyValue;
    }

    @Override
    public AxiomNameResolver valueResolver() {
        return rootImpl().valueResolver();
    }

    final class Reference implements AxiomValueReference<V> {

        public ReferenceDependency asDependency() {
            return referenceDependency;
        }

    }

    final class ReferenceDependency implements Dependency<AxiomValue<V>> {

        @Override
        public boolean isSatisfied() {
            return ValueContext.this.isSatisfied();
        }

        @Override
        public AxiomValue<V> get() {
            return lazyValue;
        }

        @Override
        public Exception errorMessage() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    @Override
    public Optional<AxiomItemDefinition> infraItemDef(AxiomName item) {
        throw new UnsupportedOperationException("Infra Items not yet supported for schema");
    }

    @Override
    public ItemBuilder startInfra(AxiomName identifier, SourceLocation loc) {
       throw new UnsupportedOperationException("Infra Items not yet supported for schema");
    }

    @Override
    public void valueIdentifier(AxiomValueIdentifier key) {
        ItemContext<V> parent = parent();
        Preconditions.checkState(parent instanceof MapItemContext<?>, "Item must be indexed");
        ((MapItemContext<V>) parent).addIdentifier(key, this);
    }

    @Override
    public void mergeCompletedIfEmpty(Optional<AxiomItem<?>> item) {
        if(item.isPresent()) {
            AxiomName name = item.get().name();
            if(!mutable().hasItem(name)) {
                mergeItem(item.get());
            }
        }
    }

    @Override
    public AxiomNameResolver infraResolver() {
        // TODO Auto-generated method stub
        return null;
    }
}
