/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.path.ItemPathImpl;

import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

import javax.xml.namespace.QName;

@Repeatable(PanelInstances.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface PanelInstance {

    /**
     * Panel instance identifier. It is used to merge configurations in different places.
     */
    String identifier() default "";

    /**
     * The type for which the panel is applicable for.
     */
    Class<? extends Containerable> applicableForType() default Containerable.class;

    /**
     * Defined the type of the operation when the panel is visible. Default behavior is
     * that the panel is visible for both - ADD new object and MODIFY object.
     */
    OperationTypeType[] applicableForOperation() default { OperationTypeType.ADD, OperationTypeType.MODIFY };

    /**
     * Defined where in the hierarchy of the details menu will be displayed link to the panel.
     * If nothing is defined (thus default Panel.class is used), the link will be displayed
     * on top level of details menu.
     */
    Class<? extends Panel> childOf() default Panel.class;

    /**
     * Defined if the panel should be default. It means, when opening object details page,
     * such panel will be displayed
     */
    boolean defaultPanel() default false;

    //probably should be removed
    Class<? extends Containerable>[] excludeTypes() default {};

    /**
     * Defined display parameters for the panels, such as an icon, label, display oreder...
     */
    PanelDisplay display();

    String containerPath() default "";

    String[] hiddenContainers() default {};

    String type() default "";

    boolean expanded() default false;
}
