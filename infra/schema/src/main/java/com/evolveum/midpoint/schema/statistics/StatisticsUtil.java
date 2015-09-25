/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.namespace.QName;

/**
 * @author Pavol Mederly
 */
public class StatisticsUtil {

    public static String getDisplayName(ShadowType shadow) {
        String objectName = PolyString.getOrig(shadow.getName());
        QName oc = shadow.getObjectClass();
        String ocName = oc != null ? oc.getLocalPart() : null;
        return objectName + " (" + shadow.getKind() + " - " + shadow.getIntent() + " - " + ocName + ")";
    }

    public static <O extends ObjectType> String getDisplayName(PrismObject<O> object) {
        if (object == null) {
            return null;
        }
        O objectable = object.asObjectable();
        if (objectable instanceof UserType) {
            return "User " + ((UserType) objectable).getFullName() + " (" + object.getName() + ")";
        } else if (objectable instanceof RoleType) {
            return "Role " + ((RoleType) objectable).getDisplayName() + " (" + object.getName() + ")";
        } else if (objectable instanceof OrgType) {
            return "Org " + ((OrgType) objectable).getDisplayName() + " (" + object.getName() + ")";
        } else if (objectable instanceof ShadowType) {
            return "Shadow " + getDisplayName((ShadowType) objectable);
        } else {
            return objectable.getClass().getSimpleName() + " " + objectable.getName();
        }
    }
}
