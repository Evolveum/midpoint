package com.evolveum.axiom.lang.spi;

import java.util.Map;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.schema.AxiomNamedDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ItemValueImpl;

public class AbstractBaseDefinition<V> extends ItemValueImpl<V> implements AxiomNamedDefinition {

    private final AxiomName name;
    private final  String documentation;

    public AbstractBaseDefinition(AxiomTypeDefinition type, V value, Map<AxiomName, AxiomItem<?>> items) {
        super(type, value, items);
        name = (AxiomName) item(Item.NAME).get().onlyValue().get();
        documentation = item(Item.DOCUMENTATION).map(i -> i.onlyValue().get().toString()).orElse(null); //
    }

    @Override
    public AxiomName name() {
        return name;
    }

    @Override
    public String documentation() {
        return documentation;
    }

}
