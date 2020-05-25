package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.evolveum.axiom.lang.api.AxiomModel;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.reactor.Dependency.Search;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public enum BasicStatementRule implements AxiomStatementRule<AxiomIdentifier> {
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

    MOVE_ARGUMENT_VALUE(all(),all()) {
        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
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

    REGISTER_TO_IDENTIFIER_SPACE(all(),all()){

        @Override
        public boolean isApplicableTo(AxiomItemDefinition definition) {
            return !definition.typeDefinition().identifierDefinitions().isEmpty();
        }

        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
            Collection<AxiomIdentifierDefinition> idDefs = context.typeDefinition().identifierDefinitions();
            Map<AxiomIdentifierDefinition, Map<AxiomIdentifier,Dependency<AxiomItemValue<Object>>>> identReq = new HashMap<>();
            for(AxiomIdentifierDefinition idDef : idDefs) {
                Map<AxiomIdentifier,Dependency<AxiomItemValue<Object>>> components = new HashMap<>();
                for(AxiomItemDefinition cmp : idDef.components()) {
                    components.put(cmp.name(), action.require(context.child(cmp, Object.class))
                            .map(v -> v.onlyValue())
                            .unsatisfied(()-> action.error("Item '%s' is required by identifier, but not defined.", cmp.name())));
                }
                identReq.put(idDef, components);
            }
            action.apply(ctx -> {
                for (AxiomIdentifierDefinition idDef : idDefs) {
                    IdentifierSpaceKey key = keyFrom(identReq.get(idDef));
                    ctx.register(idDef.space(), idDef.scope(), key);
                }
            });
        }

    },
    EXPAND_TYPE_REFERENCE(items(Item.TYPE_REFERENCE), types(Type.TYPE_REFERENCE)) {
        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
            AxiomIdentifier type = context.originalValue();
            Dependency.Search<AxiomItemValue<?>> typeDef = action.require(context.global(AxiomTypeDefinition.IDENTIFIER_SPACE, AxiomTypeDefinition.identifier(type)));
            typeDef.notFound(() ->  action.error("type '%s' was not found.", type));
            typeDef.unsatisfied(() -> action.error("Referenced type %s is not complete.", type));
            action.apply(ctx -> {
                ctx.replace(typeDef.get());
            });
        }
    },
    EXPAND_IDENTIFIER_ITEM(items(Item.ID_MEMBER), all()) {

        @Override
        public boolean isApplicableTo(AxiomItemDefinition definition) {
            return Item.ID_MEMBER.name().equals(definition.name());
        }

        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
            AxiomIdentifier itemName = context.currentValue();
            Search<AxiomItemValue<?>> itemDef = action.require(context.namespaceValue(AxiomItemDefinition.SPACE, AxiomItemDefinition.identifier(itemName)))
                    .notFound(() -> action.error("item '%s' was not found", itemName));
            action.apply((val) -> {
                val.replace(itemDef.get());
            });
        }
    },
    MATERIALIZE_FROM_SUPERTYPE(items(Item.SUPERTYPE_REFERENCE),types(Type.TYPE_REFERENCE)) {

        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
            AxiomIdentifier type = context.originalValue();
            Dependency.Search<AxiomItemValue<?>> typeDef = action.require(context.global(AxiomTypeDefinition.IDENTIFIER_SPACE, AxiomTypeDefinition.identifier(type)));
            typeDef.notFound(() ->  action.error("type '%s' was not found.", type));
            typeDef.unsatisfied(() -> action.error("Referenced type %s is not complete.", type));
            action.apply(ctx -> {
                ctx.replace(typeDef.get());
                AxiomItemValue<?> superType = typeDef.get();
                // Copy Identifiers
                Optional<AxiomItem<?>> identifiers = superType.item(Item.IDENTIFIER_DEFINITION);
                if(identifiers.isPresent()) {
                    ctx.parentValue().mergeItem(identifiers.get());
                }// Copy Items
                Optional<AxiomItem<?>> items = superType.item(Item.ITEM_DEFINITION);
                if(items.isPresent()) {
                    ctx.parentValue().mergeItem(items.get());
                }
            });
        }

    },

    IMPORT_DEFAULT_TYPES(all(), types(Type.MODEL)) {

        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
            Dependency<NamespaceContext> req = action.require(context.namespace(Item.NAMESPACE.name(), namespaceId(AxiomModel.BUILTIN_TYPES)));
            req.unsatisfied(() -> action.error("Default types not found."));
            action.apply((ctx) -> {
                ctx.root().importIdentifierSpace(req.get());
            });
        }
    },
    EXPORT_GLOBALS_FROM_MODEL(all(), types(Type.MODEL)) {

        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
            Dependency<String> namespace = action.require(context.child(Item.NAMESPACE, String.class).map(v -> v.onlyValue().get()));
            action.apply(ctx -> {
                ctx.root().exportIdentifierSpace(namespaceId(namespace.get()));
            });
        }
    },
    IMPORT_MODEL(all(),types(Type.IMPORT_DEFINITION)) {

        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
            Dependency<NamespaceContext> namespace = action.require(context.child(Item.NAMESPACE, String.class)
                    .map(item -> item.onlyValue())
                    .flatMap(uri -> context.namespace(Item.NAMESPACE.name(), namespaceId(uri.get()))
                    .unsatisfied(() -> action.error("Namespace %s not found.", uri.get()))
                    ));
            action.apply((ctx) -> {
                ctx.root().importIdentifierSpace(namespace.get());
            });
        }
    },
    APPLY_EXTENSION(all(), types(Type.EXTENSION_DEFINITION)) {

        @Override
        public void apply(Lookup<AxiomIdentifier> context, ActionBuilder<AxiomIdentifier> action) throws AxiomSemanticException {
            Dependency<AxiomValueContext<?>> targetRef = action.require(context.child(Item.TARGET, AxiomIdentifier.class)
                    .flatMap(item ->
                        context.modify(AxiomTypeDefinition.IDENTIFIER_SPACE, AxiomTypeDefinition.identifier(item.onlyValue().get()))
                        .unsatisfied(() -> action.error("Target %s not found.",item.onlyValue().get()))
                    ));
            Dependency<AxiomItem<Object>> itemDef = action.require(context.child(Item.ITEM_DEFINITION, Object.class));
            action.apply(ext -> {
                targetRef.get().mergeItem(itemDef.get());
            });
        }
    },
    /*
     * Not needed - registration is handled by identifier statement
    REGISTER_TYPE(items(Item.TYPE_DEFINITION), types(Type.TYPE_DEFINITION)) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier typeName = action.requireValue();
            action.apply(ctx -> ctx.registerAsGlobalItem(typeName));
        }
    },
     */
    ;
