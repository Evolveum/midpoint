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
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.antlr.AxiomModelStatementSource;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.evolveum.axiom.lang.api.AxiomItemValueFactory;
import com.evolveum.axiom.lang.api.AxiomSchemaContext;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomIdentifierDefinitionImpl;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.AxiomItemDefinitionImpl;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomTypeDefinitionImpl;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.reactor.RuleReactorContext;
import com.google.common.base.Strings;

import org.jetbrains.annotations.Nullable;

public class ModelReactorContext extends
        RuleReactorContext<AxiomSemanticException, ValueContext<?>, ValueActionImpl<?>, RuleContextImpl>
        implements AxiomIdentifierResolver {

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
        reactorContext.addStatementFactory(Type.IDENTIFIER_DEFINITION.name(), AxiomIdentifierDefinitionImpl.FACTORY);

        reactorContext.loadModelFromSource(BASE_LANGUAGE_SOURCE.get());
        reactorContext.loadModelFromSource(BASE_TYPES_SOURCE.get());

    }

    Collection<RuleContextImpl> rules = new ArrayList<>();

    private final AxiomSchemaContext boostrapContext;
    private final Map<IdentifierSpaceKey, CompositeIdentifierSpace> exported = new HashMap<>();

    Map<Object, AxiomValueContext<?>> globalItems = new HashMap<>();

    IdentifierSpaceHolderImpl globalSpace = new IdentifierSpaceHolderImpl(Scope.GLOBAL);

    Map<AxiomIdentifier, AxiomItemValueFactory<?, ?>> typeFactories = new HashMap<>();
    List<AxiomValueContext<?>> roots = new ArrayList<>();

    public ModelReactorContext(AxiomSchemaContext boostrapContext) {
        this.boostrapContext = boostrapContext;
    }

    public AxiomSchemaContext computeSchemaContext() throws AxiomSemanticException {
        compute();
        return createSchemaContext();
    }

    @Override
    protected void failOutstanding(Collection<ValueActionImpl<?>> outstanding) throws AxiomSemanticException {
        StringBuilder messages = new StringBuilder("Can not complete models, following errors occured:\n");
        for (ValueActionImpl<?> rule : outstanding) {
            Exception exception = rule.errorMessage();
            messages.append("Rule: ").append(rule.name()).append("\n");
            if (exception != null) {
                messages.append(exception.getMessage()).append("\n");
            }
            for (Dependency<?> req : rule.dependencies()) {
                if (!req.isSatisfied() && req.errorMessage() != null) {
                    messages.append(req.errorMessage().getMessage()).append("\n");
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

    @Override
    protected void addOutstanding(ValueActionImpl<?> action) {
        super.addOutstanding(action);
    }

    public void addRules(AxiomStatementRule<?>... rules) {
        for (AxiomStatementRule<?> statementRule : rules) {
            this.rules.add(new RuleContextImpl(statementRule));
        }
    }

    public void loadModelFromSource(AxiomModelStatementSource source) {
        SourceContext sourceCtx = new SourceContext(this, source, source.imports(), new CompositeIdentifierSpace());
        source.stream(new ItemStreamContextBuilder(sourceCtx), Optional.empty());
    }

    @Override
    public AxiomIdentifier resolveIdentifier(@Nullable String prefix, @NotNull String localName) {
        if (Strings.isNullOrEmpty(prefix)) {
            return AxiomIdentifier.axiom(localName);
        }
        return null;
    }

    public void addStatementFactory(AxiomIdentifier statementType, AxiomItemValueFactory<?, ?> factory) {
        typeFactories.put(statementType, factory);
    }

    public <V> AxiomItemValueFactory<V, AxiomItemValue<V>> typeFactory(AxiomTypeDefinition statementType) {
        Optional<AxiomTypeDefinition> current = Optional.of(statementType);
        do {
            @SuppressWarnings("unchecked")
            AxiomItemValueFactory<V, ?> maybe = (AxiomItemValueFactory<V, ?>) typeFactories.get(current.get().name());
            if (maybe != null) {
                return (AxiomItemValueFactory) maybe;
            }
            current = current.get().superType();
        } while (current.isPresent());

        return ItemValueImpl.factory();
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
    protected Collection<RuleContextImpl> rulesFor(ValueContext<?> context) {
        // TODO: Add smart filters if neccessary
        return rules;
    }

    public Optional<AxiomItemDefinition> rootDefinition(AxiomIdentifier statement) {
        return boostrapContext.getRoot(statement);
    }

    public void applyRuleDefinitions(ValueContext<?> valueContext) {
        addActionsFor(valueContext);
    }

    public void register(AxiomIdentifier space, Scope scope, IdentifierSpaceKey key, ValueContext<?> context) {
        globalSpace.register(space, scope, key, context);
    }

}
