package com.evolveum.axiom.api;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

abstract class NamedPathComponent {

    private final @NotNull AxiomName name;

    public NamedPathComponent(AxiomName name) {
        super();
        this.name = name;
    }

    public AxiomName name() {
        return name;
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NamedPathComponent other = (NamedPathComponent) obj;
        return Objects.equals(name(), other.name());
    }



}
