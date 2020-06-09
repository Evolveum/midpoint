package com.evolveum.axiom.api.stream;

import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomItemBuilder;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueBuilder;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.google.common.base.Preconditions;

public class AxiomItemTarget extends AxiomBuilderStreamTarget implements Supplier<AxiomItem<?>>, AxiomItemStream.TargetWithResolver {

    private final AxiomSchemaContext context;
    private final AxiomNameResolver resolver;
    private AxiomTypeDefinition infraType = AxiomBuiltIn.Type.AXIOM_VALUE;
    private Item<?> result;

    public AxiomItemTarget(AxiomSchemaContext context) {
        this(context, AxiomNameResolver.nullResolver());
    }

    public AxiomItemTarget(AxiomSchemaContext context, AxiomNameResolver rootResolver) {
        offer(new Root());
        this.context = context;
        this.resolver = Preconditions.checkNotNull(rootResolver, "rootResolver");
    }

    @Override
    public AxiomItem<?> get() {
        return result.get();
    }

    private final class Root implements ValueBuilder {

        @Override
        public AxiomName name() {
            return AxiomName.axiom("AbstractRoot");
        }

        @Override
        public AxiomNameResolver itemResolver() {
            return axiomAsConditionalDefault().or(resolver);
        }

        @Override
        public AxiomNameResolver valueResolver() {
            return resolver;
        }

        @Override
        public Optional<AxiomItemDefinition> childItemDef(AxiomName statement) {
            return context.getRoot(statement);
        }

        @Override
        public Optional<AxiomItemDefinition> infraItemDef(AxiomName item) {
            return infraType.itemDefinition(item);
        }

        @Override
        public ItemBuilder startItem(AxiomName name, SourceLocation loc) {
            result = new Item<>(childItemDef(name).get());
            return result;
        }

        @Override
        public void endValue(SourceLocation loc) {

        }

        @Override
        public ItemBuilder startInfra(AxiomName name, SourceLocation loc) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private class Item<V> implements ItemBuilder, Supplier<AxiomItem<V>> {

        protected final AxiomItemBuilder<V> builder;

        public Item(AxiomItemDefinition definition) {
            this.builder = new AxiomItemBuilder<>(definition);
        }

        @Override
        public AxiomName name() {
            return builder.definition().name();
        }

        @Override
        public AxiomNameResolver itemResolver() {
            return resolver;
        }

        @Override
        public AxiomNameResolver valueResolver() {
            return resolver;
        }


        protected Value<V> onlyValue() {
            return (Value<V>) builder.onlyValue();
        }

        @Override
        public ValueBuilder startValue(Object value, SourceLocation loc) {
            Value<V> newValue = new Value<>((V) value, builder.definition().typeDefinition());
            builder.addValue(newValue);
            return newValue;
        }

        @Override
        public void endNode(SourceLocation loc) {
            // Noop for now
        }

        @Override
        public AxiomItem<V> get() {
            return builder.get();
        }

    }

    private final class ValueItem<V> implements ItemBuilder, Supplier<AxiomItem<V>> {

        private Value<V> value;

        public ValueItem(Value<V> value) {
            this.value = value;
        }

        @Override
        public AxiomName name() {
            return AxiomValue.VALUE;
        }

        @Override
        public AxiomNameResolver itemResolver() {
            return resolver;
        }

        @Override
        public AxiomNameResolver valueResolver() {
            return resolver;
        }

        @Override
        public ValueBuilder startValue(Object value, SourceLocation loc) {
            this.value.setValue((V) value);
            return this.value;
        }


        @Override
        public void endNode(SourceLocation loc) {
            // Noop for now
        }

        @Override
        public AxiomItem<V> get() {
            throw new UnsupportedOperationException("Should not be called");
        }
    }

    private final class TypeItem extends Item<AxiomName> {

        private Value<?> value;

        public TypeItem(AxiomItemDefinition definition) {
            super(definition);
        }

        public TypeItem(Value<?> value, AxiomItemDefinition definition) {
            super(definition);
            this.value = value;
        }

        @Override
        public void endNode(SourceLocation loc) {
            AxiomName typeName = (AxiomName) onlyValue().get().asComplex().get().item(AxiomTypeDefinition.NAME).get().onlyValue().value();
            Optional<AxiomTypeDefinition> typeDef = context.getType(typeName);
            AxiomSemanticException.check(typeDef.isPresent(), loc, "% type is not defined.", typeName);
            this.value.setType(typeDef.get(),loc);
            super.endNode(loc);
        }
    }


    private final class Value<V> implements ValueBuilder, Supplier<AxiomValue<V>> {

        private final AxiomValueBuilder<V> builder;
        private AxiomTypeDefinition type;

        public Value(V value, AxiomTypeDefinition type) {
            this.type = type;
            builder = AxiomValueBuilder.from(type);
            if(value != null) {
                setValue(value);
            }

        }

        public void setType(AxiomTypeDefinition type, SourceLocation start) {
            AxiomSemanticException.check(type.isSubtypeOf(this.type), start, "%s is not subtype of %s", type.name(), this.type.name());
            this.type = type;
            builder.setType(type);
        }

        void setValue(V value) {
            if(type.argument().isPresent()) {
                startItem(type.argument().get().name(), null).startValue(value, null);
            } else {
                builder.setValue(value);
            }

        }

        @Override
        public AxiomName name() {
            return builder.type().name();
        }

        @Override
        public AxiomNameResolver itemResolver() {
            return AxiomNameResolver.defaultNamespaceFromType(builder.type());
        }

        @Override
        public AxiomNameResolver valueResolver() {
            return resolver;
        }

        @Override
        public Optional<AxiomItemDefinition> childItemDef(AxiomName statement) {
            return builder.type().itemDefinition(statement);
        }

        @Override
        public Optional<AxiomItemDefinition> infraItemDef(AxiomName item) {
            return infraType.itemDefinition(item);
        }

        @Override
        public ItemBuilder startItem(AxiomName name, SourceLocation loc) {
            Object itemImpl = builder.get(name, (id) -> {
                return new Item(childItemDef(name).get());
            });
            return (Item) (itemImpl);
        }

        @Override
        public void endValue(SourceLocation loc) {
            // Noop for now
        }

        @Override
        public AxiomValue<V> get() {
            return builder.get();
        }

        @Override
        public ItemBuilder startInfra(AxiomName name, SourceLocation loc) {
            if(AxiomValue.VALUE.equals(name)) {
                return new ValueItem(this);
            } else if (AxiomValue.TYPE.equals(name)) {
                return new TypeItem(this, infraItemDef(name).get());
            }
            Supplier<? extends AxiomItem<?>> itemImpl = builder.getInfra(name, (id) -> {
                return new Item<>(infraItemDef(name).get());
            });
            return (Item) itemImpl;
        }

    }
}
