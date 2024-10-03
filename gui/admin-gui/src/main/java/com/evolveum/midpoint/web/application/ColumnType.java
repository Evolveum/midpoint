/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
