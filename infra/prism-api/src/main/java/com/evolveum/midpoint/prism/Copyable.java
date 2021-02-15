package com.evolveum.midpoint.prism;

public interface Copyable<T extends Copyable<T>> {

    T copy();

}
