package com.evolveum.axiom.lang.spi;

import java.util.Collection;
import java.util.Map;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition;
import com.evolveum.axiom.lang.impl.ItemValueImpl;
import com.google.common.collect.ImmutableList;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.evolveum.axiom.lang.api.AxiomItemValueFactory;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;

public class AxiomIdentifierDefinitionImpl extends ItemValueImpl<AxiomIdentifierDefinition> implements AxiomIdentifierDefinition {

    public static final AxiomItemValueFactory<AxiomIdentifierDefinition,AxiomIdentifierDefinition> FACTORY = AxiomIdentifierDefinitionImpl::new ;


    private final Scope scope;
    private final Collection<AxiomItemDefinition> components;

    private final AxiomIdentifier space;

    public AxiomIdentifierDefinitionImpl(AxiomTypeDefinition axiomItemDefinition, AxiomIdentifierDefinition value, Map<AxiomIdentifier, AxiomItem<?>> items) {
        super(axiomItemDefinition, value, items);
        this.scope = AxiomIdentifierDefinition.scope(this.<AxiomIdentifier>item(Item.ID_SCOPE.name()).get().onlyValue().get().getLocalName());
        this.space = this.<AxiomIdentifier>item(Item.ID_SPACE.name()).get().onlyValue().get();

        ImmutableList.Builder<AxiomItemDefinition> components = ImmutableList.builder();
        for (AxiomItemValue<AxiomItemDefinition> val : this.<AxiomItemDefinition>item(Item.ID_MEMBER.name()).get().values()) {
            components.add(val.get());
        }
        this.components = components.build();
    }

    @Override
    public AxiomIdentifierDefinition get() {
        return this;
    }

    @Override
    public Collection<AxiomItemDefinition> components() {
        return components;
    }

    @Override
    public Scope scope() {
        return scope;
    }

    @Override
    public AxiomIdentifier space() {
        return space;
    }

}
