package com.evolveum.axiom.concepts;

public interface Path<P> {

    Iterable<? extends P> components();
}
