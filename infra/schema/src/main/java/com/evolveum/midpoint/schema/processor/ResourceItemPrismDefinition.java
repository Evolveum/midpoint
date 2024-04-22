/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismItemAccessDefinition;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * "Getter" interface to "prism" part of resource attribute and association definitions.
 *
 * Supports delegation to real data store.
 */
public interface ResourceItemPrismDefinition extends PrismItemAccessDefinition, Serializable, ShortDumpable {

    ResourceItemPrismDefinition prismData();

    default int getMinOccurs() {
        return prismData().getMinOccurs();
    }

    default int getMaxOccurs() {
        return prismData().getMaxOccurs();
    }

    default boolean canRead() {
        return prismData().canRead();
    }

    default boolean canModify() {
        return prismData().canModify();
    }

    default boolean canAdd() {
        return prismData().canAdd();
    }

    /** Mutable interface to resource attribute and association definitions. */
    interface Mutable extends PrismItemAccessDefinition.Mutable {

        void setMinOccurs(int value);

        void setMaxOccurs(int value);

        interface Delegable extends ResourceItemPrismDefinition.Mutable {

            ResourceItemPrismDefinition.Mutable prismData();

            default void setMinOccurs(int value) {
                prismData().setMinOccurs(value);
            }

            default void setMaxOccurs(int value) {
                prismData().setMaxOccurs(value);
            }

            default void setCanRead(boolean val) {
                prismData().setCanRead(val);
            }

            @Override
            default void setCanModify(boolean val) {
                prismData().setCanModify(val);
            }
            @Override
            default void setCanAdd(boolean val) {
                prismData().setCanAdd(val);
            }
        }
    }
}
