package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

class AxiomPathImpl implements AxiomPath {

    private final Collection<Component<?>> components;

    public AxiomPathImpl(Collection<Component<?>> components) {
        this.components = ImmutableList.copyOf(components);
    }

    @Override
    public Collection<Component<?>> components() {
        return components;
    }

    @Override
    public int hashCode() {
        return components().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other == this) {
            return true;
        }
        if(other instanceof AxiomPath) {
            return Objects.equals(this.components(), ((AxiomPath) other).components());
        }
        return false;
    }

}
