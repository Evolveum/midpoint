/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.api.stream.AxiomBuilderStreamTarget;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.antlr.AntlrDecoderContext;
import com.evolveum.axiom.lang.antlr.AxiomModelStatementSource;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.lang.spi.AxiomIdentifierDefinitionImpl;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomItemDefinitionImpl;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomTypeDefinitionImpl;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.reactor.RuleReactorContext;
import com.google.common.base.Strings;

import org.jetbrains.annotations.Nullable;

public class ModelReactorContext extends
        RuleReactorContext<AxiomSemanticException, ValueContext<?>, ValueActionImpl<?>, RuleContextImpl>
        implements AxiomNameResolver {

    private static final AxiomName ROOT = AxiomName.from("root", "root");


    private static final String AXIOM_DATA_RESOURCE = "/axiom-data.axiom";
    private static final String AXIOM_MODEL_RESOURCE = "/axiom-model.axiom";
    private static final String AXIOM_TYPES_RESOURCE = "/axiom-types.axiom";

    private static final Lazy<AxiomModelStatementSource> LANGUAGE_SOURCE = source(AXIOM_MODEL_RESOURCE);
    private static final Lazy<AxiomModelStatementSource> TYPES_SOURCE = source(AXIOM_TYPES_RESOURCE);
    private static final Lazy<AxiomModelStatementSource> INFRA_SOURCE = source(AXIOM_DATA_RESOURCE);


    public static final Lazy<AxiomSchemaContext> BASE_LANGUAGE = Lazy.from(() -> {
        ModelReactorContext reactor = boostrapReactor();
        return reactor.computeSchemaContext();
    });

    public static final ModelReactorContext reactor(AxiomSchemaContext context) {
        ModelReactorContext reactorContext = new ModelReactorContext(context);
        defaults(reactorContext);
        return reactorContext;
    }

    private static Lazy<AxiomModelStatementSource> source(String axiomModelResource) {
        return Lazy.from(() -> {
            InputStream stream = AxiomBuiltIn.class.getResourceAsStream(axiomModelResource);
            try {
                return AxiomModelStatementSource.from(axiomModelResource, stream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

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

        reactorContext.loadModelFromSource(LANGUAGE_SOURCE.get());
        reactorContext.loadModelFromSource(INFRA_SOURCE.get());
        reactorContext.loadModelFromSource(TYPES_SOURCE.get());

    }

    Collection<RuleContextImpl> rules = new ArrayList<>();

    private final AxiomSchemaContext boostrapContext;
    private final Map<AxiomValueIdentifier, CompositeIdentifierSpace> exported = new HashMap<>();

    Map<Object, AxiomValueContext<?>> globalItems = new HashMap<>();

    IdentifierSpaceHolderImpl globalSpace = new IdentifierSpaceHolderImpl(Scope.GLOBAL);

    Map<AxiomName, AxiomValueFactory<?>> typeFactories = new HashMap<>();
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
        throw new AxiomSemanticException(null, messages.toString());

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

    public ModelReactorContext loadModelFromSource(AxiomModelStatementSource source) {
        SourceContext sourceCtx = new SourceContext(this, source, source.imports(), new CompositeIdentifierSpace());
        source.stream(new AxiomBuilderStreamTarget(sourceCtx), AntlrDecoderContext.BUILTIN_DECODERS);
        return this;
    }

    @Override
    public AxiomName resolveIdentifier(@Nullable String prefix, @NotNull String localName) {
        if (Strings.isNullOrEmpty(prefix)) {
            return AxiomName.axiom(localName);
        }
        return null;
    }

    public void addStatementFactory(AxiomName statementType, AxiomValueFactory<?> factory) {
        typeFactories.put(statementType, factory);
    }

    public <V> AxiomValueFactory<V> typeFactory(AxiomTypeDefinition statementType) {
        Optional<AxiomTypeDefinition> current = Optional.of(statementType);
        do {
            @SuppressWarnings("unchecked")
            AxiomValueFactory<V> maybe = (AxiomValueFactory<V>) typeFactories.get(current.get().name());
            if (maybe != null) {
                return maybe;
            }
            current = current.get().superType();
        } while (current.isPresent());
        return AxiomValueFactory.defaultFactory();
    }

    public void exportIdentifierSpace(AxiomValueIdentifier namespace, IdentifierSpaceHolder localSpace) {
        exported(namespace).add(localSpace);
    }

    private CompositeIdentifierSpace exported(AxiomValueIdentifier namespace) {
        return exported.computeIfAbsent(namespace, k -> new CompositeIdentifierSpace());
    }

    public Dependency<NamespaceContext> namespace(AxiomName name, AxiomValueIdentifier namespaceId) {
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

    public Optional<AxiomItemDefinition> rootDefinition(AxiomName statement) {
        return boostrapContext.getRoot(statement);
    }

    public void applyRuleDefinitions(ValueContext<?> valueContext) {
        addActionsFor(valueContext);
    }

    public void register(AxiomName space, Scope scope, AxiomValueIdentifier key, ValueContext<?> context) {
        globalSpace.register(space, scope, key, context);
    }

    AxiomSchemaContext bootstrapContext() {
        return boostrapContext;
    }

}
