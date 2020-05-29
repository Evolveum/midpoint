package com.evolveum.axiom.lang.spi;

import java.util.Collection;
import java.util.Map;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ItemValueImpl;
import com.google.common.collect.ImmutableList;

public class AxiomIdentifierDefinitionImpl extends ItemValueImpl<AxiomIdentifierDefinition> implements AxiomIdentifierDefinition {

    public static final AxiomValueFactory<AxiomIdentifierDefinition,AxiomIdentifierDefinition> FACTORY = AxiomIdentifierDefinitionImpl::new ;


    private final Scope scope;
    private final Collection<AxiomItemDefinition> components;

    private final AxiomName space;

    public AxiomIdentifierDefinitionImpl(AxiomTypeDefinition axiomItemDefinition, AxiomIdentifierDefinition value, Map<AxiomName, AxiomItem<?>> items) {
        super(axiomItemDefinition, value, items);
        this.scope = AxiomIdentifierDefinition.scope(this.<AxiomName>item(Item.ID_SCOPE.name()).get().onlyValue().get().localName());
        this.space = this.<AxiomName>item(Item.ID_SPACE.name()).get().onlyValue().get();

        ImmutableList.Builder<AxiomItemDefinition> components = ImmutableList.builder();
        for (AxiomValue<AxiomItemDefinition> val : this.<AxiomItemDefinition>item(Item.ID_MEMBER.name()).get().values()) {
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
    public AxiomName space() {
        return space;
    }

}
