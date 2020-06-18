/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.reactor;

import java.util.Collection;

public interface DependantAction<E extends Exception> extends Action<E> {

    Collection<Dependency<?>> dependencies();

    @Override
    default boolean canApply() {
        return Dependency.allSatisfied(dependencies());
    }


}
