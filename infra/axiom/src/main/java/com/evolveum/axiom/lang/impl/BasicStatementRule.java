package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.meta.Inheritance;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomModel;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.reactor.Dependency;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


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
                    .map(v -> v.onlyValue().get()))
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
                    .map(v -> v.onlyValue().get()))
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
                IdentifierSpaceKey key = keyFrom(components);
                ctx.register(AxiomItemDefinition.VALUE_SPACE, Scope.PARENT, key);
            });
        }

    },
    EXPAND_TYPE_REFERENCE(all(), types(Type.TYPE_REFERENCE)) {
        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {

            Dependency<AxiomValueReference<?>> typeDef = action.require(context.child(Item.NAME, AxiomName.class)
                .unsatisfied(() -> action.error("type name is missing."))
                .map(v -> v.onlyValue().get())
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
            Dependency<AxiomTypeDefinition> typeDef =
                    action.require(
                            context.onlyItemValue(Item.REF_TARGET, AxiomTypeDefinition.class)
                            .map(v -> v.get()));
            typeDef.unsatisfied(() -> action.error("Supertype is not complete."));
            action.apply(superTypeValue -> {
                addFromType(typeDef.get(), superTypeValue.parentValue(), typeDef.get().name());
            });
        }
    },
    ITEMS_FROM_MIXIN(items(Item.USES),types(Type.TYPE_REFERENCE)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomTypeDefinition> typeDef =
                    action.require(
                            context.onlyItemValue(Item.REF_TARGET, AxiomTypeDefinition.class)
                            .map(v -> v.get()));
            typeDef.unsatisfied(() -> action.error("Supertype is not complete."));
            action.apply(superTypeValue -> {
                addFromType(typeDef.get(), superTypeValue.parentValue(), typeDef.get().name());
            });
        }
    },
    IMPORT_DEFAULT_TYPES(all(), types(Type.MODEL)) {
        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<NamespaceContext> req = action.require(context.namespace(Item.NAMESPACE.name(), namespaceId(AxiomModel.BUILTIN_TYPES)));
            req.unsatisfied(() -> action.error("Default types not found."));
            action.apply((ctx) -> {
                ctx.root().importIdentifierSpace(req.get());
            });
        }
    },
    EXPORT_GLOBALS_FROM_MODEL(all(), types(Type.MODEL)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<String> namespace = action.require(context.child(Item.NAMESPACE, String.class).map(v -> v.onlyValue().get()));
            action.apply(ctx -> {
                ctx.root().exportIdentifierSpace(namespaceId(namespace.get()));
            });
        }
    },
    IMPORT_MODEL(all(),types(Type.IMPORT_DEFINITION)) {

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
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

    APPLY_AUGMENTATION(all(), types(Type.AUGMENTATION_DEFINITION)) {

        @Override
        public boolean isApplicableTo(AxiomItemDefinition definition) {
            return super.isApplicableTo(definition);
        }

        @Override
        public void apply(Lookup<AxiomName> context, ActionBuilder<AxiomName> action) throws AxiomSemanticException {
            Dependency<AxiomValueContext<?>> targetRef = action.require(context.child(Item.TARGET, AxiomName.class)
                    .map(v -> v.onlyValue().item(Item.NAME).get()))
                    .flatMap(item ->
                        context.modify(AxiomTypeDefinition.SPACE, AxiomTypeDefinition.identifier((AxiomName) item.onlyValue().get()))
                        .unsatisfied(() -> action.error("Target %s not found.",item.onlyValue().get())
                    ));
            Dependency<AxiomItem<AxiomItemDefinition>> itemDef = action.require(context.child(Item.ITEM_DEFINITION, AxiomItemDefinition.class));
            action.apply(ext -> {
                for(AxiomValue<AxiomItemDefinition> item : itemDef.get().values()) {
                    targetRef.get().mergeItem(AxiomItem.from(Item.ITEM_DEFINITION, item.get().notInherited()));
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

    static IdentifierSpaceKey keyFrom(Map<AxiomName,Dependency<AxiomValue<Object>>> ctx) {
        ImmutableMap.Builder<AxiomName, Object> components = ImmutableMap.builder();
        for(Entry<AxiomName, Dependency<AxiomValue<Object>>> entry : ctx.entrySet()) {
            components.put(entry.getKey(), entry.getValue().get().get());
        }
        return IdentifierSpaceKey.from(components.build());
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

    private static IdentifierSpaceKey namespaceId(String uri) {
        return IdentifierSpaceKey.of(Item.NAMESPACE.name(), uri);
    }

    public static void addFromType(AxiomValue<?> source, AxiomValueContext<?> target, AxiomName targetName) {
        AxiomTypeDefinition superType = (AxiomTypeDefinition) source.get();
        Preconditions.checkState(!(superType instanceof AxiomBuiltIn.Type));
        // FIXME: Add namespace change if necessary
        // Copy Identifiers
        Optional<AxiomItem<?>> identifiers = superType.item(Item.IDENTIFIER_DEFINITION);
        if(identifiers.isPresent()) {
            target.mergeItem(identifiers.get());
        }// Copy Items
        Collection<AxiomItemDefinition> itemDefs = ImmutableList.copyOf(superType.itemDefinitions().values());
        for (AxiomItemDefinition item : superType.itemDefinitions().values()) {
            if(item.inherited()) {
                final AxiomName derivedName = Inheritance.adapt(targetName, item);
                item = item.derived(derivedName);
            }
            target.mergeItem(AxiomItem.from(Item.ITEM_DEFINITION, item));
        }
    }

}
