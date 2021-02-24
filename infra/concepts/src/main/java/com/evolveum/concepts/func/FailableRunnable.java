package com.evolveum.concepts.func;

public interface FailableRunnable<E extends Exception> {

    void run() throws E;
}
