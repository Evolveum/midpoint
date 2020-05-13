package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.evolveum.axiom.lang.api.stmt.SourceLocation;
import com.evolveum.axiom.lang.impl.AxiomStatementImpl.Factory;
import com.evolveum.axiom.lang.impl.StatementContextImpl.RuleContextImpl;

public class ModelReactorContext implements AxiomIdentifierResolver {


    private static final AxiomIdentifier ROOT = AxiomIdentifier.from("root","root");


    List<StatementRule<?>> rules = new ArrayList<>();

    public Map<AxiomIdentifier,AxiomItemDefinition> rootItems = new HashMap<>();

    Map<Object, AxiomStatement<?>> staticGlobalItems = new HashMap<>();
    Map<Object, StatementContextImpl<?>> globalItems = new HashMap<>();
    Map<AxiomIdentifier, Factory<?,?>> typeFactories = new HashMap<>();


    List<StatementContextImpl<?>.RuleContextImpl> outstanding = new ArrayList<>();
    List<StatementContextImpl<?>> roots = new ArrayList<>();


    public List<AxiomStatement<?>> process() throws AxiomSemanticException {
        boolean anyCompleted = false;
        do {
            anyCompleted = false;
            List<StatementContextImpl<?>.RuleContextImpl> toCheck = outstanding;
            outstanding = new ArrayList<>();

            Iterator<StatementContextImpl<?>.RuleContextImpl> iterator = toCheck.iterator();
            while(iterator.hasNext()) {
                StatementContextImpl<?>.RuleContextImpl ruleCtx = iterator.next();
                if(ruleCtx.canProcess()) {
                    ruleCtx.perform();
                    iterator.remove();
                    anyCompleted = true;
                }
            }
            // We add not finished items back to outstanding
            outstanding.addAll(toCheck);
        } while (anyCompleted);

        if(!outstanding.isEmpty()) {
            failOutstanding(outstanding);
        }


        return buildRoots();
    }

    private void failOutstanding(List<StatementContextImpl<?>.RuleContextImpl> report) {
        StringBuilder messages = new StringBuilder("Can not complete models, following errors occured:\n");
        for (RuleContextImpl rule : report) {
            RuleErrorMessage exception = rule.errorMessage();
            if(exception != null) {
                messages.append(exception.toString()).append("\n");

            }
        }
        throw new AxiomSemanticException(messages.toString());

    }

    private List<AxiomStatement<?>> buildRoots() {
        ArrayList<AxiomStatement<?>> ret = new ArrayList<>(roots.size());
        for (StatementContextImpl<?> root : roots) {
            ret.add(root.build().get());
        }
        return ret;
    }

    public void registerGlobalItem(AxiomIdentifier typeName, StatementContextImpl<?> context) {
        globalItems.put(typeName, context);
    }

    public Requirement<Supplier<AxiomStatement<?>>> requireGlobalItem(AxiomIdentifier key) {
        AxiomStatement<?> maybe = staticGlobalItems.get(key);
        if(maybe != null) {
            return Requirement.immediate(Lazy.instant(maybe));
        }

        return new Requirement<Supplier<AxiomStatement<?>>>() {

            @Override
            boolean isSatisfied() {
                StatementContextImpl<?> maybe = globalItems.get(key);
                if(maybe == null) {
                    return false;
                }
                return maybe.isCompleted();
            }

            @Override
            public Supplier<AxiomStatement<?>> get() {
                return (Supplier) globalItems.get(key).build();
            }
        };
    }

    public void addOutstanding(StatementContextImpl<?>.RuleContextImpl rule) {
            outstanding.add(rule);
    }

    void endStatement(StatementTreeBuilder cur, SourceLocation loc) throws AxiomSemanticException {
        if(cur instanceof StatementContextImpl) {
            StatementContextImpl<?> current = (StatementContextImpl<?>) cur;
            for (StatementRule statementRule : rules) {
                if(statementRule.isApplicableTo(current.definition())) {
                    current.addRule(statementRule);
                }
            }
        }
    }

    public void addRules(StatementRule<?>... rules) {
        for (StatementRule<?> statementRule : rules) {
            this.rules.add(statementRule);
        }
    }

    public void loadModelFromSource(AxiomStatementSource statementSource) {
        statementSource.stream(this, new AxiomStatementStreamBuilder(this, new Root()));
    }

    @Override
    public AxiomIdentifier resolveStatementIdentifier(@NotNull String prefix, @NotNull String localName) {
        return AxiomIdentifier.axiom(localName);
    }

    private class Root implements StatementTreeBuilder {

        @Override
        public void setValue(Object value) {
            // NOOP
        }

        @Override
        public Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement) {
            return Optional.ofNullable(rootItems.get(statement));
        }

        @Override
        public StatementTreeBuilder createChildNode(AxiomIdentifier identifier, SourceLocation loc) {
            StatementContextImpl<?> ret = new StatementContextImpl<>(ModelReactorContext.this, null, childDef(identifier).get(), loc);
            roots.add(ret);
            return ret;
        }

        @Override
        public AxiomIdentifier identifier() {
            return ROOT;
        }

        @Override
        public void setValue(Object value, SourceLocation loc) {

        }
    }

    public void addRootItemDef(AxiomItemDefinition modelDefinition) {
        rootItems.put(modelDefinition.name(), modelDefinition);
    }

    public void addStatementFactory(AxiomIdentifier statementType, Factory<?, ?> factory) {
        typeFactories.put(statementType, factory);
    }

    public <V> Factory<V,?> typeFactory(AxiomTypeDefinition statementType) {
        Optional<AxiomTypeDefinition> current = Optional.of(statementType);
        do {
            Factory maybe = typeFactories.get(current.get().name());
            if(maybe != null) {
                return maybe;
            }
            current = current.get().superType();
        } while (current.isPresent());

        return (Factory) AxiomStatementImpl.factory();
    }
}
