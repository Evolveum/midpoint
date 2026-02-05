/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

/**
 * @author Radovan Semancik
 */
public interface MidPointValueProducer<T> {

    T getJavaValue();

}

