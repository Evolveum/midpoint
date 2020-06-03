package com.evolveum.axiom.lang.spi;

import java.util.Collection;
import java.util.Map;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomComplexValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ItemValueImpl;
import com.google.common.collect.ImmutableList;

public class AxiomIdentifierDefinitionImpl extends ItemValueImpl implements AxiomIdentifierDefinition {

    public static final AxiomValueFactory<Collection<AxiomItem<?>>,AxiomComplexValue> FACTORY = AxiomIdentifierDefinitionImpl::new ;

    private final Collection<AxiomName> components;


    public AxiomIdentifierDefinitionImpl(AxiomTypeDefinition axiomItemDefinition, Collection<AxiomItem<?>> value, Map<AxiomName, AxiomItem<?>> items) {
        super(axiomItemDefinition, value, items);

        ImmutableList.Builder<AxiomName> components = ImmutableList.builder();
        for (AxiomValue<AxiomName> val : this.<AxiomName>item(Item.ID_MEMBER.name()).get().values()) {
            components.add(val.value());
        }
        this.components = components.build();
    }

    @Override
    public Collection<AxiomName> components() {
        return components;
    }

    public static AxiomIdentifierDefinition from(AxiomValue<?> value) {
        if (value instanceof AxiomIdentifierDefinition) {
            return (AxiomIdentifierDefinition) value;
        }
        return new AxiomIdentifierDefinitionImpl(value.type().get(), null, value.asComplex().get().itemMap());
    }

}
