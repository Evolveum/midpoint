/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/** Temporary class, to be decided what to do with this. */
public interface ShadowItemDefinition {

    default <D extends ItemDefinition<?>> D findItemDefinition(@NotNull ItemPath path, @NotNull Class<D> clazz) {
        if (path.isEmpty()) {
            argCheck(clazz.isAssignableFrom(this.getClass()),
                    "Looking for definition of class %s but found %s", clazz, this);
            //noinspection unchecked
            return (D) this;
        } else {
            return null;
        }
    }
}
