package com.evolveum.axiom.lang.spi;

import java.util.Map;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.StructuredValueImpl;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.schema.AxiomNamedDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;

public class AbstractBaseDefinition extends StructuredValueImpl implements AxiomNamedDefinition {

    private final AxiomName name;
    private final  String documentation;

    public AbstractBaseDefinition(AxiomTypeDefinition type, Map<AxiomName, AxiomItem<?>> items, Map<AxiomName, AxiomItem<?>> infraItems) {
        super(type, items, infraItems);
        name = (AxiomName) item(Item.NAME).get().onlyValue().value();
        documentation = item(Item.DOCUMENTATION).map(i -> i.onlyValue().value().toString()).orElse(null); //
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
