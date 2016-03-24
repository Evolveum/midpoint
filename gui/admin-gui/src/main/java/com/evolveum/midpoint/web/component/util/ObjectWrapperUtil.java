package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectWrapperUtil {

    public static <O extends ObjectType> ObjectWrapper createObjectWrapper(String displayName, String description, PrismObject<O> object, ContainerStatus status, PageBase pageBase) {
        return createObjectWrapper(displayName, description, object, status, false, pageBase);
    }

    public static <O extends ObjectType> ObjectWrapper createObjectWrapper(String displayName, String description, PrismObject<O> object, ContainerStatus status, boolean delayContainerCreation, PageBase pageBase) {
        ObjectWrapperFactory owf = new ObjectWrapperFactory(pageBase);
        return owf.createObjectWrapper(displayName, description, object, status, delayContainerCreation);
    }
}
