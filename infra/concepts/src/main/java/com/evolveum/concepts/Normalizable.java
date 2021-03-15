/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.concepts;

import com.google.common.base.Objects;

/**
 * Normalizable is object, which can have normalized form.
 *
 * @param <T>
 */
public interface Normalizable<N> {

    N normalized();

    static <N> boolean equals(Normalizable<N> self, Normalizable<N> other) {
        if (self == null && other == null) {
            return true;
        }
        if (self == null || other == null) {
            return false;
        }
        if (!self.getClass().equals(other.getClass())) {
            return false;
        }
        return Objects.equal(self.normalized(), other.normalized());
    }
}
