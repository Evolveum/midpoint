package com.evolveum.axiom.lang.spi;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;

public class AxiomItemDefinitionImpl extends AbstractBaseDefinition<AxiomItemDefinition> implements AxiomItemDefinition {

    public static final AxiomValueFactory<AxiomItemDefinition,AxiomItemDefinition> FACTORY = AxiomItemDefinitionImpl::new ;
    private final AxiomTypeDefinition valueType;
    private final Optional<AxiomItem<String>> minOccurs;

    public AxiomItemDefinitionImpl(AxiomTypeDefinition axiomItemDefinition, AxiomItemDefinition value, Map<AxiomIdentifier, AxiomItem<?>> items) {
        super(axiomItemDefinition, value, items);
        this.valueType = this.<AxiomTypeDefinition>item(Item.TYPE_REFERENCE.name()).get().onlyValue().get();
        minOccurs = this.<String>item(Item.MIN_OCCURS.name());
    }

    @Override
    public AxiomTypeDefinition definingType() {
        return null;
    }

    @Override
    public boolean operational() {
        return false;
    }

    @Override
    public AxiomItemDefinition get() {
        return this;
    }

    @Override
    public AxiomTypeDefinition typeDefinition() {
        return valueType;
    }

    @Override
    public boolean required() {
        return minOccurs() > 0;
    }

    @Override
    public int minOccurs() {
        return minOccurs.map(i -> Integer.parseInt(i.onlyValue().get())).orElse(0);
    }

    @Override
    public int maxOccurs() {
        // FIXME: Return real value
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return AxiomItemDefinition.toString(this);
    }

}
