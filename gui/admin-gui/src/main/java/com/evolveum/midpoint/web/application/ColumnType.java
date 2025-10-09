/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.prism.Containerable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnType {

    /**
     * Column name. It is used as an identifier (also to merge configurations in different places).
     */
    String identifier() default "";

    /**
     * The type which the column can be used for.
     */
    Class<? extends Containerable> applicableForType() default Containerable.class;

    /**
     * Defined display parameters for the column, such as label, display order...
     */
    PanelDisplay display() default @PanelDisplay(label = "");


}
