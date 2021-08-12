/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.markup.html.panel.Panel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PanelInstance {

    String identifier() default "";
    Class<? extends ObjectType> applicableFor() default ObjectType.class;
    ItemStatus[] status() default {ItemStatus.ADDED, ItemStatus.NOT_CHANGED};
    Class<? extends Panel> childOf() default Panel.class;
}
