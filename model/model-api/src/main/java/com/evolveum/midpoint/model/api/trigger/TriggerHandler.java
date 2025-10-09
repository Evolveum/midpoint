/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
