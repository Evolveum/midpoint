package com.evolveum.axiom.concepts;

public interface CheckedFunction<I,O,E extends Exception> {

    O apply(I input) throws E;
}
