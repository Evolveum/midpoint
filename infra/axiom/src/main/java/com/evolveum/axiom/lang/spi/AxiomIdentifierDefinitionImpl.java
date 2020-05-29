package com.evolveum.axiom.lang.spi;

import java.util.Collection;
import java.util.Map;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ItemValueImpl;
import com.google.common.collect.ImmutableList;

public class AxiomIdentifierDefinitionImpl extends ItemValueImpl<AxiomIdentifierDefinition> implements AxiomIdentifierDefinition {

    public static final AxiomValueFactory<AxiomIdentifierDefinition,AxiomIdentifierDefinition> FACTORY = AxiomIdentifierDefinitionImpl::new ;

    private final Collection<AxiomName> components;


    public AxiomIdentifierDefinitionImpl(AxiomTypeDefinition axiomItemDefinition, AxiomIdentifierDefinition value, Map<AxiomName, AxiomItem<?>> items) {
        super(axiomItemDefinition, value, items);

        ImmutableList.Builder<AxiomName> components = ImmutableList.builder();
        for (AxiomValue<AxiomName> val : this.<AxiomName>item(Item.ID_MEMBER.name()).get().values()) {
            components.add(val.get());
        }
        this.components = components.build();
    }

    @Override
    public AxiomIdentifierDefinition get() {
        return this;
    }

    @Override
    public Collection<AxiomName> components() {
        return components;
    }

}
