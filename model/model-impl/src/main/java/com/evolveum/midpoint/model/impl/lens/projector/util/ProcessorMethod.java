/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.util;

import com.evolveum.midpoint.util.annotation.Experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for medic-invocable projection processor method.
 * It is used basically as a reminder that this method is called under checks
 * of ClockworkMedic. In the future we might declare execution requirements here.
 *
 * (Unfortunately, Java does not provide us with the annotation of the method referenced in
 * "component::method" way. So this is only a wish for the time being.)
 *
 * We should consider finding a better name for this annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Experimental
public @interface ProcessorMethod {
}
