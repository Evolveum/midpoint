package com.evolveum.concepts.func;

public interface FailableSupplier<O,E extends Exception> {

    O get();

    default <I> FailableFunction<I, O, E> toConstantFunction() {
        return i -> get();
    }
}
