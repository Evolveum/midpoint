/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.cli.ninja.util;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public enum ObjectType {

    CONNECTOR("connector", ConnectorType.class),
    CONNECTOR_HOST("connectorHost", ConnectorHostType.class),
    GENERIC_OBJECT("genericObject", GenericObjectType.class),
    RESOURCE("resource", ResourceType.class),
    USER("user", UserType.class),
    OBJECT_TEMPLATE("objectTemplate", ObjectTemplateType.class),
    SYSTEM_CONFIGURATION("systemConfiguration", SystemConfigurationType.class),
    TASK("task", TaskType.class),
    SHADOW("shadow", ShadowType.class),
    ROLE("role", RoleType.class),
    VALUE_POLICY("valuePolicy", ValuePolicyType.class),
    NODE("node", NodeType.class),
    ORG("org", OrgType.class),
    ABSTRACT_ROLE("abstractRole", AbstractRoleType.class),
    FOCUS_TYPE("focus", FocusType.class),
    REPORT("report", ReportType.class),
    REPORT_OUTPUT("reportOutput", ReportOutputType.class),
    SECURITY_POLICY("securityPolicy", SecurityPolicyType.class),
    OBJECT("object", com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.class);

    private String name;
    private Class<? extends com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType> type;

    ObjectType(String name, Class<? extends com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType> type) {
        this.name = name;
        this.type = type;
    }

    public String getParameterName() {
        return name;
    }

    public QName getType() {
        return ModelClientUtil.getTypeQName(type);
    }

    public static QName getType(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        for (ObjectType ot : ObjectType.values()) {
            if (ot.name.equalsIgnoreCase(name)) {
                return ot.getType();
            }
        }

        return null;
    }
}
