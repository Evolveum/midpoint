package com.evolveum.axiom.api;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

public class AxiomItemName extends AxiomName implements AxiomPath.Item {

    private static final Interner<AxiomItemName> INTERNER = Interners.newWeakInterner();

    AxiomItemName(AxiomName name) {
        super(name.namespace(), name.localName());
    }

    AxiomItemName(String namespace, String localName) {
        super(namespace, localName);
    }

    @Override
    public AxiomName name() {
        return this;
    }

    public static AxiomItemName of(AxiomName name) {
        if(name instanceof AxiomItemName) {
            return (AxiomItemName) name;
        }
        return INTERNER.intern(new AxiomItemName(name));
    }

}
