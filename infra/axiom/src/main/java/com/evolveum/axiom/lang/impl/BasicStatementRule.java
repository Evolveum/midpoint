package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.collect.ImmutableSet;


public enum BasicStatementRule implements StatementRule<AxiomIdentifier> {

    COPY_ARGUMENT_VALUE(all(),all()) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            Optional<AxiomItemDefinition> argument = rule.typeDefinition().argument();
            if(argument.isPresent()) {
               rule.apply(ctx -> ctx.createEffectiveChild(argument.get().name(), ctx.requireValue()));
            }
        }
    },
    REGISTER_TYPE(items(Item.TYPE_DEFINITION), types(Type.TYPE_DEFINITION)) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier typeName = rule.requireValue();
            rule.apply(ctx -> ctx.registerAsGlobalItem(typeName));
        }
    },

    EXPAND_TYPE_REFERENCE(all(), types(Type.TYPE_REFERENCE)) {
        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier type = rule.requireValue();
            Requirement<AxiomStatement<?>> typeDef = rule.requireGlobalItem(Item.TYPE_DEFINITION, type);
            rule.apply(ctx -> {
                ctx.replace(typeDef);
            });
            rule.errorMessage(() ->  rule.error("type '%s' was not found.", type));
        }
    };
/*
    ADD_SUPERTYPE(items(), types(Type.TYPE_DEFINITION)) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            Optional<AxiomIdentifier> superType = rule.optionalChildValue(Item.SUPERTYPE_REFERENCE, AxiomIdentifier.class);
            if(superType.isPresent()) {
                Requirement<AxiomStatement<?>> req = rule.requireGlobalItem(Item.TYPE_DEFINITION, superType.get());
                rule.apply((ctx) -> {
                    //ctx.builder().add(Item.SUPERTYPE_REFERENCE, req.get());
                });
                rule.errorMessage(() -> {
                    if(!req.isSatisfied()) {
                        return rule.error("Supertype %s is not defined", superType.get());
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

    @Override
    public boolean isApplicableTo(AxiomItemDefinition definition) {
        return (items.isEmpty() || items.contains(definition.name()))
                && (types.isEmpty() || types.contains(definition.type().name()));
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
}
