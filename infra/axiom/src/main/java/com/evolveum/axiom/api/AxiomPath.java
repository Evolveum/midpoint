package com.evolveum.axiom.api;

import java.util.Collection;

public interface AxiomPath {

    Collection<Component<?>> components();

    /**
     *
     * Marker interface for AxiomPath Arguments
     *
     * @param <T> Specialization type
     */
    interface Component<T extends Component<T>> {

    }

    static AxiomItemName item(AxiomName name) {
        return AxiomItemName.of(name);
    }

    interface Variable extends Component<Variable> {
        AxiomName name();
    }

    interface InfraItem extends Component<InfraItem> {
        AxiomName name();
    }


    interface Item extends Component<Item> {
        AxiomName name();
    }

    interface Value extends Component<Value> {
        AxiomValueIdentifier identifier();
    }
}
