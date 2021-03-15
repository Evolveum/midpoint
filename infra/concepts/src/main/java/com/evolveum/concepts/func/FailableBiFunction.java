package com.evolveum.concepts.func;

public interface FailableBiFunction<T,U,O,E extends Exception> {

    O apply(T first, U second) throws E;
}
