/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.concepts;

public abstract class LazyDelegate<T> extends AbstractLazy<T> {

    public LazyDelegate(Lazy.Supplier<T> supplier) {
        super(supplier);
    }

    protected T delegate() {
        return unwrap();
    }
}
