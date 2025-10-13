/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync.reactions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * URI or URIs pointing to the given action.
 *
 * Used to find action implementation for given URI.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionUris {
    String[] value();
}
