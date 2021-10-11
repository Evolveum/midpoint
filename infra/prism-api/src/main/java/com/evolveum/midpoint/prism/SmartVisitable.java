/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

/**
 *  Visitable element that is smart enough to avoid being visited twice during one visitation.
 *  It is needed for object graphs with cycles. Standard visitations fail with stack overflow there.
 *
 *  (Or should we call it CycleProofVisitable with CycleProofVisitation?)
 *
 *  A little bit experimental.
 */
public interface SmartVisitable<T extends SmartVisitable<T>> extends Visitable<T> {

    /**
     * @return false if we already was here
     */
    boolean accept(Visitor<T> visitor, SmartVisitation<T> visitation);
}
