/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * TODO better place, better name
 */
@Experimental
public interface Countable {

    String getIdentifier();

    void setCount(int value);
}
