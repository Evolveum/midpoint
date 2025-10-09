/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.util.function.BiConsumer;

public interface SerializableBiConsumer<T, U> extends BiConsumer<T, U>, Serializable {
}
