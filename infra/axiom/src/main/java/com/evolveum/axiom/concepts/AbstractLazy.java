/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.concepts;

public abstract class AbstractLazy<T> {

    private Object value;

    AbstractLazy(Object supplier) {
        value = supplier;
    }

    T unwrap() {
        if (value instanceof Lazy.Supplier<?>) {
            //noinspection unchecked
            value = ((Lazy.Supplier<T>) value).get();
            return unwrap();
        }
        //noinspection unchecked
        return (T) value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
