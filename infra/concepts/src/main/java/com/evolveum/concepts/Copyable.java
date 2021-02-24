/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.concepts;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Copyable<T extends Copyable<T>> {


    @NotNull T copy();

    interface Strategy {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Nullable default <T> T copy(T object) {
            if (object instanceof StrategyAware) {
                return (T) ((StrategyAware<?>) object).copy(this);
            }
            if (object instanceof Copyable<?>) {
                return (T) copyCopyable((Copyable) object);
            }

            return copyUnaware(object);
        }

        @Nullable <T extends Copyable<T>> T copyCopyable(T object);

        @Nullable <T> T copyUnaware(T object);
    }

    interface StrategyAware<T extends StrategyAware<T>> extends Copyable<T> {

        @NotNull T copy(Copyable.Strategy strategy);

    }

}
