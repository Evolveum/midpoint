/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.util;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies requirements on execution of processor methods through ClockworkMedic.partialExecute
 * and related methods.
 * <p/>
 * Beware that these requirements apply to <b>all</b> methods callable via ClockworkMedic ({@link ProcessorMethod}).
 * <p/>
 * In the future we might consider declaring execution requirements directly for those methods.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Experimental
public @interface ProcessorExecution {

    /**
     * Is the focus context required? (Usually yes, but let's be explicit.)
     */
    boolean focusRequired() default false;

    /**
     * What kind of focus there should be in order for the processor methods to be executed?
     * Checked only if focusRequired = true.
     */
    Class<? extends ObjectType> focusType() default ObjectType.class;

    /**
     * Should the methods execution be skipped if the focus is going to be deleted?
     */
    boolean skipWhenFocusDeleted() default false;

    /**
     * Should the execution be skipped if the projection is to be deleted?
     * (We should perhaps make this more flexible in the future.)
     */
    boolean skipWhenProjectionDeleted() default false;
}