/*
    ADD_SUPERTYPE(items(), types(Type.TYPE_DEFINITION)) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            Optional<AxiomIdentifier> superType = action.optionalChildValue(Item.SUPERTYPE_REFERENCE, AxiomIdentifier.class);
            if(superType.isPresent()) {
                Requirement<AxiomStatement<?>> req = action.requireGlobalItem(Item.TYPE_DEFINITION, superType.get());
                action.apply((ctx) -> {
                    //ctx.builder().add(Item.SUPERTYPE_REFERENCE, req.get());
                });
                action.errorMessage(() -> {
                    if(!req.isSatisfied()) {
                        return action.error("Supertype %s is not defined", superType.get());
                    }
                    return null;
                });
            }
        }
    };*/

    private final Set<AxiomIdentifier> items;
    private final Set<AxiomIdentifier> types;


    private BasicStatementRule(Set<AxiomIdentifier> items, Set<AxiomIdentifier> types) {
        this.items = ImmutableSet.copyOf(items);
        this.types = ImmutableSet.copyOf(types);
    }

    static IdentifierSpaceKey keyFrom(Map<AxiomIdentifier,Dependency<AxiomItemValue<Object>>> ctx) {
        ImmutableMap.Builder<AxiomIdentifier, Object> components = ImmutableMap.builder();
        for(Entry<AxiomIdentifier, Dependency<AxiomItemValue<Object>>> entry : ctx.entrySet()) {
            components.put(entry.getKey(), entry.getValue().get().get());
        }
        return IdentifierSpaceKey.from(components.build());
    }

    @Override
    public boolean isApplicableTo(AxiomItemDefinition definition) {
        return (items.isEmpty() || items.contains(definition.name()))
                && (types.isEmpty() || types.contains(definition.typeDefinition().name()));
    }

    private static ImmutableSet<AxiomIdentifier> types(AxiomTypeDefinition... types) {
        ImmutableSet.Builder<AxiomIdentifier> builder = ImmutableSet.builder();
        for (AxiomTypeDefinition item : types) {
            builder.add(item.name());
        }
        return builder.build();

    }

    private static ImmutableSet<AxiomIdentifier> items(AxiomItemDefinition... items) {
        ImmutableSet.Builder<AxiomIdentifier> builder = ImmutableSet.builder();
        for (AxiomItemDefinition item : items) {
            builder.add(item.name());
        }
        return builder.build();
    }

    private static ImmutableSet<AxiomIdentifier> all() {
        return ImmutableSet.of();
    }

    private static IdentifierSpaceKey namespaceId(String uri) {
        return IdentifierSpaceKey.of(Item.NAMESPACE.name(), uri);
    }
}
