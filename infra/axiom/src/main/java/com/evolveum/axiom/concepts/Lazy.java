/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.concepts;

public class Lazy<T> implements java.util.function.Supplier<T> {

    private static final Lazy NULL = Lazy.instant(null);
    private Object value;

    private Lazy(Object supplier) {
        value = supplier;
    }

    public static final <T> Lazy<T> from(Supplier<? extends T> supplier) {
        return new Lazy<>(supplier);
    }


    public static <T> Lazy<T> instant(T value) {
        return new Lazy<T>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Lazy<T> nullValue() {
        return NULL;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        if(value instanceof Supplier<?>) {
            value = ((Supplier<?>) value).get();
        }
        return (T) value;
    }

    public interface Supplier<T> extends java.util.function.Supplier<T> {

    }

}
