/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.value;

import dev.cel.common.values.CelValue;

/**
 * @author Radovan Semancik
 */
public interface MidPointValueProducer<T> {

    T getJavaValue();

}

