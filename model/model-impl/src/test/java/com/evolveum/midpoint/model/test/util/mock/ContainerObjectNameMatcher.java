/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.test.util.mock;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 *
 * @author lazyman
 */
public class ContainerObjectNameMatcher extends BaseMatcher<ObjectContainerType> {

    private String name;

    public ContainerObjectNameMatcher(String name) {
        this.name = name;
    }

    @Override
    public boolean matches(Object item) {
        ObjectContainerType cont = (ObjectContainerType) item;
        ObjectType object = cont.getObject();
        System.out.println(object.getName());

        return object.getName().equals(name);
    }

    @Override
    public void describeTo(Description description) {
    }
}
