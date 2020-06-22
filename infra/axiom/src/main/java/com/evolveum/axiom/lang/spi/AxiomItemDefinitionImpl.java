package com.evolveum.axiom.lang.spi;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomStructuredValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;

public class AxiomItemDefinitionImpl extends AbstractBaseDefinition implements AxiomItemDefinition {

    public static final AxiomStructuredValue.Factory FACTORY = AxiomItemDefinitionImpl::new;
    private final AxiomValue<AxiomTypeDefinition> valueType;
    private final Optional<AxiomItem<String>> minOccurs;
    private Optional<AxiomIdentifierDefinition> identifierDef;
    private Optional<AxiomName> substitutionOf;
    private Optional<AxiomValue<?>> defaultValue;
    private Optional<AxiomValue<?>> constantValue;


    public AxiomItemDefinitionImpl(AxiomTypeDefinition axiomItemDefinition, Map<AxiomName, AxiomItem<?>> items, Map<AxiomName, AxiomItem<?>> infraItems) {
        super(axiomItemDefinition, items, infraItems);
        this.valueType = require(asComplex().get().onlyValue(AxiomTypeDefinition.class,Item.TYPE_REFERENCE, Item.REF_TARGET));
        this.identifierDef = asComplex().get().onlyValue(AxiomIdentifierDefinition.class, Item.IDENTIFIER_DEFINITION).map(v -> AxiomIdentifierDefinitionImpl.from(v));
        minOccurs = as(String.class,item(Item.MIN_OCCURS.name()));
        substitutionOf = as(AxiomName.class, item(Item.SUBSTITUTION_OF.name())).map(v -> v.onlyValue().value());
        defaultValue = item(DEFAULT).map(AxiomItem::onlyValue);
        constantValue = item(CONSTANT).map(AxiomItem::onlyValue);
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
    public AxiomTypeDefinition typeDefinition() {
        return AxiomTypeDefinitionImpl.from(valueType.asComplex().get());
    }

    @Override
    public boolean required() {
        return minOccurs() > 0;
    }

    @Override
    public int minOccurs() {
        return minOccurs.map(i -> Integer.parseInt(i.onlyValue().value())).orElse(0);
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

    @Override
    public Optional<AxiomName> substitutionOf() {
        return substitutionOf;
    }

    @Override
    public Optional<AxiomValue<?>> constantValue() {
        return constantValue;
    }

    @Override
    public Optional<AxiomValue<?>> defaultValue() {
        return defaultValue;
    }

    public static AxiomItemDefinition from(AxiomValue<?> value) {
        if(value instanceof AxiomItemDefinition) {
            return (AxiomItemDefinition) value;
        }
        return new AxiomItemDefinitionImpl(value.type().get(), value.asComplex().get().itemMap(), value.asComplex().get().infraItems());
    }

}
