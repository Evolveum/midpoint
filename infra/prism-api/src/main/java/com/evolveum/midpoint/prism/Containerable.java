/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public interface Containerable extends Serializable, DebugDumpable {

    static <T extends Containerable> PrismContainerValue<T> asPrismContainerValue(Containerable containerable) {
        //noinspection unchecked
        return containerable != null ? containerable.asPrismContainerValue() : null;
    }

    static boolean equivalent(Containerable c1, Containerable c2) {
        if (c1 == null && c2 == null) {
            return true;
        } else if (c1 == null || c2 == null) {
            return false;
        } else {
            //noinspection unchecked
            return c1.asPrismContainerValue().equivalent(c2.asPrismContainerValue());
        }
    }

    PrismContainerValue asPrismContainerValue();

    /**
     * Creates a clone but with ID removed.
     *
     * TODO Make this something like copyForReuse, using cloneComplex(CloneStrategy.REUSE).
     *  Unfortunately, this currently fails because of the required conversion of Containerable -> PCV -> Containerable.
     *  So for now we use a minimalistic version here.
     */
    @Experimental
    default <C extends Containerable> C cloneWithoutId() {
        //noinspection unchecked
        C clone = (C) CloneUtil.clone(this);
        clone.asPrismContainerValue().setId(null);
        return clone;
    }

    /**
     * Setup value to the containerable representation. This is used to after (empty) containerable is created to
     * initialize it with a correct prism container value.
     * Note: This method DOES NOT change the container value parent.
     */
    void setupContainerValue(PrismContainerValue container);

    @Override
    default String debugDump(int indent) {
        return asPrismContainerValue().debugDump(indent);
    }
}
