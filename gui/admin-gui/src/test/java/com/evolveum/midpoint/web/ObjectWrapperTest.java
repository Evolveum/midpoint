/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Field;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ObjectWrapperTest extends BaseGuiTest {

    @Test
    public void testEmptyPolyString() throws Exception {
        PrismObject<UserType> user = prismContext.parseObject(new File("./src/test/resources/wrapper/user.xml"));

        ObjectWrapper wrapper = new ObjectWrapper(null, null, user, null, ContainerStatus.MODIFYING, null);
        //simulate change on honorific prefix
        ContainerWrapper containerWrapper = null;
        for (ContainerWrapper container : wrapper.getContainers()) {
            if (container.isMain()) {
                containerWrapper = container;
                break;
            }
        }

        PropertyWrapper propertyWrapper = containerWrapper.findPropertyWrapper(UserType.F_HONORIFIC_SUFFIX);
        ValueWrapper valueWrapper = propertyWrapper.getValues().get(0);
        PolyString value = (PolyString) valueWrapper.getValue().getValue();

        Field orig = PolyString.class.getDeclaredField("orig");
        orig.setAccessible(true);
        orig.set(value, null);
        orig.setAccessible(false);

        ObjectDelta delta = wrapper.getObjectDelta();
        AssertJUnit.assertNotNull(delta);
    }
}
