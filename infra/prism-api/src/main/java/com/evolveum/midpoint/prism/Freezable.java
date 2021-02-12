/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

/**
 *  Something that can be made immutable.
 */
public interface Freezable {

    boolean isImmutable();

    void freeze();

    default void checkMutable() {
        if (isImmutable()) {
            throw new IllegalStateException("An attempt to modify an immutable: " + toString());
        }
    }

    default void checkImmutable() {
        if (!isImmutable()) {
            throw new IllegalStateException("Item is not immutable even if it should be: " + toString());
        }
    }

    static void freezeNullable(Freezable target) {
        if (target != null) {
            target.freeze();
        }
    }

}
