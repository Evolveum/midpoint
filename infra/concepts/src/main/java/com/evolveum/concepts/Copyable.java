/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.concepts;

public interface Copyable<T extends Copyable<T>> {


    T copy();

    interface Strategy {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        default <T> T copy(T object) {
            if (object instanceof StrategyAware) {
                return (T) ((StrategyAware<?>) object).copy(this);
            }
            if (object instanceof Copyable<?>) {
                return (T) copyCopyable((Copyable) object);
            }

            return copyUnaware(object);
        }

        <T extends Copyable<T>> T copyCopyable(T object);

        <T> T copyUnaware(T object);
    }

    interface StrategyAware<T extends StrategyAware<T>> extends Copyable<T> {

        T copy(Copyable.Strategy strategy);

    }

}
