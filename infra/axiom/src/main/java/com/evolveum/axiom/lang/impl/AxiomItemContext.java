package com.evolveum.axiom.lang.impl;

public interface AxiomItemContext<T> {

    AxiomValueContext<T> addValue(T value);

    AxiomValueContext<?> parent();

    T onlyValue();

    void addOperationalValue(AxiomValueReference<T> value);

}
