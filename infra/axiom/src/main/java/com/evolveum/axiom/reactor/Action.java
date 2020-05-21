package com.evolveum.axiom.reactor;

import java.util.Optional;

public interface Action<E extends Exception> {

    void apply();

    boolean successful();

    /**
     * Returns true if action can be applied.
     *
     * Return false if action application of action failed with exception, which is non-retriable.
     *
     * @return
     */
    boolean canApply();

    /**
     *
     * @param e Exception which occured during call of {@link #apply()}
     * @throws E If action specific exception if failed critically and all computation should be stopped.
     */
    void fail(Exception e) throws E;

    Optional<E> error();

}
