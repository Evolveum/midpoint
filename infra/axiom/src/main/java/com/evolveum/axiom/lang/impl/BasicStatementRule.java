package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.collect.ImmutableSet;

import static com.evolveum.axiom.lang.api.AxiomBuiltIn.Item.*;

public enum BasicStatementRule implements StatementRule<AxiomIdentifier> {

    COPY_ARGUMENT_VALUE(TYPE_DEFINITION.identifier()) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            Optional<AxiomIdentifier> argument = rule.optionalChildValue(ARGUMENT, AxiomIdentifier.class);
            if(argument.isPresent()) {
               rule.apply((ctx) -> {
                   ctx.createChild(argument.get(), ctx.requireValue());
               });
            }
        }
    },


    REGISTER_TYPE(TYPE_DEFINITION.identifier()) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier typeName = rule.requireValue();
            rule.apply(ctx -> ctx.registerAsGlobalItem(typeName));
        }
    },

    ADD_TYPE_TO_ITEM(TYPE_REFERENCE.identifier()) {
        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            if(Type.TYPE_REFERENCE.identifier().equals(rule.typeDefinition().identifier())) {
                AxiomIdentifier type = rule.requireValue();
                Requirement<Supplier<AxiomStatement<?>>> typeDef = rule.requireGlobalItem(TYPE_DEFINITION, type);
                rule.apply(ctx -> {
                    ctx.parent().builder().add(TYPE_DEFINITION.identifier(), typeDef.get());
                });
                rule.errorMessage(() ->  "type " + type + " was not found.");
            }

        }
    },

    ADD_SUPERTYPE(TYPE_DEFINITION.identifier()) {

        @Override
        public void apply(StatementRuleContext<AxiomIdentifier> rule) throws AxiomSemanticException {
            Optional<AxiomIdentifier> superType = rule.optionalChildValue(SUPERTYPE_REFERENCE, AxiomIdentifier.class);
            if(superType.isPresent()) {
                Requirement<Supplier<AxiomStatement<?>>> req = rule.requireGlobalItem(TYPE_DEFINITION, superType.get());
                rule.apply((ctx) -> {
                    ctx.builder().add(SUPERTYPE_REFERENCE.identifier(), req.get());
                });
                rule.errorMessage(() -> {
                    if(!req.isSatisfied()) {
                        return "Supertype " + superType.get() + " is not defined";
                    }
                    return null;
                });
            }
        }
    };

    private final Set<AxiomIdentifier> applicable;

    private BasicStatementRule(AxiomIdentifier... applicable) {
        this.applicable = ImmutableSet.copyOf(applicable);
    }

    @Override
    public boolean isApplicableTo(AxiomItemDefinition definition) {
        return applicable.contains(definition.identifier());
    }

}
