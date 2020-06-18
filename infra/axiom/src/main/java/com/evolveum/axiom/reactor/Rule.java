/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.reactor;

import java.util.Collection;

public interface Rule<C, A extends Action<?>> {

    boolean applicableTo(C context);

    Collection<A> applyTo(C context);

}
