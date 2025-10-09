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
public @interface ActionType {

    /**
     * Action identifier. It is used to merge configurations in different places.
     */
    String identifier() default "";

    /**
     * The type which the action is applicable for.
     */
    Class<? extends Containerable> applicableForType() default Containerable.class;

    /**
     * Defined display parameters for the action, such as an icon, label, display order...
     * todo rename and unify PanelDisplay?
     */
    PanelDisplay display() default @PanelDisplay(label = "");

    /**
     * If the confirmation is required before the action is executed.
     */
    boolean confirmationRequired() default false;

    /**
     * If the action should be displayed as a button.
     */
    boolean button() default false;

    /**
     * If the action can be applied to a group of objects
     */
    boolean bulkAction() default true;

    String[] parameterName() default {};

}
