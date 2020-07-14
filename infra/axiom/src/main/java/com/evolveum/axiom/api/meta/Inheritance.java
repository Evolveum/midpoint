/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.meta;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;

public interface Inheritance {

    Inheritance INHERIT = Inheritance::inheritNamespace;
    Inheritance NO_CHANGE = Inheritance::noChange;
    Inheritance NO_NAMESPACE = Inheritance::noNamespace;
    Inheritance CURRENT = NO_CHANGE;


    static AxiomName adapt(AxiomName parent, AxiomName child) {
        return CURRENT.apply(parent, child);
    }

    static AxiomName adapt(AxiomName parent, AxiomItemDefinition child) {
        return child.inherited() ? adapt(parent, child.name()) : child.name();
    }

    AxiomName apply(AxiomName parent, AxiomName child);

    static AxiomName inheritNamespace(AxiomName parent, AxiomName name) {
        return parent.localName(name.localName());
    }

    static AxiomName noNamespace(AxiomName parent, AxiomName name) {
        return name.defaultNamespace();
    }

    static AxiomName noChange(AxiomName parent, AxiomName name) {
        return name;
    }
}
