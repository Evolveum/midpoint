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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    public static QName getObjectType(ObjectType objectType, PrismContext prismContext) {
        if (objectType == null) {
            return null;
        }
        PrismObjectDefinition def = objectType.asPrismObject().getDefinition();
        if (def == null) {
            Class<? extends Objectable> clazz = objectType.asPrismObject().getCompileTimeClass();
            if (clazz == null) {
                return null;
            }
            def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
            if (def == null) {
                return ObjectType.COMPLEX_TYPE;
            }
        }
        return def.getTypeName();
    }

	public static boolean isEmpty(EnvironmentalPerformanceInformationType info) {
		return info == null ||
				(isEmpty(info.getProvisioningStatistics())
				&& isEmpty(info.getMappingsStatistics())
				&& isEmpty(info.getNotificationsStatistics())
				&& info.getLastMessage() == null
				&& info.getLastMessageTimestamp() == null);
	}

	public static boolean isEmpty(NotificationsStatisticsType notificationsStatistics) {
		return notificationsStatistics == null || notificationsStatistics.getEntry().isEmpty();
	}

	public static boolean isEmpty(MappingsStatisticsType mappingsStatistics) {
		return mappingsStatistics == null || mappingsStatistics.getEntry().isEmpty();
	}

	public static boolean isEmpty(ProvisioningStatisticsType provisioningStatistics) {
		return provisioningStatistics == null || provisioningStatistics.getEntry().isEmpty();
	}


}
