/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomStructuredValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.reactor.Dependency;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;


public enum BasicStatementRule implements AxiomStatementRule<AxiomName> {
    /*
    REQUIRE_REQUIRED_ITEMS(all(),all()) {
        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomTypeDefinition typeDef = action.typeDefinition();
            for(AxiomItemDefinition required : typeDef.requiredItems()) {
                //action.requireChild(required).unsatisfiedMessage(() -> action.error("%s does not have required statement %s", action.itemDefinition().name() ,required.name()));
                action.apply((ctx) -> {});
            }
        }
    },*/
    /*
    SET_TYPE(all(),all()) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {

        }

    },*/

    APPLY_CONSTANTS(all(),all()) {

        @Override
        public boolean isApplicableTo(AxiomItemDefinition definition) {
            return Iterables.any(definition.typeDefinition().itemDefinitions().values(), i -> i.constantValue().isPresent());
        }

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Iterable<AxiomItemDefinition> constants = Iterables.filter(context.typeDefinition().itemDefinitions().values(), i -> i.constantValue().isPresent());
            action.apply(ctx -> {
                for(AxiomItemDefinition constant : constants) {
                    ctx.childItem(constant.name()).addValue(constant.constantValue().get().value());
                }
            });
        }
    },

    ITEM_LOCALNAME(items(Item.ITEM_DEFINITION), types(Type.ITEM_DEFINITION)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomName> itemName = action.require(context.child(Item.NAME, AxiomName.class).map(v -> v.onlyValue().value()));
            action.apply(item -> {
                item.valueIdentifier(AxiomValueIdentifier.of(Item.NAME.name(), itemName.get().defaultNamespace()));
            });
        }
    },

    MOVE_ARGUMENT_VALUE(all(),all()) {
        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            if(context.isMutable()) {
                Optional<AxiomItemDefinition> argument = context.typeDefinition().argument();
                if(argument.isPresent() && context.originalValue() != null) {
                   action.apply(ctx -> {
                       ctx.childItem(argument.get()).addValue(ctx.currentValue());
                       ctx.replaceValue(null);
                   });
                }
            }
        }
    },
    REGISTER_TYPE(all(), types(Type.TYPE_DEFINITION)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomName> typeName = action.require(context.child(Item.NAME, AxiomName.class)
                    .map(v -> v.onlyValue().value()))
                    .unsatisfied(() -> action.error("Type does not have name defined."));
            action.apply(ctx -> {
                ctx.register(AxiomTypeDefinition.SPACE, AxiomIdentifierDefinition.Scope.GLOBAL, AxiomTypeDefinition.identifier(typeName.get()));
            });
        }
    },
    REGISTER_ROOT(all(), types(Type.ROOT_DEFINITION)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomName> typeName = action.require(context.child(Item.NAME, AxiomName.class)
                    .map(v -> v.onlyValue().value()))
                    .unsatisfied(() -> action.error("Type does not have name defined."));
            action.apply(ctx -> {
                ctx.register(AxiomItemDefinition.ROOT_SPACE, AxiomIdentifierDefinition.Scope.GLOBAL, AxiomItemDefinition.identifier(typeName.get()));
            });
        }
    },
    ITEM_VALUE_IDENTIFIER(all(),all()){

        @Override
        public boolean isApplicableTo(AxiomItemDefinition definition) {
            if(definition.identifierDefinition().isPresent()) {
                return true;
            }
            return false;
        }

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            AxiomIdentifierDefinition idDef = context.itemDefinition().identifierDefinition().get();
            Map<AxiomName,Dependency<AxiomValue<Object>>> components = new HashMap<>();
            for(AxiomName key : idDef.components()) {
                components.put(key, action.require(context.child(key, Object.class))
                        .unsatisfied(()-> context.error("Item '%s' is required by identifier, but not defined.", key))
                        .map(v -> v.onlyValue()));
            }
            action.apply(ctx -> {
                AxiomValueIdentifier key = keyFrom(components);
                ctx.valueIdentifier(key);
            });
        }

    },
    EXPAND_TYPE_REFERENCE(all(), types(Type.TYPE_REFERENCE)) {
        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            if(!context.isMutable()) return;
            Dependency<AxiomValueReference<?>> typeDef = action.require(context.child(Item.NAME, AxiomName.class)
                .unsatisfied(() -> action.error("type name is missing."))
                .map(v -> v.onlyValue().value())
                .flatMap(name ->
                    context.reference(AxiomTypeDefinition.SPACE,AxiomTypeDefinition.identifier(name))
                        .notFound(() ->  action.error("type '%s' was not found.", name))
                        .unsatisfied(() -> action.error("Referenced type %s is not complete.", name))
                )
            );
            action.apply(ctx -> {
                ctx.childItem(Item.REF_TARGET).addOperationalValue((AxiomValueReference) typeDef.get());
            });
        }
    },
    ITEMS_FROM_SUPERTYPE(items(Item.SUPERTYPE_REFERENCE),types(Type.TYPE_REFERENCE)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomStructuredValue> typeDef =
                    action.require(
                            context.onlyItemValue(Item.REF_TARGET, AxiomTypeDefinition.class)
                            .map(v -> v.asComplex().get()));
            typeDef.unsatisfied(() -> action.error("Supertype %s, is not complete.", context.onlyItemValue(Item.NAME, AxiomName.class).get().value()));
            action.apply(superTypeValue -> {
                addFromType(typeDef.get(), superTypeValue.parentValue(), true);
            });
        }
    },
    ITEMS_FROM_MIXIN(items(Item.INCLUDE),types(Type.TYPE_REFERENCE)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomStructuredValue> typeDef =
                    action.require(
                            context.onlyItemValue(Item.REF_TARGET, AxiomTypeDefinition.class)
                            .map(v -> v.asComplex().get()));
            typeDef.unsatisfied(() -> action.error("Supertype is not complete."));
            action.apply(includeItem -> {
                addFromType(typeDef.get(), includeItem.parentValue(), true);
            });
        }
    },
    /*ITEM_FROM_SUBSTITUTION(items(Item.SUBSTITUTION_DEFINITION), types(Type.SUBSTITUTION_DEFINITION)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            // FIXME: Resolve original item
            Dependency<AxiomValueContext<?>> parent = action.require(context.parentValue().modify());
            action.apply(value -> {
                parent.get().childItem(Item.ITEM_DEFINITION).addOperationalValue(value);
            });
        }

    },*/
    SUBSTITUTION(all(),all()) {

        @Override
        public boolean isApplicableTo(AxiomItemDefinition definition) {
            return definition.substitutionOf().isPresent();
        }

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomValueContext<?>> parent = action.require(context.parentValue().modify());
            action.apply(value -> {
                parent.get().childItem(context.itemDefinition().substitutionOf().get()).addOperationalValue(value);
            });
        }

    },
    IMPORT_DEFAULTS(all(), types(Type.MODEL)) {
        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<NamespaceContext> types = action.require(context.namespace(Item.NAMESPACE.name(), namespaceId(AxiomName.TYPE_NAMESPACE)));
            Dependency<NamespaceContext> data = action.require(context.namespace(Item.NAMESPACE.name(), namespaceId(AxiomName.DATA_NAMESPACE)));

            types.unsatisfied(() -> action.error("Default types not found."));
            action.apply((ctx) -> {
                ctx.root().importIdentifierSpace(types.get());
                ctx.root().importIdentifierSpace(data.get());
            });
        }
    },
    EXPORT_GLOBALS_FROM_MODEL(all(), types(Type.MODEL)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<String> namespace = action.require(context.child(Item.NAMESPACE, String.class).map(v -> v.onlyValue().value()));
            action.apply(ctx -> {
                ctx.root().exportIdentifierSpace(namespaceId(namespace.get()));
            });
        }
    },
    IMPORT_MODEL(all(),types(Type.IMPORT_DEFINITION)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<NamespaceContext> namespace = action.require(context.child(Item.NAMESPACE, String.class)
                    .map(item -> item.onlyValue().value())
                    .flatMap(uri -> context.namespace(Item.NAMESPACE.name(), namespaceId(uri))
                    .unsatisfied(() -> action.error("Namespace %s not found.", uri))
                    ));
            action.apply((ctx) -> {
                ctx.root().importIdentifierSpace(namespace.get());
            });
        }
    },

    APPLY_AUGMENTATION(all(), types(Type.AUGMENTATION_DEFINITION)) {

        @Override
        public boolean isApplicableTo(AxiomItemDefinition definition) {
            return super.isApplicableTo(definition);
        }

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomValueContext<?>> targetRef = action.require(context.child(Item.TARGET, AxiomName.class)
                    .map(v -> v.onlyValue().asComplex().get().item(Item.NAME).get().onlyValue()))
                    .flatMap(item ->
                        context.modify(AxiomTypeDefinition.SPACE, AxiomTypeDefinition.identifier((AxiomName) item.value()))
                        .unsatisfied(() -> action.error("Target %s not found.",item.value())
                    ));
            Dependency<AxiomItem<AxiomItemDefinition>> itemDef = action.require(context.child(Item.ITEM_DEFINITION, AxiomItemDefinition.class));
            action.apply(ext -> {
                for(AxiomValue<AxiomItemDefinition> item : itemDef.get().values()) {
                    // FIXME: Add inheritance metadata
                    targetRef.get().mergeItem(AxiomItem.from(Item.ITEM_DEFINITION, item));
                }
            });
        }
    };

    private final Set<AxiomName> items;
    private final Set<AxiomName> types;


    private BasicStatementRule(Set<AxiomName> items, Set<AxiomName> types) {
        this.items = ImmutableSet.copyOf(items);
        this.types = ImmutableSet.copyOf(types);
    }

    static AxiomValueIdentifier keyFrom(Map<AxiomName,Dependency<AxiomValue<Object>>> ctx) {
        ImmutableMap.Builder<AxiomName, Object> components = ImmutableMap.builder();
        for(Entry<AxiomName, Dependency<AxiomValue<Object>>> entry : ctx.entrySet()) {
            components.put(entry.getKey(), entry.getValue().get().value());
        }
        return AxiomValueIdentifier.from(components.build());
    }

    @Override
    public boolean isApplicableTo(AxiomItemDefinition definition) {
        if (items.isEmpty() || items.contains(definition.name())) {
            if(types.isEmpty()) {
                return true;
            }
            for(AxiomName type : types) {
                if(definition.typeDefinition().isSubtypeOf(type)) {
                    return true;
                }
            }

        }
        return false;
    }

    private static ImmutableSet<AxiomName> types(AxiomTypeDefinition... types) {
        ImmutableSet.Builder<AxiomName> builder = ImmutableSet.builder();
        for (AxiomTypeDefinition item : types) {
            builder.add(item.name());
        }
        return builder.build();

    }

    private static ImmutableSet<AxiomName> items(AxiomItemDefinition... items) {
        ImmutableSet.Builder<AxiomName> builder = ImmutableSet.builder();
        for (AxiomItemDefinition item : items) {
            builder.add(item.name());
        }
        return builder.build();
    }

    private static ImmutableSet<AxiomName> all() {
        return ImmutableSet.of();
    }

    private static AxiomValueIdentifier namespaceId(String uri) {
        return AxiomValueIdentifier.of(Item.NAMESPACE.name(), uri);
    }

    public static void addFromType(AxiomStructuredValue source, AxiomValueContext<?> target, boolean inheritance) {
        Optional<AxiomItem<?>> identifiers = source.item(Item.IDENTIFIER_DEFINITION);

        if(identifiers.isPresent()) {
            target.mergeItem(identifiers.get());
        }// Copy Items

        Optional<AxiomItem<?>> itemDefs = source.item(Item.ITEM_DEFINITION);
        if(itemDefs.isPresent()) {
            AxiomItemContext<?> targetItemDef = target.childItem(Item.ITEM_DEFINITION);
            for(AxiomValue<?> itemDef : itemDefs.get().values()) {
                // FIXME: This should be checked
                AxiomName name= (AxiomName) itemDef.asComplex().get().item(Item.NAME.name()).get().onlyValue().value();
                AxiomValueIdentifier key;
                if(inheritance) {
                     // Exact match (same module)
                    key = itemKey(name.defaultNamespace());
                } else {
                    key = itemKey(name);
                }
                Optional<? extends AxiomValueContext<?>> localDef = targetItemDef.value(key);
                if(localDef.isPresent()) {
                    //
                    specializeItem(itemDef.asComplex().get(),localDef.get());


                    //localDef.get().mergeIfEmpty(itemDef.asComplex().get().item(Item.DEFAULT));
                } else {
                    targetItemDef.addCompletedValue(itemDef);
                }
            }

            //target.mergeItem(itemDefs.get());
        }
    }

    static void specializeItem(AxiomStructuredValue itemDef, AxiomValueContext<?> targetDef) {
        targetDef.childItem(Item.NAME).only().replace(itemDef.item(Item.NAME).get().onlyValue());
        targetDef.mergeCompletedIfEmpty(itemDef.item(Item.TYPE_REFERENCE));

    }

    void mergeIfMissing(AxiomValueContext<?> target, AxiomValue<?> source,
            AxiomItemDefinition typeReference) {
        // TODO Auto-generated method stub

    }

    public static AxiomValueIdentifier idFrom(AxiomItemDefinition definition, AxiomValue<?> value) {
        return AxiomValueIdentifier.from(definition.identifierDefinition().get(), value);
    }

    public static AxiomValueIdentifier itemKey(AxiomName name) {
        return AxiomValueIdentifier.of(Item.NAME.name(), name);
    }
}
