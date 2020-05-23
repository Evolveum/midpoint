package com.evolveum.axiom.lang.spi;

import java.util.Map;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomNamedDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.impl.ItemValueImpl;

public class AbstractBaseDefinition<V> extends ItemValueImpl<V> implements AxiomNamedDefinition {

    private final AxiomIdentifier name;
    private final  String documentation;

    public AbstractBaseDefinition(AxiomTypeDefinition type, V value, Map<AxiomIdentifier, AxiomItem<?>> items) {
        super(type, value, items);
        name = (AxiomIdentifier) item(Item.NAME).get().onlyValue().get();
        documentation = item(Item.DOCUMENTATION).map(i -> i.onlyValue().get().toString()).orElse(null); //
    }

    @Override
    public AxiomIdentifier name() {
        return name;
    }

    @Override
    public String documentation() {
        return documentation;
    }

}
