package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public enum BasicStatementRule implements StatementRule<AxiomIdentifier> {

    REQUIRE_REQUIRED_ITEMS(all(),all()) {
        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomTypeDefinition typeDef = rule.typeDefinition();
            for(AxiomItemDefinition required : typeDef.requiredItems()) {
                rule.requireChild(required).unsatisfiedMessage(() -> rule.error("%s does not have required statement %s"));
                rule.apply((ctx) -> {});
            }
        }
    },

    COPY_ARGUMENT_VALUE(all(),all()) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            Optional<AxiomItemDefinition> argument = rule.typeDefinition().argument();
            if(argument.isPresent() && rule.optionalValue().isPresent()) {
               rule.apply(ctx -> ctx.createEffectiveChild(argument.get().name(), ctx.requireValue()));
            }
        }
    },
    REGISTER_TO_IDENTIFIER_SPACE(all(),all()) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            Collection<AxiomIdentifierDefinition> idDefs = rule.typeDefinition().identifiers();
            if(!idDefs.isEmpty()) {
                rule.apply(ctx -> {
                    for (AxiomIdentifierDefinition idDef : idDefs) {
                        IdentifierSpaceKey key = keyFrom(idDef, rule);
                        ctx.register(idDef.space(), idDef.scope(), key);
                    }

                });
            }
        }
    },
    /*
     * Not needed - registration is handled by identifier statement
    REGISTER_TYPE(items(Item.TYPE_DEFINITION), types(Type.TYPE_DEFINITION)) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier typeName = rule.requireValue();
            rule.apply(ctx -> ctx.registerAsGlobalItem(typeName));
        }
    },
     */
    EXPAND_TYPE_REFERENCE(all(), types(Type.TYPE_REFERENCE)) {
        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier type = rule.requireValue();
            Requirement<AxiomStatement<?>> typeDef = rule.requireGlobalItem(AxiomTypeDefinition.IDENTIFIER_SPACE, AxiomTypeDefinition.identifier(type));
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

    static IdentifierSpaceKey keyFrom(AxiomIdentifierDefinition idDef, StatementRuleContext<AxiomIdentifier> ctx) {
        ImmutableMap.Builder<AxiomIdentifier, Object> components = ImmutableMap.builder();
        for(AxiomItemDefinition cmp : idDef.components()) {
            components.put(cmp.name(), ctx.requiredChildValue(cmp, Object.class));
        }
        return IdentifierSpaceKey.from(components.build());
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
