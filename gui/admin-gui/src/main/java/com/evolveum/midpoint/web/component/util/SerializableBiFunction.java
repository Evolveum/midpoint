/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface SerializableBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {
}
