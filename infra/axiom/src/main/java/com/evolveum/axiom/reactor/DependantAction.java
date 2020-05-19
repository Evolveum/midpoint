package com.evolveum.axiom.reactor;

import java.util.Collection;

public interface DependantAction<E extends Exception> extends Action<E> {

    Collection<Depedency<?>> dependencies();

    @Override
    default boolean canApply() {
        for (Depedency<?> dependency : dependencies()) {
            if(!dependency.isSatisfied()) {
                return false;
            }
        }
        return true;
    }


}
