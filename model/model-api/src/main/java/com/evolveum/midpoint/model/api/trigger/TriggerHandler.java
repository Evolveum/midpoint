/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.trigger;

/**
 * TODO
 */
public interface TriggerHandler {

    default boolean isIdempotent() {
        return false;
    }

}
