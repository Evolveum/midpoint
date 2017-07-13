/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectActionsExecutedEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.Date;

/**
 * @author Pavol
 */
public class ActionsExecutedObjectsTableLineDto implements Comparable<ActionsExecutedObjectsTableLineDto> {

    //public static final String F_OBJECT_TYPE = "objectType";
    public static final String F_OPERATION = "operation";
    public static final String F_CHANNEL = "channel";
    public static final String F_SUCCESS_COUNT = "successCount";
    public static final String F_LAST_SUCCESS_OBJECT = "lastSuccessObject";
    public static final String F_LAST_SUCCESS_TIMESTAMP = "lastSuccessTimestamp";
    public static final String F_FAILURE_COUNT = "failureCount";

    private static QName TYPES_ORDER[] = {
            UserType.COMPLEX_TYPE, OrgType.COMPLEX_TYPE, RoleType.COMPLEX_TYPE, ShadowType.COMPLEX_TYPE,
            ResourceType.COMPLEX_TYPE, ReportType.COMPLEX_TYPE
    };

    private ObjectActionsExecutedEntryType entry;

    public ActionsExecutedObjectsTableLineDto(ObjectActionsExecutedEntryType entry) {
        this.entry = entry;
    }

    public String getObjectTypeLocalizationKey() {
        ObjectTypes type = ObjectTypes.getObjectTypeFromTypeQName(entry.getObjectType());
        ObjectTypeGuiDescriptor descriptor = ObjectTypeGuiDescriptor.getDescriptor(type);
        if (descriptor != null) {
            return descriptor.getLocalizationKey();
        } else {
            return null;
        }
    }

    public QName getObjectType() {
        return entry.getObjectType();
    }

    public ChangeType getOperation() {
        return ChangeType.toChangeType(entry.getOperation());
    }

    public String getChannel() {
        String channel = entry.getChannel();
        if (channel == null) {
            return null;
        }
        int i = channel.indexOf('#');
        if (i < 0) {
            return channel;
        }
        return channel.substring(i + 1);
    }

    public int getSuccessCount() {
        return entry.getTotalSuccessCount();
    }

    public int getFailureCount() {
        return entry.getTotalFailureCount();
    }

    public String getLastSuccessObject() {
        if (entry.getLastSuccessObjectDisplayName() != null) {
            return entry.getLastSuccessObjectDisplayName();
        } else if (entry.getLastSuccessObjectName() != null) {
            return entry.getLastSuccessObjectName();
        } else {
            return entry.getLastSuccessObjectOid();
        }
    }

    public String getLastSuccessTimestamp() {
        return formatDate(entry.getLastSuccessTimestamp());
    }

    private String formatDate(XMLGregorianCalendar date) {
        return formatDate(XmlTypeConverter.toDate(date));
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toLocaleString();
    }

    @Override
    public int compareTo(ActionsExecutedObjectsTableLineDto o) {
        int i1 = compareObjectType(entry.getObjectType(), o.entry.getObjectType());
        if (i1 != 0) {
            return i1;
        }
        int i2 = compareOperation(entry.getOperation(), o.entry.getOperation());
        if (i2 != 0) {
            return i2;
        }
        return compareChannel(entry.getChannel(), o.entry.getChannel());
    }

    private int compareChannel(String ch1, String ch2) {
        if (ch1 == null && ch2 != null) {
            return 1;
        } else if (ch1 != null && ch2 == null) {
            return -1;
        } else if (ch1 == null && ch2 == null) {
            return 0;
        } else {
            return ch1.compareTo(ch2);
        }
    }

    private int compareOperation(ChangeTypeType op1, ChangeTypeType op2) {
        return Integer.compare(op1.ordinal(), op2.ordinal());
    }

    private int compareObjectType(QName ot1, QName ot2) {
        int i1 = objectTypeIndex(ot1);
        int i2 = objectTypeIndex(ot2);
        if (i1 < 0 && i2 < 0) {
            return ot1.getLocalPart().compareTo(ot2.getLocalPart());
        } else if (i1 < 0) {
            return 1;
        } else if (i2 < 0) {
            return -1;
        } else {
            return Integer.compare(i1, i2);
        }
    }

    private int objectTypeIndex(QName typeName) {
        for (int i = 0; i < TYPES_ORDER.length; i++) {
            if (typeName.equals(TYPES_ORDER[i])) {
                return i;
            }
        }
        return -1;
    }
}
