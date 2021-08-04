package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.markup.html.panel.Panel;

import javax.xml.namespace.QName;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
public @interface PanelDescription {

    String identifier() default "";
    String panelIdentifier() default "";
    Class<? extends ObjectType> applicableFor() default ObjectType.class;
    ItemStatus[] status() default {ItemStatus.ADDED, ItemStatus.NOT_CHANGED};
    Class<? extends Panel> childOf() default Panel.class;
    boolean generic() default false;


    //probably not needed
    String path() default "";

}
