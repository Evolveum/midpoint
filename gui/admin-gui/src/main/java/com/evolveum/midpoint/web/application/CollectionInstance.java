/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
