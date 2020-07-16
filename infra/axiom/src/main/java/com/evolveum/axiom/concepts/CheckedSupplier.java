package com.evolveum.axiom.concepts;

public interface CheckedSupplier<O,E extends Exception> {

    O get() throws E;
}
