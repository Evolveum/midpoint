/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

/**
 *  Keeps the state of the visitation in order to avoid visiting one object multiple times.
 */
public interface SmartVisitation<T extends SmartVisitable<T>> {

    boolean alreadyVisited(T visitable);

    void registerVisit(T visitable);
}
