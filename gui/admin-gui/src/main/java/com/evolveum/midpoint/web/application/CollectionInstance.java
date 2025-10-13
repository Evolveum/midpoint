/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionInstance {

    String identifier() default "";

    /**
     * The type for which the panel is applicable for.
     */
    Class<? extends Containerable> applicableForType() default ObjectType.class;

    /**
     * Defined the type of the operation when the panel is visible. Default behavior is
     * that the panel is visible for both - ADD new object and MODIFY object.
     */
    OperationTypeType[] applicableForOperation() default { OperationTypeType.ADD, OperationTypeType.MODIFY };

    /**
     * Defined display parameters for the panels, such as an icon, label, display oreder...
     */
    PanelDisplay display();
}
