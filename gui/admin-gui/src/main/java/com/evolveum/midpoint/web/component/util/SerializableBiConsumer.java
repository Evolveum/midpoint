/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.util.function.BiConsumer;

public interface SerializableBiConsumer<T, U> extends BiConsumer<T, U>, Serializable {
}
