/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.concepts;

public abstract class AbstractLazy<T> {

    private volatile Object value;

    AbstractLazy(Object supplier) {
        value = supplier;
    }

    T unwrap() {
        Object val = this.value;
        if (val instanceof Lazy.Supplier<?>) {
            //noinspection unchecked
            this.value = ((Lazy.Supplier<T>) val).get();
            return unwrap();
        }
        //noinspection unchecked
        return (T) val;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
