package com.evolveum.axiom.lang.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.lang.antlr.AxiomModelStatementSource;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.AxiomItemDefinitionImpl;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomStatementImpl;
import com.evolveum.axiom.lang.spi.AxiomStreamTreeBuilder;
import com.evolveum.axiom.lang.spi.AxiomTypeDefinitionImpl;
import com.evolveum.axiom.lang.spi.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomStatementImpl.Factory;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.reactor.RuleReactorContext;
import com.google.common.base.Strings;

import org.jetbrains.annotations.Nullable;

public class ModelReactorContext extends RuleReactorContext<AxiomSemanticException, StatementContextImpl<?>, StatementRuleContextImpl<?>, RuleContextImpl> implements AxiomIdentifierResolver {

    private static final AxiomIdentifier ROOT = AxiomIdentifier.from("root", "root");

    private static final String AXIOM_LANG_RESOURCE = "/axiom-lang.axiom";
    private static final String AXIOM_BUILTIN_RESOURCE = "/axiom-base-types.axiom";

    private static final Lazy<AxiomModelStatementSource> BASE_LANGUAGE_SOURCE = Lazy.from(() -> {
        InputStream stream = AxiomBuiltIn.class.getResourceAsStream(AXIOM_LANG_RESOURCE);
        try {
            return AxiomModelStatementSource.from(AXIOM_LANG_RESOURCE, stream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    private static final Lazy<AxiomModelStatementSource> BASE_TYPES_SOURCE = Lazy.from(() -> {
        InputStream stream = AxiomBuiltIn.class.getResourceAsStream(AXIOM_BUILTIN_RESOURCE);
        try {
            return AxiomModelStatementSource.from(AXIOM_BUILTIN_RESOURCE, stream);
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

    Collection<RuleContextImpl> rules = new ArrayList<>();

    private final AxiomSchemaContext boostrapContext;
    private final Map<IdentifierSpaceKey, CompositeIdentifierSpace> exported = new HashMap<>();



    Map<Object, StatementContextImpl<?>> globalItems = new HashMap<>();

    IdentifierSpaceHolderImpl globalSpace = new IdentifierSpaceHolderImpl(Scope.GLOBAL);

    Map<AxiomIdentifier, Factory<?, ?>> typeFactories = new HashMap<>();
    List<StatementContextImpl<?>> roots = new ArrayList<>();

    public ModelReactorContext(AxiomSchemaContext boostrapContext) {
        this.boostrapContext = boostrapContext;
    }

    public AxiomSchemaContext computeSchemaContext() throws AxiomSemanticException {
        compute();
        return createSchemaContext();
    }

    @Override
    protected void failOutstanding(Collection<StatementRuleContextImpl<?>> outstanding) throws AxiomSemanticException {
        StringBuilder messages = new StringBuilder("Can not complete models, following errors occured:\n");
        for (StatementRuleContextImpl<?> rule : outstanding) {
            RuleErrorMessage exception = rule.errorMessage();
            if (exception != null) {
                messages.append(exception.toString()).append("\n");
            }
            for (Dependency<?> req : rule.dependencies()) {
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

    void endStatement(StatementContextImpl<?> cur, SourceLocation loc) throws AxiomSemanticException {
        if (cur instanceof StatementContextImpl) {
            StatementContextImpl<?> current = cur;
            addActionsFor(current);
        }
    }

    @Override
    protected void addOutstanding(StatementRuleContextImpl<?> action) {
        super.addOutstanding(action);
    }

    public void addRules(AxiomStatementRule<?>... rules) {
        for (AxiomStatementRule<?> statementRule : rules) {
            this.rules.add(new RuleContextImpl(statementRule));
        }
    }

    public void loadModelFromSource(AxiomModelStatementSource statementSource) {
        statementSource.stream(this, new AxiomStreamTreeBuilder(new Root()));
    }

    @Override
    public AxiomIdentifier resolveIdentifier(@Nullable String prefix, @NotNull String localName) {
        if(Strings.isNullOrEmpty(prefix)) {
            return AxiomIdentifier.axiom(localName);
        }
        return null;
    }

    private class Root implements AxiomStreamTreeBuilder.NodeBuilder {

        @Override
        public Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement) {
            return boostrapContext.getRoot(statement);
        }

        @Override
        public StatementContextImpl<?> startChildNode(AxiomIdentifier identifier, SourceLocation loc) {
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

        @Override
        public void endNode(SourceLocation loc) {
            // TODO Auto-generated method stub

        }
    }

    public void addStatementFactory(AxiomIdentifier statementType, Factory<?, ?> factory) {
        typeFactories.put(statementType, factory);
    }

    public <V> Factory<V, ?> typeFactory(AxiomTypeDefinition statementType) {
        Optional<AxiomTypeDefinition> current = Optional.of(statementType);
        do {
            @SuppressWarnings("unchecked")
            Factory<V,?> maybe = (Factory<V,?>) typeFactories.get(current.get().name());
            if (maybe != null) {
                return maybe;
            }
            current = current.get().superType();
        } while (current.isPresent());

        return AxiomStatementImpl.factory();
    }

    public void exportIdentifierSpace(IdentifierSpaceKey namespace, IdentifierSpaceHolder localSpace) {
        exported(namespace).add(localSpace);
    }

    private CompositeIdentifierSpace exported(IdentifierSpaceKey namespace) {
        return exported.computeIfAbsent(namespace, k -> new CompositeIdentifierSpace());
    }

    public Dependency<NamespaceContext> namespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId) {
        return Dependency.orNull(exported.get(namespaceId));
    }

    @Override
    protected AxiomSemanticException createException() {
        return null;
    }

    @Override
    protected Collection<RuleContextImpl> rulesFor(StatementContextImpl<?> context) {
        // TODO: Add smart filters if neccessary
        return rules;
    }


}
