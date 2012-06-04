/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * @author lazyman
 */
public final class DtoUtils {

    private DtoUtils() {
    }

    public static <T extends ObjectType> String getName(PrismObject<T> object) {
        String name = null;
        PrismProperty nameProperty = object.findProperty(ObjectType.F_NAME);
        if (nameProperty != null) {
            name = (String) nameProperty.getRealValue();
        }

        return name;
    }
}
