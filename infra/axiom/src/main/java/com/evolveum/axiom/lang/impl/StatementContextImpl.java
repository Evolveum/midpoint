package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.concepts.Optionals;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.evolveum.axiom.lang.api.stmt.SourceLocation;
import com.evolveum.axiom.lang.impl.AxiomStatementImpl.Factory;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;

public class StatementContextImpl<V> implements StatementContext<V>, StatementTreeBuilder {

    private final ModelReactorContext reactor;
    private final AxiomItemDefinition definition;
    private final StatementContextImpl<?> parent;

    private final List<StatementContext<?>> childrenList = new ArrayList<>();
    private final Multimap<AxiomIdentifier, StatementContext<?>> children = HashMultimap.create();
    private final List<RuleContextImpl> rules = new ArrayList<>();

    private V value;

    private AxiomStatementBuilder<V> builder;
    private SourceLocation startLocation;
    private SourceLocation endLocation;
    private SourceLocation valueLocation;


    StatementContextImpl(ModelReactorContext reactor, StatementContextImpl<?> parent, AxiomItemDefinition definition, SourceLocation loc) {
        this.parent = parent;
        this.reactor = reactor;
        this.definition = definition;
        this.startLocation = loc;
        this.builder = new AxiomStatementBuilder<>(definition.name(), reactor.typeFactory(definition.type()));
    }

    @Override
    public void setValue(Object value) {
        this.value = (V) value;
        this.builder.setValue((V) value);
    }

    boolean isChildAllowed(AxiomIdentifier child) {
        return definition.type().item(child).isPresent();
    }

    public <V> StatementContextImpl<V> createChild(AxiomIdentifier child, SourceLocation loc) {
        StatementContextImpl<V> childCtx = new StatementContextImpl<V>(reactor, this, childDef(child).get(), loc);
        childrenList.add(childCtx);
        children.put(child, childCtx);
        builder.add(child, childCtx.build());
        return childCtx;
    }

    @Override
    public StatementTreeBuilder createChildNode(AxiomIdentifier identifier, SourceLocation loc) {
        return createChild(identifier, loc);
    }


    Supplier<AxiomStatement<V>> build() {
        return Lazy.from(builder);
    }

    @Override
    public String toString() {
        return "Context(" + definition.name() + ")";
    }

    @Override
    public Optional<AxiomItemDefinition> childDef(AxiomIdentifier child) {
        return definition.type().item(child);
    }

    @Override
    public AxiomIdentifier identifier() {
        return definition.name();
    }

    @Override
    public V requireValue() throws AxiomSemanticException {
        return AxiomSemanticException.checkNotNull(value, definition, "must have argument specified.");
    }

    @Override
    public AxiomItemDefinition definition() {
        return definition;
    }

    @Override
    public AxiomStatementBuilder<V> builder() {
        return builder;
    }

    @Override
    public void registerAsGlobalItem(AxiomIdentifier typeName) throws AxiomSemanticException {
        reactor.registerGlobalItem(typeName, this);
    }

    public boolean isCompleted() {
        for (RuleContextImpl rule : rules) {
            if(!rule.isApplied()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <V> StatementContext<V> createEffectiveChild(AxiomIdentifier axiomIdentifier, V value) {
        StatementContextImpl<V> child = createChild(axiomIdentifier, null);
        child.setValue(value);
        return child;
    }

    class RuleContextImpl implements StatementRuleContext<V> {

        private final StatementRule<V> rule;
        private final List<Requirement<?>> requirements = new ArrayList<>();
        private Action<V> action;
        private Supplier<RuleErrorMessage> errorReport = () -> null;
        private boolean applied = false;



        public RuleContextImpl(StatementRule<V> rule) {
            this.rule = rule;
        }

        public StatementRule<V> rule() {
            return rule;
        }

        @Override
        public <V> Optional<V> optionalChildValue(AxiomItemDefinition child, Class<V> type) {
            return (Optional) firstChild(child).flatMap(v -> v.optionalValue());
        }

        @Override
        public Requirement<Supplier<AxiomStatement<?>>> requireGlobalItem(AxiomItemDefinition typeDefinition,
                AxiomIdentifier axiomIdentifier) {
            return requirement(reactor.requireGlobalItem(axiomIdentifier));
        }

        private <V> Requirement<V> requirement(Requirement<V> req) {
            this.requirements.add(req);
            return req;
        }

        @Override
        public RuleContextImpl apply(Action<V> action) {
            this.action = action;
            registerRule(this);
            return this;
        }

        public boolean canProcess() {
            for (Requirement<?> requirement : requirements) {
                if(!requirement.isSatisfied()) {
                    return false;
                }
            }
            return true;
        }

        public void perform() throws AxiomSemanticException {
            this.action.apply(StatementContextImpl.this);
            this.applied = true;
        }

        @Override
        public <V> V requiredChildValue(AxiomItemDefinition supertypeReference, Class<V> type) throws AxiomSemanticException {
            return null;
        }

        @Override
        public V requireValue() throws AxiomSemanticException {
            return StatementContextImpl.this.requireValue();
        }

        public boolean isApplied() {
            return applied;
        }



        @Override
        public StatementRuleContext<V> errorMessage(Supplier<RuleErrorMessage> errorFactory) {
            this.errorReport = errorFactory;
            return this;
        }

        RuleErrorMessage errorMessage() {
            return errorReport.get();
        }

        @Override
        public String toString() {
            return StatementContextImpl.this.toString() + ":" + rule;
        }

        @Override
        public AxiomTypeDefinition typeDefinition() {
            return definition.type();
        }

        @Override
        public RuleErrorMessage error(String format, Object... arguments) {
            return RuleErrorMessage.from(startLocation, format, arguments);
        }

    }

    public void registerRule(StatementContextImpl<V>.RuleContextImpl rule) {
        this.rules.add(rule);
        this.reactor.addOutstanding(rule);
    }

    public <V> Optional<StatementContext<V>> firstChild(AxiomItemDefinition child) {
        return Optionals.first(children(child.name()));
    }

    private <V> Collection<StatementContext<V>> children(AxiomIdentifier identifier) {
        return (Collection) children.get(identifier);
    }

    public void addRule(StatementRule<V> statementRule) throws AxiomSemanticException {
        statementRule.apply(new RuleContextImpl(statementRule));
    }

    @Override
    public Optional<V> optionalValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public void replace(Supplier<AxiomStatement<?>> supplier) {
        // TODO Auto-generated method stub

    }

    @Override
    public StatementContext<?> parent() {
        return parent;
    }

    @Override
    public void setValue(Object value, SourceLocation loc) {
        setValue(value);
        this.valueLocation = loc;
    }

}
