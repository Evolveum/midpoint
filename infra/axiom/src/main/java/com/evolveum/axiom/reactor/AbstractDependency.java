/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.reactor;

import java.util.function.Supplier;

public abstract class AbstractDependency<V> implements Dependency<V> {


    private Supplier<? extends Exception> errorMessage;

    @Override
    public Dependency<V> unsatisfied(Supplier<? extends Exception> unsatisfiedMessage) {
        errorMessage = unsatisfiedMessage;
        return this;
    }


    @Override
    public Exception errorMessage() {
        if(errorMessage != null) {
            return errorMessage.get();
        }
        return null;
    }
}
