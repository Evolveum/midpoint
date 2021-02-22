package com.evolveum.concepts.func;

public interface FailableConsumer<I, E extends Exception> {

    void accept(I input) throws E;

}
