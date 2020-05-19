package com.evolveum.axiom.lang.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.impl.AxiomStatementImpl.Factory;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.SourceLocation;

import org.jetbrains.annotations.Nullable;

public class ModelReactorContext implements AxiomIdentifierResolver {

    private static final AxiomIdentifier ROOT = AxiomIdentifier.from("root", "root");

    private static final String AXIOM_LANG_RESOURCE = "/axiom-lang.axiom";
    private static final String AXIOM_BUILTIN_RESOURCE = "/axiom-base-types.axiom";

    private static final Lazy<AxiomStatementSource> BASE_LANGUAGE_SOURCE = Lazy.from(() -> {
        InputStream stream = AxiomBuiltIn.class.getResourceAsStream(AXIOM_LANG_RESOURCE);
        try {
            return AxiomStatementSource.from(AXIOM_LANG_RESOURCE, stream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    private static final Lazy<AxiomStatementSource> BASE_TYPES_SOURCE = Lazy.from(() -> {
        InputStream stream = AxiomBuiltIn.class.getResourceAsStream(AXIOM_BUILTIN_RESOURCE);
        try {
            return AxiomStatementSource.from(AXIOM_BUILTIN_RESOURCE, stream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    public static final Lazy<AxiomSchemaContext> BASE_LANGUAGE = Lazy.from(() -> {
        ModelReactorContext reactor = boostrapReactor();
        return reactor.computeSchemaContext();
    });

    public static final ModelReactorContext reactor(AxiomSchemaContext context) {
        ModelReactorContext reactorContext = new ModelReactorContext(context);
        defaults(reactorContext);
        return reactorContext;
    }

    public static final ModelReactorContext boostrapReactor() {
        ModelReactorContext reactorContext = new ModelReactorContext(AxiomSchemaContextImpl.boostrapContext());
        defaults(reactorContext);

        return reactorContext;
    }

    public static final ModelReactorContext defaultReactor() {
        return reactor(BASE_LANGUAGE.get());
    }

    private static void defaults(ModelReactorContext reactorContext) {
        reactorContext.addRules(BasicStatementRule.values());
        reactorContext.addStatementFactory(Type.TYPE_DEFINITION.name(), AxiomTypeDefinitionImpl.FACTORY);
        reactorContext.addStatementFactory(Type.ITEM_DEFINITION.name(), AxiomItemDefinitionImpl.FACTORY);
        reactorContext.loadModelFromSource(BASE_LANGUAGE_SOURCE.get());
        reactorContext.loadModelFromSource(BASE_TYPES_SOURCE.get());

    }

    List<StatementRule<?>> rules = new ArrayList<>();

    private final AxiomSchemaContext boostrapContext;
    private final Map<IdentifierSpaceKey, CompositeIdentifierSpace> exported = new HashMap<>();



    Map<Object, StatementContextImpl<?>> globalItems = new HashMap<>();

    IdentifierSpaceHolderImpl globalSpace = new IdentifierSpaceHolderImpl(Scope.GLOBAL);

    Map<AxiomIdentifier, Factory<?, ?>> typeFactories = new HashMap<>();
    List<StatementRuleContextImpl> outstanding = new ArrayList<>();
    List<StatementContextImpl<?>> roots = new ArrayList<>();

    public ModelReactorContext(AxiomSchemaContext boostrapContext) {
        this.boostrapContext = boostrapContext;
    }

    public AxiomSchemaContext computeSchemaContext() throws AxiomSemanticException {
        boolean anyCompleted = false;
        do {
            anyCompleted = false;
            List<StatementRuleContextImpl> toCheck = outstanding;
            outstanding = new ArrayList<>();

            Iterator<StatementRuleContextImpl> iterator = toCheck.iterator();
            while (iterator.hasNext()) {
                StatementRuleContextImpl ruleCtx = iterator.next();
                if (ruleCtx.canProcess() && ruleCtx.notFailed()) {
                    ruleCtx.perform();
                    if(ruleCtx.successful()) {
                        iterator.remove();
                        anyCompleted = true;
                    }
                }
            }
            // We add not finished items back to outstanding
            outstanding.addAll(toCheck);
        } while (anyCompleted);

        if (!outstanding.isEmpty()) {
            failOutstanding(outstanding);
        }

        return createSchemaContext();
    }

    private void failOutstanding(List<StatementRuleContextImpl> report) {
        StringBuilder messages = new StringBuilder("Can not complete models, following errors occured:\n");
        for (StatementRuleContextImpl<?> rule : report) {
            RuleErrorMessage exception = rule.errorMessage();
            if (exception != null) {
                messages.append(exception.toString()).append("\n");
            }
            for (Requirement<?> req : rule.requirements()) {
                if(!req.isSatisfied()) {
                    messages.append(req.errorMessage()).append("\n");
                }

            }
        }
        throw new AxiomSemanticException(messages.toString());

    }

    private AxiomSchemaContext createSchemaContext() {
        return new AxiomSchemaContextImpl(globalSpace.build());
    }


    public IdentifierSpaceHolderImpl space() {
        return globalSpace;
    }

    public void registerGlobal(AxiomIdentifier space, IdentifierSpaceKey key, StatementContextImpl<?> item) {

    }


    public void addOutstanding(StatementRuleContextImpl rule) {
        outstanding.add(rule);
    }

    void endStatement(StatementTreeBuilder cur, SourceLocation loc) throws AxiomSemanticException {
        if (cur instanceof StatementContextImpl) {
            StatementContextImpl<?> current = (StatementContextImpl<?>) cur;
            for (StatementRule statementRule : rules) {
                if (statementRule.isApplicableTo(current.definition())) {
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
    public AxiomIdentifier resolveIdentifier(@Nullable String prefix, @NotNull String localName) {
        return AxiomIdentifier.axiom(localName);
    }

    private class Root implements StatementTreeBuilder {

        @Override
        public void setValue(Object value) {
            // NOOP
        }

        @Override
        public Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement) {
            return boostrapContext.getRoot(statement);
        }

        @Override
        public StatementTreeBuilder createChildNode(AxiomIdentifier identifier, SourceLocation loc) {
            StatementContextImpl<?> ret = new StatementContextImpl.Root<>(ModelReactorContext.this,
                    childDef(identifier).get(), loc);
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

    public void addStatementFactory(AxiomIdentifier statementType, Factory<?, ?> factory) {
        typeFactories.put(statementType, factory);
    }

    public <V> Factory<V, ?> typeFactory(AxiomTypeDefinition statementType) {
        Optional<AxiomTypeDefinition> current = Optional.of(statementType);
        do {
            Factory maybe = typeFactories.get(current.get().name());
            if (maybe != null) {
                return maybe;
            }
            current = current.get().superType();
        } while (current.isPresent());

        return (Factory) AxiomStatementImpl.factory();
    }

    public void exportIdentifierSpace(IdentifierSpaceKey namespace, IdentifierSpaceHolder localSpace) {
        exported(namespace).add(localSpace);
    }

    private CompositeIdentifierSpace exported(IdentifierSpaceKey namespace) {
        return exported.computeIfAbsent(namespace, k -> new CompositeIdentifierSpace());
    }

    public Requirement<NamespaceContext> namespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId) {
        return Requirement.orNull(exported.get(namespaceId));
    }


}
