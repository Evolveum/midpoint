/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectWrapperUtil {

    public static <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName, String description,
            PrismObject<O> object, ContainerStatus status, PageBase pageBase) {
        return createObjectWrapper(displayName, description, object, status, false, pageBase);
    }

    public static <O extends ObjectType> ObjectWrapper<O> createObjectWrapper(String displayName, String description,
			PrismObject<O> object, ContainerStatus status, boolean delayContainerCreation, PageBase pageBase) {
        ObjectWrapperFactory owf = new ObjectWrapperFactory(pageBase);
        return owf.createObjectWrapper(displayName, description, object, status, delayContainerCreation,
                AuthorizationPhaseType.REQUEST);
    }
}
