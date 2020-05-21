package com.evolveum.axiom.reactor;

import java.util.Collection;

public interface Rule<C, A extends Action<?>> {

    boolean applicableTo(C context);

    Collection<A> applyTo(C context);

}
