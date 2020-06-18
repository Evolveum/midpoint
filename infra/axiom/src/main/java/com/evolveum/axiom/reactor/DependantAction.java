package com.evolveum.axiom.reactor;

import java.util.Collection;

public interface DependantAction<E extends Exception> extends Action<E> {

    Collection<Dependency<?>> dependencies();

    @Override
    default boolean canApply() {
        return Dependency.allSatisfied(dependencies());
    }


}
