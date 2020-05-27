package com.evolveum.axiom.lang.api;

import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueBuilder;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.SourceLocation;

public class AxiomItemTarget extends AxiomBuilderStreamTarget implements Supplier<AxiomItem<?>>, AxiomItemStream.TargetWithResolver {

    private final AxiomSchemaContext context;
    private final AxiomIdentifierResolver resolver;
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
        public AxiomIdentifier name() {
            return AxiomIdentifier.axiom("AbstractRoot");
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
        public Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement) {
            return context.getRoot(statement);
        }

        @Override
        public ItemBuilder startItem(AxiomIdentifier identifier, SourceLocation loc) {
            result = new Item<>(childDef(identifier).get());
            return result;
        }

        @Override
        public void endValue(SourceLocation loc) {

        }

    }

    private final class Item<V> implements ItemBuilder, Supplier<AxiomItem<V>> {

        private AxiomItemBuilder<V> builder;

        public Item(AxiomItemDefinition definition) {
            this.builder = new AxiomItemBuilder<>(definition);
        }

        @Override
        public AxiomIdentifier name() {
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

    private final class Value<V> implements ValueBuilder, Supplier<AxiomValue<V>> {

        private final AxiomValueBuilder<V, ?> builder;

        public Value(V value, AxiomTypeDefinition type) {
            builder = AxiomValueBuilder.from(type);
            builder.setValue(value);
            if(value != null && type.argument().isPresent()) {
                AxiomItemDefinition argument = type.argument().get();
                startItem(argument.name(), null).startValue(value, null);
            } else {
                builder.setValue(value);
            }
        }

        @Override
        public AxiomIdentifier name() {
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
        public Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement) {
            return builder.type().itemDefinition(statement);
        }

        @Override
        public ItemBuilder startItem(AxiomIdentifier identifier, SourceLocation loc) {
            Object itemImpl = builder.get(identifier, (id) -> {
                return new Item(childDef(identifier).get());
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

    }
}
