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
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;

public class AxiomItemTarget extends AxiomBuilderStreamTarget implements Supplier<AxiomItem<?>>, AxiomItemStream.TargetWithResolver {

    private final AxiomSchemaContext context;
    private final AxiomIdentifierResolver resolver;
    private AxiomTypeDefinition infraType = AxiomBuiltIn.Type.AXIOM_VALUE;
    private Item<?> result;

    public AxiomItemTarget(AxiomSchemaContext context, AxiomIdentifierResolver resolver) {
        offer(new Root());
        this.context = context;
        this.resolver = resolver;
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
        public AxiomIdentifierResolver itemResolver() {
            return resolver;
        }

        @Override
        public AxiomIdentifierResolver valueResolver() {
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

    private final class Item<V> implements ItemBuilder, Supplier<AxiomItem<V>> {

        private AxiomItemBuilder<V> builder;

        public Item(AxiomItemDefinition definition) {
            this.builder = new AxiomItemBuilder<>(definition);
        }

        @Override
        public AxiomName name() {
            return builder.definition().name();
        }

        @Override
        public AxiomIdentifierResolver itemResolver() {
            return resolver;
        }

        @Override
        public AxiomIdentifierResolver valueResolver() {
            return resolver;
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
        public AxiomIdentifierResolver itemResolver() {
            return resolver;
        }

        @Override
        public AxiomIdentifierResolver valueResolver() {
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

    private final class Value<V> implements ValueBuilder, Supplier<AxiomValue<V>> {

        private final AxiomValueBuilder<V, ?> builder;
        private AxiomTypeDefinition type;

        public Value(V value, AxiomTypeDefinition type) {
            this.type = type;
            builder = AxiomValueBuilder.from(type);
            if(value != null) {
                setValue(value);
            }

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
        public AxiomIdentifierResolver itemResolver() {
            return AxiomIdentifierResolver.defaultNamespaceFromType(builder.type());
        }

        @Override
        public AxiomIdentifierResolver valueResolver() {
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
            }
            Supplier<? extends AxiomItem<?>> itemImpl = builder.getInfra(name, (id) -> {
                return new Item<>(infraItemDef(name).get());
            });
            return (Item) itemImpl;
        }

    }
}
