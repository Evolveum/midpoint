package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.Containerable;
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
public @interface PanelType {

    String name() default "";
    boolean generic() default false;
    String defaultContainerPath() default "";

    Class<? extends Containerable> defaultType() default Containerable.class;

}
