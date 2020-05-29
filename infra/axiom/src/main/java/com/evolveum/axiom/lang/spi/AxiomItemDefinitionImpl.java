package com.evolveum.axiom.lang.spi;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;

public class AxiomItemDefinitionImpl extends AbstractBaseDefinition<AxiomItemDefinition> implements AxiomItemDefinition {

    public static final AxiomValueFactory<AxiomItemDefinition,AxiomItemDefinition> FACTORY = AxiomItemDefinitionImpl::new ;
    private final AxiomValue<AxiomTypeDefinition> valueType;
    private final Optional<AxiomItem<String>> minOccurs;
    private Optional<AxiomIdentifierDefinition> identifierDef;

    public AxiomItemDefinitionImpl(AxiomTypeDefinition axiomItemDefinition, AxiomItemDefinition value, Map<AxiomName, AxiomItem<?>> items) {
        super(axiomItemDefinition, value, items);
        this.valueType = require(onlyValue(AxiomTypeDefinition.class,Item.TYPE_REFERENCE, Item.REF_TARGET));
        this.identifierDef = onlyValue(AxiomIdentifierDefinition.class, Item.IDENTIFIER_DEFINITION).map(v -> v.get());
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
        return valueType.get();
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

    @Override
    public Optional<AxiomIdentifierDefinition> identifierDefinition() {
        return identifierDef;
    }

}
