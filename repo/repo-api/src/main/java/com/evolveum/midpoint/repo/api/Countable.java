/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
