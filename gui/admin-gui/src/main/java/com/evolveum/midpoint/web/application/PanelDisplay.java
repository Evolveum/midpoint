/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Retention(RetentionPolicy.RUNTIME)
public @interface PanelDisplay {

    String label() default "";
    String icon() default "";

    int order() default 1000;

}
