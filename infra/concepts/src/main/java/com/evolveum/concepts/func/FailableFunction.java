package com.evolveum.concepts.func;

public interface FailableFunction<I,O,E extends Exception> {

    O apply(I input) throws E;
}
