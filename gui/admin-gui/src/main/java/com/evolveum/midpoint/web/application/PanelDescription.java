package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.namespace.QName;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PanelDescription {

    String identifier() default "";

    Class<? extends ObjectType> applicableFor() default ObjectType.class;

    String label() default "";
    String icon() default "";

}
