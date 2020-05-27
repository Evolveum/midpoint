package com.evolveum.axiom.api.meta;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.AxiomItemDefinition;

public interface Inheritance {

    Inheritance INHERIT = Inheritance::inheritNamespace;
    Inheritance NO_CHANGE = Inheritance::noChange;
    Inheritance NO_NAMESPACE = Inheritance::noNamespace;
    Inheritance CURRENT = NO_CHANGE;


    static AxiomIdentifier adapt(AxiomIdentifier parent, AxiomIdentifier child) {
        return CURRENT.apply(parent, child);
    }

    static AxiomIdentifier adapt(AxiomIdentifier parent, AxiomItemDefinition child) {
        return child.inherited() ? adapt(parent, child.name()) : child.name();
    }

    AxiomIdentifier apply(AxiomIdentifier parent, AxiomIdentifier child);

    static AxiomIdentifier inheritNamespace(AxiomIdentifier parent, AxiomIdentifier name) {
        return parent.localName(name.localName());
    }

    static AxiomIdentifier noNamespace(AxiomIdentifier parent, AxiomIdentifier name) {
        return name.defaultNamespace();
    }

    static AxiomIdentifier noChange(AxiomIdentifier parent, AxiomIdentifier name) {
        return name;
    }
}
